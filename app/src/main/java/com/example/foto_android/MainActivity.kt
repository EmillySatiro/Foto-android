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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pedir permissão de câmera se necessário
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

@Composable
fun CameraApp() {
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var ipAddress by remember { mutableStateOf("192.168.0.100") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as Bitmap
            imageBitmap = bitmap
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        // Alinhamento no centro (vertical e horizontal)
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
            // Espaço entre imagem e campo
            Spacer(modifier = Modifier.height(16.dp)) 
        }

        OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("IP do servidor") },
            modifier = Modifier.fillMaxWidth()
        )

        // Espaço entre campo e botão
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            launcher.launch(intent)
        }) {
            Text("Tirar Foto e Enviar")
        }

    }
}
