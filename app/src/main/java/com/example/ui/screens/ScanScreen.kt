package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.model.Company
import com.example.model.ExpenseEntity
import com.example.model.FormatUtils
import com.example.model.Product
import com.example.model.Supplier
import com.example.pdf.PdfGenerator
import com.example.scan.DetectedDocType
import com.example.scan.LineAction
import com.example.scan.ScanExtractionResult
import com.example.scan.SmartScanManager
import com.example.ui.theme.DynicTealPrimary
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ScanScreen(
    scanManager: SmartScanManager,
    company: Company,
    onSaveExpense: (ExpenseEntity, Supplier) -> Unit,
    onSaveScannedProducts: (List<Product>, Supplier) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var extractionResult by remember { mutableStateOf<ScanExtractionResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionDeniedWarning by remember { mutableStateOf(false) }

    fun processCapturedImage(uri: Uri?, manualPreset: Int? = null) {
        isProcessing = true
        coroutineScope.launch {
            val res = scanManager.analyzeAndExtract(uri, manualPreset)
            extractionResult = res
            isProcessing = false
        }
    }

    // Camera take picture contract
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && currentPhotoUri != null) {
            processCapturedImage(currentPhotoUri)
        }
    }

    // Camera permission contract
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showPermissionDeniedWarning = false
            // Create target file and launch camera
            try {
                val photoFile = File(context.cacheDir, "photos/scan_${System.currentTimeMillis()}.jpg").apply {
                    parentFile?.mkdirs()
                }
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                currentPhotoUri = photoUri
                takePictureLauncher.launch(photoUri)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erreur création fichier photo : ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            showPermissionDeniedWarning = true
        }
    }

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val photoFile = File(context.cacheDir, "photos/scan_${System.currentTimeMillis()}.jpg").apply {
                    parentFile?.mkdirs()
                }
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                currentPhotoUri = photoUri
                takePictureLauncher.launch(photoUri)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erreur lancement caméra : ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            processCapturedImage(uri)
        }
    }

    if (extractionResult == null && !isProcessing) {
        // Step 1: Single-tap Scan Launch Screen
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                    Text(
                        text = "Scan Intelligent IA",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (showPermissionDeniedWarning) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permission Caméra requise",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFC62828)
                                )
                                Text(
                                    text = "L'accès à la caméra est nécessaire pour photographier vos factures et bons de livraison. Veuillez autoriser l'accès.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB71C1C)
                                )
                            }
                        }
                    }
                }
            }

            // Big Central Scan Hero Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(DynicTealPrimary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = DynicTealPrimary,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Photographier un document",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Un seul clic : l'application lance la caméra, détermine automatiquement si c'est une Facture ou un BL, et extrait toutes les informations.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Primary Action: Launch Camera
                        Button(
                            onClick = { launchCamera() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("button_open_camera"),
                            colors = ButtonDefaults.buttonColors(containerColor = DynicTealPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ouvrir la Caméra", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Secondary Action: Select from Gallery
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choisir une photo depuis la galerie", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Quick Demo Simulation Card (For instant testing in emulator)
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DynicTealPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tester avec des exemples de documents", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Simulez instantanément la prise de photo pour tester la détection automatique et l'extraction :",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { processCapturedImage(null, 1) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Facture Démo", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { processCapturedImage(null, 2) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("BL Réception", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    } else if (isProcessing) {
        // Step 2: Processing and Auto-Classification in progress
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                CircularProgressIndicator(
                    color = DynicTealPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Analyse & Détection Automatique...",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Identification du type (BL ou Facture) et extraction des données par IA",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else if (extractionResult != null) {
        // Step 3: Review, Auto-Detected Type Badge, and EXPORT
        val res = extractionResult!!
        var detectedType by remember { mutableStateOf(res.detectedType) }
        var supplierNom by remember { mutableStateOf(res.supplierNom) }
        var supplierIce by remember { mutableStateOf(res.supplierIce) }
        var docNumero by remember { mutableStateOf(res.numero) }
        var totalHT by remember { mutableStateOf(res.totalHT.toString()) }
        var totalTVA by remember { mutableStateOf(res.totalTVA.toString()) }
        var totalTTC by remember { mutableStateOf(res.totalTTC.toString()) }
        var categorie by remember { mutableStateOf(res.categorie) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Résultat du Scan",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (res.isRealOcrExtraction) "Extrait depuis votre photo" else "Extraction terminée (${(res.confidenceScore * 100).toInt()}% confiance)",
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    TextButton(onClick = { extractionResult = null }) {
                        Text("Nouveau Scan")
                    }
                }
            }

            // Automatic Classification Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = DynicTealPrimary.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DynicTealPrimary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = DynicTealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Type détecté automatiquement :",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(android.graphics.Color.parseColor(detectedType.badgeColorHex))
                            ) {
                                Text(
                                    text = detectedType.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Type override chips if user wants to change
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Changer le type :", fontSize = 11.sp, color = Color.Gray)
                            DetectedDocType.values().forEach { t ->
                                FilterChip(
                                    selected = detectedType == t,
                                    onClick = { detectedType = t },
                                    label = { Text(t.label, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DynicTealPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = DynicTealPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Raw OCR Text Viewer
            if (res.rawTextSummary.isNotBlank()) {
                item {
                    var showRawText by remember { mutableStateOf(false) }
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRawText = !showRawText },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DynicTealPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (res.isRealOcrExtraction) "Texte détecté sur votre photo" else "Texte de l'exemple",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = if (showRawText) "Masquer" else "Afficher le texte",
                                    fontSize = 11.sp,
                                    color = DynicTealPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (showRawText) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF6F8F7),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = res.rawTextSummary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = Color(0xFF263238),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Duplicate Alert Warning
            if (res.isDuplicate) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = res.duplicateMessage,
                                color = Color(0xFFBF360C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Extracted Information Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Informations du document", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DynicTealPrimary)

                        OutlinedTextField(
                            value = supplierNom,
                            onValueChange = { supplierNom = it },
                            label = { Text("Fournisseur / Tiers *") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = docNumero,
                                onValueChange = { docNumero = it },
                                label = { Text("Numéro") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = supplierIce,
                                onValueChange = { supplierIce = it },
                                label = { Text("ICE (15 chiffres)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = res.date,
                                onValueChange = { res.date = it },
                                label = { Text("Date") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = categorie,
                                onValueChange = { categorie = it },
                                label = { Text("Catégorie") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (detectedType != DetectedDocType.BON_LIVRAISON) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = totalHT,
                                    onValueChange = { totalHT = it },
                                    label = { Text("Total HT (DH)") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = totalTVA,
                                    onValueChange = { totalTVA = it },
                                    label = { Text("TVA 20% (DH)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = totalTTC,
                                onValueChange = { totalTTC = it },
                                label = { Text("Total TTC (DH) *") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Extracted Items / Lines
            if (res.lines.isNotEmpty()) {
                item {
                    Text(
                        text = "Articles détectés (${res.lines.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                itemsIndexed(res.lines) { _, line ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(line.designation, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${line.quantite} ${line.um}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Text(
                                text = "Réf: ${line.ref.ifBlank { "-" }} • PU: ${FormatUtils.formatMoneyWithCurrency(line.prixUnitaireHT)}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // EXPORT SECTION
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Actions & Exportation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DynicTealPrimary
                        )

                        // 1. Primary Save Action
                        Button(
                            onClick = {
                                val supplier = Supplier(
                                    nom = supplierNom.ifBlank { "Fournisseur Scan" },
                                    ice = supplierIce
                                )
                                if (detectedType == DetectedDocType.FACTURE) {
                                    val parsedHT = totalHT.replace(",", ".").toDoubleOrNull() ?: 0.0
                                    val parsedTVA = totalTVA.replace(",", ".").toDoubleOrNull() ?: 0.0
                                    val parsedTTC = totalTTC.replace(",", ".").toDoubleOrNull() ?: (parsedHT + parsedTVA)
                                    val expense = ExpenseEntity(
                                        date = res.date,
                                        supplierNom = supplierNom,
                                        categorie = categorie,
                                        montantHT = parsedHT,
                                        tvaTaux = 20.0,
                                        montantTVA = parsedTVA,
                                        montantTTC = parsedTTC,
                                        referenceFacture = docNumero,
                                        statut = "PAYEE"
                                    )
                                    onSaveExpense(expense, supplier)
                                } else {
                                    val products = res.lines.map {
                                        Product(
                                            ref = it.ref,
                                            designation = it.designation,
                                            um = it.um,
                                            prixAchatHT = it.prixUnitaireHT,
                                            prixVenteHT = it.prixUnitaireHT * 1.40,
                                            stock = it.quantite
                                        )
                                    }
                                    onSaveScannedProducts(products, supplier)
                                }
                                extractionResult = null
                                Toast.makeText(context, "Document enregistré avec succès !", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("button_save_scanned_doc"),
                            colors = ButtonDefaults.buttonColors(containerColor = DynicTealPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (detectedType == DetectedDocType.FACTURE) "Enregistrer dans les Dépenses" else "Enregistrer dans le Catalogue / Stock",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 2. Export to Official PDF
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val currentUpdated = res.copy(
                                        detectedType = detectedType,
                                        supplierNom = supplierNom,
                                        supplierIce = supplierIce,
                                        numero = docNumero,
                                        categorie = categorie,
                                        totalHT = totalHT.replace(",", ".").toDoubleOrNull() ?: res.totalHT,
                                        totalTVA = totalTVA.replace(",", ".").toDoubleOrNull() ?: res.totalTVA,
                                        totalTTC = totalTTC.replace(",", ".").toDoubleOrNull() ?: res.totalTTC
                                    )
                                    val pdfFile = scanManager.generateScanPdf(currentUpdated, company)
                                    PdfGenerator.sharePdf(context, pdfFile)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exporter & Partager en PDF", fontWeight = FontWeight.Bold)
                        }

                        // 3. Share text summary (WhatsApp / SMS / Mail)
                        OutlinedButton(
                            onClick = {
                                val currentUpdated = res.copy(
                                    detectedType = detectedType,
                                    supplierNom = supplierNom,
                                    supplierIce = supplierIce,
                                    numero = docNumero,
                                    categorie = categorie,
                                    totalHT = totalHT.replace(",", ".").toDoubleOrNull() ?: res.totalHT,
                                    totalTVA = totalTVA.replace(",", ".").toDoubleOrNull() ?: res.totalTVA,
                                    totalTTC = totalTTC.replace(",", ".").toDoubleOrNull() ?: res.totalTTC
                                )
                                val textSummary = scanManager.generateExportText(currentUpdated)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, textSummary)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Partager les informations du scan"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Partager le récapitulatif (WhatsApp / E-mail)")
                        }

                        // 4. Copy to clipboard
                        OutlinedButton(
                            onClick = {
                                val currentUpdated = res.copy(
                                    detectedType = detectedType,
                                    supplierNom = supplierNom,
                                    supplierIce = supplierIce,
                                    numero = docNumero,
                                    categorie = categorie,
                                    totalHT = totalHT.replace(",", ".").toDoubleOrNull() ?: res.totalHT,
                                    totalTVA = totalTVA.replace(",", ".").toDoubleOrNull() ?: res.totalTVA,
                                    totalTTC = totalTTC.replace(",", ".").toDoubleOrNull() ?: res.totalTTC
                                )
                                val textSummary = scanManager.generateExportText(currentUpdated)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Scan DYNIC", textSummary)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Informations copiées dans le presse-papier", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copier les données extraites")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }
}
