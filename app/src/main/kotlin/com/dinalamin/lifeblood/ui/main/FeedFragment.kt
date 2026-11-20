package com.dinalamin.lifeblood.ui.main

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
import com.dinalamin.lifeblood.databinding.FragmentFeedBinding
import com.dinalamin.lifeblood.model.BloodRequest
import com.dinalamin.lifeblood.ui.util.bindContent
import com.dinalamin.lifeblood.data.Districts
import com.dinalamin.lifeblood.ui.util.safeCall
import com.dinalamin.lifeblood.ui.util.selectedLabel
import com.dinalamin.lifeblood.ui.util.setBloodOptions
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RequestAdapter
    private var allRequests: List<BloodRequest> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RequestAdapter(items = emptyList(), onClick = { req ->
            RequestDetailSheet.newInstance(req).show(parentFragmentManager, "request_detail")
        })
        binding.feedList.layoutManager = LinearLayoutManager(requireContext())
        binding.feedList.adapter = adapter

        binding.swipeRefresh.setColorSchemeResources(R.color.lb_primary)
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.lb_surface)
        binding.swipeRefresh.setOnChildScrollUpCallback { _, _ -> binding.feedList.canScrollVertically(-1) }
        binding.swipeRefresh.setOnRefreshListener { loadFeed() }
        val all = getString(R.string.feed_filter_all)
        binding.feedFilter.setBloodOptions(
            listOf(all, getString(R.string.emergency)) + Districts.bloodGroups, selected = all
        )
        binding.feedFilter.setOnCheckedStateChangeListener { _, _ -> applyFilter() }

        loadFeed()
    }

    private fun loadFeed() {
        val firstLoad = allRequests.isEmpty()
        binding.swipeRefresh.isRefreshing = !firstLoad
        if (firstLoad) {
            binding.feedEmpty.root.visibility = View.GONE
            binding.feedList.adapter = SkeletonAdapter(4)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val requests = safeCall { FirebaseRepo.fetchActiveRequests() }
            binding.swipeRefresh.isRefreshing = false
            binding.feedList.adapter = adapter
            if (requests == null) {
                allRequests = emptyList()
                binding.feedEmpty.bindContent(R.drawable.ic_alert, R.string.error_generic, R.string.error_load)
            } else {
                allRequests = requests
                binding.feedEmpty.bindContent(R.drawable.ic_inbox, R.string.empty_requests_title, R.string.empty_requests_body)
            }
            applyFilter()
        }
    }

    private fun applyFilter() {
        val selected = binding.feedFilter.selectedLabel()
        val shown = when (selected) {
            null, getString(R.string.feed_filter_all) -> allRequests
            getString(R.string.emergency) -> allRequests.filter { it.isEmergency }
            else -> allRequests.filter { it.blood == selected }
        }
        adapter.update(shown)
        binding.feedEmpty.root.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        binding.feedCount.text = resources.getQuantityString(R.plurals.active_requests, shown.size, shown.size)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
