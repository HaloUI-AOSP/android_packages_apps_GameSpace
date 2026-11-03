/*
 * SPDX-FileCopyrightText: 2026 haloUI-AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package io.chaldeaprjkt.gamespace.library

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import io.chaldeaprjkt.gamespace.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CoverflowCarousel(context: Context, attrs: AttributeSet?) :
    RecyclerView(context, attrs) {

    var onSelectionChanged: ((position: Int) -> Unit)? = null

    var selection = NO_POSITION
        private set

    private val tileWidth = resources.getDimensionPixelSize(R.dimen.coverflow_tile_width)
    private val tileSpacing = resources.getDimensionPixelSize(R.dimen.coverflow_tile_spacing)
    private val cardElevation = CARD_ELEVATION_DP * resources.displayMetrics.density
    private val cardAdvance = (tileWidth + 2 * tileSpacing).toFloat()

    private val centerTint = ColorStateList.valueOf(context.getColor(R.color.librarySecondaryContainer))
    private val sideTint = ColorStateList.valueOf(context.getColor(R.color.librarySurfaceContainerHigh))

    private val snapHelper = CarouselSnapHelper()

    init {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        itemAnimator = null
        snapHelper.attachToRecyclerView(this)
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                restyleCards()
            }
        })
    }

    fun snapTo(position: Int) {
        val count = adapter?.itemCount ?: 0
        if (position in 0 until count) centerOn(position)
    }

    fun glideTo(position: Int) {
        val count = adapter?.itemCount ?: 0
        if (position in 0 until count) snapHelper.glideTo(position)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val gutter = ((w - tileWidth) / 2 - tileSpacing).coerceAtLeast(0)
        setPadding(gutter, paddingTop, gutter, paddingBottom)
        if (oldw != 0 && selection != NO_POSITION) centerOn(selection)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        restyleCards()
    }

    private fun centerOn(position: Int) {
        val manager = layoutManager as? LinearLayoutManager ?: return
        manager.scrollToPositionWithOffset(position, 0)
    }

    private fun restyleCards() {
        var closestChild: View? = null
        var closestOffset = Float.MAX_VALUE
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            val offset = (child.left + child.right) / 2f - width / 2f
            if (abs(offset) < abs(closestOffset)) {
                closestOffset = offset
                closestChild = child
            }
            restyleCard(child, offset)
        }
        closestChild?.let(::selectCard)
    }

    private fun restyleCard(child: View, offset: Float) {
        val distance = (abs(offset) / cardAdvance).coerceAtMost(1f)
        child.cameraDistance = width * 2.2f
        child.rotationY = -(offset / cardAdvance).coerceIn(-1f, 1f) * MAX_TILT_DEGREES
        child.scaleX = 1f - distance * SIDE_SCALE_LOSS
        child.scaleY = child.scaleX
        child.alpha = 1f - distance * SIDE_ALPHA_LOSS
        child.translationZ = (1f - distance) * cardElevation
    }

    private fun selectCard(child: View) {
        val position = getChildAdapterPosition(child)
        if (position == NO_POSITION || position == selection) return
        selection = position
        for (i in 0 until childCount) {
            val other = getChildAt(i) ?: continue
            other.backgroundTintList = if (other === child) centerTint else sideTint
        }
        onSelectionChanged?.invoke(position)
    }

    private inner class CarouselSnapHelper : PagerSnapHelper() {

        fun glideTo(position: Int) {
            val manager = layoutManager as? LinearLayoutManager ?: return
            manager.startSmoothScroll(glideScroller().apply { targetPosition = position })
        }

        override fun createScroller(layoutManager: RecyclerView.LayoutManager) = glideScroller()

        private fun glideScroller() =
            object : LinearSmoothScroller(this@CoverflowCarousel.context) {

                override fun onTargetFound(
                    targetView: View,
                    state: RecyclerView.State,
                    action: RecyclerView.SmoothScroller.Action
                ) {
                    val manager = this@CoverflowCarousel.layoutManager ?: return
                    val snap = calculateDistanceToFinalSnap(manager, targetView) ?: return
                    val duration = calculateTimeForDeceleration(max(abs(snap[0]), abs(snap[1])))
                    if (duration > 0) {
                        action.update(snap[0], snap[1], duration, mDecelerateInterpolator)
                    }
                }

                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics) =
                    GLIDE_MILLISECONDS_PER_INCH / displayMetrics.densityDpi

                override fun calculateTimeForScrolling(dx: Int) =
                    min(MAX_FLING_SETTLE_MS, super.calculateTimeForScrolling(dx))
            }
    }

    companion object {
        private const val MAX_TILT_DEGREES = 18f
        private const val SIDE_SCALE_LOSS = 0.08f
        private const val SIDE_ALPHA_LOSS = 0.35f
        private const val CARD_ELEVATION_DP = 12f
        private const val GLIDE_MILLISECONDS_PER_INCH = 200f
        private const val MAX_FLING_SETTLE_MS = 80
    }
}
