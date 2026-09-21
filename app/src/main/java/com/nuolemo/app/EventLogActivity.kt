package com.nuolemo.app

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class EventLogActivity : AppCompatActivity() {
    private lateinit var logTextView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var clearButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_log)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "短信接收日志"

        logTextView = findViewById(R.id.logTextView)
        scrollView = findViewById(R.id.logScrollView)
        clearButton = findViewById(R.id.clearLogButton)

        clearButton.setOnClickListener {
            showClearConfirmDialog()
        }

        refreshLog()
    }

    override fun onResume() {
        super.onResume()
        refreshLog()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshLog() {
        val events = EventLogger.loadEvents(this)
        if (events.isEmpty()) {
            logTextView.text = "暂无日志记录\n\n收到短信后会自动记录：\n• 发送者\n• 短信内容（前60字符）\n• 是否匹配关键词/车牌\n• 是否触发报警"
        } else {
            val logText = events.joinToString(separator = "\n\n━━━━━━━━━━━━━━━━━━━━\n\n") { event ->
                EventLogger.formatEvent(event)
            }
            logTextView.text = logText
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_UP)
            }
        }
    }

    private fun showClearConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("清空日志")
            .setMessage("确定要清空所有日志记录吗？")
            .setPositiveButton("清空") { _, _ ->
                EventLogger.clearEvents(this)
                refreshLog()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
