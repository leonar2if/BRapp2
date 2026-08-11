package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Selector de foto para productos/servicios del catálogo (sección 19 del
 * prompt maestro): reemplaza el campo "URL de imagen" por un flujo de
 * seleccionar-desde-galería + previsualizar + recortar a 1:1.
 *
 * - `existingImageUrl`: URL ya guardada (para editar un producto existente;
 *   se sigue usando esa columna/URL, no se rompe compatibilidad).
 * - `onImageSelected`: entrega los bytes JPEG ya recortados 1:1, listos para
 *   subir con el pipeline existente (ProductService.createProduct /
 *   AdminViewModel.saveProduct), o null si el usuario borra la selección.
 */
@Composable
fun PhotoPickerField(
    existingImageUrl: String?,
    onImageSelected: (ByteArray?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hasNewSelection by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val cropped = loadAndCropSquare(context, uri)
            if (cropped != null) {
                previewBitmap = cropped
                hasNewSelection = true
                onImageSelected(bitmapToJpegBytes(cropped))
            }
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "Foto",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable {
                    launcher.launch(ActivityResultContracts.PickVisualMedia.Request(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                previewBitmap != null -> {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Foto seleccionada (1:1)",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                !existingImageUrl.isNullOrBlank() -> {
                    AsyncImage(
                        model = existingImageUrl,
                        contentDescription = "Foto actual",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Toca para elegir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Formato cuadrado 1:1. Se recorta automáticamente.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (hasNewSelection) {
            TextButton(onClick = {
                previewBitmap = null
                hasNewSelection = false
                onImageSelected(null)
            }) {
                Text("Quitar selección")
            }
        }
    }
}

/** Carga la imagen desde la Uri y la recorta centrada a un cuadrado 1:1. */
private fun loadAndCropSquare(context: Context, uri: Uri): Bitmap? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(input)
        input.close()
        if (original == null) return null

        val size = min(original.width, original.height)
        val x = (original.width - size) / 2
        val y = (original.height - size) / 2
        val squared = Bitmap.createBitmap(original, x, y, size, size)

        // Limita el lado máximo para no subir imágenes gigantes (razonable
        // para catálogo): 1080x1080 como pide el prompt maestro.
        if (squared.width > 1080) {
            Bitmap.createScaledBitmap(squared, 1080, 1080, true)
        } else {
            squared
        }
    } catch (e: Exception) {
        null
    }
}

private fun bitmapToJpegBytes(bitmap: Bitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    return stream.toByteArray()
}
