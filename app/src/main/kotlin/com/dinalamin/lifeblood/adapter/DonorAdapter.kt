package com.dinalamin.lifeblood.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dinalamin.lifeblood.R
import com.dinalamin.lifeblood.databinding.ItemDonorBinding
import com.dinalamin.lifeblood.model.User
import com.dinalamin.lifeblood.ui.util.Motion

class DonorAdapter(
    private var items: List<User>,
    private val onCall: (User) -> Unit,
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<DonorAdapter.VH>() {

    private var lastAnimated = -1

    class VH(val b: ItemDonorBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemDonorBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val b = holder.b
        val ctx = b.root.context

        b.donorBlood.text = item.blood
        b.donorName.text = item.name
        b.donorDistrict.text = item.district.ifBlank { "-" }

        val color = ctx.getColor(if (item.available) R.color.lb_success else R.color.lb_ink_subtle)
        b.donorStatus.setText(if (item.available) R.string.available else R.string.not_available)
        b.donorStatus.setTextColor(color)
        b.donorStatusDot.backgroundTintList = ColorStateList.valueOf(color)
        b.donorStatusChip.setBackgroundResource(if (item.available) R.drawable.bg_chip_success else R.drawable.bg_chip_muted)

        b.btnCall.setOnClickListener { onCall(item) }
        b.donorCard.setOnClickListener { onClick(item) }

        if (position > lastAnimated) {
            Motion.enterListItem(b.root, position)
            lastAnimated = position
        } else {
            Motion.resetListItem(b.root)
        }
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        Motion.resetListItem(holder.itemView)
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<User>) {
        items = newItems
        lastAnimated = -1
        notifyDataSetChanged()
    }
}
