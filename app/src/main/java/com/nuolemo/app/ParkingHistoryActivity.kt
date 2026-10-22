package com.nuolemo.app

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 停车历史记录页面
 */
class ParkingHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonBack: MaterialButton
    private lateinit var buttonClearHistory: MaterialButton
    private lateinit var textEmpty: TextView
    private lateinit var adapter: ParkingHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parking_history)

        bindViews()
        applySystemBarInsets()
        setupRecyclerView()
        bindActions()
        loadHistory()
    }

    private fun bindViews() {
        recyclerView = findViewById(R.id.recyclerParkingHistory)
        buttonBack = findViewById(R.id.buttonBack)
        buttonClearHistory = findViewById(R.id.buttonClearHistory)
        textEmpty = findViewById(R.id.textEmptyHistory)
    }

    private fun applySystemBarInsets() {
        val rootView = findViewById<View>(R.id.historyRootContent)
        val baseStart = recyclerView.paddingStart
        val baseTop = recyclerView.paddingTop
        val baseEnd = recyclerView.paddingEnd
        val baseBottom = recyclerView.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            recyclerView.updatePaddingRelative(
                start = baseStart + systemBars.left,
                top = baseTop + systemBars.top,
                end = baseEnd + systemBars.right,
                bottom = baseBottom + systemBars.bottom,
            )
            insets
        }
        ViewCompat.requestApplyInsets(rootView)
    }

    private fun setupRecyclerView() {
        adapter = ParkingHistoryAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun bindActions() {
        buttonBack.setOnClickListener { finish() }

        buttonClearHistory.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("清空历史")
                .setMessage("确定要清空所有停车历史记录吗？")
                .setPositiveButton("确定") { _, _ ->
                    ParkingStore.clearHistory(this)
                    loadHistory()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun loadHistory() {
        val history = ParkingStore.getHistory(this)
        adapter.submitList(history)
        textEmpty.visibility = if (history.isEmpty()) View.VISIBLE else View.GONE
        buttonClearHistory.isEnabled = history.isNotEmpty()
    }
}

/**
 * 停车历史记录适配器
 */
class ParkingHistoryAdapter : RecyclerView.Adapter<ParkingHistoryAdapter.ViewHolder>() {

    private var historyList = emptyList<ParkingHistory>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textDuration: TextView = view.findViewById(R.id.textHistoryDuration)
        val textLocation: TextView = view.findViewById(R.id.textHistoryLocation)
        val textTime: TextView = view.findViewById(R.id.textHistoryTime)
        val imagePhoto: android.widget.ImageView = view.findViewById(R.id.imageHistoryPhoto)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val history = historyList[position]
        holder.textDuration.text = history.getFormattedDuration()
        holder.textLocation.text = history.location ?: "位置未记录"
        holder.textTime.text = formatTimeRange(history.startTime, history.endTime)

        if (history.photoPath != null && File(history.photoPath).exists()) {
            holder.imagePhoto.setImageURI(Uri.fromFile(File(history.photoPath)))
            holder.imagePhoto.visibility = View.VISIBLE
        } else {
            holder.imagePhoto.visibility = View.GONE
        }
    }

    override fun getItemCount() = historyList.size

    fun submitList(list: List<ParkingHistory>) {
        historyList = list
        notifyDataSetChanged()
    }

    private fun formatTimeRange(start: Long, end: Long): String {
        val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val startStr = formatter.format(Date(start))
        val endStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(end))
        return "$startStr - $endStr"
    }
}
