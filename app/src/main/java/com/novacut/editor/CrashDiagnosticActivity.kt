package com.novacut.editor

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

/**
 * Crash-only diagnostic screen.
 *
 * Runs in its own process so it can render even when the main application
 * process crashes on the main thread during startup.
 */
class CrashDiagnosticActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashFile = intent.getStringExtra(EXTRA_CRASH_FILE)
        val throwableClass = intent.getStringExtra(EXTRA_THROWABLE_CLASS).orEmpty()
        val throwableMessage = intent.getStringExtra(EXTRA_THROWABLE_MESSAGE).orEmpty()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Vidora — Crash Diagnostic"
            textSize = 22f
            setPadding(0, 0, 0, 20)
        }

        val summary = TextView(this).apply {
            text = buildString {
                append("سبب إغلاق التطبيق:\n\n")
                append(throwableClass.ifBlank { "Unknown exception" })
                if (throwableMessage.isNotBlank()) {
                    append("\n\n")
                    append(throwableMessage)
                }
                append("\n\nتم حفظ التفاصيل الكاملة في ملف التشخيص.")
                if (!crashFile.isNullOrBlank()) {
                    append("\n\n")
                    append(crashFile)
                }
            }
            textSize = 16f
        }

        val details = TextView(this).apply {
            textSize = 13f
            setPadding(0, 24, 0, 24)
            text = if (!crashFile.isNullOrBlank()) {
                runCatching { File(crashFile).readText(Charsets.UTF_8) }
                    .getOrElse { "تعذر قراءة ملف التشخيص: $it" }
            } else {
                "لم يتم إنشاء ملف التشخيص."
            }
        }

        val close = Button(this).apply {
            text = "إغلاق"
            setOnClickListener { finishAndRemoveTask() }
        }

        root.addView(title)
        root.addView(summary)
        root.addView(ScrollView(this).apply { addView(details) },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        )
        root.addView(close)
        setContentView(root)
    }

    companion object {
        const val EXTRA_CRASH_FILE = "crash_file"
        const val EXTRA_THROWABLE_CLASS = "throwable_class"
        const val EXTRA_THROWABLE_MESSAGE = "throwable_message"
    }
}
