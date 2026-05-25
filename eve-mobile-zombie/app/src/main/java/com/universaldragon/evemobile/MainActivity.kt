package com.universaldragon.evemobile

import android.app.Activity
import android.os.Bundle
import android.provider.Settings
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var log: TextView
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 54, 28, 28)
            setBackgroundColor(Color.rgb(3, 8, 12))
        }
        scroll.addView(root)

        root.addView(label("UNIVERSAL DRAGON", 24, true))
        root.addView(label("EVE MOBILE ZOMBIE", 30, true))
        root.addView(label("Phone is not app-based. Phone is assistant-based.", 14, true))

        input = EditText(this).apply {
            hint = "Ask EVE..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            minLines = 2
            setBackgroundColor(Color.rgb(8, 20, 28))
            setPadding(18, 18, 18, 18)
        }
        root.addView(input, LinearLayout.LayoutParams(-1, -2))

        root.addView(button("▶ RUN COMMAND") {
            val cmd = input.text.toString().trim()
            log.text = if (cmd.isEmpty()) "EVE >> waiting..." else "EVE >> $cmd\nCommand received. Brain layer next."
        })

        root.addView(button("📞 Call") { openUri("tel:") })
        root.addView(button("📷 Camera") { startSafe(Intent("android.media.action.IMAGE_CAPTURE")) })
        root.addView(button("🌐 Universal Dragon") { openUri("https://universaldragon.com") })
        root.addView(button("🧠 EVE Pi") { openUri("http://nova-pi.local:5058") })
        root.addView(button("⚙ Android Settings") { startSafe(Intent(Settings.ACTION_SETTINGS)) })
        root.addView(button("🏠 Home Settings") { startSafe(Intent(Settings.ACTION_HOME_SETTINGS)) })

        log = label("EVE READY\nZombie launcher mode active.\nSafe exit: Settings → Apps → Default apps → Home app.", 16, false).apply {
            gravity = Gravity.START
            setBackgroundColor(Color.rgb(0, 18, 20))
            setPadding(18, 18, 18, 18)
        }
        root.addView(log, LinearLayout.LayoutParams(-1, -2))

        setContentView(scroll)
    }

    private fun label(text: String, size: Int, center: Boolean): TextView = TextView(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        textSize = size.toFloat()
        gravity = if (center) Gravity.CENTER else Gravity.START
        setPadding(10, 12, 10, 12)
    }

    private fun button(text: String, action: () -> Unit): Button = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(12, 36, 48))
        setPadding(8, 12, 8, 12)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 8, 0, 8)
        }
        setOnClickListener { action() }
    }

    private fun openUri(uri: String) = startSafe(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))

    private fun startSafe(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            log.text = "EVE error: ${e.message}"
        }
    }
}
