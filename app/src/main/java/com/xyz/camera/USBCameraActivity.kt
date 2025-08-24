package com.xyz.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.ViewGroup

class USBCameraActivity : AppCompatActivity() {
    private lateinit var surfaceView: SurfaceView
    private lateinit var btnCapture: Button
    private lateinit var btnShowParams: Button
    private lateinit var imageView: ImageView
    private lateinit var usbCam: USBCam
    private lateinit var overlayView: View
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 200
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usb_camera)

        surfaceView = findViewById(R.id.surfaceView)
        btnCapture = findViewById(R.id.btnCapture)
        btnShowParams = findViewById(R.id.btnShowParams)
        imageView = findViewById(R.id.imageView)

        // 检查权限
        if (checkPermissions()) {
            initializeCamera()
        } else {
            requestPermissions()
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
            // 如果权限申请失败，记录错误并提示用户
            Toast.makeText(this, "权限申请失败，请手动授予相机权限", Toast.LENGTH_LONG).show()
            // 可以选择返回或继续等待权限
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
                Toast.makeText(this, "相机权限已授予，正在初始化摄像头", Toast.LENGTH_SHORT).show()
                initializeCamera()
            } else {
                Toast.makeText(this, "需要相机权限才能使用USB摄像头功能", Toast.LENGTH_LONG).show()
                // 权限被拒绝，可以选择返回或提示用户
                showPermissionDeniedMessage()
            }
        }
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(this, "请在系统权限设置中手动授予相机权限", Toast.LENGTH_LONG).show()
        // 可以选择返回上一页或继续等待权限
    }

    private fun initializeCamera() {
        usbCam = USBCam()
        usbCam.initMonitor(this, surfaceView)
        usbCam.register()

        btnCapture.setOnClickListener {
            usbCam.captureImageFromPreview { bitmap ->
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    Toast.makeText(this, "拍照成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "拍照失败", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnShowParams.setOnClickListener {
            showCameraParamsOverlay()
        }
    }

    private fun showCameraParamsOverlay() {
        // 创建蒙层视图
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.camera_params_overlay, null)
        
        // 获取参数显示文本视图
        val tvCameraParams = overlayView.findViewById<TextView>(R.id.tvCameraParams)
        val btnClose = overlayView.findViewById<Button>(R.id.btnClose)
        
        // 构建参数信息
        val paramsInfo = buildCameraParamsInfo()
        tvCameraParams.text = paramsInfo
        
        // 设置关闭按钮
        btnClose.setOnClickListener {
            hideCameraParamsOverlay()
        }
        
        // 添加到根布局
        val rootLayout = findViewById<View>(android.R.id.content)
        (rootLayout as ViewGroup).addView(overlayView)
    }

    private fun hideCameraParamsOverlay() {
        if (::overlayView.isInitialized) {
            val rootLayout = findViewById<View>(android.R.id.content)
            (rootLayout as ViewGroup).removeView(overlayView)
        }
    }

    private fun buildCameraParamsInfo(): String {
        val params = StringBuilder()
        
        params.append("=== USB摄像头参数信息 ===\n\n")
        
        // 获取摄像头实例
        usbCam.uvcCamera?.let { camera ->
            try {
                params.append("摄像头状态:\n")
                
                // 使用try-catch包装可能不存在的属性
                try {
                    val isPreviewing = camera.javaClass.getMethod("isPreviewing").invoke(camera) as? Boolean ?: false
                    params.append("• 预览状态: ${if (isPreviewing) "运行中" else "已停止"}\n")
                } catch (e: Exception) {
                    params.append("• 预览状态: 无法获取\n")
                }
                
                try {
                    val isCapturing = camera.javaClass.getMethod("isCapturing").invoke(camera) as? Boolean ?: false
                    params.append("• 捕获状态: ${if (isCapturing) "运行中" else "已停止"}\n")
                } catch (e: Exception) {
                    params.append("• 捕获状态: 无法获取\n")
                }
                
                try {
                    val isConnected = camera.javaClass.getMethod("isConnected").invoke(camera) as? Boolean ?: false
                    params.append("• 连接状态: ${if (isConnected) "已连接" else "未连接"}\n")
                } catch (e: Exception) {
                    params.append("• 连接状态: 无法获取\n")
                }
                params.append("\n")
                
                // 基本信息
                params.append("基本信息:\n")
                try {
                    val name = camera.javaClass.getMethod("getName").invoke(camera) as? String ?: "未知"
                    params.append("• 摄像头名称: $name\n")
                } catch (e: Exception) {
                    params.append("• 摄像头名称: 无法获取\n")
                }
                
                try {
                    val id = camera.javaClass.getMethod("getId").invoke(camera) as? String ?: "未知"
                    params.append("• 摄像头ID: $id\n")
                } catch (e: Exception) {
                    params.append("• 摄像头ID: 无法获取\n")
                }
                params.append("\n")
                
                // 分辨率信息
                try {
                    val sizes = camera.supportedSizeList
                    params.append("支持的分辨率 (共${sizes.size}种):\n")
                    for ((index, size) in sizes.withIndex()) {
                        params.append("• [${index}] ${size.width} x ${size.height}\n")
                    }
                    params.append("\n")
                } catch (e: Exception) {
                    params.append("分辨率信息获取失败: ${e.message}\n\n")
                }
                
                // 当前预览尺寸
                try {
                    if (camera.previewSize != null) {
                        params.append("当前预览尺寸:\n")
                        params.append("• ${camera.previewSize.width} x ${camera.previewSize.height}\n\n")
                    }
                } catch (e: Exception) {
                    params.append("预览尺寸信息获取失败: ${e.message}\n\n")
                }
                
                // 格式信息
                try {
                    val formats = camera.supportedFormatList
                    params.append("支持的格式 (共${formats.size}种):\n")
                    for ((index, format) in formats.withIndex()) {
                        params.append("• [${index}] $format\n")
                    }
                    params.append("\n")
                } catch (e: Exception) {
                    params.append("格式信息获取失败: ${e.message}\n\n")
                }
                
                // 控制参数
                try {
                    params.append("控制参数:\n")
                    
                    try {
                        val brightness = camera.javaClass.getMethod("getBrightness").invoke(camera) as? Int ?: -1
                        params.append("• 亮度: $brightness\n")
                    } catch (e: Exception) {
                        params.append("• 亮度: 无法获取\n")
                    }
                    
                    try {
                        val contrast = camera.javaClass.getMethod("getContrast").invoke(camera) as? Int ?: -1
                        params.append("• 对比度: $contrast\n")
                    } catch (e: Exception) {
                        params.append("• 对比度: 无法获取\n")
                    }
                    
                    try {
                        val saturation = camera.javaClass.getMethod("getSaturation").invoke(camera) as? Int ?: -1
                        params.append("• 饱和度: $saturation\n")
                    } catch (e: Exception) {
                        params.append("• 饱和度: 无法获取\n")
                    }
                    
                    try {
                        val sharpness = camera.javaClass.getMethod("getSharpness").invoke(camera) as? Int ?: -1
                        params.append("• 锐度: $sharpness\n")
                    } catch (e: Exception) {
                        params.append("• 锐度: 无法获取\n")
                    }
                    
                    try {
                        val gain = camera.javaClass.getMethod("getGain").invoke(camera) as? Int ?: -1
                        params.append("• 增益: $gain\n")
                    } catch (e: Exception) {
                        params.append("• 增益: 无法获取\n")
                    }
                    
                    try {
                        val whiteBalance = camera.javaClass.getMethod("getWhiteBalance").invoke(camera) as? Int ?: -1
                        params.append("• 白平衡: $whiteBalance\n")
                    } catch (e: Exception) {
                        params.append("• 白平衡: 无法获取\n")
                    }
                    
                    try {
                        val focus = camera.javaClass.getMethod("getFocus").invoke(camera) as? Int ?: -1
                        params.append("• 对焦: $focus\n")
                    } catch (e: Exception) {
                        params.append("• 对焦: 无法获取\n")
                    }
                    
                    try {
                        val zoom = camera.javaClass.getMethod("getZoom").invoke(camera) as? Int ?: -1
                        params.append("• 变焦: $zoom\n")
                    } catch (e: Exception) {
                        params.append("• 变焦: 无法获取\n")
                    }
                    
                    params.append("\n")
                } catch (e: Exception) {
                    params.append("控制参数获取失败: ${e.message}\n\n")
                }
                
                // 设备能力
                try {
                    val capabilities = camera.javaClass.getMethod("getDeviceCapabilities").invoke(camera)
                    if (capabilities != null) {
                        params.append("设备能力:\n")
                        params.append("• $capabilities\n\n")
                    } else {

                    }
                } catch (e: Exception) {
                    params.append("设备能力信息获取失败: ${e.message}\n\n")
                }
                
            } catch (e: Exception) {
                params.append("参数获取过程中发生错误: ${e.message}\n")
            }
        } ?: run {
            params.append("摄像头未初始化或未连接\n")
        }
        
        // 添加Surface信息
        params.append("Surface信息:\n")
        params.append("• 宽度: ${surfaceView.width}\n")
        params.append("• 高度: ${surfaceView.height}\n")
        params.append("• 是否可见: ${if (surfaceView.visibility == View.VISIBLE) "是" else "否"}\n\n")
        
        params.append("=== 参数信息结束 ===")
        
        return params.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::usbCam.isInitialized) {
            usbCam.preViewClose()
        }
    }
}
