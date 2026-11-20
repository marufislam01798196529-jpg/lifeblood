package com.dinalamin.lifeblood.ui.splash

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dinalamin.lifeblood.R
import com.dinalamin.lifeblood.data.FirebaseRepo
import com.dinalamin.lifeblood.databinding.ActivitySplashBinding
import com.dinalamin.lifeblood.ui.auth.AuthActivity
import com.dinalamin.lifeblood.ui.main.MainActivity
import com.dinalamin.lifeblood.ui.util.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var ringAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        playIntro()
        routeUser()
    }

    private fun playIntro() = with(binding) {
        splashLogo.alpha = 0f
        splashLogo.scaleX = 0.6f
        splashLogo.scaleY = 0.6f
        splashLogo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(650)
            .setInterpolator(OvershootInterpolator(1.4f))
            .start()

        listOf(splashTitle to 280L, splashTagline to 420L).forEach { (view, delayMs) ->
            view.alpha = 0f
            view.translationY = dp(16)
            view.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(delayMs)
                .setDuration(450)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        splashProgress.alpha = 0f
        splashProgress.animate().alpha(1f).setStartDelay(600).setDuration(400).start()

        ringAnimator = ObjectAnimator.ofPropertyValuesHolder(
            splashRing,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.55f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.55f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 0f)
        ).apply {
            duration = 1500
            startDelay = 500
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun routeUser() {
        lifecycleScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            val target = resolveTarget()
            val remaining = MIN_SPLASH_MS - (SystemClock.elapsedRealtime() - startedAt)
            if (remaining > 0) delay(remaining)
            startActivity(Intent(this@SplashActivity, target))
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
    }

    private suspend fun resolveTarget(): Class<out Activity> {
        if (FirebaseRepo.auth.currentUser == null) return AuthActivity::class.java
        return try {
            val user = FirebaseRepo.fetchCurrentUser()
            when {
                user == null -> {
                    // Account record is gone: sign out so the user can start over.
                    FirebaseRepo.logout()
                    AuthActivity::class.java
                }
                else -> MainActivity::class.java
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Likely offline: keep the session instead of signing the user out.
            MainActivity::class.java
        }
    }

    override fun onDestroy() {
        ringAnimator?.cancel()
        super.onDestroy()
    }

    private companion object {
        const val MIN_SPLASH_MS = 1400L
    }
}
