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
import java.io.DataOutputStream
import java.net.Socket
import java.nio.ByteBuffer

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


// Dentro da função sendImageToServer
fun sendImageToServer(bitmap: Bitmap, ip: String, port: Int = 5001) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            Log.d("FotoAndroid", "Iniciando envio para IP: $ip na porta: $port")
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val byteArray = stream.toByteArray()

            Log.d("FotoAndroid", "Tamanho da imagem em bytes: ${byteArray.size}")

            val socket = Socket(ip, port)
            Log.d("FotoAndroid", "Conexão com o servidor estabelecida.")

            val outputStream = DataOutputStream(socket.getOutputStream())
            outputStream.writeInt(byteArray.size) // Use writeInt para simplificar
            outputStream.write(byteArray)
            outputStream.flush()

            socket.close()
            Log.d("FotoAndroid", "Imagem enviada com sucesso!")
        } catch (e: Exception) {
            Log.e("FotoAndroid", "Erro ao enviar imagem: ${e.message}", e)
        }
    }
}
// Função que desenha a tela (com compose)
@Composable
fun CameraApp() {
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var ipAddress by remember { mutableStateOf("10.180.43.210") }

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
