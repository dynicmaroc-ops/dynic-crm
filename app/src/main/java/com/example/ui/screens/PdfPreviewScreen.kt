package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Company
import com.example.model.DocumentEntity
import com.example.model.DocumentStatus
import com.example.model.DocumentType
import com.example.model.DocumentWithLines
import com.example.model.FormatUtils
import com.example.model.PaymentEntity
import com.example.pdf.PdfGenerator
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DynicTealPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    documentWithLines: DocumentWithLines,
    company: Company,
    onEditDocument: (Long) -> Unit,
    onConvertDocument: (Long, DocumentType) -> Unit,
    onDuplicateDocument: (Long) -> Unit,
    onDeleteDocument: (DocumentEntity) -> Unit,
    onAddPayment: (PaymentEntity) -> Unit,
    onToggleStamp: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var pdfFile by remember { mutableStateOf<File?>(null) }
    var pageBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentAmount by remember {
        mutableStateOf(documentWithLines.document.resteAPayer.toString())
    }
    var paymentMode by remember { mutableStateOf("Virement") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun refreshPdf() {
        isLoading = true
        coroutineScope.launch {
            val file = withContext(Dispatchers.IO) {
                PdfGenerator.generatePdfFile(context, documentWithLines, company)
            }
            val bitmaps = withContext(Dispatchers.IO) {
                PdfGenerator.renderPdfToBitmaps(file)
            }
            pdfFile = file
            pageBitmaps = bitmaps
            isLoading = false
        }
    }

    LaunchedEffect(documentWithLines, company) {
        refreshPdf()
    }

    val doc = documentWithLines.document

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${doc.type.label} ${doc.numero}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = doc.tiersNom.ifBlank { "Client Divers" },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    // Share Button
                    IconButton(
                        onClick = {
                            pdfFile?.let { PdfGenerator.sharePdf(context, it) }
                        },
                        modifier = Modifier.testTag("action_share_pdf")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Partager", tint = DynicTealPrimary)
                    }

                    // Edit Button
                    IconButton(onClick = { onEditDocument(doc.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = DynicTealPrimary)
                    }

                    // Duplicate Button
                    IconButton(onClick = { onDuplicateDocument(doc.id) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Dupliquer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Conversions and quick action bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (doc.type == DocumentType.DEVIS) {
                        Button(
                            onClick = { onConvertDocument(doc.id, DocumentType.BON_LIVRAISON) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("En BL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onConvertDocument(doc.id, DocumentType.FACTURE) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DynicTealPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("En Facture", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (doc.type == DocumentType.BON_LIVRAISON) {
                        Button(
                            onClick = { onConvertDocument(doc.id, DocumentType.FACTURE) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DynicTealPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Facturer ce BL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (doc.type == DocumentType.FACTURE) {
                        if (doc.status != DocumentStatus.PAYE) {
                            Button(
                                onClick = { showPaymentDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paiement", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            pdfFile?.let { PdfGenerator.sharePdf(context, it) }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Partager", fontSize = 12.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFE8ECEB))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Document Status and Controls Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(status = doc.status)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = doc.date,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Cachet DYNIC", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = doc.avecCachet,
                                    onCheckedChange = {
                                        onToggleStamp(it)
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = DynicTealPrimary)
                                )
                            }
                        }

                        if (doc.type != DocumentType.BON_LIVRAISON) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Total TTC", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        FormatUtils.formatMoneyWithCurrency(doc.totalTTC),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = DynicTealPrimary
                                    )
                                }

                                if (doc.type == DocumentType.FACTURE) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Reste dû", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            FormatUtils.formatMoneyWithCurrency(doc.resteAPayer),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (doc.resteAPayer > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PDF Rendered Pages
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = DynicTealPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Génération du PDF DYNIC...", fontSize = 13.sp, color = Color.DarkGray)
                        }
                    }
                }
            } else {
                itemsIndexed(pageBitmaps) { pageIdx, bitmap ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Page ${pageIdx + 1} / ${pageBitmaps.size}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp)),
                            shadowElevation = 6.dp,
                            color = Color.White
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Page PDF ${pageIdx + 1}",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Danger Zone (Delete)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Supprimer ce document", fontSize = 12.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }

    // Modal: Record Payment
    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Enregistrer un paiement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Facture : ${doc.numero} • Client : ${doc.tiersNom}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = { Text("Montant reçu (DH)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = paymentMode,
                        onValueChange = { paymentMode = it },
                        label = { Text("Mode de paiement (Espèces, Virement, Chèque)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            onAddPayment(
                                PaymentEntity(
                                    documentId = doc.id,
                                    documentNumero = doc.numero,
                                    clientNom = doc.tiersNom,
                                    date = FormatUtils.currentDateFrench(),
                                    montant = amount,
                                    modePaiement = paymentMode
                                )
                            )
                        }
                        showPaymentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DynicTealPrimary)
                ) {
                    Text("Valider le paiement")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal: Delete Confirmation
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirmer la suppression") },
            text = { Text("Êtes-vous sûr de vouloir supprimer définitivement le document ${doc.numero} ?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteDocument(doc)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
