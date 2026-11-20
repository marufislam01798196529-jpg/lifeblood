package com.dinalamin.lifeblood.adapter

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dinalamin.lifeblood.R
import com.dinalamin.lifeblood.databinding.ItemRequestBinding
import com.dinalamin.lifeblood.model.BloodRequest
import com.dinalamin.lifeblood.ui.util.Motion
import com.dinalamin.lifeblood.ui.util.relativeTime

class RequestAdapter(
    private var items: List<BloodRequest>,
    private val showOwnerActions: Boolean = false,
    private val onClick: (BloodRequest) -> Unit,
    private val onEdit: ((BloodRequest) -> Unit)? = null,
    private val onDelete: ((BloodRequest) -> Unit)? = null
) : RecyclerView.Adapter<RequestAdapter.VH>() {

    private var lastAnimated = -1

    class VH(val b: ItemRequestBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val b = holder.b
        val ctx = b.root.context

        b.reqBlood.text = item.blood
        b.reqPatient.text = item.patient
        b.reqMeta.text = listOf(item.hospital, item.location).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "-" }
        b.reqTime.text = item.createdAt.relativeTime()
        b.reqProblem.text = item.problem.ifBlank { ctx.getString(R.string.sheet_no_details) }
        b.reqEmergencyBadge.visibility = if (item.isEmergency) View.VISIBLE else View.GONE
        b.reqAccent.visibility = if (item.isEmergency) View.VISIBLE else View.GONE

        // Emergency requests get a red call button; others a green one.
        val callColor = ctx.getColor(if (item.isEmergency) R.color.lb_blood else R.color.lb_success)
        b.btnCall.backgroundTintList = ColorStateList.valueOf(callColor)
        b.btnCall.visibility = if (showOwnerActions) View.GONE else View.VISIBLE
        b.btnCall.setOnClickListener {
            ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.phone}")))
        }

        b.requestCard.setOnClickListener { onClick(item) }

        if (showOwnerActions) {
            b.ownerActions.visibility = View.VISIBLE
            b.btnEdit.setOnClickListener { onEdit?.invoke(item) }
            b.btnDelete.setOnClickListener { onDelete?.invoke(item) }
        } else {
            b.ownerActions.visibility = View.GONE
        }

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

    fun update(newItems: List<BloodRequest>) {
        items = newItems
        lastAnimated = -1
        notifyDataSetChanged()
    }
}
