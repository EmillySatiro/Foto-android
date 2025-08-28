// Identificação do pacote do app
package com.example.foto_android

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.app.Activity
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import android.util.Log
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Caso necessário, pede a permissão da câmera
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0
            )
        }

        // Inicia o Jetpack Compose
        setContent {
            CameraApp()
        }
    }

    // Verifica se a permissão da câmera foi concedida
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}

// Função para enviar a imagem para o servidor
fun sendImageToServer(bitmap: Bitmap, ip: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL("http://$ip/upload")
            val connection = url.openConnection() as HttpURLConnection
            connection.doOutput = true
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "image/jpeg")

            // Converte o bitmap para JPEG
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val byteArray = stream.toByteArray()

            // Envia os bytes
            val outputStream: OutputStream = connection.outputStream
            outputStream.write(byteArray)
            outputStream.flush()
            outputStream.close()

            val responseCode = connection.responseCode
            Log.d("FotoAndroid", "Resposta do servidor: $responseCode")
            connection.disconnect()
        } catch (e: Exception) {
            Log.e("FotoAndroid", "Erro ao enviar imagem: ${e.message}")
        }
    }
}

// Função que desenha a tela (com compose)
@Composable
fun CameraApp() {
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var ipAddress by remember { mutableStateOf("192.168.0.100") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.getParcelable<Bitmap>("data")
            imageBitmap = bitmap
            // 👉 Não envia aqui, só guarda a imagem
        }
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

        Spacer(modifier = Modifier.height(16.dp))

        // Botão para tirar a foto
        Button(onClick = {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            launcher.launch(intent)
        }) {
            Text("Tirar Foto")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 👉 Botão para enviar só quando já existe imagem
        if (imageBitmap != null) {
            Button(onClick = {
                sendImageToServer(imageBitmap!!, ipAddress)
            }) {
                Text("Enviar para o Servidor")
            }
        }
    }
}
