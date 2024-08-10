package com.kakapo.virtualpetoverlay.ui

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.res.Resources
import android.graphics.PixelFormat
import android.graphics.Point
import android.util.DisplayMetrics
import android.util.Log
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

    private var view: View
    private var windowManager: WindowManager
    private var characterDirection: Direction = Direction.Right
    private var isDragged = true
    private val img = mutableIntStateOf(R.drawable.img_mumei_right)
    private var layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
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
        } catch (e: Exception) {
            Log.e("CharacterWindow", "error create window${e.message}")
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
        } catch (e: Exception) {
            Log.e("CharacterWindow", "error remove window${e.message}")
        }
    }

    suspend fun updatePosition() {
        val (screenWidth, screenHeight) = screenWidthAndHeight()
        while (true) {
            Log.d(
                "CharacterWindow",
                "window: width: $screenWidth, x:${layoutParams.x}, y: ${layoutParams.y}, $isDragged"
            )
            if (isDragged) {
                createDirection(screenHeight, screenWidth)
                windowManager.updateViewLayout(view, layoutParams)
            }
            delay(10)
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
        } catch (e: Exception) {
            Log.e("CharacterWindow", "Error update window ${e.message}")
        }
    }

    private fun createDirection(screenHeight: Int, screenWidth: Int) {
        when (characterDirection) {
            Direction.UP -> if (layoutParams.y == screenHeight) {
                characterDirection = Direction.DOWN
            } else {
                layoutParams.y += 1
            }

            Direction.DOWN -> if (layoutParams.y == 0) {
                characterDirection = if (layoutParams.x == screenWidth) {
                    Direction.Left
                } else {
                    Direction.Right
                }
            } else {
                layoutParams.y -= 1
            }

            Direction.Left -> {
                img.intValue = R.drawable.img_mumei_left
                if (layoutParams.x <= -screenWidth) {
                    randomMovementOnTheEdgeOfWidth { characterDirection = Direction.Right }
                } else {
                    layoutParams.x -= 1
                }
            }

            Direction.Right -> {
                img.intValue = R.drawable.img_mumei_right
                if (layoutParams.x >= screenWidth) {
                    randomMovementOnTheEdgeOfWidth { characterDirection = Direction.Left }
                } else {
                    layoutParams.x += 1
                }
            }
        }
    }

    private fun screenWidthAndHeight(): Pair<Int, Int> {
        val displayMetrics = DisplayMetrics()
        val layoutThreshold = CHARACTER_SIZE_PX
        val screenWidth = displayMetrics.widthPixels / 2 - layoutThreshold / 2
        val screedHeight =
            displayMetrics.heightPixels - (displayMetrics.heightPixels / 100 * 5) - layoutThreshold
        return screenWidth to screedHeight
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
        Right
    }

    companion object {
        const val CHARACTER_SIZE_DP = 60
        val CHARACTER_SIZE_PX = (CHARACTER_SIZE_DP * Resources.getSystem().displayMetrics.density).toInt()
    }
}
