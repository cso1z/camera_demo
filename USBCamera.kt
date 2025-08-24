package com.svision.aiassistant.tools

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
import android.view.View
import androidx.core.content.FileProvider
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import com.svision.aiassistant.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class USBCam {
    lateinit var usbMonitor: USBMonitor
    var uvcCamera: UVCCamera? = null
    val TAG:String="USBCam"
    lateinit var mContext:Context
    lateinit var mSurface:SurfaceView
    fun initMonitor(context: Context, surface:SurfaceView){
        mContext=context
        mSurface=surface
        usbMonitor = USBMonitor(context, object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice?) {
                Log.d(TAG, "onAttach: 1")
                Log.d("USBDevice", "Device Attached: " + device?.getDeviceName());
                Log.d(
                    "USBDevice",
                    "hasPermission hasPermission: " + usbMonitor.hasPermission(device)
                )
                usbMonitor.requestPermission(device)
            }

            override fun onDetach(device: UsbDevice?) {
                Log.d(TAG, "onDetach: 2")
            }

            override fun onDeviceOpen(
                device: UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
                Log.d(TAG, "onDeviceOpen: 3")
                uvcCamera = UVCCamera(null)
                //uvcCamera?.startCapture(surface.holder.surface)
                uvcCamera?.apply {
                    open(ctrlBlock)
                    val sizes = supportedSizeList
                    for (item in sizes) {
                        Log.d(TAG, "onsize" + item.width + "/" + item.height)
                    }
                    val sizes2 = supportedSizeOne
                    if (sizes2 != null) {
                        Log.d(TAG, "sizes2:" + sizes2.width + "/" + sizes2.height)
                    }
                    if (supportedSize != null) {
                        Log.d(TAG, "supportedSize:" + supportedSize)
                    }
                    if (previewSize != null) {
                        Log.d(TAG, "previewSize:" + previewSize.width)
                    }
                    val formatlist = supportedFormatList
                    for (f in formatlist) {
                        Log.d(TAG, "formatlist:" + f.toString())
                    }
                    try {
                        if (supportedSize != null) {
                            Log.d(TAG, "setPreviewSize")
                            //val s1920 = sizes[0]
                            val s1280 = sizes[3]
                            setPreviewSize(s1280)
                        };
                        //uvcCamera.setPreviewSize(1280,720)
                    } catch (e: Error) {
                        Log.d(TAG, "Error:" + e.message)
                    }

                    setPreviewDisplay(surface.holder); // surfaceHolder 是你的 SurfaceView 的 SurfaceHolder
                    startPreview();
                }

            }

            override fun onDeviceClose(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                Log.d(TAG, "onDeviceClose: 4")
                preViewClose()
            }

            override fun onCancel(device: UsbDevice?) {
                Log.d(TAG, "onCancel: 5")
            }
        })
    }
    fun register(){
        usbMonitor.register()
    }
    //拍照预览
    fun captureImageFromPreview(callback: (v: Bitmap?) -> Unit): Bitmap? {

        val width = mSurface.width
        val height = mSurface.height

        if (width <= 0 || height <= 0) {
            return null
        }

        // 创建一个 Bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 将 SurfaceView 的内容绘制到 Bitmap 中
        mSurface.draw(canvas)
        try {
            // 使用 PixelCopy 捕获 SurfaceView 的内容
            PixelCopy.request(mSurface, bitmap, { copyResult ->
                if (copyResult == PixelCopy.SUCCESS) {
                    callback(bitmap)
                } else {
                    callback(null)
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            callback(null)
        }

        //saveBitmapToFile(bitmap)
        return bitmap
    }
    var currentUri: Uri = Uri.EMPTY
    fun fileToUri(file: File): Uri? {
        val uri = FileProvider.getUriForFile(
            mContext,
            mContext.packageName + ".provider",
            file
        )
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
            //readyImageViewShow(BitmapFactory.decodeFile(imgFile.absolutePath))
        } catch (e: IOException) {
            e.printStackTrace()
            //toastMsg("Failed to save photo")
        }
        return imgFile
    }
    private fun saveFileToStore(imgFile: File, bitmap: Bitmap) {
        val outputStream = FileOutputStream(imgFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        outputStream.flush()
        outputStream.close()
    }
    //第三方库如‌UVCCamera‌或‌AndroidUSBCamera‌：
    fun preViewStart() {
        // val uvcCamera:UVCCamera = UVCCamera(UVCParam(Size(1,640,320,21,null),50))
// 初始化 UVCCamera
        //uvcCamera = UVCCamera()
        usbMonitor.register()
    }
    fun preViewClose() {
        usbMonitor.unregister()
        uvcCamera?.apply {
            stopPreview();
            destroy();
            uvcCamera = null;
        }
        //关闭给回调
        //closeTakePhotoUI()
    }
}