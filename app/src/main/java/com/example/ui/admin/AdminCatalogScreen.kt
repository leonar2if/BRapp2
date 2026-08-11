package com.example.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.models.Product
import com.example.ui.components.PhotoPickerField
import com.example.ui.components.ProductCard
import com.example.ui.viewmodels.AdminViewModel

@Composable
fun AdminCatalogScreen(
    viewModel: AdminViewModel
) {
    val products by viewModel.allProducts.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gestión de Catálogo (${products.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f, fill = false),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            editingProduct = null
                            showDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar")
                        Spacer(modifier = Modifier.width(4.dp))
                        // Texto en una sola línea garantizado: el botón usa
                        // wrapContentWidth (crece según su contenido) en vez
                        // de un ancho fijo que forzaba el wrap ("Nu"/"evo").
                        Text("Nuevo", maxLines = 1, softWrap = false)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
            ) {
                items(products) { product ->
                    ProductCard(
                        product = product,
                        isAdmin = true,
                        onEdit = {
                            editingProduct = product
                            showDialog = true
                        },
                        onDelete = {
                            viewModel.deleteProduct(product.id)
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        var name by remember { mutableStateOf(editingProduct?.name ?: "") }
        var description by remember { mutableStateOf(editingProduct?.description ?: "") }
        var price by remember { mutableStateOf(editingProduct?.price?.toString() ?: "10.0") }
        var isActive by remember { mutableStateOf(editingProduct?.isActive ?: true) }
        // Bytes de la nueva foto elegida (1:1, ya recortada por PhotoPickerField).
        // Si es null y hay editingProduct, se conserva la imageUrl1 existente
        // (compatibilidad con productos que ya tenían una URL guardada).
        var newImageBytes by remember { mutableStateOf<ByteArray?>(null) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = if (editingProduct == null) "Agregar Producto" else "Editar Producto",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del producto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Precio (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PhotoPickerField(
                        existingImageUrl = editingProduct?.imageUrl1,
                        onImageSelected = { bytes -> newImageBytes = bytes }
                    )

                    if (editingProduct != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Producto Activo")
                            Switch(checked = isActive, onCheckedChange = { isActive = it })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && price.toDoubleOrNull() != null) {
                            val p = Product(
                                id = editingProduct?.id ?: 0L,
                                name = name,
                                description = description,
                                price = price.toDoubleOrNull() ?: 10.0,
                                imageUrl1 = editingProduct?.imageUrl1, // se sobreescribe con la URL nueva tras subir, si hay foto nueva
                                isActive = isActive
                            )
                            val filename = if (newImageBytes != null) {
                                "product_${System.currentTimeMillis()}.jpg"
                            } else null
                            viewModel.saveProduct(p, newImageBytes, filename)
                            showDialog = false
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
