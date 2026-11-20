package com.dinalamin.lifeblood.ui.main

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dinalamin.lifeblood.R
import com.dinalamin.lifeblood.databinding.ActivityMainBinding
import com.dinalamin.lifeblood.ui.util.Motion
import com.dinalamin.lifeblood.ui.util.tick

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentItemId = R.id.nav_home

    /** Left-to-right order of the tabs, used to pick the slide direction. */
    private val tabOrder = listOf(R.id.nav_home, R.id.nav_search, R.id.nav_request, R.id.nav_feed, R.id.nav_profile)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentItemId = binding.bottomNav.selectedItemId
        // The middle slot is only a spacer for the plus button: no selection pill behind it.
        binding.bottomNav.post { binding.bottomNav.findViewById<View>(R.id.nav_request)?.background = null }
        if (savedInstanceState == null) {
            showFragment(HomeFragment(), animate = false)
            playIntro()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == currentItemId) return@setOnItemSelectedListener true
            val forward = tabOrder.indexOf(item.itemId) > tabOrder.indexOf(currentItemId)
            currentItemId = item.itemId
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_search -> SearchFragment()
                R.id.nav_request -> RequestFragment()
                R.id.nav_feed -> FeedFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> HomeFragment()
            }
            showFragment(fragment, forward = forward)
            binding.bottomNav.findViewById<View>(item.itemId)?.let { Motion.pop(it); it.tick() }
            true
        }
        binding.bottomNav.setOnItemReselectedListener { /* keep current screen */ }

        binding.fabRequest.setOnClickListener {
            it.tick()
            it.animate().rotationBy(90f).setDuration(260).start()
            binding.bottomNav.selectedItemId = R.id.nav_request
        }
        binding.headerBell.setOnClickListener {
            it.tick()
            Motion.wiggle(it)
            binding.bottomNav.selectedItemId = R.id.nav_feed
        }

    }

    /** Header slides down, nav pill rises, FAB pops in. */
    private fun playIntro() {
        val d = resources.displayMetrics.density
        binding.appHeader.translationY = -24 * d
        binding.appHeader.alpha = 0f
        binding.appHeader.animate().translationY(0f).alpha(1f).setDuration(420).start()

        binding.navCard.translationY = 70 * d
        binding.navCard.alpha = 0f
        binding.navCard.animate().translationY(0f).alpha(1f).setStartDelay(150).setDuration(450).start()

        binding.fabRequest.scaleX = 0f
        binding.fabRequest.scaleY = 0f
        binding.fabRequest.animate().scaleX(1f).scaleY(1f).setStartDelay(450).setDuration(420)
            .setInterpolator(OvershootInterpolator(2.4f)).start()
    }

    private var openMineOnce = false

    /** Jump to the Request tab with "My requests" preselected. */
    fun openMyRequests() {
        openMineOnce = true
        binding.bottomNav.selectedItemId = R.id.nav_request
    }

    fun consumeOpenMine(): Boolean = openMineOnce.also { openMineOnce = false }

    private fun showFragment(fragment: Fragment, animate: Boolean = true, forward: Boolean = true) {
        val tx = supportFragmentManager.beginTransaction()
        if (animate) {
            if (forward) tx.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
            else tx.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        tx.replace(R.id.fragmentContainer, fragment).commit()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
