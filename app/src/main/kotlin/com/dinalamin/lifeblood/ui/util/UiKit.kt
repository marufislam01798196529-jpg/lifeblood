package com.dinalamin.lifeblood.ui.util

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.dinalamin.lifeblood.R
import com.dinalamin.lifeblood.databinding.ViewEmptyStateBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import java.util.Calendar

// ---------- Units ----------

fun Context.dp(value: Int): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics)

// ---------- Haptics ----------

fun View.tick() {
    performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
}

// ---------- Coroutines ----------

/** Runs [block] and returns null on failure, while still letting coroutine cancellation propagate. */
suspend fun <T> safeCall(block: suspend () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}

// ---------- Text helpers ----------

fun greetingForNow(context: Context): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 5 -> context.getString(R.string.greeting_night)
        hour < 12 -> context.getString(R.string.greeting_morning)
        hour < 17 -> context.getString(R.string.greeting_afternoon)
        hour < 21 -> context.getString(R.string.greeting_evening)
        else -> context.getString(R.string.greeting_night)
    }
}

fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

fun Any?.relativeTime(): String {
    val millis = (this as? Timestamp)?.toDate()?.time ?: return ""
    return DateUtils.getRelativeTimeSpanString(
        millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
    ).toString()
}

fun Any?.millisOrZero(): Long = (this as? Timestamp)?.toDate()?.time ?: 0L

// ---------- Dropdowns & chips ----------

fun AutoCompleteTextView.setItems(items: List<String>, selected: String? = items.firstOrNull()) {
    setAdapter(ArrayAdapter(context, R.layout.item_dropdown, items))
    setText(selected ?: "", false)
}

fun ChipGroup.setBloodOptions(options: List<String>, selected: String? = null) {
    removeAllViews()
    val inflater = LayoutInflater.from(context)
    options.forEach { label ->
        val chip = inflater.inflate(R.layout.item_chip_blood, this, false) as Chip
        chip.id = View.generateViewId()
        chip.text = label
        addView(chip)
        if (label == selected) check(chip.id)
    }
}

fun ChipGroup.selectedLabel(): String? =
    findViewById<Chip>(checkedChipId)?.text?.toString()

fun ChipGroup.selectLabel(label: String?) {
    for (i in 0 until childCount) {
        val chip = getChildAt(i) as? Chip ?: continue
        if (chip.text?.toString() == label) {
            check(chip.id)
            return
        }
    }
    clearCheck()
}

// ---------- Empty state ----------

fun ViewEmptyStateBinding.bindContent(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes body: Int
) {
    emptyIcon.setImageResource(icon)
    emptyTitle.setText(title)
    emptyBody.setText(body)
}

// ---------- Motion ----------

object Motion {

    /** Staggered slide-up + fade for list rows as they first appear. */
    fun enterListItem(view: View, position: Int) {
        view.animate().cancel()
        view.alpha = 0f
        view.translationY = view.context.dp(18)
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(minOf(position, 8) * 45L)
            .setDuration(320)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()
    }

    fun resetListItem(view: View) {
        view.animate().cancel()
        view.alpha = 1f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
    }

    /** Subtle scale feedback while a card is pressed. Does not consume the touch. */
    @SuppressLint("ClickableViewAccessibility")
    fun pressable(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
            }
            false
        }
    }

    /** Fades and lifts the visible children of [parent] in one after another. */
    fun staggerIn(parent: ViewGroup, startDelayMs: Long = 0, stepMs: Long = 55) {
        var index = 0
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child.visibility == View.GONE) continue
            child.animate().cancel()
            child.alpha = 0f
            child.translationY = parent.context.dp(20)
            child.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(startDelayMs + index * stepMs)
                .setDuration(380)
                .setInterpolator(DecelerateInterpolator(1.6f))
                .start()
            index++
        }
    }

    /** Quick bell-style wiggle. */
    fun wiggle(view: View) {
        android.animation.ObjectAnimator.ofFloat(view, View.ROTATION, 0f, -18f, 14f, -10f, 6f, 0f)
            .setDuration(520)
            .start()
    }

    /** Small pop used when a bottom-navigation icon becomes selected. */
    fun pop(view: View) {
        view.animate().cancel()
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.animate().scaleX(1f).scaleY(1f).setDuration(280)
            .setInterpolator(android.view.animation.OvershootInterpolator(2.2f)).start()
    }

    fun countUp(target: TextView, to: Int, durationMs: Long = 900) {
        ValueAnimator.ofInt(0, to).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { target.text = (it.animatedValue as Int).toString() }
            start()
        }
    }

    fun fadeIn(view: View, durationMs: Long = 250) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate().alpha(1f).setDuration(durationMs).start()
    }

    fun crossfadeText(view: TextView, text: CharSequence) {
        if (view.text == text) return
        view.animate().alpha(0f).setDuration(110).withEndAction {
            view.text = text
            view.animate().alpha(1f).setDuration(160).start()
        }.start()
    }
}


// ---------- Donation eligibility ----------

/** Minimum rest period between whole-blood donations used by the app. */
const val DONATION_INTERVAL_DAYS = 90

data class Eligibility(
    val ready: Boolean,
    val daysSince: Int?,      // null when no donation is recorded
    val daysLeft: Int,        // 0 when ready
    val progress: Int         // 0..100
)

fun eligibilityFor(lastDonationMillis: Long?): Eligibility {
    if (lastDonationMillis == null || lastDonationMillis <= 0L) return Eligibility(true, null, 0, 100)
    val days = ((System.currentTimeMillis() - lastDonationMillis) / DateUtils.DAY_IN_MILLIS).toInt().coerceAtLeast(0)
    return if (days >= DONATION_INTERVAL_DAYS) {
        Eligibility(true, days, 0, 100)
    } else {
        Eligibility(false, days, DONATION_INTERVAL_DAYS - days, days * 100 / DONATION_INTERVAL_DAYS)
    }
}

fun Context.achievementFor(donations: Long): Pair<String, String> = when {
    donations >= 10 -> getString(R.string.tier_gold) to getString(R.string.tier_top)
    donations >= 5 -> getString(R.string.tier_silver) to getString(R.string.tier_to_next, (10 - donations).toInt(), getString(R.string.tier_gold))
    donations >= 1 -> getString(R.string.tier_bronze) to getString(R.string.tier_to_next, (5 - donations).toInt(), getString(R.string.tier_silver))
    else -> getString(R.string.tier_new) to getString(R.string.tier_to_next, 1, getString(R.string.tier_bronze))
}

fun Context.shareText(text: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    startActivity(android.content.Intent.createChooser(intent, null))
}
