package com.dinalamin.lifeblood.adapter

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/** Pulsing placeholder rows shown while a list is loading. */
class SkeletonAdapter(private val count: Int = 4) : RecyclerView.Adapter<SkeletonAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        var pulse: ObjectAnimator? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(
        LayoutInflater.from(parent.context)
            .inflate(com.dinalamin.lifeblood.R.layout.item_skeleton, parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = Unit

    override fun onViewAttachedToWindow(holder: VH) {
        holder.pulse?.cancel()
        holder.pulse = ObjectAnimator.ofFloat(holder.itemView, View.ALPHA, 0.45f, 1f).apply {
            duration = 800
            startDelay = holder.bindingAdapterPosition.coerceAtLeast(0) * 120L
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        holder.pulse?.cancel()
        holder.itemView.alpha = 1f
    }

    override fun getItemCount() = count
}
