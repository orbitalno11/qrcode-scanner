package me.orbitalno11.qrscanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.android.synthetic.main.activity_qr_scanner.*
import me.orbitalno11.qrscanner.listener.ScannerListener
import me.orbitalno11.qrscanner.model.ScannerResponse
import me.orbitalno11.qrscanner.model.ScannerResult
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRScanner: AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        @JvmStatic
        fun createIntent(mContext: Context): Intent {
            return Intent(mContext, QRScanner::class.java)
        }
    }

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.createSurfaceProvider())
                }

            imageCapture = ImageCapture.Builder().build()

            val analyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRAnalyzer(QRScannerListener()))
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    analyzer
                )
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Can not access camera", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private inner class QRAnalyzer(private val listener: ScannerListener?) : ImageAnalysis.Analyzer {
        private val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        private var qrScanner = BarcodeScanning.getClient(options)

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()
            val data = ByteArray(remaining())
            get(data)
            return data
        }

        override fun analyze(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()

            val inputImage = InputImage.fromByteArray(
                data,
                image.width,
                image.height,
                image.imageInfo.rotationDegrees,
                InputImage.IMAGE_FORMAT_YV12
            )
            val result = qrScanner.process(inputImage)
            result.addOnSuccessListener { qrCodes ->
                for (code in qrCodes) {
                    val rawValue = code.rawValue
                    rawValue?.let {
                        listener?.onSuccess(rawValue)
                    }
                }
            }
            result.addOnFailureListener { error ->
                listener?.onFailure(error)
            }
            image.close()
        }
    }

    private inner class QRScannerListener: ScannerListener {
        override fun onSuccess(value: String?) {
            val result = Intent().apply {
                putExtra(ScannerResult.QR_SUCCESS_RESULT_CODE.value, value)
            }
            setResult(ScannerResponse.QR_RESULT_CODE.value, result)
            finish()
        }

        override fun onFailure(value: Exception) {
            Log.d(QRScanner::class.simpleName, value.toString())
        }
    }
}