package com.dinalamin.lifeblood.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dinalamin.lifeblood.R
import com.dinalamin.lifeblood.adapter.RequestAdapter
import com.dinalamin.lifeblood.data.Districts
import com.dinalamin.lifeblood.data.FirebaseRepo
import com.dinalamin.lifeblood.databinding.FragmentRequestBinding
import com.dinalamin.lifeblood.model.BloodRequest
import com.dinalamin.lifeblood.ui.util.Motion
import com.dinalamin.lifeblood.ui.util.bindContent
import com.dinalamin.lifeblood.ui.util.safeCall
import com.dinalamin.lifeblood.ui.util.setItems
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class RequestFragment : Fragment() {

    private var _binding: FragmentRequestBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RequestAdapter

    private var isEditing = false
    private var editingId: String? = null
    private var busy = false

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.reqBlood.setItems(Districts.bloodGroups, selected = null)
        binding.reqLocation.setItems(Districts.all, selected = null)
        binding.myEmpty.bindContent(R.drawable.ic_file_text, R.string.empty_mine_title, R.string.empty_mine_body)

        binding.reqDate.setOnClickListener { showDatePicker() }
        binding.reqDateLayout.setEndIconOnClickListener { showDatePicker() }

        binding.requestToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) showTab(newTab = checkedId == R.id.tabNew)
        }

        adapter = RequestAdapter(
            emptyList(),
            showOwnerActions = true,
            onClick = { req -> RequestDetailSheet.newInstance(req).show(parentFragmentManager, "request_detail") },
            onEdit = { req -> prepareEdit(req) },
            onDelete = { req -> confirmDelete(req) }
        )
        binding.myRequestsList.layoutManager = LinearLayoutManager(requireContext())
        binding.myRequestsList.adapter = adapter

        binding.btnPostRequest.setOnClickListener { submitRequest() }
        binding.btnCancelEdit.setOnClickListener { cancelEdit() }
        (binding.newRequestView.getChildAt(0) as? ViewGroup)?.let { Motion.staggerIn(it, startDelayMs = 80, stepMs = 45) }

        if ((activity as? MainActivity)?.consumeOpenMine() == true) {
            binding.requestToggle.check(R.id.tabMine)
        }
    }

    // ---------- Tabs ----------

    private fun showTab(newTab: Boolean) {
        if (newTab) {
            binding.myRequestsList.visibility = View.GONE
            binding.myEmpty.root.visibility = View.GONE
            Motion.fadeIn(binding.newRequestView)
        } else {
            cancelEdit()
            binding.newRequestView.visibility = View.GONE
            loadMyRequests()
        }
    }

    private fun loadMyRequests() {
        viewLifecycleOwner.lifecycleScope.launch {
            val requests = safeCall { FirebaseRepo.fetchMyRequests() }.orEmpty()
            adapter.update(requests)
            binding.myRequestsList.visibility = if (requests.isEmpty()) View.GONE else View.VISIBLE
            binding.myEmpty.root.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
            if (requests.isNotEmpty()) Motion.fadeIn(binding.myRequestsList)
        }
    }

    // ---------- Date picker ----------

    private fun showDatePicker() {
        val existing = runCatching { dateFormat.parse(binding.reqDate.text?.toString().orEmpty())?.time }.getOrNull()
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
            .build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.select_date)
            .setCalendarConstraints(constraints)
            .setSelection(existing ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            binding.reqDate.setText(dateFormat.format(millis))
        }
        picker.show(childFragmentManager, "date_picker")
    }

    // ---------- Edit flow ----------

    private fun prepareEdit(req: BloodRequest) {
        isEditing = true
        editingId = req.id
        binding.requestToggle.check(R.id.tabNew)

        binding.reqPatient.setText(req.patient)
        binding.reqProblem.setText(req.problem)
        binding.reqHospital.setText(req.hospital)
        binding.reqDate.setText(req.date)
        binding.reqPhone.setText(req.phone)
        binding.reqNote.setText(req.note)
        binding.switchEmergency.isChecked = req.isEmergency
        binding.reqBlood.setText(req.blood, false)
        binding.reqLocation.setText(req.location, false)

        binding.editBanner.visibility = View.VISIBLE
        binding.btnPostRequest.setText(R.string.request_update)
    }

    private fun cancelEdit() {
        isEditing = false
        editingId = null
        binding.editBanner.visibility = View.GONE
        binding.btnPostRequest.setText(R.string.request_post)
        listOf(
            binding.reqPatient, binding.reqProblem, binding.reqHospital,
            binding.reqDate, binding.reqPhone, binding.reqNote
        ).forEach { it.setText("") }
        listOf(
            binding.reqPatientLayout, binding.reqProblemLayout, binding.reqHospitalLayout,
            binding.reqLocationLayout, binding.reqPhoneLayout
        ).forEach { it.error = null }
        binding.reqBloodLayout.error = null
        binding.reqBlood.setText("", false)
        binding.reqLocation.setText("", false)
        binding.switchEmergency.isChecked = false
    }

    private fun confirmDelete(req: BloodRequest) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_request_title)
            .setMessage(R.string.dialog_delete_request_body)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val ok = safeCall { FirebaseRepo.deleteRequest(req.id) } != null
                    toast(if (ok) R.string.toast_request_deleted else R.string.error_generic)
                    if (ok) loadMyRequests()
                }
            }
            .show()
    }

    // ---------- Submit ----------

    private fun submitRequest() {
        if (busy) return
        val patient = binding.reqPatient.text?.toString()?.trim().orEmpty()
        val problem = binding.reqProblem.text?.toString()?.trim().orEmpty()
        val hospital = binding.reqHospital.text?.toString()?.trim().orEmpty()
        val blood = binding.reqBlood.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val date = binding.reqDate.text?.toString()?.trim().orEmpty()
        val location = binding.reqLocation.text?.toString()?.trim().orEmpty()
        val phone = binding.reqPhone.text?.toString()?.trim().orEmpty()
        val note = binding.reqNote.text?.toString()?.trim().orEmpty()
        val isEmergency = binding.switchEmergency.isChecked

        val required = getString(R.string.error_field_required)
        binding.reqPatientLayout.error = if (patient.isEmpty()) required else null
        binding.reqProblemLayout.error = if (problem.isEmpty()) required else null
        binding.reqHospitalLayout.error = if (hospital.isEmpty()) required else null
        binding.reqLocationLayout.error = if (location.isEmpty()) required else null
        binding.reqPhoneLayout.error = if (phone.count { it.isDigit() } < 10) getString(R.string.error_phone_invalid) else null
        binding.reqBloodLayout.error = if (blood == null) required else null

        if (patient.isEmpty() || problem.isEmpty() || hospital.isEmpty() || location.isEmpty() ||
            phone.count { it.isDigit() } < 10 || blood == null
        ) return

        val request = BloodRequest(
            id = editingId ?: "",
            patient = patient, problem = problem, hospital = hospital, blood = blood,
            date = date, location = location, phone = phone, note = note, isEmergency = isEmergency
        )

        setBusy(true)
        val wasEditing = isEditing && editingId != null
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = safeCall {
                if (wasEditing) FirebaseRepo.updateRequest(request) else FirebaseRepo.postRequest(request)
            } != null
            setBusy(false)
            if (ok) {
                toast(
                    when {
                        wasEditing -> R.string.toast_request_updated
                        isEmergency -> R.string.toast_emergency_posted
                        else -> R.string.toast_request_posted
                    }
                )
                cancelEdit()
                binding.requestToggle.check(R.id.tabMine)
            } else {
                toast(R.string.error_generic)
            }
        }
    }

    private fun setBusy(loading: Boolean) {
        busy = loading
        binding.reqLoader.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnPostRequest.text = if (loading) "" else getString(
            if (isEditing) R.string.request_update else R.string.request_post
        )
    }

    private fun toast(resId: Int) = Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
