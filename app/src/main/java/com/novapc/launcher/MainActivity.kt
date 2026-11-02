package com.novapc.launcher

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private data class GameEntry(
    val id: String,
    val name: String,
    val uri: String,
    var profile: String,
    val isFolder: Boolean = false
)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("novapc_store", MODE_PRIVATE) }
    private val games = mutableListOf<GameEntry>()
    private lateinit var content: LinearLayout
    private lateinit var gameListContainer: LinearLayout

    private val bg = Color.rgb(9, 13, 24)
    private val panel = Color.rgb(18, 25, 43)
    private val panelLight = Color.rgb(25, 34, 56)
    private val white = Color.rgb(244, 246, 255)
    private val muted = Color.rgb(155, 166, 190)
    private val purple = Color.rgb(139, 124, 255)
    private val cyan = Color.rgb(89, 220, 223)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        loadGames()
        buildScreen()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            clipToPadding = false
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(34))
        }
        scroll.addView(content, ScrollView.LayoutParams(-1, -2))
        setContentView(scroll)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = TextView(this).apply {
            text = "NOVA"
            textSize = 23f
            setTextColor(white)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            letterSpacing = 0.08f
        }
        val pc = TextView(this).apply {
            text = "PC"
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = rounded(purple, 9)
            setPadding(dp(9), dp(5), dp(9), dp(5))
        }
        header.addView(brand)
        val pcParams = LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) }
        header.addView(pc, pcParams)
        val spacer = View(this)
        header.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(label("ANDROID GAME HUB", 10f, cyan, true))
        content.addView(header)

        addSpace(23)
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(21))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(48, 47, 104), Color.rgb(24, 45, 75), Color.rgb(17, 65, 77))
            ).apply { cornerRadius = dp(22).toFloat() }
        }
        hero.addView(label("WINDOWS GAMES, ORGANIZED", 10f, cyan, true))
        hero.addView(spaceView(9))
        hero.addView(TextView(this).apply {
            text = "Your PC library.\nOn Android."
            textSize = 29f
            setTextColor(white)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setLineSpacing(dp(1).toFloat(), 1.0f)
        })
        hero.addView(spaceView(9))
        hero.addView(body("Keep your Windows installers and game shortcuts in one place, with per-game performance profiles.", Color.rgb(218, 226, 247)))
        hero.addView(spaceView(17))
        val importActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        importActions.addView(actionButton("＋ Add EXE", purple) { pickExe() }, LinearLayout.LayoutParams(0, dp(44), 1f))
        importActions.addView(spaceView(8, horizontal = true))
        importActions.addView(actionButton("＋ Game folder", Color.rgb(41, 75, 93)) { pickGameFolder() }, LinearLayout.LayoutParams(0, dp(44), 1f))
        hero.addView(importActions)
        content.addView(hero)

        addSpace(22)
        content.addView(sectionTitle("Your library", "${games.size} ${if (games.size == 1) "GAME" else "GAMES"}"))
        addSpace(10)
        gameListContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(gameListContainer, LinearLayout.LayoutParams(-1, -2))
        renderGameList()

        addSpace(22)
        content.addView(sectionTitle("Device overview", "LIVE DEVICE INFO"))
        addSpace(10)
        content.addView(deviceCard())

        addSpace(22)
        content.addView(sectionTitle("Performance setup", "SAVED PREFERENCES"))
        addSpace(10)
        content.addView(settingsCard())

        addSpace(18)
        content.addView(disclaimerCard())
        addSpace(12)
        content.addView(label("NOVA PC  •  VERSION 0.1.0", 10f, muted, true).apply { gravity = Gravity.CENTER })
    }

    private fun renderGameList() {
        gameListContainer.removeAllViews()
        if (games.isEmpty()) {
            val empty = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(18), dp(23), dp(18), dp(23))
                background = rounded(panel, 18, Color.rgb(44, 55, 80), 1)
            }
            empty.addView(TextView(this).apply {
                text = "▣"
                textSize = 28f
                setTextColor(purple)
                gravity = Gravity.CENTER
            })
            empty.addView(spaceView(5))
            empty.addView(TextView(this).apply {
                text = "Your library is empty"
                textSize = 16f
                setTextColor(white)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            })
            empty.addView(spaceView(5))
            empty.addView(body("Add a Windows .exe file to create your first library entry.", muted).apply { gravity = Gravity.CENTER })
            gameListContainer.addView(empty)
        } else {
            games.forEach { game ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(15), dp(14), dp(15), dp(14))
                    background = rounded(panel, 17)
                }
                val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                val icon = TextView(this).apply {
                    text = if (game.isFolder) "DIR" else "EXE"
                    textSize = 10f
                    setTextColor(cyan)
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    background = rounded(Color.rgb(22, 63, 76), 9)
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                }
                top.addView(icon)
                val nameBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                nameBox.addView(TextView(this).apply {
                    text = game.name
                    textSize = 15f
                    setTextColor(white)
                    typeface = Typeface.DEFAULT_BOLD
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
                nameBox.addView(TextView(this).apply {
                    text = "${if (game.isFolder) "Game folder" else "Windows executable"} • ${game.profile} profile"
                    textSize = 11f
                    setTextColor(muted)
                })
                top.addView(nameBox, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(12) })
                top.addView(TextView(this).apply {
                    text = "⋮"
                    textSize = 24f
                    setTextColor(muted)
                    gravity = Gravity.CENTER
                    setPadding(dp(8), 0, dp(3), 0)
                    setOnClickListener { showGameMenu(game) }
                })
                card.addView(top)
                card.addView(spaceView(13))
                val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                buttons.addView(actionButton(if (game.isFolder) "Runtime details" else "Open / Share", purple) { explainRuntime(game) }, LinearLayout.LayoutParams(0, dp(42), 1f))
                buttons.addView(spaceView(8, horizontal = true))
                buttons.addView(actionButton("Profile", panelLight) { chooseGameProfile(game) }, LinearLayout.LayoutParams(0, dp(42), 1f))
                card.addView(buttons)
                gameListContainer.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
            }
        }
    }

    private fun deviceCard(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = rounded(panel, 17)
        }
        val mem = ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mem)
        val totalRam = formatBytes(mem.totalMem)
        val freeRam = formatBytes(mem.availMem)
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"
        val rows = listOf(
            "Processor architecture" to abi,
            "Android version" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Total memory" to totalRam,
            "Available memory" to freeRam
        )
        rows.forEachIndexed { index, pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(TextView(this).apply { text = pair.first; textSize = 12f; setTextColor(muted) }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(this).apply { text = pair.second; textSize = 12f; setTextColor(white); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.RIGHT })
            box.addView(row)
            if (index != rows.lastIndex) box.addView(spaceView(13))
        }
        return box
    }

    private fun settingsCard(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = rounded(panel, 17)
        }
        val profile = prefs.getString("default_profile", "Balanced") ?: "Balanced"
        val resolution = prefs.getString("resolution", "1280 × 720") ?: "1280 × 720"
        val fps = prefs.getString("fps", "60 FPS") ?: "60 FPS"
        box.addView(settingRow("Default profile", profile) { chooseDefaultProfile() })
        box.addView(divider())
        box.addView(settingRow("Preferred resolution", resolution) { chooseResolution() })
        box.addView(divider())
        box.addView(settingRow("Frame-rate target", fps) { chooseFps() })
        box.addView(spaceView(10))
        box.addView(body("These are saved preferences for a future runtime adapter. They do not change the phone GPU or an external emulator yet.", muted))
        return box
    }

    private fun disclaimerCard(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(14), dp(15), dp(14))
            background = rounded(Color.rgb(45, 37, 26), 15, Color.rgb(104, 83, 43), 1)
        }
        box.addView(label("RUNTIME NOTE", 10f, Color.rgb(255, 207, 123), true))
        box.addView(spaceView(6))
        box.addView(body("NovaPC currently manages your library; it does not contain Wine, Box64, DXVK, or a Windows runtime. An EXE cannot run on Android by itself. Many games also need their complete game folder, not just one EXE. No compatibility or frame-rate guarantee is possible.", Color.rgb(239, 224, 198)))
        return box
    }

    private fun settingRow(title: String, value: String, onClick: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            isClickable = true
            setOnClickListener { onClick() }
        }
        row.addView(TextView(this).apply { text = title; textSize = 13f; setTextColor(white) }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(this).apply { text = "$value  ›"; textSize = 12f; setTextColor(cyan); typeface = Typeface.DEFAULT_BOLD })
        return row
    }

    private fun sectionTitle(title: String, trailing: String): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this).apply { text = title; textSize = 18f; setTextColor(white); typeface = Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(label(trailing, 9f, muted, true))
        return row
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.04f
    }

    private fun body(text: String, color: Int): TextView = TextView(this).apply {
        this.text = text
        textSize = 12f
        setTextColor(color)
        setLineSpacing(dp(3).toFloat(), 1.0f)
    }

    private fun actionButton(text: String, color: Int, onClick: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(Color.WHITE)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        background = rounded(color, 12)
        isClickable = true
        setOnClickListener { onClick() }
        minHeight = dp(42)
    }

    private fun rounded(color: Int, radius: Int, strokeColor: Int? = null, strokeDp: Int = 1): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (strokeColor != null) setStroke(dp(strokeDp), strokeColor)
    }

    private fun divider(): View = View(this).apply { setBackgroundColor(Color.rgb(42, 51, 72)); layoutParams = LinearLayout.LayoutParams(-1, dp(1)) }
    private fun spaceView(height: Int, horizontal: Boolean = false): View = View(this).apply {
        layoutParams = if (horizontal) LinearLayout.LayoutParams(dp(height), 1) else LinearLayout.LayoutParams(1, dp(height))
    }
    private fun addSpace(height: Int) { content.addView(spaceView(height)) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun pickExe() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            startActivityForResult(intent, REQUEST_PICK_EXE)
        } catch (_: Exception) {
            Toast.makeText(this, "No file picker is available on this device.", Toast.LENGTH_LONG).show()
        }
    }

    private fun pickGameFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            startActivityForResult(intent, REQUEST_PICK_FOLDER)
        } catch (_: Exception) {
            Toast.makeText(this, "No folder picker is available on this device.", Toast.LENGTH_LONG).show()
        }
    }

    private fun persistReadPermission(uri: Uri, data: Intent?) {
        try {
            val flags = (data?.flags ?: 0) and Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Some document providers grant only temporary access. The entry can still be saved.
        } catch (_: Exception) {
            // Continue; the URI may remain usable for this app session.
        }
    }

    @Deprecated("Uses the platform document picker for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        if (requestCode == REQUEST_PICK_FOLDER) {
            val name = try {
                android.provider.DocumentsContract.getTreeDocumentId(uri)
                    .substringAfterLast('/')
                    .substringAfterLast(':')
                    .ifBlank { "Windows game folder" }
            } catch (_: Exception) { "Windows game folder" }
            persistReadPermission(uri, data)
            val defaultProfile = prefs.getString("default_profile", "Balanced") ?: "Balanced"
            games.add(GameEntry(System.currentTimeMillis().toString(), name, uri.toString(), defaultProfile, true))
            saveGames()
            refreshLibrary()
            Toast.makeText(this, "Added $name to your library.", Toast.LENGTH_SHORT).show()
            return
        }
        if (requestCode != REQUEST_PICK_EXE) return
        val name = queryDisplayName(uri) ?: "Windows game.exe"
        if (!name.lowercase(Locale.ROOT).endsWith(".exe")) {
            Toast.makeText(this, "Please choose a Windows .exe file.", Toast.LENGTH_LONG).show()
            return
        }
        persistReadPermission(uri, data)
        val cleanName = name.dropLast(4)
        val defaultProfile = prefs.getString("default_profile", "Balanced") ?: "Balanced"
        games.add(GameEntry(System.currentTimeMillis().toString(), cleanName.ifBlank { name }, uri.toString(), defaultProfile, false))
        saveGames()
        refreshLibrary()
        Toast.makeText(this, "Added $name to your library.", Toast.LENGTH_SHORT).show()
    }

    private fun queryDisplayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) cursor.getString(0) else uri.lastPathSegment
        } catch (_: Exception) {
            uri.lastPathSegment
        } finally {
            cursor?.close()
        }
    }

    private fun explainRuntime(game: GameEntry) {
        val detail = if (game.isFolder) {
            "${game.name} is saved as a folder entry. Android cannot execute Windows programs directly, and this build does not include a Windows compatibility engine. A future runtime must be able to read the selected folder and launch the game's EXE inside it."
        } else {
            "${game.name}.exe is saved in your NovaPC library. Android cannot execute Windows programs directly. This build does not include a Windows compatibility engine.\n\nYou can share the EXE to another app, but that app must explicitly support receiving and launching Windows executables. Many games also need their full game folder and supporting files."
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Runtime required")
            .setMessage(detail)
            .setNegativeButton("Close", null)
        if (!game.isFolder) dialog.setPositiveButton("Share EXE") { _, _ -> shareExe(game) }
        dialog.show()
    }

    private fun shareExe(game: GameEntry) {
        val uri = Uri.parse(game.uri)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newUri(contentResolver, game.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(intent, "Send EXE to a compatible app"))
        } catch (_: Exception) {
            Toast.makeText(this, "No app can receive this file.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showGameMenu(game: GameEntry) {
        AlertDialog.Builder(this)
            .setTitle(game.name)
            .setItems(arrayOf("Change profile", "Remove from library")) { _, which ->
                if (which == 0) chooseGameProfile(game) else confirmRemove(game)
            }
            .show()
    }

    private fun confirmRemove(game: GameEntry) {
        AlertDialog.Builder(this)
            .setTitle("Remove game?")
            .setMessage("Remove ${game.name} from the NovaPC library? The original file will not be deleted.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                games.removeAll { it.id == game.id }
                saveGames()
                refreshLibrary()
            }
            .show()
    }

    private fun chooseGameProfile(game: GameEntry) {
        val choices = arrayOf("Performance", "Balanced", "Compatibility")
        val selected = choices.indexOf(game.profile).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("${game.name} profile")
            .setSingleChoiceItems(choices, selected) { dialog, which ->
                game.profile = choices[which]
                saveGames()
                refreshLibrary()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun chooseDefaultProfile() {
        chooseSetting("Default profile", arrayOf("Performance", "Balanced", "Compatibility"), "default_profile")
    }

    private fun chooseResolution() {
        chooseSetting("Preferred resolution", arrayOf("640 × 360", "960 × 540", "1280 × 720", "1600 × 900", "1920 × 1080"), "resolution")
    }

    private fun chooseFps() {
        chooseSetting("Frame-rate target", arrayOf("30 FPS", "45 FPS", "60 FPS", "90 FPS", "120 FPS"), "fps")
    }

    private fun chooseSetting(title: String, choices: Array<String>, key: String) {
        val current = prefs.getString(key, choices[0]) ?: choices[0]
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(choices, choices.indexOf(current).coerceAtLeast(0)) { dialog, which ->
                prefs.edit().putString(key, choices[which]).apply()
                dialog.dismiss()
                refreshLibrary()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshLibrary() {
        // Rebuild from persisted entries to keep every displayed count and setting in sync.
        buildScreen()
    }

    private fun loadGames() {
        games.clear()
        val raw = prefs.getString("games", "[]") ?: "[]"
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                games.add(GameEntry(
                    obj.optString("id"),
                    obj.optString("name"),
                    obj.optString("uri"),
                    obj.optString("profile", "Balanced"),
                    obj.optBoolean("isFolder", false)
                ))
            }
        } catch (_: Exception) {
            prefs.edit().remove("games").apply()
        }
    }

    private fun saveGames() {
        val arr = JSONArray()
        games.forEach { game ->
            arr.put(JSONObject().apply {
                put("id", game.id)
                put("name", game.name)
                put("uri", game.uri)
                put("profile", game.profile)
                put("isFolder", game.isFolder)
            })
        }
        prefs.edit().putString("games", arr.toString()).apply()
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f GB", gb)
    }

    companion object {
        private const val REQUEST_PICK_EXE = 4102
        private const val REQUEST_PICK_FOLDER = 4103
    }
}
