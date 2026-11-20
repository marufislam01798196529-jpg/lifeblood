package com.dinalamin.lifeblood.ui.main

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dinalamin.lifeblood.R
import com.dinalamin.lifeblood.databinding.SheetDonorDetailBinding
import com.dinalamin.lifeblood.model.User
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** State is passed through arguments so the sheet survives rotation and process death. */
class DonorDetailSheet : BottomSheetDialogFragment() {

    private var _binding: SheetDonorDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetDonorDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val phone = args.getString(K_PHONE).orEmpty()
        val available = args.getBoolean(K_AVAILABLE)

        binding.sheetDonorBlood.text = args.getString(K_BLOOD)
        binding.sheetDonorName.text = args.getString(K_NAME)
        binding.sheetDonorLocation.text = args.getString(K_DISTRICT).orEmpty().ifBlank { "-" }
        binding.sheetDonorDonations.text = args.getLong(K_DONATIONS).toString()
        binding.sheetDonorPhone.text = phone.ifBlank { "-" }

        val color = requireContext().getColor(if (available) R.color.lb_success else R.color.lb_ink_subtle)
        binding.sheetDonorStatus.setText(if (available) R.string.available else R.string.not_available)
        binding.sheetDonorStatus.setTextColor(color)
        binding.sheetDonorStatusDot.backgroundTintList = ColorStateList.valueOf(color)

        binding.sheetDonorCallBtn.isEnabled = phone.isNotBlank()
        binding.sheetDonorCallBtn.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val K_BLOOD = "blood"
        private const val K_NAME = "name"
        private const val K_DISTRICT = "district"
        private const val K_PHONE = "phone"
        private const val K_AVAILABLE = "available"
        private const val K_DONATIONS = "donations"

        fun newInstance(donor: User) = DonorDetailSheet().apply {
            arguments = Bundle().apply {
                putString(K_BLOOD, donor.blood)
                putString(K_NAME, donor.name)
                putString(K_DISTRICT, donor.district)
                putString(K_PHONE, donor.phone)
                putBoolean(K_AVAILABLE, donor.available)
                putLong(K_DONATIONS, donor.donationCount)
            }
        }
    }
}
