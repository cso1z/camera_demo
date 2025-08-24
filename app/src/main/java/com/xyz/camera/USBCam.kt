package com.xyz.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.core.content.FileProvider
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class USBCam {
    lateinit var usbMonitor: USBMonitor
    var uvcCamera: UVCCamera? = null
    val TAG:String="USBCam"
    lateinit var mContext:Context
    lateinit var mSurface:SurfaceView
    private lateinit var logger: Logger
    
    fun initMonitor(context: Context, surface:SurfaceView){
        mContext=context
        mSurface=surface
        logger = Logger(context)
        
        usbMonitor = USBMonitor(context, object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice?) {
                logger.d("onAttach: USB device attached")
                logger.d("Device Attached: ${device?.deviceName}")
                logger.d("hasPermission: ${usbMonitor.hasPermission(device)}")
                usbMonitor.requestPermission(device)
            }

            override fun onDetach(device: UsbDevice?) {
                logger.d("onDetach: USB device detached")
            }

            override fun onDeviceOpen(
                device: UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
                logger.d("onDeviceOpen: USB device opened")
                uvcCamera = UVCCamera(null)
                uvcCamera?.apply {
                    open(ctrlBlock)
                    val sizes = supportedSizeList
                    logger.d("Supported sizes count: ${sizes.size}")
                    for (item in sizes) {
                        logger.d("Supported size: ${item.width}x${item.height}")
                    }
                    val sizes2 = supportedSizeOne
                    if (sizes2 != null) {
                        logger.d("sizes2: ${sizes2.width}x${sizes2.height}")
                    }
                    if (supportedSize != null) {
                        logger.d("supportedSize: $supportedSize")
                    }
                    if (previewSize != null) {
                        logger.d("previewSize: ${previewSize.width}")
                    }
                    val formatlist = supportedFormatList
                    for (f in formatlist) {
                        logger.d("formatlist: $f")
                    }
                    try {
                        if (supportedSize != null) {
                            logger.d("setPreviewSize")
                            val s1280 = sizes[3]
                            setPreviewSize(s1280)
                        }
                    } catch (e: Error) {
                        logger.e("Error setting preview size: ${e.message}")
                    }

                    setPreviewDisplay(surface.holder)
                    startPreview()
                    logger.d("Preview started successfully")
                }
            }

            override fun onDeviceClose(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                logger.d("onDeviceClose: USB device closed")
                preViewClose()
            }

            override fun onCancel(device: UsbDevice?) {
                logger.d("onCancel: USB permission cancelled")
            }
        })
    }
    
    fun register(){
        logger.d("Registering USB monitor")
        usbMonitor.register()
    }
    
    //拍照预览
    fun captureImageFromPreview(callback: (v: Bitmap?) -> Unit): Bitmap? {
        logger.d("Starting image capture from preview")
        
        val width = mSurface.width
        val height = mSurface.height

        if (width <= 0 || height <= 0) {
            logger.e("Invalid surface dimensions: ${width}x${height}")
            return null
        }

        logger.d("Surface dimensions: ${width}x${height}")
        
        // 创建一个 Bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 将 SurfaceView 的内容绘制到 Bitmap 中
        mSurface.draw(canvas)
        try {
            // 使用 PixelCopy 捕获 SurfaceView 的内容
            PixelCopy.request(mSurface, bitmap, { copyResult ->
                if (copyResult == PixelCopy.SUCCESS) {
                    logger.d("Image capture successful")
                    callback(bitmap)
                } else {
                    logger.e("Image capture failed with result: $copyResult")
                    callback(null)
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: IllegalArgumentException) {
            logger.e("PixelCopy error: ${e.message}")
            callback(null)
        }

        return bitmap
    }
    
    var currentUri: Uri = Uri.EMPTY
    fun fileToUri(file: File): Uri? {
        val uri = FileProvider.getUriForFile(
            mContext,
            mContext.packageName + ".provider",
            file
        )
        logger.d("File converted to URI: $uri")
        return uri
    }
    
    var fileName=""
    private fun saveBitmapToFile(bitmap: Bitmap):File {
        fileName = "pre.jpg"
        val imgFile = File(mContext.externalCacheDir, fileName)
        try {
            saveFileToStore(imgFile, bitmap)
            fileToUri(imgFile)?.apply {
                currentUri = this
            }
            logger.d("Bitmap saved to file: ${imgFile.absolutePath}")
        } catch (e: IOException) {
            logger.e("Failed to save bitmap: ${e.message}")
        }
        return imgFile
    }
    
    private fun saveFileToStore(imgFile: File, bitmap: Bitmap) {
        val outputStream = FileOutputStream(imgFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        outputStream.flush()
        outputStream.close()
        logger.d("File saved to storage: ${imgFile.absolutePath}")
    }
    
    //第三方库如‌UVCCamera‌或‌AndroidUSBCamera‌：
    fun preViewStart() {
        logger.d("Starting preview")
        usbMonitor.register()
    }
    
    fun preViewClose() {
        logger.d("Closing preview")
        usbMonitor.unregister()
        uvcCamera?.apply {
            stopPreview()
            destroy()
            uvcCamera = null
        }
        logger.d("Preview closed and camera destroyed")
    }
}