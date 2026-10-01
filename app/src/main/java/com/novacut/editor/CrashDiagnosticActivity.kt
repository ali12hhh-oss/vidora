package com.novacut.editor

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

/**
 * Crash-only diagnostic screen.
 *
 * Shows the crash in short, copy-friendly sections and provides copy/share actions.
 */
class CrashDiagnosticActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashFile = intent.getStringExtra(EXTRA_CRASH_FILE)
        val throwableClass = intent.getStringExtra(EXTRA_THROWABLE_CLASS).orEmpty()
        val throwableMessage = intent.getStringExtra(EXTRA_THROWABLE_MESSAGE).orEmpty()

        val rawDetails = if (!crashFile.isNullOrBlank()) {
            runCatching { File(crashFile).readText(Charsets.UTF_8) }
                .getOrElse { "تعذر قراءة ملف التشخيص: " + it }
        } else {
            ""
        }

        val importantLines = extractImportantLines(rawDetails, throwableClass, throwableMessage)

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
                    append("\n")
                    append(throwableMessage)
                }
            }
            textSize = 16f
        }

        val sectionTitle = TextView(this).apply {
            text = "أهم الأسطر — أرسل هذه فقط"
            textSize = 18f
            setPadding(0, 28, 0, 12)
        }

        val important = TextView(this).apply {
            text = importantLines
            textSize = 15f
            setTextIsSelectable(true)
            movementMethod = ScrollingMovementMethod()
            setPadding(16, 16, 16, 16)
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val copy = Button(this).apply {
            text = "نسخ السبب"
            setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Vidora crash diagnostic", importantLines))
                Toast.makeText(this@CrashDiagnosticActivity, "تم نسخ سبب الكراش", Toast.LENGTH_SHORT).show()
            }
        }

        val share = Button(this).apply {
            text = "مشاركة الملف"
            setOnClickListener {
                if (crashFile.isNullOrBlank()) {
                    Toast.makeText(this@CrashDiagnosticActivity, "لا يوجد ملف تشخيص", Toast.LENGTH_SHORT).show()
                } else {
                    shareCrashFile(File(crashFile))
                }
            }
        }

        actions.addView(copy, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(share, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val hint = TextView(this).apply {
            text = "يمكنك نسخ السبب المختصر أو مشاركة ملف التشخيص الكامل."
            textSize = 13f
            setPadding(0, 12, 0, 20)
        }

        val detailsTitle = TextView(this).apply {
            text = "التفاصيل الكاملة (اختياري)"
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }

        val details = TextView(this).apply {
            textSize = 12f
            setTextIsSelectable(true)
            movementMethod = ScrollingMovementMethod()
            text = rawDetails.ifBlank { "لم يتم إنشاء ملف التشخيص." }
        }

        val close = Button(this).apply {
            text = "إغلاق"
            setOnClickListener { finishAndRemoveTask() }
        }

        root.addView(title)
        root.addView(summary)
        root.addView(sectionTitle)
        root.addView(
            ScrollView(this).apply { addView(important) },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )
        root.addView(actions)
        root.addView(hint)
        root.addView(detailsTitle)
        root.addView(
            ScrollView(this).apply { addView(details) },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                260,
            ),
        )
        root.addView(close)
        setContentView(root)
    }

    private fun shareCrashFile(file: File) {
        if (!file.exists()) {
            Toast.makeText(this, "ملف التشخيص غير موجود", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = try {
            androidx.core.content.FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".fileprovider",
                file,
            )
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر تجهيز ملف المشاركة: " + e.message, Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Vidora crash diagnostic")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "مشاركة ملف التشخيص"))
    }

    private fun extractImportantLines(
        rawDetails: String,
        throwableClass: String,
        throwableMessage: String,
    ): String {
        val lines = rawDetails
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        val selected = mutableListOf<String>()

        fun add(value: String) {
            val clean = value.trim()
            if (clean.isNotBlank() && selected.none { it == clean }) {
                selected += clean
            }
        }

        add("Exception: " + throwableClass.ifBlank { "Unknown" })
        if (throwableMessage.isNotBlank()) add("Message: " + throwableMessage)

        lines.firstOrNull { it.contains("FATAL EXCEPTION", ignoreCase = true) }?.let(::add)
        lines.firstOrNull { it.startsWith("Caused by:", ignoreCase = true) }?.let(::add)
        lines.firstOrNull { it.contains("at com.novacut.editor.", ignoreCase = true) }?.let(::add)
        lines.firstOrNull {
            it.contains("at android.", ignoreCase = true) ||
                it.contains("at androidx.", ignoreCase = true)
        }?.let(::add)

        if (selected.size <= 2 && lines.isNotEmpty()) {
            lines.take(4).forEach(::add)
        }

        return selected.take(7).joinToString("\n\n")
            .ifBlank { "لم يتم العثور على أسطر تشخيص واضحة." }
    }

    companion object {
        const val EXTRA_CRASH_FILE = "crash_file"
        const val EXTRA_THROWABLE_CLASS = "throwable_class"
        const val EXTRA_THROWABLE_MESSAGE = "throwable_message"
    }
}
