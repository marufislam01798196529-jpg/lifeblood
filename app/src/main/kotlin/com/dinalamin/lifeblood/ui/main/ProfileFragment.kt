package com.dinalamin.lifeblood.ui.main

import android.content.Intent
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dinalamin.lifeblood.R
import com.dinalamin.lifeblood.adapter.DonationHistoryAdapter
import com.dinalamin.lifeblood.data.Districts
import com.dinalamin.lifeblood.data.FirebaseRepo
import com.dinalamin.lifeblood.databinding.DialogEditProfileBinding
import com.dinalamin.lifeblood.databinding.FragmentProfileBinding
import com.dinalamin.lifeblood.model.User
import com.dinalamin.lifeblood.ui.auth.AuthActivity
import com.dinalamin.lifeblood.ui.util.Motion
import com.dinalamin.lifeblood.ui.util.achievementFor
import com.dinalamin.lifeblood.ui.util.eligibilityFor
import com.dinalamin.lifeblood.ui.util.millisOrZero
import com.dinalamin.lifeblood.ui.util.safeCall
import com.dinalamin.lifeblood.ui.util.setItems
import com.dinalamin.lifeblood.ui.util.shareText
import com.dinalamin.lifeblood.ui.util.tick
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyAdapter: DonationHistoryAdapter
    private var user: User? = null
    private var historyLoaded = false
    private var historyExpanded = false
    private var suppressToggleCallback = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyAdapter = DonationHistoryAdapter(emptyList())
        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.adapter = historyAdapter

        binding.historyHeader.setOnClickListener { toggleHistory() }
        binding.btnLogout.setOnClickListener { confirmLogout() }
        binding.rowMyRequests.setOnClickListener { (requireActivity() as MainActivity).openMyRequests() }
        binding.rowEditProfile.setOnClickListener { showEditDialog() }
        binding.rowInvite.setOnClickListener { requireContext().shareText(getString(R.string.invite_message)) }
        binding.rowShareCard.setOnClickListener {
            user?.let {
                requireContext().shareText(getString(R.string.donor_card_message, it.blood, it.district))
            }
        }
        binding.rowSafety.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.tools_safety_title)
                .setMessage(R.string.tools_safety_body)
                .setPositiveButton(R.string.tools_close, null)
                .show()
        }
        binding.switchAvailability.setOnCheckedChangeListener { button, isChecked ->
            if (!suppressToggleCallback) {
                button.tick()
                updateAvailability(isChecked)
            }
        }

        Motion.staggerIn(binding.profileContent, startDelayMs = 100)
        loadProfile()
    }

    private fun loadProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            val u = safeCall { FirebaseRepo.fetchCurrentUser() }
            if (u == null) {
                logout(); return@launch
            }
            user = u
            bindUser(u)

            if (u.role == "donor") {
                val last = safeCall { FirebaseRepo.fetchMyDonationHistory() }
                    ?.maxOfOrNull { it.date.millisOrZero() }
                bindEligibility(last)
            }
        }
    }

    private fun bindUser(u: User) {
        val ctx = requireContext()
        val isDonor = u.role == "donor"

        binding.profileName.text = u.name
        binding.profileDistrict.text = u.district.ifBlank { "-" }
        binding.profileBloodBadge.text = u.blood.ifBlank { "--" }
        binding.profileRole.setText(if (isDonor) R.string.role_label_donor else R.string.role_label_receiver)
        binding.profileDonationsCount.text = u.donationCount.toString()
        binding.profileLives.text = (u.donationCount * 3).toString()

        // Donor-only blocks.
        binding.donorSection.visibility = if (isDonor) View.VISIBLE else View.GONE
        binding.availabilityCard.visibility = if (isDonor) View.VISIBLE else View.GONE
        binding.rowShareCard.visibility = if (isDonor) View.VISIBLE else View.GONE

        val (title, sub) = ctx.achievementFor(u.donationCount)
        binding.achievementTitle.text = title
        binding.achievementSub.text = sub

        applyStatus(if (isDonor) u.available else true)
        suppressToggleCallback = true
        binding.switchAvailability.isChecked = u.available
        suppressToggleCallback = false
    }

    private fun bindEligibility(lastMillis: Long?) {
        val e = eligibilityFor(lastMillis)
        val color = requireContext().getColor(if (e.ready) R.color.lb_success else R.color.lb_warning)
        if (e.ready) {
            binding.profileEligTitle.setText(R.string.eligibility_ready)
            binding.profileEligSub.text = if (e.daysSince == null) getString(R.string.eligibility_ready_sub_none)
            else getString(R.string.eligibility_ready_sub, e.daysSince)
        } else {
            binding.profileEligTitle.text = getString(R.string.eligibility_wait, e.daysLeft)
            binding.profileEligSub.text = getString(R.string.eligibility_wait_sub, e.daysSince ?: 0)
        }
        binding.profileEligBar.setIndicatorColor(color)
        binding.profileEligBar.setProgressCompat(e.progress, true)
    }

    private fun applyStatus(active: Boolean) {
        binding.profileStatus.setText(if (active) R.string.status_active else R.string.status_inactive)
        binding.profileStatus.setTextColor(
            requireContext().getColor(if (active) R.color.lb_success else R.color.lb_ink_subtle)
        )
    }

    private fun updateAvailability(isAvailable: Boolean) {
        applyStatus(isAvailable)
        val uid = FirebaseRepo.currentUid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = safeCall { FirebaseRepo.setAvailability(uid, isAvailable) } != null
            if (ok) {
                toast(if (isAvailable) R.string.toast_now_active else R.string.toast_now_inactive)
            } else {
                suppressToggleCallback = true
                binding.switchAvailability.isChecked = !isAvailable
                suppressToggleCallback = false
                applyStatus(!isAvailable)
                toast(R.string.error_no_network)
            }
        }
    }

    // ---------- Edit profile ----------

    private fun showEditDialog() {
        val current = user ?: return
        val form = DialogEditProfileBinding.inflate(LayoutInflater.from(requireContext()))
        form.editName.setText(current.name)
        form.editPhone.setText(current.phone)
        form.editDistrict.setItems(Districts.all, selected = current.district.takeIf { it.isNotBlank() })

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_profile_title)
            .setView(form.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val name = form.editName.text?.toString()?.trim().orEmpty()
                val phone = form.editPhone.text?.toString()?.trim().orEmpty()
                val district = form.editDistrict.text?.toString()?.trim().orEmpty()

                form.editNameLayout.error = if (name.isEmpty()) getString(R.string.error_name_required) else null
                form.editPhoneLayout.error =
                    if (phone.count { it.isDigit() } < 10) getString(R.string.error_phone_invalid) else null
                form.editDistrictLayout.error = if (district.isEmpty()) getString(R.string.error_district_required) else null
                if (name.isEmpty() || phone.count { it.isDigit() } < 10 || district.isEmpty()) return@setOnClickListener

                dialog.dismiss()
                val uid = FirebaseRepo.currentUid ?: return@setOnClickListener
                viewLifecycleOwner.lifecycleScope.launch {
                    val ok = safeCall { FirebaseRepo.updateProfile(uid, name, phone, district) } != null
                    toast(if (ok) R.string.toast_profile_saved else R.string.error_generic)
                    if (ok) loadProfile()
                }
            }
        }
        dialog.show()
    }

    // ---------- History ----------

    private fun toggleHistory() {
        historyExpanded = !historyExpanded
        TransitionManager.beginDelayedTransition(binding.root as ViewGroup, AutoTransition().apply { duration = 220 })
        binding.historyChevron.animate().rotation(if (historyExpanded) 180f else 0f).setDuration(220).start()

        if (!historyExpanded) {
            binding.historyList.visibility = View.GONE
            binding.historyEmpty.visibility = View.GONE
            return
        }
        if (historyLoaded) {
            showHistoryContent()
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                val history = safeCall { FirebaseRepo.fetchMyDonationHistory() }.orEmpty()
                historyAdapter.update(history)
                historyLoaded = true
                if (historyExpanded) showHistoryContent()
            }
        }
    }

    private fun showHistoryContent() {
        val empty = historyAdapter.itemCount == 0
        binding.historyList.visibility = if (empty) View.GONE else View.VISIBLE
        binding.historyEmpty.visibility = if (empty) View.VISIBLE else View.GONE
    }

    // ---------- Logout ----------

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_logout_confirm_title)
            .setMessage(R.string.profile_logout_confirm_body)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.profile_logout) { _, _ -> logout() }
            .show()
    }

    private fun logout() {
        FirebaseRepo.logout()
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun toast(resId: Int) = Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
