package com.smartvault.app.ui.screens.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.smartvault.app.scanner.CardTextParser
import com.smartvault.app.scanner.ParsedCardData
import com.smartvault.app.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScanScreen(
    onNavigateBack: () -> Unit,
    onScanSuccess: (ParsedCardData, String?, String?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var scanSide by remember { mutableStateOf("FRONT") } // FRONT or BACK
    var frontImagePath by remember { mutableStateOf<String?>(null) }
    var backImagePath by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        val perm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
        if (perm == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            processImageUri(context, uri) { parsedData, savedPath ->
                isProcessing = false
                if (scanSide == "FRONT") {
                    frontImagePath = savedPath
                    onScanSuccess(parsedData, savedPath, null)
                } else {
                    backImagePath = savedPath
                    onScanSuccess(parsedData, frontImagePath, savedPath)
                }
            }
        }
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (scanSide == "FRONT") "Scan Card (Front Side)" else "Scan Card (Back Side)",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Align card edges within the frame",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
                    }
                },
                actions = {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Import Gallery", tint = IndigoLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianDark)
            )
        },
        containerColor = ObsidianDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Camera permission required", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Please allow camera access to scan physical cards.", color = TextSecondaryDark)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Text("Grant Permission")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Text("Pick from Gallery Instead", color = TextPrimaryDark)
                        }
                    }
                }
            }

            // Card Framing Overlay Guide Box (aspect ratio ~1.58 matching credit / business cards)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .border(
                            width = 2.dp,
                            color = if (isProcessing) EmeraldAccent else IndigoLight,
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    // Corner accents
                    Surface(
                        shape = RoundedCornerShape(topStart = 16.dp),
                        color = IndigoPrimary,
                        modifier = Modifier
                            .size(24.dp, 24.dp)
                            .align(Alignment.TopStart)
                    ) {}
                    Surface(
                        shape = RoundedCornerShape(topEnd = 16.dp),
                        color = IndigoPrimary,
                        modifier = Modifier
                            .size(24.dp, 24.dp)
                            .align(Alignment.TopEnd)
                    ) {}
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 16.dp),
                        color = IndigoPrimary,
                        modifier = Modifier
                            .size(24.dp, 24.dp)
                            .align(Alignment.BottomStart)
                    ) {}
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 16.dp),
                        color = IndigoPrimary,
                        modifier = Modifier
                            .size(24.dp, 24.dp)
                            .align(Alignment.BottomEnd)
                    ) {}
                }
            }

            // Capture Controls Bottom Bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(ObsidianDark.copy(alpha = 0.85f))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = IndigoPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Extracting card details with on-device OCR...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryDark
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery button
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(ObsidianSurface)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = TextPrimaryDark)
                        }

                        // Capture shutter button
                        Surface(
                            onClick = {
                                if (imageCapture != null && !isProcessing) {
                                    isProcessing = true
                                    val photoFile = File(
                                        context.filesDir,
                                        "cards/card_${System.currentTimeMillis()}_$scanSide.jpg"
                                    )
                                    photoFile.parentFile?.mkdirs()

                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                    val executor = ContextCompat.getMainExecutor(context)

                                    imageCapture!!.takePicture(
                                        outputOptions,
                                        executor,
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                runOcrOnFile(context, photoFile) { parsedData ->
                                                    isProcessing = false
                                                    if (scanSide == "FRONT") {
                                                        frontImagePath = photoFile.absolutePath
                                                        onScanSuccess(parsedData, photoFile.absolutePath, null)
                                                    } else {
                                                        backImagePath = photoFile.absolutePath
                                                        onScanSuccess(parsedData, frontImagePath, photoFile.absolutePath)
                                                    }
                                                }
                                            }

                                            override fun onError(exc: ImageCaptureException) {
                                                isProcessing = false
                                                Toast.makeText(context, "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            },
                            shape = CircleShape,
                            color = IndigoPrimary,
                            border = androidx.compose.foundation.BorderStroke(4.dp, Color.White),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }

                        // Flash / Switch side button
                        IconButton(
                            onClick = {
                                scanSide = if (scanSide == "FRONT") "BACK" else "FRONT"
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(ObsidianSurface)
                        ) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip Side", tint = TextPrimaryDark)
                        }
                    }
                }
            }
        }
    }
}

fun runOcrOnFile(context: Context, file: File, onComplete: (ParsedCardData) -> Unit) {
    try {
        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val parsed = CardTextParser.parse(visionText.text)
                onComplete(parsed)
            }
            .addOnFailureListener {
                onComplete(CardTextParser.parse(""))
            }
    } catch (e: Exception) {
        onComplete(CardTextParser.parse(""))
    }
}

fun processImageUri(context: Context, uri: Uri, onComplete: (ParsedCardData, String) -> Unit) {
    val photoFile = File(context.filesDir, "cards/imported_${System.currentTimeMillis()}.jpg")
    photoFile.parentFile?.mkdirs()

    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(photoFile).use { output ->
            input.copyTo(output)
        }
    }
    runOcrOnFile(context, photoFile) { parsed ->
        onComplete(parsed, photoFile.absolutePath)
    }
}
