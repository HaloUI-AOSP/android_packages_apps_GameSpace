/*
 * SPDX-FileCopyrightText: 2026 haloUI-AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package io.chaldeaprjkt.gamespace.utils

import android.content.Intent
import android.os.BatteryManager
import io.chaldeaprjkt.gamespace.R

data class BatteryReading(val percentage: Int, val charging: Boolean) {

    val icon: Int
        get() = when {
            charging && percentage >= 90 -> R.drawable.ic_battery_charging_90
            charging && percentage >= 80 -> R.drawable.ic_battery_charging_80
            charging && percentage >= 30 -> R.drawable.ic_battery_charging_30
            charging -> R.drawable.ic_battery_charging_20
            percentage >= 90 -> R.drawable.ic_battery_full
            percentage >= 80 -> R.drawable.ic_battery_5_bar
            percentage >= 60 -> R.drawable.ic_battery_4_bar
            percentage >= 40 -> R.drawable.ic_battery_3_bar
            percentage >= 20 -> R.drawable.ic_battery_2_bar
            percentage >= 10 -> R.drawable.ic_battery_1_bar
            else -> R.drawable.ic_battery_0_bar
        }
}

fun batteryReadingOf(intent: Intent): BatteryReading? {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    return BatteryReading(
        percentage = level * 100 / scale,
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    )
}
