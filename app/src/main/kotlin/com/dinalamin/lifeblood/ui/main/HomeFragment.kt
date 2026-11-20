package com.dinalamin.lifeblood.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dinalamin.lifeblood.R
import com.dinalamin.lifeblood.adapter.RequestAdapter
import com.dinalamin.lifeblood.adapter.SkeletonAdapter
import com.dinalamin.lifeblood.data.FirebaseRepo
import com.dinalamin.lifeblood.databinding.FragmentHomeBinding
import com.dinalamin.lifeblood.ui.util.Motion
import com.dinalamin.lifeblood.ui.util.bindContent
import com.dinalamin.lifeblood.ui.util.eligibilityFor
import com.dinalamin.lifeblood.ui.util.greetingForNow
import com.dinalamin.lifeblood.ui.util.initialsOf
import com.dinalamin.lifeblood.ui.util.millisOrZero
import com.dinalamin.lifeblood.ui.util.safeCall
import com.dinalamin.lifeblood.ui.util.shareText
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RequestAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.greetingLabel.text = greetingForNow(requireContext())
        binding.homeEmpty.bindContent(R.drawable.ic_inbox, R.string.empty_requests_title, R.string.empty_requests_body)

        adapter = RequestAdapter(items = emptyList(), onClick = { req ->
            RequestDetailSheet.newInstance(req).show(parentFragmentManager, "request_detail")
        })
        binding.recentRequestsList.layoutManager = LinearLayoutManager(requireContext())
        binding.recentRequestsList.adapter = adapter

        listOf(
            binding.serviceFeed, binding.serviceMine, binding.serviceBenefits, binding.serviceVolunteers,
            binding.toolCompat, binding.toolEligible, binding.toolEmergency, binding.toolInvite
        ).forEach { Motion.pressable(it) }

        binding.btnHeroRequest.setOnClickListener { switchBottomTab(R.id.nav_request) }
        binding.btnHeroFind.setOnClickListener { switchBottomTab(R.id.nav_search) }
        binding.seeAll.setOnClickListener { switchBottomTab(R.id.nav_feed) }

        binding.serviceFeed.setOnClickListener { switchBottomTab(R.id.nav_feed) }
        binding.serviceMine.setOnClickListener { (requireActivity() as MainActivity).openMyRequests() }
        binding.serviceBenefits.setOnClickListener {
            showInfo(R.string.tools_benefits_title, getString(R.string.tools_benefits_body))
        }
        binding.serviceVolunteers.setOnClickListener { showVolunteers() }
        binding.toolCompat.setOnClickListener {
            showInfo(R.string.tools_compat_title, getString(R.string.tools_compat_body))
        }
        binding.toolEligible.setOnClickListener {
            showInfo(R.string.tools_eligible_title, getString(R.string.tools_eligible_body))
        }
        binding.toolEmergency.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:999")))
        }
        binding.toolInvite.setOnClickListener {
            requireContext().shareText(getString(R.string.invite_message))
        }

        Motion.staggerIn(binding.homeContent, startDelayMs = 120)
        load()
    }

    private fun load() {
        binding.homeLoading.visibility = View.GONE
        binding.recentRequestsList.adapter = SkeletonAdapter(2)
        viewLifecycleOwner.lifecycleScope.launch {
            val user = safeCall { FirebaseRepo.fetchCurrentUser() }
            if (user != null) {
                binding.greeting.text = user.name.trim().split(" ").firstOrNull().orEmpty().ifBlank { user.name }
                binding.homeAvatar.text = initialsOf(user.name).take(1)
                binding.heroBloodValue.text = user.blood.ifBlank { "--" }
                binding.eligibilityCard.visibility = if (user.role == "donor") View.VISIBLE else View.GONE
            }

            val stats = safeCall { FirebaseRepo.fetchHomeStats() }
            if (stats != null) {
                Motion.countUp(binding.homeStatActive, stats.first, 700)
                Motion.countUp(binding.homeStatDonors, stats.second, 700)
                Motion.countUp(binding.homeStatFulfilled, stats.third, 700)
            }

            val requests = safeCall { FirebaseRepo.fetchActiveRequests() }
            binding.recentRequestsList.adapter = adapter
            adapter.update(requests.orEmpty().take(5))
            binding.homeEmpty.root.visibility = if (requests.isNullOrEmpty()) View.VISIBLE else View.GONE

            if (user?.role == "donor") {
                val last = safeCall { FirebaseRepo.fetchMyDonationHistory() }
                    ?.maxOfOrNull { it.date.millisOrZero() }
                bindEligibility(last)
            }
        }
    }

    private fun bindEligibility(lastMillis: Long?) {
        val e = eligibilityFor(lastMillis)
        val ctx = requireContext()
        if (e.ready) {
            binding.eligibilityTitle.setText(R.string.eligibility_ready)
            binding.eligibilitySub.text = if (e.daysSince == null) getString(R.string.eligibility_ready_sub_none)
            else getString(R.string.eligibility_ready_sub, e.daysSince)
        } else {
            binding.eligibilityTitle.text = getString(R.string.eligibility_wait, e.daysLeft)
            binding.eligibilitySub.text = getString(R.string.eligibility_wait_sub, e.daysSince ?: 0)
        }
        val color = ctx.getColor(if (e.ready) R.color.lb_success else R.color.lb_warning)
        binding.eligibilityBar.setIndicatorColor(color)
        binding.eligibilityBar.setProgressCompat(e.progress, true)
        binding.eligibilityIcon.setColorFilter(color)
        binding.eligibilityIcon.setBackgroundResource(if (e.ready) R.drawable.bg_chip_success else R.drawable.bg_chip_warning)
    }

    private fun showVolunteers() {
        viewLifecycleOwner.lifecycleScope.launch {
            val team = safeCall { FirebaseRepo.fetchTeam() }.orEmpty()
            val body = if (team.isEmpty()) getString(R.string.tools_team_empty)
            else team.joinToString("\n\n") { "${it.name}\n${it.role}" }
            showInfo(R.string.tools_team_title, body)
        }
    }

    private fun showInfo(titleRes: Int, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setMessage(message)
            .setPositiveButton(R.string.tools_close, null)
            .show()
    }

    private fun switchBottomTab(itemId: Int) {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = itemId
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
