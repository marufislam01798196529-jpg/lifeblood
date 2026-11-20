package com.dinalamin.lifeblood.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dinalamin.lifeblood.R
import com.dinalamin.lifeblood.databinding.SheetRequestDetailBinding
import com.dinalamin.lifeblood.model.BloodRequest
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** State is passed through arguments so the sheet survives rotation and process death. */
class RequestDetailSheet : BottomSheetDialogFragment() {

    private var _binding: SheetRequestDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetRequestDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val phone = args.getString(K_PHONE).orEmpty()
        val emergency = args.getBoolean(K_EMERGENCY)
        val date = args.getString(K_DATE).orEmpty()

        binding.sheetBadge.visibility = if (emergency) View.VISIBLE else View.GONE
        binding.sheetBlood.text = args.getString(K_BLOOD)
        if (emergency) {
            binding.sheetBlood.setBackgroundResource(R.drawable.bg_blood_badge_solid)
            binding.sheetBlood.setTextColor(requireContext().getColor(R.color.white))
        }
        binding.sheetPatient.text = args.getString(K_PATIENT)
        binding.sheetProblem.text = args.getString(K_PROBLEM).orEmpty().ifBlank { getString(R.string.sheet_no_details) }
        binding.sheetHospital.text = args.getString(K_HOSPITAL).orEmpty().ifBlank { "-" }
        binding.sheetLocation.text = args.getString(K_LOCATION).orEmpty().ifBlank { "-" }
        binding.sheetDate.text = date
        binding.sheetDateRow.visibility = if (date.isBlank()) View.GONE else View.VISIBLE
        binding.sheetPhone.text = phone.ifBlank { "-" }
        binding.sheetNote.text = args.getString(K_NOTE).orEmpty().ifBlank { getString(R.string.sheet_no_note) }

        binding.sheetCallBtn.isEnabled = phone.isNotBlank()
        binding.sheetCallBtn.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val K_BLOOD = "blood"
        private const val K_PATIENT = "patient"
        private const val K_PROBLEM = "problem"
        private const val K_HOSPITAL = "hospital"
        private const val K_LOCATION = "location"
        private const val K_DATE = "date"
        private const val K_PHONE = "phone"
        private const val K_NOTE = "note"
        private const val K_EMERGENCY = "emergency"

        fun newInstance(r: BloodRequest) = RequestDetailSheet().apply {
            arguments = Bundle().apply {
                putString(K_BLOOD, r.blood)
                putString(K_PATIENT, r.patient)
                putString(K_PROBLEM, r.problem)
                putString(K_HOSPITAL, r.hospital)
                putString(K_LOCATION, r.location)
                putString(K_DATE, r.date)
                putString(K_PHONE, r.phone)
                putString(K_NOTE, r.note)
                putBoolean(K_EMERGENCY, r.isEmergency)
            }
        }
    }
}
