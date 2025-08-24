package com.xyz.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var btnUSBCamera: Button
    private lateinit var btnPhoneCamera: Button
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnUSBCamera = findViewById(R.id.btnUSBCamera)
        btnPhoneCamera = findViewById(R.id.btnPhoneCamera)

        // 延迟检查权限，确保Activity完全初始化
        Handler(Looper.getMainLooper()).postDelayed({
            checkAndRequestPermissions()
        }, 100)
    }

    private fun checkAndRequestPermissions() {
        if (checkPermissions()) {
            setupButtons()
        } else {
            requestPermissions()
        }
    }

    private fun setupButtons() {
        btnUSBCamera.setOnClickListener {
            val intent = Intent(this, USBCameraActivity::class.java)
            startActivity(intent)
        }

        btnPhoneCamera.setOnClickListener {
            val intent = Intent(this, PhoneCameraActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        try {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                PERMISSION_REQUEST_CODE
            )
        } catch (e: Exception) {
            // 如果权限申请失败，记录错误并尝试继续
            Toast.makeText(this, "权限申请失败，请手动授予相机权限", Toast.LENGTH_LONG).show()
            setupButtons() // 即使权限申请失败，也尝试设置按钮
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "相机权限已授予", Toast.LENGTH_SHORT).show()
                setupButtons()
            } else {
                Toast.makeText(this, "需要相机权限才能使用摄像头功能", Toast.LENGTH_LONG).show()
                // 权限被拒绝，可以选择重新申请或提示用户
                showPermissionDeniedMessage()
            }
        }
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(this, "请在系统权限设置中手动授予相机权限", Toast.LENGTH_LONG).show()
        // 即使权限被拒绝，也尝试设置按钮，让用户可以进入功能页面
        setupButtons()
    }
}
