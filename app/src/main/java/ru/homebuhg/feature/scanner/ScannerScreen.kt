package ru.homebuhg.feature.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onClose: () -> Unit,
    onScanned: (amountMinor: Long, dateMs: Long, note: String) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) viewModel.onPermissionDenied()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.scannedEvent.collect { receipt ->
            val note = "ФН:${receipt.fn} ФД:${receipt.fd} ФП:${receipt.fp}"
            onScanned(receipt.amountMinor, receipt.dateMs, note)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сканер чека ФНС") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                ScannerViewModel.UiState.NoCameraPermission -> NoCameraPermissionContent(onClose)
                ScannerViewModel.UiState.Scanning -> {
                    val previewView = remember { PreviewView(context) }

                    val barcodeScanner = remember {
                        BarcodeScanning.getClient(
                            BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                        )
                    }

                    DisposableEffect(lifecycleOwner) {
                        val executor = Executors.newSingleThreadExecutor()
                        var cameraProvider: ProcessCameraProvider? = null
                        val future = ProcessCameraProvider.getInstance(context)

                        future.addListener({
                            runCatching {
                                cameraProvider = future.get()
                                val preview = Preview.Builder().build().apply {
                                    surfaceProvider = previewView.surfaceProvider
                                }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build().apply {
                                        setAnalyzer(executor) { imageProxy ->
                                            processImageProxy(barcodeScanner, imageProxy) { raw ->
                                                viewModel.onBarcodeDetected(raw)
                                            }
                                        }
                                    }
                                cameraProvider?.unbindAll()
                                cameraProvider?.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis
                                )
                            }
                        }, ContextCompat.getMainExecutor(context))

                        onDispose {
                            cameraProvider?.unbindAll()
                            barcodeScanner.close()
                            executor.shutdown()
                        }
                    }

                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                    QrScannerOverlay(modifier = Modifier.fillMaxSize())

                    Text(
                        text = "Наведите камеру на QR-код чека",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 40.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoCameraPermissionContent(onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Доступ к камере не предоставлен",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Разрешите доступ к камере в настройках приложения",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onClose) { Text("Закрыть") }
        }
    }
}

@Composable
private fun QrScannerOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val squareSize = minOf(size.width, size.height) * 0.65f
        val left = (size.width - squareSize) / 2f
        val top = (size.height - squareSize) / 2f

        val overlayPath = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, size.width, size.height))
            addRect(Rect(left, top, left + squareSize, top + squareSize))
        }
        drawPath(overlayPath, Color.Black.copy(alpha = 0.55f))

        val corner = squareSize * 0.1f
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        val white = Color.White

        // Верхний-левый
        drawLine(white, Offset(left, top + corner), Offset(left, top), stroke)
        drawLine(white, Offset(left, top), Offset(left + corner, top), stroke)
        // Верхний-правый
        drawLine(white, Offset(left + squareSize - corner, top), Offset(left + squareSize, top), stroke)
        drawLine(white, Offset(left + squareSize, top), Offset(left + squareSize, top + corner), stroke)
        // Нижний-левый
        drawLine(white, Offset(left, top + squareSize - corner), Offset(left, top + squareSize), stroke)
        drawLine(white, Offset(left, top + squareSize), Offset(left + corner, top + squareSize), stroke)
        // Нижний-правый
        drawLine(white, Offset(left + squareSize - corner, top + squareSize), Offset(left + squareSize, top + squareSize), stroke)
        drawLine(white, Offset(left + squareSize, top + squareSize), Offset(left + squareSize, top + squareSize - corner), stroke)
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun processImageProxy(
    scanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.rawValue?.let(onDetected)
        }
        .addOnCompleteListener { imageProxy.close() }
}
