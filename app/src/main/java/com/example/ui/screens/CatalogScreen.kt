package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FormatUtils
import com.example.model.Product
import com.example.ui.components.SearchBar
import com.example.ui.theme.DynicTealPrimary

@Composable
fun CatalogScreen(
    products: List<Product>,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    // Dialog state
    var editRef by remember { mutableStateOf("") }
    var editDesignation by remember { mutableStateOf("") }
    var editDetails by remember { mutableStateOf("") }
    var editUm by remember { mutableStateOf("U") }
    var editPrixAchat by remember { mutableStateOf("0") }
    var editPrixVente by remember { mutableStateOf("0") }
    var editStock by remember { mutableStateOf("0") }

    fun openProductEditor(p: Product?) {
        productToEdit = p
        if (p != null) {
            editRef = p.ref
            editDesignation = p.designation
            editDetails = p.details
            editUm = p.um
            editPrixAchat = p.prixAchatHT.toString()
            editPrixVente = p.prixVenteHT.toString()
            editStock = p.stock.toString()
        } else {
            editRef = ""
            editDesignation = ""
            editDetails = ""
            editUm = "U"
            editPrixAchat = "0"
            editPrixVente = "0"
            editStock = "0"
        }
        showEditDialog = true
    }

    val filteredProducts = products.filter {
        searchQuery.isBlank() ||
                it.designation.contains(searchQuery, ignoreCase = true) ||
                it.ref.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "Catalogue & Articles",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Rechercher par référence, désignation..."
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun article trouvé.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts) { p ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openProductEditor(p) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = p.designation,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (p.ref.isNotBlank()) {
                                            Text(
                                                text = "Réf: ${p.ref} • Unité: ${p.um}",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        if (p.details.isNotBlank()) {
                                            Text(
                                                text = p.details,
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(
                                            onClick = { openProductEditor(p) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = DynicTealPrimary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { onDeleteProduct(p) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Prix Vente HT", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            FormatUtils.formatMoneyWithCurrency(p.prixVenteHT),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = DynicTealPrimary
                                        )
                                    }

                                    Column {
                                        Text("Prix Achat HT", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            FormatUtils.formatMoneyWithCurrency(p.prixAchatHT),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = Color.DarkGray
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Stock disponible", fontSize = 11.sp, color = Color.Gray)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (p.stock > 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                        ) {
                                            Text(
                                                text = "${FormatUtils.formatMoney(p.stock)} ${p.um}",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (p.stock > 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // FAB to add product
        FloatingActionButton(
            onClick = { openProductEditor(null) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_new_product"),
            containerColor = DynicTealPrimary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nouvel article")
        }
    }

    // Modal: Product Editor
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (productToEdit != null) "Modifier l'article" else "Nouvel article") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editDesignation,
                        onValueChange = { editDesignation = it },
                        label = { Text("Désignation *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editRef,
                            onValueChange = { editRef = it },
                            label = { Text("Référence") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editUm,
                            onValueChange = { editUm = it },
                            label = { Text("UM (U, M², ML...)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = editDetails,
                        onValueChange = { editDetails = it },
                        label = { Text("Détails / Caractéristiques") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editPrixVente,
                            onValueChange = { editPrixVente = it },
                            label = { Text("Prix Vente HT (DH)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editPrixAchat,
                            onValueChange = { editPrixAchat = it },
                            label = { Text("Prix Achat HT (DH)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = editStock,
                        onValueChange = { editStock = it },
                        label = { Text("Stock initial") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = (productToEdit ?: Product(designation = editDesignation)).copy(
                            ref = editRef,
                            designation = editDesignation.ifBlank { "Article" },
                            details = editDetails,
                            um = editUm.ifBlank { "U" },
                            prixVenteHT = editPrixVente.replace(",", ".").toDoubleOrNull() ?: 0.0,
                            prixAchatHT = editPrixAchat.replace(",", ".").toDoubleOrNull() ?: 0.0,
                            stock = editStock.replace(",", ".").toDoubleOrNull() ?: 0.0
                        )
                        onSaveProduct(p)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DynicTealPrimary)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
