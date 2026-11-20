package com.dinalamin.lifeblood.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dinalamin.lifeblood.R
import com.dinalamin.lifeblood.databinding.ItemDonationBinding
import com.dinalamin.lifeblood.model.Donation
import java.text.SimpleDateFormat
import java.util.Locale

class DonationHistoryAdapter(private var items: List<Donation>) :
    RecyclerView.Adapter<DonationHistoryAdapter.VH>() {

    class VH(val b: ItemDonationBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemDonationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx = holder.b.root.context
        holder.b.historyPatient.text = ctx.getString(R.string.history_saved, item.patient)
        holder.b.historyDate.text = item.date?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
        } ?: "-"
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<Donation>) {
        items = newItems
        notifyDataSetChanged()
    }
}
