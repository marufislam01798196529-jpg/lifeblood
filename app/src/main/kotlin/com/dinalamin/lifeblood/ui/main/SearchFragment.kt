package com.dinalamin.lifeblood.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dinalamin.lifeblood.R
import com.dinalamin.lifeblood.adapter.DonorAdapter
import com.dinalamin.lifeblood.adapter.SkeletonAdapter
import com.dinalamin.lifeblood.data.Districts
import com.dinalamin.lifeblood.data.FirebaseRepo
import com.dinalamin.lifeblood.databinding.FragmentSearchBinding
import com.dinalamin.lifeblood.model.User
import com.dinalamin.lifeblood.ui.util.bindContent
import com.dinalamin.lifeblood.ui.util.safeCall
import com.dinalamin.lifeblood.ui.util.setItems
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DonorAdapter
    private var searchJob: Job? = null
    private var loaded: List<User> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val allBlood = getString(R.string.all_blood_groups)
        val allDistricts = getString(R.string.all_districts)
        binding.filterBlood.setItems(listOf(allBlood) + Districts.bloodGroups, selected = allBlood)
        binding.filterDistrict.setItems(listOf(allDistricts) + Districts.all, selected = allDistricts)

        adapter = DonorAdapter(
            emptyList(),
            onCall = { donor -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${donor.phone}"))) },
            onClick = { donor -> DonorDetailSheet.newInstance(donor).show(parentFragmentManager, "donor_detail") }
        )
        binding.donorsList.layoutManager = LinearLayoutManager(requireContext())
        binding.donorsList.adapter = adapter

        // Blood group and district are server-side filters; the text box filters what was loaded.
        binding.filterBlood.setOnItemClickListener { _, _, _, _ -> fetch() }
        binding.filterDistrict.setOnItemClickListener { _, _, _, _ -> fetch() }
        binding.searchQuery.doAfterTextChanged {
            binding.searchClear.visibility = if (it.isNullOrEmpty()) View.GONE else View.VISIBLE
            render()
        }
        binding.searchClear.setOnClickListener { binding.searchQuery.setText("") }

        fetch()
    }

    private fun fetch() {
        val allBlood = getString(R.string.all_blood_groups)
        val allDistricts = getString(R.string.all_districts)
        val blood = binding.filterBlood.text?.toString()?.takeIf { it.isNotBlank() && it != allBlood }
        val district = binding.filterDistrict.text?.toString()?.takeIf { it.isNotBlank() && it != allDistricts }

        searchJob?.cancel()
        binding.resultsSummary.setText(R.string.search_searching)
        binding.availablePill.visibility = View.INVISIBLE
        binding.searchLoading.visibility = View.INVISIBLE
        binding.searchEmpty.root.visibility = View.GONE
        binding.donorsList.adapter = SkeletonAdapter(4)
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            val results = safeCall { FirebaseRepo.searchDonors(blood, district) }
            binding.donorsList.adapter = adapter

            if (results == null) {
                loaded = emptyList()
                adapter.update(emptyList())
                binding.resultsSummary.setText(R.string.error_load)
                binding.searchEmpty.bindContent(R.drawable.ic_alert, R.string.error_generic, R.string.error_load)
                binding.searchEmpty.root.visibility = View.VISIBLE
                return@launch
            }
            loaded = results.sortedByDescending { it.available }
            render()
        }
    }

    private fun render() {
        if (_binding == null) return
        val query = binding.searchQuery.text?.toString()?.trim().orEmpty()
        val shown = if (query.isEmpty()) loaded else loaded.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.phone.contains(query) ||
                it.district.contains(query, ignoreCase = true)
        }
        adapter.update(shown)

        val availableCount = shown.count { it.available }
        binding.resultsSummary.text = getString(R.string.search_summary, shown.size)
        binding.availableCount.text = getString(R.string.search_available_now, availableCount)
        binding.availablePill.visibility = if (shown.isEmpty()) View.INVISIBLE else View.VISIBLE

        binding.searchEmpty.bindContent(R.drawable.ic_search, R.string.empty_donors_title, R.string.empty_donors_body)
        binding.searchEmpty.root.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
