package com.example.smartirr

import android.animation.ObjectAnimator
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter

@BindingAdapter(value = ["changeIcon", "previousValue", "animateChange"], requireAll = false)
fun setChangeIndicator(view: ImageView, currentObj: Any?, previousObj: Any?, animateChange: Boolean? = null) {
    try {
        val cur = when (currentObj) {
            is Double -> currentObj
            is Float -> currentObj.toDouble()
            is Int -> currentObj.toDouble()
            is String -> currentObj.toDoubleOrNull()
            else -> null
        }
        val prev = when (previousObj) {
            is Double -> previousObj
            is Float -> previousObj.toDouble()
            is Int -> previousObj.toDouble()
            is String -> previousObj.toDoubleOrNull()
            else -> null
        }

        val context = view.context
        if (cur != null && prev != null) {
            if (cur > prev) {
                view.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_arrow_up))
                view.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_light))
                if (animateChange == true) pulse(view)
            } else if (cur < prev) {
                view.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_arrow_down))
                view.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_light))
                if (animateChange == true) pulse(view)
            } else {
                view.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_no_change))
                view.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
            }
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.INVISIBLE
        }
    } catch (e: Exception) {
        view.visibility = View.INVISIBLE
    }
}

private fun pulse(view: View) {
    val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.25f, 1f)
    val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.25f, 1f)
    scaleX.duration = 450
    scaleY.duration = 450
    scaleX.start()
    scaleY.start()
}