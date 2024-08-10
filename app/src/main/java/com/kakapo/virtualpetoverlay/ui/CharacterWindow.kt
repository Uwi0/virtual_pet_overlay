package com.kakapo.virtualpetoverlay.ui

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.res.Resources
import android.graphics.PixelFormat
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kakapo.virtualpetoverlay.R
import kotlinx.coroutines.delay

class CharacterWindow(context: Context) {

    private val view: View
    private val windowManager: WindowManager
    private var characterDirection: Direction = Direction.Right
    private var isDragged = true
    private val img = mutableIntStateOf(R.drawable.img_mumei_right)
    private val layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        CHARACTER_SIZE_PX,
        CHARACTER_SIZE_PX,
        0,
        0,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    )

    init {
        view = contentView(context)
        layoutParams.gravity = Gravity.BOTTOM
        windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        view.registerDraggableTouchListener(
            initialPosition = { Point(layoutParams.x, layoutParams.y) },
            positionListener = { x, y ->
                randomMovementAfterFallingDown()
                setPosition(x, y)
                isDragged = y == 0
            }
        )
    }

    fun showCharacter() {
        try {
            if (view.windowToken == null) {
                if (view.parent == null) {
                    windowManager.addView(view, layoutParams)
                }
            }
        } catch (_: Exception) {

        }
    }


    fun removeCharacter() {
        try {
            windowManager.removeView(view)
            view.invalidate()
            if (view.parent != null) {
                val parent = view.parent as ViewGroup
                parent.removeAllViews()
            }
        } catch (_: Exception) {

        }
    }

    suspend fun updatePosition() {
        val (screenWidth, screenHeight) = calculateScreenSize()
        while (true) {
            if (isDragged) {
                createDirection(screenHeight, screenWidth)
                windowManager.updateViewLayout(view, layoutParams)
            }
            delay(50)
        }
    }

    private fun contentView(context: Context) = windowViewFactory(context) {
        Box(modifier = Modifier.size(CHARACTER_SIZE_DP.dp)) {
            Image(
                modifier = Modifier
                    .size(80.dp, 128.dp),
                painter = painterResource(id = img.intValue),
                contentDescription = ""
            )
        }
    }

    private fun setPosition(x: Int, y: Int) {
        layoutParams.x = x
        layoutParams.y = y
        try {
            windowManager.updateViewLayout(view, layoutParams)
        } catch (_: Exception) {

        }
    }

    private fun createDirection(screenHeight: Int, screenWidth: Int) {
        when (characterDirection) {
            Direction.UP -> if (layoutParams.y == screenHeight) {
                characterDirection = Direction.DOWN
            } else {
                randomizeJumpEvent()
            }

            Direction.DOWN -> if (layoutParams.y <= 0) {
                characterDirection = if (layoutParams.x == screenWidth) {
                    Direction.Left
                } else {
                    Direction.Right
                }
            } else {
                layoutParams.y -= FALL_SPEED
            }

            Direction.Left -> {
                img.intValue = R.drawable.img_mumei_left
                if (layoutParams.x <= -screenWidth) {
                    randomMovementOnTheEdgeOfWidth { characterDirection = Direction.Right }
                } else {
                    layoutParams.x -= X_SPEED
                }
            }

            Direction.Right -> {
                img.intValue = R.drawable.img_mumei_right
                if (layoutParams.x >= screenWidth) {
                    randomMovementOnTheEdgeOfWidth { characterDirection = Direction.Left }
                } else {
                    layoutParams.x += X_SPEED
                }
            }

            Direction.JUMP_RIGHT -> {
                img.intValue = R.drawable.img_mumei_right
                if (layoutParams.y >= screenHeight) {
                    characterDirection = Direction.DOWN
                }else if (layoutParams.x <= screenWidth) {
                    layoutParams.x += X_JUMP_SPEED
                    layoutParams.y += Y_SPEED
                }else{
                    layoutParams.y += Y_SPEED
                    randomizeJumpEvent()
                }
            }

            Direction.JUMP_LEFT -> {
                img.intValue = R.drawable.img_mumei_left
                if (layoutParams.y >= screenHeight) {
                    characterDirection = Direction.DOWN
                } else if (layoutParams.x >= -screenWidth) {
                    layoutParams.x -= X_JUMP_SPEED
                    layoutParams.y += Y_SPEED
                }else {
                    layoutParams.y += Y_SPEED
                    randomizeJumpEvent()
                }

            }
        }
    }

    private fun randomizeJumpEvent() {
        val random = (1..15).random()
        characterDirection =
            if (random % 2 == 0 && img.intValue == R.drawable.img_mumei_left) Direction.JUMP_RIGHT
            else if (random % 3 == 0 && img.intValue == R.drawable.img_mumei_right) Direction.JUMP_LEFT
            else Direction.UP
    }

    private fun calculateScreenSize(): Pair<Int, Int> {
        val displayMetrics = getDisplayMetrics()
        val layoutThreshold = CHARACTER_SIZE_PX
        val screenWidth = displayMetrics.widthPixels / 2 - layoutThreshold / 2 - 16
        val screedHeight = displayMetrics.heightPixels - layoutThreshold
        return screenWidth to screedHeight
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics
    }

    private fun randomMovementOnTheEdgeOfWidth(changeDirection: () -> Unit) {
        val random = (1..10).random()
        if (random % 2 == 0) characterDirection = Direction.UP
        else changeDirection.invoke()
    }


    private fun randomMovementAfterFallingDown() {
        val random = (1..10).random()
        characterDirection = if (random % 2 == 0) Direction.Left
        else Direction.Right
    }

    enum class Direction {
        UP,
        DOWN,
        Left,
        Right,
        JUMP_RIGHT,
        JUMP_LEFT
    }

    companion object {
        const val CHARACTER_SIZE_DP = 60
        val CHARACTER_SIZE_PX = (CHARACTER_SIZE_DP * Resources.getSystem().displayMetrics.density).toInt()
        private const val X_SPEED = 5
        private const val Y_SPEED = 5
        private const val X_JUMP_SPEED = 10
        private const val Y_JUMP_SPEED = 10
        private const val FALL_SPEED = 20
    }
}
