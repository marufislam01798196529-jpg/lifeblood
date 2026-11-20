package com.dinalamin.lifeblood.ui.auth

import android.content.Intent
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dinalamin.lifeblood.R
import com.dinalamin.lifeblood.data.Districts
import com.dinalamin.lifeblood.data.FirebaseRepo
import com.dinalamin.lifeblood.databinding.ActivityAuthBinding
import com.dinalamin.lifeblood.model.User
import com.dinalamin.lifeblood.ui.main.MainActivity
import com.dinalamin.lifeblood.ui.util.Motion
import com.dinalamin.lifeblood.ui.util.selectedLabel
import com.dinalamin.lifeblood.ui.util.setBloodOptions
import com.dinalamin.lifeblood.ui.util.setItems
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.regBloodGroup.setBloodOptions(Districts.bloodGroups)
        binding.regDistrict.setItems(Districts.all, selected = null)

        binding.authToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) showForm(login = checkedId == R.id.tabLogin)
        }
        binding.btnLogin.setOnClickListener { handleLogin() }
        binding.btnRegister.setOnClickListener { handleRegister() }
        binding.loginPass.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleLogin(); true
            } else false
        }
        binding.regBloodGroup.setOnCheckedStateChangeListener { _, _ ->
            binding.regBloodError.visibility = View.GONE
        }

        playIntro()
    }

    /** Header text eases in, then the sheet rises and the fields follow one by one. */
    private fun playIntro() {
        val d = resources.displayMetrics.density
        listOf(binding.authHeadline, binding.authSubtitle).forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 14 * d
            v.animate().alpha(1f).translationY(0f).setStartDelay(80L + i * 90L).setDuration(420).start()
        }
        binding.authSheet.translationY = 80 * d
        binding.authSheet.alpha = 0f
        binding.authSheet.animate().translationY(0f).alpha(1f).setStartDelay(120).setDuration(480)
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.8f)).start()
        Motion.staggerIn(binding.loginForm, startDelayMs = 380, stepMs = 70)
    }

    private fun showForm(login: Boolean) {
        TransitionManager.beginDelayedTransition(
            binding.authSheet, AutoTransition().apply { duration = 220 }
        )
        binding.loginForm.visibility = if (login) View.VISIBLE else View.GONE
        binding.registerForm.visibility = if (login) View.GONE else View.VISIBLE
        Motion.crossfadeText(
            binding.authHeadline,
            getString(if (login) R.string.auth_welcome_back else R.string.auth_create_account)
        )
        Motion.crossfadeText(
            binding.authSubtitle,
            getString(if (login) R.string.auth_welcome_back_sub else R.string.auth_create_account_sub)
        )
    }

    // ---------- Login ----------

    private fun handleLogin() {
        if (busy) return
        val email = binding.loginEmail.text?.toString()?.trim().orEmpty()
        val pass = binding.loginPass.text?.toString().orEmpty()

        binding.loginEmailLayout.error = null
        binding.loginPassLayout.error = null
        var valid = true
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.loginEmailLayout.error = getString(R.string.error_email_invalid)
            valid = false
        }
        if (pass.isEmpty()) {
            binding.loginPassLayout.error = getString(R.string.error_password_required)
            valid = false
        }
        if (!valid) return

        setBusy(true, binding.btnLogin, binding.loginLoader, R.string.auth_login)
        lifecycleScope.launch {
            try {
                FirebaseRepo.login(email, pass)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setBusy(false, binding.btnLogin, binding.loginLoader, R.string.auth_login)
                showMessage(friendlyError(e))
                return@launch
            }
            goToApp()
        }
    }

    // ---------- Register ----------

    private fun handleRegister() {
        if (busy) return
        val name = binding.regName.text?.toString()?.trim().orEmpty()
        val email = binding.regEmail.text?.toString()?.trim().orEmpty()
        val phone = binding.regPhone.text?.toString()?.trim().orEmpty()
        val pass = binding.regPass.text?.toString().orEmpty()
        val blood = binding.regBloodGroup.selectedLabel()
        val role = if (binding.regRoleGroup.checkedButtonId == R.id.regRoleReceiver) "receiver" else "donor"
        val district = binding.regDistrict.text?.toString()?.trim().orEmpty()

        binding.regNameLayout.error = null
        binding.regEmailLayout.error = null
        binding.regPhoneLayout.error = null
        binding.regPassLayout.error = null
        binding.regDistrictLayout.error = null
        binding.regBloodError.visibility = View.GONE

        var valid = true
        if (name.isEmpty()) {
            binding.regNameLayout.error = getString(R.string.error_name_required); valid = false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.regEmailLayout.error = getString(R.string.error_email_invalid); valid = false
        }
        if (phone.count { it.isDigit() } < 10) {
            binding.regPhoneLayout.error = getString(R.string.error_phone_invalid); valid = false
        }
        if (pass.length < 6) {
            binding.regPassLayout.error = getString(R.string.error_password_short); valid = false
        }
        if (blood == null) {
            binding.regBloodError.visibility = View.VISIBLE; valid = false
        }
        if (district.isEmpty()) {
            binding.regDistrictLayout.error = getString(R.string.error_district_required); valid = false
        }
        if (!valid) return

        setBusy(true, binding.btnRegister, binding.regLoader, R.string.auth_register)
        lifecycleScope.launch {
            try {
                val user = User(
                    name = name, email = email, phone = phone,
                    blood = blood.orEmpty(), role = role, district = district
                )
                FirebaseRepo.register(user, pass)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setBusy(false, binding.btnRegister, binding.regLoader, R.string.auth_register)
                showMessage(friendlyError(e))
                return@launch
            }
            goToApp()
        }
    }

    // ---------- Helpers ----------

    private suspend fun goToApp() {
        startActivity(Intent(this, MainActivity::class.java))
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    private fun setBusy(loading: Boolean, button: MaterialButton, loader: View, labelRes: Int) {
        busy = loading
        loader.visibility = if (loading) View.VISIBLE else View.GONE
        button.text = if (loading) "" else getString(labelRes)
    }

    private fun friendlyError(e: Exception): String = getString(
        when (e) {
            is FirebaseAuthInvalidCredentialsException,
            is FirebaseAuthInvalidUserException -> R.string.error_invalid_credentials
            is FirebaseAuthUserCollisionException -> R.string.error_email_taken
            is FirebaseNetworkException -> R.string.error_no_network
            else -> R.string.error_generic
        }
    )

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
