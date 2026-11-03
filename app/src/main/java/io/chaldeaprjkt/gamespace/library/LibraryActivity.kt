/*
 * SPDX-FileCopyrightText: 2026 haloUI-AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package io.chaldeaprjkt.gamespace.library

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.data.GameConfig
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.data.UserGame
import io.chaldeaprjkt.gamespace.preferences.AppListPreferences
import io.chaldeaprjkt.gamespace.preferences.appselector.AppSelectorActivity
import io.chaldeaprjkt.gamespace.settings.PerAppSettingsActivity
import io.chaldeaprjkt.gamespace.settings.PerAppSettingsFragment
import io.chaldeaprjkt.gamespace.settings.SettingsActivity
import io.chaldeaprjkt.gamespace.utils.GameModeUtils
import javax.inject.Inject

@AndroidEntryPoint(ComponentActivity::class)
class LibraryActivity : Hilt_LibraryActivity() {

    @Inject
    lateinit var settings: SystemSettings

    @Inject
    lateinit var gameModeUtils: GameModeUtils

    @Inject
    lateinit var appSettings: AppSettings

    private lateinit var coverflow: CoverflowCarousel
    private lateinit var adapter: CoverflowAdapter
    private lateinit var contentView: View
    private lateinit var emptyView: View
    private lateinit var gameTitle: TextView
    private lateinit var gameMode: TextView
    private lateinit var playButton: Button
    private lateinit var gameSettingsButton: Button
    private lateinit var addGamesButton: Button
    private lateinit var settingsButton: ImageButton

    private val selectorResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getStringExtra(AppListPreferences.EXTRA_APP)?.let(::register)
            }
        }

    private val perAppResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getStringExtra(PerAppSettingsFragment.PREF_UNREGISTER)?.let(::unregister)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)
        bindViews()
        setupCoverflow()
        setupActions()
    }

    override fun onResume() {
        super.onResume()
        refresh()
        if (contentView.isVisible) coverflow.requestFocus()
    }

    override fun onPause() {
        saveSelection()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun bindViews() {
        coverflow = findViewById(R.id.coverflow)
        contentView = findViewById(R.id.library_content)
        emptyView = findViewById(R.id.library_empty)
        gameTitle = findViewById(R.id.game_title)
        gameMode = findViewById(R.id.game_mode)
        playButton = findViewById(R.id.play)
        gameSettingsButton = findViewById(R.id.game_settings)
        addGamesButton = findViewById(R.id.add_games)
        settingsButton = findViewById(R.id.settings_button)
    }

    private fun setupCoverflow() {
        adapter = CoverflowAdapter().apply {
            onItemClick = { position ->
                if (position == coverflow.selection) playCentered() else coverflow.glideTo(position)
            }
        }
        coverflow.adapter = adapter
        coverflow.onSelectionChanged = { position -> showGameInfo(position) }
        coverflow.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    coverflow.glideTo(coverflow.selection - 1)
                    true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    coverflow.glideTo(coverflow.selection + 1)
                    true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    settingsButton.requestFocus()
                    true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    playButton.requestFocus()
                    true
                }
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_BUTTON_A -> {
                    playCentered()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupActions() {
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        playButton.setOnClickListener { playCentered() }
        gameSettingsButton.setOnClickListener {
            val entry = adapter.get(coverflow.selection) ?: return@setOnClickListener
            perAppResult.launch(Intent(this, PerAppSettingsActivity::class.java).apply {
                putExtra(PerAppSettingsActivity.EXTRA_PACKAGE, entry.packageName)
            })
        }
        addGamesButton.setOnClickListener {
            selectorResult.launch(Intent(this, AppSelectorActivity::class.java))
        }
    }

    private fun playCentered() {
        val entry = adapter.get(coverflow.selection) ?: return
        val component = packageManager.getLaunchIntentForPackage(entry.packageName)?.component ?: return
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(component)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        )
    }

    private fun showGameInfo(position: Int) {
        val entry = adapter.get(position) ?: return
        gameTitle.text = entry.label
        gameMode.text = resources.getStringArray(R.array.game_mode_names)
            .getOrNull(entry.mode) ?: getString(R.string.game_mode_unsupported)
    }

    private fun refresh() {
        val pm = packageManager
        val flags = PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        val wanted = adapter.get(coverflow.selection)?.packageName ?: appSettings.lastGame
        val entries = settings.userGames.mapNotNull { game ->
            runCatching {
                val info = pm.getApplicationInfo(game.packageName, flags)
                LibraryEntry(
                    game.packageName,
                    game.mode,
                    info.loadLabel(pm).toString(),
                    info.loadIcon(pm),
                    info.loadBanner(pm)?.let(::flattenBanner)
                )
            }.getOrNull()
        }.sortedBy { it.label.lowercase() }

        adapter.submit(entries)
        if (entries.isEmpty()) {
            contentView.isVisible = false
            emptyView.isVisible = true
            return
        }
        contentView.isVisible = true
        emptyView.isVisible = false

        val target = entries.indexOfFirst { it.packageName == wanted }.coerceAtLeast(0)
        coverflow.snapTo(target)
        showGameInfo(target)
    }

    private fun register(packageName: String) {
        val games = settings.userGames.filter { it.packageName != packageName }.toMutableList()
        games.add(UserGame(packageName))
        settings.userGames = games
        gameModeUtils.setIntervention(packageName, GameConfig.ModeBuilder.build())
        refresh()
    }

    private fun unregister(packageName: String) {
        settings.userGames = settings.userGames.filter { it.packageName != packageName }
        gameModeUtils.setIntervention(packageName, null)
        refresh()
    }

    private fun flattenBanner(drawable: Drawable): Drawable =
        if (drawable !is AdaptiveIconDrawable) {
            drawable
        } else {
            BitmapDrawable(
                resources,
                drawable.toBitmap(
                    resources.getDimensionPixelSize(R.dimen.coverflow_tile_width),
                    resources.getDimensionPixelSize(R.dimen.coverflow_tile_height)
                )
            )
        }

    private fun saveSelection() {
        adapter.get(coverflow.selection)?.packageName?.let { appSettings.lastGame = it }
    }
}
