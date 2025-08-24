package com.xyz.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PhoneCameraActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private lateinit var surfaceView: SurfaceView
    private lateinit var btnTakePhoto: Button
    private lateinit var btnSwitchCamera: Button
    private lateinit var btnShowParams: Button
    private lateinit var imageView: ImageView
    private lateinit var overlayView: View
    private lateinit var sizeSelectionPicture: SizeSelectionView
    private lateinit var sizeSelectionPreview: SizeSelectionView
    private lateinit var logger: Logger
    
    private var mCamera: Camera? = null
    private var cameraId = 0 // 0为后置，1为前置
    private var isPreview = false
    
    // 支持的尺寸数据
    private var supportedPictureSizes = mutableListOf<SizeItem>()
    private var supportedPreviewSizes = mutableListOf<SizeItem>()
    private var currentPictureSize: SizeItem? = null
    private var currentPreviewSize: SizeItem? = null
    
    // 相机参数
    private var mPreviewSize: Camera.Size? = null
    private var mPictureSize: Camera.Size? = null
    private var mAspectRatio: Float = 16f / 9f
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 300
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_camera)

        // 初始化Logger
        logger = Logger(this)

        surfaceView = findViewById(R.id.surfaceView)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnShowParams = findViewById(R.id.btnShowParams)
        imageView = findViewById(R.id.imageView)
        sizeSelectionPicture = findViewById(R.id.sizeSelectionPicture)
        sizeSelectionPreview = findViewById(R.id.sizeSelectionPreview)

        // 初始化尺寸选择器
        setupSizeSelectors()

        // 设置SurfaceHolder回调
        surfaceView.holder.addCallback(this)

        // 检查权限
        if (checkPermissions()) {
            initCamera()
        } else {
            requestPermissions()
        }

        btnTakePhoto.setOnClickListener {
            takePhoto()
        }

        btnSwitchCamera.setOnClickListener {
            switchCamera()
        }

        btnShowParams.setOnClickListener {
            showCameraParamsOverlay()
        }
        
        logger.i("PhoneCameraActivity onCreate completed")
    }

    private fun setupSizeSelectors() {
        // 设置图片尺寸选择器
        sizeSelectionPicture.setLabel("图片尺寸:")
        sizeSelectionPicture.setOnSizeSelectedListener { sizeItem ->
            logger.i("图片尺寸选择: ${sizeItem.toString()}")
            currentPictureSize = sizeItem
            updateCameraParameters()
        }

        // 设置预览尺寸选择器
        sizeSelectionPreview.setLabel("预览尺寸:")
        sizeSelectionPreview.setOnSizeSelectedListener { sizeItem ->
            logger.i("预览尺寸选择: ${sizeItem.toString()}")
            currentPreviewSize = sizeItem
            updateCameraParameters()
        }
    }

    private fun initCamera() {
        try {
            mCamera = Camera.open(cameraId)
            logger.i("相机初始化成功，ID: $cameraId")
            
            // 获取支持的尺寸
            getSupportedSizes()
            
            // 设置相机参数
            setupCameraParameters()
            
        } catch (e: Exception) {
            logger.e("相机初始化失败: ${e.message}")
            Toast.makeText(this, "相机初始化失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.ECLAIR)
    private fun getSupportedSizes() {
        mCamera?.let { camera ->
            val parameters = camera.parameters
            
            // 获取支持的预览尺寸
            supportedPreviewSizes.clear()
            for (size in parameters.supportedPreviewSizes) {
                supportedPreviewSizes.add(SizeItem("preview_${size.width}_${size.height}", size.width, size.height))
            }
            
            // 获取支持的图片尺寸
            supportedPictureSizes.clear()
            for (size in parameters.supportedPictureSizes) {
                supportedPictureSizes.add(SizeItem("picture_${size.width}_${size.height}", size.width, size.height))
            }
            
            logger.i("获取到支持的预览尺寸: ${supportedPreviewSizes.size}个")
            logger.i("获取到支持的图片尺寸: ${supportedPictureSizes.size}个")
            
            // 更新尺寸选择器
            sizeSelectionPreview.setSizeList(supportedPreviewSizes)
            sizeSelectionPicture.setSizeList(supportedPictureSizes)
            
            // 选择最优尺寸
            selectOptimalSizes()
        }
    }

    private fun selectOptimalSizes() {
        if (supportedPreviewSizes.isNotEmpty()) {
            currentPreviewSize = chooseOptimalPreviewSize(supportedPreviewSizes, mAspectRatio)
            sizeSelectionPreview.setSelectedSize(currentPreviewSize)
            logger.i("选择最优预览尺寸: ${currentPreviewSize?.toString()}")
        }
        
        if (supportedPictureSizes.isNotEmpty()) {
            currentPictureSize = chooseOptimalPictureSize(supportedPictureSizes, mAspectRatio)
            sizeSelectionPicture.setSelectedSize(currentPictureSize)
            logger.i("选择最优图片尺寸: ${currentPictureSize?.toString()}")
        }
    }

    private fun setupCameraParameters() {
        mCamera?.let { camera ->
            try {
                val parameters = camera.parameters
                
                // 设置预览尺寸
                currentPreviewSize?.let { sizeItem ->
                    mPreviewSize = findCameraSize(parameters.supportedPreviewSizes, sizeItem.width, sizeItem.height)
                    mPreviewSize?.let { size ->
                        parameters.setPreviewSize(size.width, size.height)
                        logger.i("设置预览尺寸: ${size.width}x${size.height}")
                    }
                }
                
                // 设置图片尺寸
                currentPictureSize?.let { sizeItem ->
                    mPictureSize = findCameraSize(parameters.supportedPictureSizes, sizeItem.width, sizeItem.height)
                    mPictureSize?.let { size ->
                        parameters.setPictureSize(size.width, size.height)
                        logger.i("设置图片尺寸: ${size.width}x${size.height}")
                    }
                }
                
                // 设置其他参数
                parameters.setPictureFormat(ImageFormat.JPEG)
                parameters.setPreviewFormat(PixelFormat.YCbCr_420_I)
                
                // 设置预览方向
                val displayOrientation = getDisplayOrientation()
                camera.setDisplayOrientation(displayOrientation)
                parameters.setRotation(displayOrientation)
                
                // 应用参数
                camera.parameters = parameters
                logger.i("相机参数设置完成")
                
            } catch (e: Exception) {
                logger.e("设置相机参数失败: ${e.message}")
            }
        }
    }

    private fun findCameraSize(sizes: List<Camera.Size>, width: Int, height: Int): Camera.Size? {
        return sizes.find { it.width == width && it.height == height }
    }

    private fun updateCameraParameters() {
        if (isPreview && mCamera != null) {
            setupCameraParameters()
        }
    }

    private fun switchCamera() {
        // 停止预览
        stopPreview()
        
        // 释放当前相机
        releaseCamera()
        
        // 切换相机ID
        cameraId = if (cameraId == 0) 1 else 0
        logger.i("切换相机到: ${if (cameraId == 0) "后置" else "前置"}")
        
        // 重新初始化相机
        initCamera()
        
        // 如果Surface已经创建，开始预览
        if (surfaceView.holder.surface.isValid) {
            startPreview()
        }
    }

    private fun startPreview() {
        mCamera?.let { camera ->
            try {
                camera.setPreviewDisplay(surfaceView.holder)
                camera.startPreview()
                isPreview = true
                logger.i("相机预览开始")
            } catch (e: Exception) {
                logger.e("开始预览失败: ${e.message}")
            }
        }
    }

    private fun stopPreview() {
        mCamera?.let { camera ->
            try {
                camera.stopPreview()
                isPreview = false
                logger.i("相机预览停止")
            } catch (e: Exception) {
                logger.e("停止预览失败: ${e.message}")
            }
        }
    }

    private fun releaseCamera() {
        mCamera?.let { camera ->
            try {
                camera.release()
                mCamera = null
                logger.i("相机释放完成")
            } catch (e: Exception) {
                logger.e("释放相机失败: ${e.message}")
            }
        }
    }

    private fun takePhoto() {
        if (!isPreview || mCamera == null) {
            Toast.makeText(this, "相机未准备好", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mCamera?.takePicture(null, null) { data, camera ->
                // 保存图片
                saveImage(data)
                
                // 显示图片
                displayImage(data)
                
                // 重新开始预览
                camera.startPreview()
            }
            logger.i("拍照完成")
        } catch (e: Exception) {
            logger.e("拍照失败: ${e.message}")
            Toast.makeText(this, "拍照失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImage(data: ByteArray) {
        try {
            val photoFile = File(
                outputDirectory,
                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
                    .format(System.currentTimeMillis()) + ".jpg"
            )
            
            FileOutputStream(photoFile).use { fos ->
                fos.write(data)
            }
            
            logger.i("图片保存成功: ${photoFile.absolutePath}")
            Toast.makeText(this, "图片保存成功", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            logger.e("保存图片失败: ${e.message}")
            Toast.makeText(this, "保存图片失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayImage(data: ByteArray) {
        try {
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            imageView.setImageBitmap(bitmap)
            logger.i("图片显示成功")
        } catch (e: Exception) {
            logger.e("显示图片失败: ${e.message}")
        }
    }

    private val outputDirectory: File by lazy {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun getDisplayOrientation(): Int {
        val info = android.content.pm.ActivityInfo()
        val rotation = windowManager.defaultDisplay.rotation
        var degrees = 0
        
        when (rotation) {
            android.view.Surface.ROTATION_0 -> degrees = 0
            android.view.Surface.ROTATION_90 -> degrees = 90
            android.view.Surface.ROTATION_180 -> degrees = 180
            android.view.Surface.ROTATION_270 -> degrees = 270
        }
        
        val result = if (cameraId == 0) {
            (info.orientation - degrees + 360) % 360
        } else {
            (info.orientation + degrees) % 360
        }
        
        return result
    }

    /**
     * 根据宽高比选择最优预览尺寸
     */
    private fun chooseOptimalPreviewSize(sizes: List<SizeItem>, targetAspectRatio: Float): SizeItem? {
        if (sizes.isEmpty()) return null
        
        // 按宽高比分组
        val aspectRatioGroups = sizes.groupBy { size ->
            String.format("%.2f", size.width.toFloat() / size.height.toFloat())
        }
        
        // 找到最接近目标宽高比的组
        val closestGroup = aspectRatioGroups.entries.minByOrNull { entry ->
            kotlin.math.abs(entry.key.toFloat() - targetAspectRatio)
        }
        
        // 在最佳宽高比组中选择最高分辨率
        return closestGroup?.value?.maxByOrNull { it.width * it.height } ?: sizes.first()
    }

    /**
     * 根据宽高比选择最优图片尺寸
     */
    private fun chooseOptimalPictureSize(sizes: List<SizeItem>, targetAspectRatio: Float): SizeItem? {
        if (sizes.isEmpty()) return null
        
        // 按宽高比分组
        val aspectRatioGroups = sizes.groupBy { size ->
            String.format("%.2f", size.width.toFloat() / size.height.toFloat())
        }
        
        // 找到最接近目标宽高比的组
        val closestGroup = aspectRatioGroups.entries.minByOrNull { entry ->
            kotlin.math.abs(entry.key.toFloat() - targetAspectRatio)
        }
        
        // 在最佳宽高比组中选择最高分辨率
        return closestGroup?.value?.maxByOrNull { it.width * it.height } ?: sizes.first()
    }

    // SurfaceHolder.Callback 实现
    override fun surfaceCreated(holder: SurfaceHolder) {
        logger.i("Surface创建完成")
        if (mCamera != null) {
            startPreview()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        logger.i("Surface尺寸变化: ${width}x${height}")
        mAspectRatio = width.toFloat() / height.toFloat()
        
        // 重新选择最优尺寸
        if (mCamera != null) {
            selectOptimalSizes()
            updateCameraParameters()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        logger.i("Surface销毁")
        stopPreview()
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
            Toast.makeText(this, "权限申请失败，请手动授予相机权限", Toast.LENGTH_LONG).show()
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
                initCamera()
            } else {
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_LONG).show()
                showPermissionDeniedMessage()
            }
        }
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(this, "请在系统权限设置中手动授予相机权限", Toast.LENGTH_LONG).show()
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
        
        params.append("=== Camera1相机参数信息 ===\n\n")
        
        // 权限信息
        params.append("权限状态:\n")
        params.append("• 相机权限: ${if (checkPermissions()) "已授予" else "未授予"}\n\n")
        
        // 相机状态
        params.append("相机状态:\n")
        params.append("• 相机实例: ${if (mCamera != null) "已初始化" else "未初始化"}\n")
        params.append("• 预览状态: ${if (isPreview) "正在预览" else "未预览"}\n")
        params.append("• 相机ID: $cameraId (${if (cameraId == 0) "后置" else "前置"})\n\n")
        
        // 当前选择的尺寸
        params.append("当前设置:\n")
        params.append("• 图片尺寸: ${currentPictureSize?.toString() ?: "未选择"}\n")
        params.append("• 预览尺寸: ${currentPreviewSize?.toString() ?: "未选择"}\n")
        params.append("• 预览区域宽高比: $mAspectRatio\n\n")
        
        // 支持的尺寸信息
        params.append("支持的尺寸:\n")
        params.append("• 图片尺寸数量: ${supportedPictureSizes.size}\n")
        params.append("• 预览尺寸数量: ${supportedPreviewSizes.size}\n")
        if (supportedPictureSizes.isNotEmpty()) {
            params.append("• 最高图片分辨率: ${supportedPictureSizes.first().toString()}\n")
        }
        if (supportedPreviewSizes.isNotEmpty()) {
            params.append("• 最高预览分辨率: ${supportedPreviewSizes.first().toString()}\n")
        }
        params.append("\n")
        
        // 相机参数信息
        mCamera?.let { camera ->
            try {
                val parameters = camera.parameters
                params.append("相机参数:\n")
                params.append("• 预览格式: ${parameters.previewFormat}\n")
                params.append("• 图片格式: ${parameters.pictureFormat}\n")
                params.append("• 闪光灯模式: ${parameters.flashMode}\n")
                params.append("• 对焦模式: ${parameters.focusMode}\n")
                params.append("• 白平衡: ${parameters.whiteBalance}\n")
                params.append("• 场景模式: ${parameters.sceneMode}\n")
                params.append("\n")
            } catch (e: Exception) {
                params.append("相机参数获取失败: ${e.message}\n\n")
            }
        }
        
        // 设备信息
        params.append("设备信息:\n")
        params.append("• Android版本: ${android.os.Build.VERSION.RELEASE}\n")
        params.append("• API级别: ${android.os.Build.VERSION.SDK_INT}\n")
        params.append("• 设备型号: ${android.os.Build.MODEL}\n")
        params.append("• 制造商: ${android.os.Build.MANUFACTURER}\n")
        params.append("• 品牌: ${android.os.Build.BRAND}\n\n")
        
        // 应用信息
        params.append("应用信息:\n")
        params.append("• 包名: ${packageName}\n")
        params.append("• 版本名: ${try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "未知" }}\n")
        params.append("• 版本号: ${try { packageManager.getPackageInfo(packageName, 0).versionCode } catch (e: Exception) { "未知" }}\n\n")
        
        params.append("=== 参数信息结束 ===")
        
        return params.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.i("PhoneCameraActivity onDestroy")
        stopPreview()
        releaseCamera()
    }
}
