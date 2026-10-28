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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.model.Client
import com.example.model.DocumentEntity
import com.example.model.DocumentLineEntity
import com.example.model.DocumentStatus
import com.example.model.DocumentType
import com.example.model.DocumentWithLines
import com.example.model.FormatUtils
import com.example.model.Product
import com.example.ui.theme.DynicTealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentEditScreen(
    initialDocWithLines: DocumentWithLines?,
    initialType: DocumentType,
    nextGeneratedNumber: String,
    clients: List<Client>,
    catalogProducts: List<Product>,
    onSaveDocument: (DocumentEntity, List<DocumentLineEntity>) -> Unit,
    onBack: () -> Unit
) {
    val isEdit = initialDocWithLines != null

    var selectedType by remember {
        mutableStateOf(initialDocWithLines?.document?.type ?: initialType)
    }

    var documentNumber by remember {
        mutableStateOf(initialDocWithLines?.document?.numero ?: nextGeneratedNumber)
    }

    var documentDate by remember {
        mutableStateOf(initialDocWithLines?.document?.date ?: FormatUtils.currentDateFrench())
    }

    var selectedClient by remember {
        val existingClientId = initialDocWithLines?.document?.clientId
        mutableStateOf(clients.find { it.id == existingClientId } ?: clients.firstOrNull())
    }

    var clientCustomName by remember {
        mutableStateOf(initialDocWithLines?.document?.tiersNom ?: (selectedClient?.nom ?: ""))
    }
    var clientCustomAddress by remember {
        mutableStateOf(initialDocWithLines?.document?.tiersAdresse ?: (selectedClient?.adresse ?: ""))
    }
    var clientCustomIce by remember {
        mutableStateOf(initialDocWithLines?.document?.tiersIce ?: (selectedClient?.ice ?: ""))
    }

    var tvaTaux by remember {
        mutableDoubleStateOf(initialDocWithLines?.document?.tvaTaux ?: 20.0)
    }

    var avecCachet by remember {
        mutableStateOf(initialDocWithLines?.document?.avecCachet ?: true)
    }

    var notes by remember {
        mutableStateOf(initialDocWithLines?.document?.notes ?: "Validité de l'offre : 30 jours. Règlement : 50% à la commande, solde à la livraison.")
    }

    val lines = remember {
        mutableStateListOf<DocumentLineEntity>().apply {
            if (initialDocWithLines != null) {
                addAll(initialDocWithLines.lines)
            }
        }
    }

    // Modal state for adding/editing a line
    var showLineEditorDialog by remember { mutableStateOf(false) }
    var lineEditingIndex by remember { mutableStateOf<Int?>(null) }
    var showCatalogPickerModal by remember { mutableStateOf(false) }

    // Line Editor form state
    var editingLineRef by remember { mutableStateOf("") }
    var editingLineDesignation by remember { mutableStateOf("") }
    var editingLineUm by remember { mutableStateOf("U") }
    var editingLineQte by remember { mutableStateOf("1") }
    var editingLinePu by remember { mutableStateOf("0") }

    // Computations
    val totalHT = lines.sumOf { (it.quantite * it.prixUnitaireHT) - it.remise }
    val totalTVA = totalHT * (tvaTaux / 100.0)
    val totalTTC = totalHT + totalTVA

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEdit) "Modifier ${selectedType.label}" else "Nouveau ${selectedType.label}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = DynicTealPrimary
                )
            )
        },
        bottomBar = {
            // Bottom Action Bar with live totals and save button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (selectedType != DocumentType.BON_LIVRAISON) {
                            Text(
                                text = "Total TTC",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = FormatUtils.formatMoneyWithCurrency(totalTTC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = DynicTealPrimary
                            )
                        } else {
                            Text(
                                text = "${lines.size} articles",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val docToSave = (initialDocWithLines?.document ?: DocumentEntity(
                                type = selectedType,
                                numero = documentNumber,
                                date = documentDate,
                                clientId = selectedClient?.id,
                                status = DocumentStatus.VALIDE
                            )).copy(
                                type = selectedType,
                                numero = documentNumber,
                                date = documentDate,
                                clientId = selectedClient?.id,
                                tiersNom = clientCustomName,
                                tiersAdresse = clientCustomAddress,
                                tiersIce = clientCustomIce,
                                tvaTaux = tvaTaux,
                                totalHT = totalHT,
                                totalTVA = totalTVA,
                                totalTTC = totalTTC,
                                avecCachet = avecCachet,
                                notes = notes
                            )
                            onSaveDocument(docToSave, lines.toList())
                        },
                        modifier = Modifier.testTag("button_save_document"),
                        colors = ButtonDefaults.buttonColors(containerColor = DynicTealPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enregistrer & Aperçu", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // General Header Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Informations Générales",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DynicTealPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Type dropdown
                        var typeExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedType.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Type de document") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false }
                            ) {
                                DocumentType.values().forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.label) },
                                        onClick = {
                                            selectedType = type
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = documentNumber,
                                onValueChange = { documentNumber = it },
                                label = { Text("Numéro") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = documentDate,
                                onValueChange = { documentDate = it },
                                label = { Text("Date") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Client Info Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Client / Destinataire",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = DynicTealPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Client Selector Dropdown
                        var clientExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = clientExpanded,
                            onExpandedChange = { clientExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = clientCustomName.ifBlank { "Sélectionner un client..." },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Client enregistré") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DynicTealPrimary) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = clientExpanded,
                                onDismissRequest = { clientExpanded = false }
                            ) {
                                clients.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.nom) },
                                        onClick = {
                                            selectedClient = c
                                            clientCustomName = c.nom
                                            clientCustomAddress = c.adresse
                                            clientCustomIce = c.ice
                                            clientExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = clientCustomName,
                            onValueChange = { clientCustomName = it },
                            label = { Text("Nom du client (sur le document)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = clientCustomAddress,
                            onValueChange = { clientCustomAddress = it },
                            label = { Text("Adresse") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = clientCustomIce,
                            onValueChange = { clientCustomIce = it },
                            label = { Text("Identifiant Commun de l'Entreprise (ICE)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Lines Header with Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Articles & Prestations (${lines.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showCatalogPickerModal = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Catalogue", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                editingLineRef = ""
                                editingLineDesignation = ""
                                editingLineUm = "U"
                                editingLineQte = "1"
                                editingLinePu = "0"
                                lineEditingIndex = null
                                showLineEditorDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DynicTealPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajouter", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Lines List
            if (lines.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucune ligne ajoutée. Cliquez sur 'Ajouter' ou 'Catalogue'.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(lines) { index, line ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = line.designation,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (line.ref.isNotBlank()) {
                                    Text(
                                        text = "Réf : ${line.ref}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    text = "${FormatUtils.formatMoney(line.quantite)} ${line.um} × ${FormatUtils.formatMoney(line.prixUnitaireHT)} DH",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (selectedType != DocumentType.BON_LIVRAISON) {
                                    Text(
                                        text = FormatUtils.formatMoneyWithCurrency(line.totalHT),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DynicTealPrimary
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingLineRef = line.ref
                                            editingLineDesignation = line.designation
                                            editingLineUm = line.um
                                            editingLineQte = line.quantite.toString()
                                            editingLinePu = line.prixUnitaireHT.toString()
                                            lineEditingIndex = index
                                            showLineEditorDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { lines.removeAt(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Document Options Card (Cachet, TVA, Notes)
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Options & Rendu PDF",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DynicTealPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Apposer le cachet officiel DYNIC", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Affiché sous les totaux sur le PDF", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = avecCachet,
                                onCheckedChange = { avecCachet = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = DynicTealPrimary)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        if (selectedType != DocumentType.BON_LIVRAISON) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Taux de TVA", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(20.0, 14.0, 10.0, 0.0).forEach { rate ->
                                        OutlinedButton(
                                            onClick = { tvaTaux = rate },
                                            colors = if (tvaTaux == rate) ButtonDefaults.outlinedButtonColors(containerColor = DynicTealPrimary.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors()
                                        ) {
                                            Text("${rate.toInt()}%", fontSize = 12.sp, color = if (tvaTaux == rate) DynicTealPrimary else Color.DarkGray)
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Conditions et mentions légales") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    // Modal: Line Editor Dialog
    if (showLineEditorDialog) {
        AlertDialog(
            onDismissRequest = { showLineEditorDialog = false },
            title = { Text(if (lineEditingIndex != null) "Modifier la ligne" else "Ajouter une ligne") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editingLineDesignation,
                        onValueChange = { editingLineDesignation = it },
                        label = { Text("Désignation *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editingLineRef,
                        onValueChange = { editingLineRef = it },
                        label = { Text("Référence (optionnel)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editingLineUm,
                            onValueChange = { editingLineUm = it },
                            label = { Text("UM") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editingLineQte,
                            onValueChange = { editingLineQte = it },
                            label = { Text("Quantité") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (selectedType != DocumentType.BON_LIVRAISON) {
                        OutlinedTextField(
                            value = editingLinePu,
                            onValueChange = { editingLinePu = it },
                            label = { Text("Prix Unitaire HT (DH)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qte = editingLineQte.replace(",", ".").toDoubleOrNull() ?: 1.0
                        val pu = editingLinePu.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val total = qte * pu
                        val newLine = DocumentLineEntity(
                            documentId = initialDocWithLines?.document?.id ?: 0L,
                            ref = editingLineRef,
                            designation = editingLineDesignation.ifBlank { "Article" },
                            um = editingLineUm.ifBlank { "U" },
                            quantite = qte,
                            prixUnitaireHT = pu,
                            totalHT = total
                        )

                        val index = lineEditingIndex
                        if (index != null && index in lines.indices) {
                            lines[index] = newLine
                        } else {
                            lines.add(newLine)
                        }
                        showLineEditorDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DynicTealPrimary)
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLineEditorDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal: Catalog Picker
    if (showCatalogPickerModal) {
        AlertDialog(
            onDismissRequest = { showCatalogPickerModal = false },
            title = { Text("Choisir depuis le Catalogue") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    itemsIndexed(catalogProducts) { _, prod ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    lines.add(
                                        DocumentLineEntity(
                                            documentId = initialDocWithLines?.document?.id ?: 0L,
                                            productId = prod.id,
                                            ref = prod.ref,
                                            designation = prod.designation,
                                            details = prod.details,
                                            um = prod.um,
                                            quantite = 1.0,
                                            prixUnitaireHT = prod.prixVenteHT,
                                            totalHT = prod.prixVenteHT
                                        )
                                    )
                                    showCatalogPickerModal = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(prod.designation, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Réf: ${prod.ref} • ${prod.um}", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        FormatUtils.formatMoneyWithCurrency(prod.prixVenteHT),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = DynicTealPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCatalogPickerModal = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}
