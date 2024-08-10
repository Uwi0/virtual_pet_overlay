package com.kakapo.virtualpetoverlay.ui

import android.animation.TimeInterpolator
import android.view.animation.PathInterpolator

class LightGravityInterpolator : TimeInterpolator {
    private val pathInterpolator = PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f)
    override fun getInterpolation(input: Float): Float {
        return pathInterpolator.getInterpolation(input)
    }
}