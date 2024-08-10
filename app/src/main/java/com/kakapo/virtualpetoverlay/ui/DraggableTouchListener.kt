package com.kakapo.virtualpetoverlay.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Point
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import java.util.Timer
import kotlin.concurrent.timerTask
import kotlin.math.hypot

fun View.registerDraggableTouchListener(
    initialPosition: () -> Point,
    positionListener: (x: Int, y: Int) -> Unit
) {
    WindowHeaderTouchListener(context, this, initialPosition, positionListener)
}


class WindowHeaderTouchListener(
    context: Context,
    private val view: View,
    private val initialPosition: () -> Point,
    val positionListener: (x: Int, y: Int) -> Unit
) : View.OnTouchListener {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longClickInterval = ViewConfiguration.getLongPressTimeout()
    private val gestureDetector = GestureDetector(context, FlingGestureListener())
    private var pointerStartX = 0
    private var pointerStartY = 0
    private var initialX = 0
    private var initialY = 0
    private var moving = false
    private var longClickPerformed = false
    private var timer: Timer? = null
    private var xAxis = 0
    private var yAxis = 0

    init {
        view.setOnTouchListener(this)
    }

    private fun scheduleLongClickTimer() {
        if (timer == null) {
            timer = Timer()
            timer?.schedule(timerTask {
                if (!moving && !longClickPerformed) {
                    view.post {
                        view.performLongClick()
                    }
                    longClickPerformed = true
                }
                cancelLongClickTimer()
            }, longClickInterval.toLong())
        }
    }

    private fun cancelLongClickTimer() {
        timer?.cancel()
        timer = null
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(motionEvent)
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                pointerStartX = motionEvent.rawX.toInt()
                pointerStartY = motionEvent.rawY.toInt()
                with(initialPosition()) {
                    initialX = x
                    initialY = y
                }
                moving = false
                longClickPerformed = false
                scheduleLongClickTimer()
            }

            MotionEvent.ACTION_MOVE -> {
                if (!longClickPerformed) {
                    val deltaX = motionEvent.rawX - pointerStartX
                    val deltaY = motionEvent.rawY - pointerStartY
                    val invertedDeltaY = -deltaY
                    xAxis = initialX + deltaX.toInt()
                    yAxis = initialY + invertedDeltaY.toInt()

                    if (moving || hypot(deltaX, invertedDeltaY) > touchSlop) {
                        cancelLongClickTimer()
                        positionListener(xAxis, yAxis)
                        moving = true
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                cancelLongClickTimer()
                if (!moving && !longClickPerformed) {
                    view.performClick()
                } else if (yAxis > 0) {
                    animateWindowToTop()
                }
            }
        }

        return true
    }

    private fun animateWindowToTop() {
        val startY = yAxis
        val endY = 0
        val duration = 1_000

        val animator = ValueAnimator.ofInt(startY, endY)
        animator.duration = duration.toLong()
        animator.interpolator = LightGravityInterpolator()

        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            positionListener(xAxis, animatedValue)
        }

        animator.start()
    }

    inner class FlingGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val deltaX = (velocityX / FLING_FACTOR).toInt()
            val deltaY = (velocityY / FLING_FACTOR).toInt()
            positionListener(deltaX, deltaY)
            return true
        }


    }

    companion object {
        const val FLING_FACTOR = 100f
    }
}