package com.example.foto_android

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.Socket

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0
            )
        }
        setContent {
            CameraApp()
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}

suspend fun sendImageToServer(bitmap: Bitmap, ip: String, port: Int) {
    withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            Log.d("FotoAndroid", "Enviando imagem para $ip:$port")
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val byteArray = stream.toByteArray()

            socket = Socket(ip, port)
            val outputStream = DataOutputStream(socket.getOutputStream())

            outputStream.writeInt(byteArray.size)
            outputStream.write(byteArray)
            outputStream.flush()

            // Sinaliza que terminou de enviar os dados (para evitar que o socket feche antes do esperado)
            socket.shutdownOutput()
            Log.d("FotoAndroid", "Imagem enviada com sucesso")
        } catch (e: Exception) {
            Log.e("FotoAndroid", "Erro ao enviar imagem: ${e.message}", e)
        } finally {
            socket?.close()
            Log.d("FotoAndroid", "Socket fechado.")
        }
    }
}

// Função unificada para carregar e rotacionar o Bitmap a partir de uma Uri
fun getRotatedBitmapFromUri(context: Context, imageUri: Uri): Bitmap? {
    try {
        val orientation = context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            ExifInterface(inputStream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val originalBitmap = context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        } ?: return null

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return originalBitmap
        }

        return Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)

    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
}


@Composable
fun CameraApp() {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var ipAddress by remember { mutableStateOf("192.168.1.86") }
    var port by remember { mutableStateOf("5001") }
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    
    // Gatilho para forçar o recarregamento da imagem
    var triggerLoad by rememberSaveable { mutableStateOf(0) }
    
    val scope = rememberCoroutineScope()

    // Executado sempre que a tela aparece (ou "triggerLoad" mudar)
    LaunchedEffect(triggerLoad) {
        photoUri?.let { uri ->
            scope.launch(Dispatchers.IO) {
                Log.d("FotoAndroid", "Carregando bitmap da URI: $uri")
                val loadedBitmap = getRotatedBitmapFromUri(context, uri)
                withContext(Dispatchers.Main) {
                    imageBitmap = loadedBitmap
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Faz com que LaunchedEffect recarregue a imagem
            triggerLoad++
        }
    }

    fun createImageFile(): File {
        val storageDir: File = context.cacheDir
        return File.createTempFile("foto_", ".jpg", storageDir)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        imageBitmap?.let { img ->
            Image(
                bitmap = img.asImageBitmap(),
                contentDescription = "Foto Tirada",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("IP do servidor") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = port,
            onValueChange = { if (it.all { char -> char.isDigit() }) port = it },
            label = { Text("Porta do servidor") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val photoFile = createImageFile()
            // Define o local onde a câmera deve salvar a foto
            photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }
            launcher.launch(intent)
        }) {
            Text("Tirar Foto")
        }

        Spacer(modifier = Modifier.height(16.dp))

        val bitmapToSend = imageBitmap
        if (bitmapToSend != null) {
            val portInt = port.toIntOrNull() ?: 5001
            Button(onClick = {
                scope.launch {
                    sendImageToServer(bitmapToSend, ipAddress, portInt)
                }
            }) {
                Text("Enviar para o Servidor")
            }
        }
    }
}
