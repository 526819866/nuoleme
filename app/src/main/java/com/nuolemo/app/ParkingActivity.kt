package com.nuolemo.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 停车记录与管理页面
 */
class ParkingActivity : AppCompatActivity() {

    private lateinit var rootView: View
    private lateinit var scrollView: View
    private lateinit var textLocation: TextView
    private lateinit var textDuration: TextView
    private lateinit var textStartTime: TextView
    private lateinit var imagePhoto: ImageView
    private lateinit var buttonTakePhoto: MaterialButton
    private lateinit var buttonSetReminder: MaterialButton
    private lateinit var buttonEndParking: MaterialButton
    private lateinit var buttonBack: MaterialButton

    private var currentPhotoPath: String? = null
    private var currentPhotoUri: Uri? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            } else {
                Snackbar.make(rootView, "需要相机权限才能拍照", Snackbar.LENGTH_SHORT).show()
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentPhotoUri != null) {
                val record = ParkingStore.getCurrentRecord(this)
                if (record != null) {
                    val updatedRecord = record.copy(photoPath = currentPhotoPath)
                    ParkingStore.saveCurrentRecord(this, updatedRecord)
                    loadPhoto()
                    Snackbar.make(rootView, "照片已保存", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parking)

        bindViews()
        applySystemBarInsets()
        bindActions()
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun bindViews() {
        rootView = findViewById(R.id.parkingRootContent)
        scrollView = findViewById(R.id.parkingScrollView)
        textLocation = findViewById(R.id.textParkingLocation)
        textDuration = findViewById(R.id.textParkingDuration)
        textStartTime = findViewById(R.id.textParkingStartTime)
        imagePhoto = findViewById(R.id.imageParkingPhoto)
        buttonTakePhoto = findViewById(R.id.buttonTakePhoto)
        buttonSetReminder = findViewById(R.id.buttonSetReminder)
        buttonEndParking = findViewById(R.id.buttonEndParking)
        buttonBack = findViewById(R.id.buttonBack)
    }

    private fun applySystemBarInsets() {
        val baseStart = scrollView.paddingStart
        val baseTop = scrollView.paddingTop
        val baseEnd = scrollView.paddingEnd
        val baseBottom = scrollView.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePaddingRelative(
                start = baseStart + systemBars.left,
                top = baseTop + systemBars.top,
                end = baseEnd + systemBars.right,
                bottom = baseBottom + systemBars.bottom,
            )
            insets
        }
        ViewCompat.requestApplyInsets(scrollView)
    }

    private fun bindActions() {
        buttonBack.setOnClickListener { finish() }

        buttonTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        buttonSetReminder.setOnClickListener {
            showReminderDialog()
        }

        buttonEndParking.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("结束停车")
                .setMessage("确定要结束当前停车记录吗？")
                .setPositiveButton("确定") { _, _ ->
                    endParking()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        textLocation.setOnClickListener {
            showLocationDialog()
        }
    }

    private fun refreshUi() {
        val record = ParkingStore.getCurrentRecord(this)
        if (record != null) {
            textDuration.text = record.getFormattedDuration()
            textLocation.text = record.location ?: "点击设置车位信息"
            textStartTime.text = "开始时间: ${formatTimestamp(record.startTime)}"
            loadPhoto()
        } else {
            finish()  // 没有停车记录，关闭页面
        }
    }

    private fun loadPhoto() {
        val record = ParkingStore.getCurrentRecord(this) ?: return
        if (record.photoPath != null && File(record.photoPath).exists()) {
            imagePhoto.setImageURI(Uri.fromFile(File(record.photoPath)))
            imagePhoto.visibility = View.VISIBLE
        } else {
            imagePhoto.visibility = View.GONE
        }
    }

    private fun launchCamera() {
        val photoFile = createImageFile()
        currentPhotoPath = photoFile.absolutePath
        currentPhotoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(currentPhotoUri)
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("PARKING_${timestamp}_", ".jpg", storageDir)
    }

    private fun showLocationDialog() {
        val input = TextInputEditText(this).apply {
            val record = ParkingStore.getCurrentRecord(this@ParkingActivity)
            setText(record?.location ?: "")
            hint = "例如：A区23号"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("设置车位信息")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val location = input.text?.toString()?.trim()
                val record = ParkingStore.getCurrentRecord(this)
                if (record != null && !location.isNullOrEmpty()) {
                    val updatedRecord = record.copy(location = location)
                    ParkingStore.saveCurrentRecord(this, updatedRecord)
                    refreshUi()
                    Snackbar.make(rootView, "车位信息已保存", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showReminderDialog() {
        val options = arrayOf("15分钟", "30分钟", "1小时", "2小时", "3小时", "不提醒")
        val minutes = arrayOf(15, 30, 60, 120, 180, 0)

        MaterialAlertDialogBuilder(this)
            .setTitle("设置停车提醒")
            .setItems(options) { _, which ->
                val reminderMinutes = minutes[which]
                ParkingAlarmReceiver.scheduleReminder(this, reminderMinutes)
                val message = if (reminderMinutes > 0) {
                    "已设置 ${options[which]} 后提醒"
                } else {
                    "已取消提醒"
                }
                Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun endParking() {
        ParkingAlarmReceiver.cancelReminder(this)
        ParkingTimerService.stop(this)
        ParkingStore.endParking(this)
        Snackbar.make(rootView, "停车记录已保存到历史", Snackbar.LENGTH_SHORT).show()
        finish()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}
