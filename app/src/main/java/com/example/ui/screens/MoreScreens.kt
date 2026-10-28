package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Client
import com.example.model.Company
import com.example.model.ExpenseEntity
import com.example.model.FormatUtils
import com.example.model.PaymentEntity
import com.example.model.Supplier
import com.example.ui.theme.DynicTealPrimary

enum class MoreSubScreen {
    MENU,
    CLIENTS,
    SUPPLIERS,
    EXPENSES,
    PAYMENTS,
    SETTINGS
}

@Composable
fun MoreHubScreen(
    currentSubScreen: MoreSubScreen,
    company: Company,
    clients: List<Client>,
    suppliers: List<Supplier>,
    expenses: List<ExpenseEntity>,
    payments: List<PaymentEntity>,
    onSelectSubScreen: (MoreSubScreen) -> Unit,
    onSaveClient: (Client) -> Unit,
    onDeleteClient: (Client) -> Unit,
    onSaveSupplier: (Supplier) -> Unit,
    onDeleteSupplier: (Supplier) -> Unit,
    onSaveExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onSaveCompany: (Company) -> Unit
) {
    when (currentSubScreen) {
        MoreSubScreen.MENU -> {
            MoreMenuScreen(onSelectSubScreen = onSelectSubScreen)
        }
        MoreSubScreen.CLIENTS -> {
            ClientsListScreen(
                clients = clients,
                onSaveClient = onSaveClient,
                onDeleteClient = onDeleteClient,
                onBack = { onSelectSubScreen(MoreSubScreen.MENU) }
            )
        }
        MoreSubScreen.SUPPLIERS -> {
            SuppliersListScreen(
                suppliers = suppliers,
                onSaveSupplier = onSaveSupplier,
                onDeleteSupplier = onDeleteSupplier,
                onBack = { onSelectSubScreen(MoreSubScreen.MENU) }
            )
        }
        MoreSubScreen.EXPENSES -> {
            ExpensesListScreen(
                expenses = expenses,
                onSaveExpense = onSaveExpense,
                onDeleteExpense = onDeleteExpense,
                onBack = { onSelectSubScreen(MoreSubScreen.MENU) }
            )
        }
        MoreSubScreen.PAYMENTS -> {
            PaymentsListScreen(
                payments = payments,
                onBack = { onSelectSubScreen(MoreSubScreen.MENU) }
            )
        }
        MoreSubScreen.SETTINGS -> {
            CompanySettingsScreen(
                company = company,
                onSaveCompany = onSaveCompany,
                onBack = { onSelectSubScreen(MoreSubScreen.MENU) }
            )
        }
    }
}

@Composable
fun MoreMenuScreen(onSelectSubScreen: (MoreSubScreen) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Plus de fonctionnalités",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            MoreMenuItem(
                title = "Fichier Clients",
                subtitle = "Fiches contacts, ICE, devis & historique des ventes",
                icon = Icons.Default.People,
                color = DynicTealPrimary,
                onClick = { onSelectSubScreen(MoreSubScreen.CLIENTS) }
            )
        }

        item {
            MoreMenuItem(
                title = "Fournisseurs",
                subtitle = "Gestion des partenaires, ICE et coordonnées",
                icon = Icons.Default.Business,
                color = Color(0xFF00796B),
                onClick = { onSelectSubScreen(MoreSubScreen.SUPPLIERS) }
            )
        }

        item {
            MoreMenuItem(
                title = "Dépenses & Achats",
                subtitle = "Suivi des charges, factures d'achat et TVA déductible",
                icon = Icons.Default.Receipt,
                color = Color(0xFFE65100),
                onClick = { onSelectSubScreen(MoreSubScreen.EXPENSES) }
            )
        }

        item {
            MoreMenuItem(
                title = "Règlements & Paiements",
                subtitle = "Historique des encaissements reçus des clients",
                icon = Icons.Default.Payment,
                color = Color(0xFF2E7D32),
                onClick = { onSelectSubScreen(MoreSubScreen.PAYMENTS) }
            )
        }

        item {
            MoreMenuItem(
                title = "Paramètres de la Société",
                subtitle = "Identité légale DYNIC (ICE, RC, IF, Patente, Banque) et numérotation",
                icon = Icons.Default.Settings,
                color = Color(0xFF5E35B1),
                onClick = { onSelectSubScreen(MoreSubScreen.SETTINGS) }
            )
        }
    }
}

@Composable
fun MoreMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }

            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.LightGray)
        }
    }
}

// 1. Clients List Screen
@Composable
fun ClientsListScreen(
    clients: List<Client>,
    onSaveClient: (Client) -> Unit,
    onDeleteClient: (Client) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var clientToEdit by remember { mutableStateOf<Client?>(null) }

    var nom by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var ice by remember { mutableStateOf("") }
    var tel by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    fun openDialog(c: Client?) {
        clientToEdit = c
        nom = c?.nom ?: ""
        adresse = c?.adresse ?: ""
        ice = c?.ice ?: ""
        tel = c?.telephone ?: ""
        email = c?.email ?: ""
        showDialog = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Clients (${clients.size})", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) { Text("Retour") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(clients) { c ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(c.nom, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    if (c.adresse.isNotBlank()) Text(c.adresse, fontSize = 12.sp, color = Color.DarkGray)
                                    if (c.ice.isNotBlank()) Text("ICE: ${c.ice}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Row {
                                    IconButton(onClick = { openDialog(c) }) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = DynicTealPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onDeleteClient(c) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            if (c.telephone.isNotBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Call button
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.telephone}"))
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DynicTealPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Appeler", fontSize = 12.sp)
                                    }

                                    // WhatsApp button
                                    Button(
                                        onClick = {
                                            val cleanPhone = c.telephone.replace(" ", "").replace("+", "")
                                            val url = "https://api.whatsapp.com/send?phone=$cleanPhone"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("WhatsApp", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { openDialog(null) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = DynicTealPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nouveau client")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (clientToEdit != null) "Modifier le client" else "Nouveau client") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom du client *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = adresse, onValueChange = { adresse = it }, label = { Text("Adresse") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ice, onValueChange = { ice = it }, label = { Text("ICE (15 chiffres)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tel, onValueChange = { tel = it }, label = { Text("Téléphone (ex: 06 61 22 33 44)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val c = (clientToEdit ?: Client(nom = nom)).copy(
                            nom = nom.ifBlank { "Client" },
                            adresse = adresse,
                            ice = ice,
                            telephone = tel,
                            email = email
                        )
                        onSaveClient(c)
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DynicTealPrimary)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Annuler") }
            }
        )
    }
}

// 2. Suppliers List Screen
@Composable
fun SuppliersListScreen(
    suppliers: List<Supplier>,
    onSaveSupplier: (Supplier) -> Unit,
    onDeleteSupplier: (Supplier) -> Unit,
    onBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var supplierToEdit by remember { mutableStateOf<Supplier?>(null) }
    var nom by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var ice by remember { mutableStateOf("") }
    var tel by remember { mutableStateOf("") }

    fun openDialog(s: Supplier?) {
        supplierToEdit = s
        nom = s?.nom ?: ""
        adresse = s?.adresse ?: ""
        ice = s?.ice ?: ""
        tel = s?.telephone ?: ""
        showDialog = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fournisseurs (${suppliers.size})", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) { Text("Retour") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(suppliers) { s ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.nom, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (s.adresse.isNotBlank()) Text(s.adresse, fontSize = 12.sp, color = Color.DarkGray)
                                if (s.ice.isNotBlank()) Text("ICE: ${s.ice}", fontSize = 11.sp, color = Color.Gray)
                                if (s.telephone.isNotBlank()) Text("Tél: ${s.telephone}", fontSize = 11.sp, color = DynicTealPrimary)
                            }
                            Row {
                                IconButton(onClick = { openDialog(s) }) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = DynicTealPrimary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDeleteSupplier(s) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { openDialog(null) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = DynicTealPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nouveau fournisseur")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (supplierToEdit != null) "Modifier le fournisseur" else "Nouveau fournisseur") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom de la société *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = adresse, onValueChange = { adresse = it }, label = { Text("Adresse") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ice, onValueChange = { ice = it }, label = { Text("ICE") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tel, onValueChange = { tel = it }, label = { Text("Téléphone") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = (supplierToEdit ?: Supplier(nom = nom)).copy(
                            nom = nom.ifBlank { "Fournisseur" },
                            adresse = adresse,
                            ice = ice,
                            telephone = tel
                        )
                        onSaveSupplier(s)
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DynicTealPrimary)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Annuler") }
            }
        )
    }
}

// 3. Expenses List Screen
@Composable
fun ExpensesListScreen(
    expenses: List<ExpenseEntity>,
    onSaveExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var fournisseur by remember { mutableStateOf("") }
    var categorie by remember { mutableStateOf("Matière première") }
    var montantHT by remember { mutableStateOf("0") }
    var refFacture by remember { mutableStateOf("") }

    val totalExpensesTTC = expenses.sumOf { it.montantTTC }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dépenses & Achats", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Total TTC: ${FormatUtils.formatMoneyWithCurrency(totalExpensesTTC)}", fontSize = 13.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onBack) { Text("Retour") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(expenses) { exp ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exp.supplierNom, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Catégorie : ${exp.categorie}", fontSize = 12.sp, color = Color.Gray)
                                Text("${exp.date} • Réf: ${exp.referenceFacture.ifBlank { "-" }}", fontSize = 11.sp, color = Color.DarkGray)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    FormatUtils.formatMoneyWithCurrency(exp.montantTTC),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFE65100)
                                )
                                IconButton(onClick = { onDeleteExpense(exp) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = {
                fournisseur = ""
                categorie = "Matière première"
                montantHT = "0"
                refFacture = ""
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = Color(0xFFE65100),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nouvelle dépense")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nouvelle Dépense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fournisseur, onValueChange = { fournisseur = it }, label = { Text("Fournisseur *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = categorie, onValueChange = { categorie = it }, label = { Text("Catégorie (Matière, Transport, Loyer...)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = montantHT, onValueChange = { montantHT = it }, label = { Text("Montant HT (DH) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = refFacture, onValueChange = { refFacture = it }, label = { Text("N° de Facture Fournisseur") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ht = montantHT.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val tva = ht * 0.20
                        val ttc = ht + tva
                        onSaveExpense(
                            ExpenseEntity(
                                date = FormatUtils.currentDateFrench(),
                                supplierNom = fournisseur.ifBlank { "Fournisseur Divers" },
                                categorie = categorie,
                                montantHT = ht,
                                tvaTaux = 20.0,
                                montantTVA = tva,
                                montantTTC = ttc,
                                referenceFacture = refFacture
                            )
                        )
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Annuler") }
            }
        )
    }
}

// 4. Payments List Screen
@Composable
fun PaymentsListScreen(
    payments: List<PaymentEntity>,
    onBack: () -> Unit
) {
    val totalReceived = payments.sumOf { it.montant }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Encaissements & Règlements", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Total encaissé : ${FormatUtils.formatMoneyWithCurrency(totalReceived)}", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onBack) { Text("Retour") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (payments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucun paiement enregistré pour le moment.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(payments) { p ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(p.clientNom, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Facture N° ${p.documentNumero} • ${p.modePaiement}", fontSize = 12.sp, color = Color.Gray)
                                Text(p.date, fontSize = 11.sp, color = Color.DarkGray)
                            }
                            Text(
                                FormatUtils.formatMoneyWithCurrency(p.montant),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 5. Company Settings Screen
@Composable
fun CompanySettingsScreen(
    company: Company,
    onSaveCompany: (Company) -> Unit,
    onBack: () -> Unit
) {
    var nom by remember { mutableStateOf(company.nom) }
    var slogan by remember { mutableStateOf(company.slogan) }
    var adresse by remember { mutableStateOf(company.adresse) }
    var ville by remember { mutableStateOf(company.ville) }
    var fixe by remember { mutableStateOf(company.fixe) }
    var mobile1 by remember { mutableStateOf(company.mobile1) }
    var mobile2 by remember { mutableStateOf(company.mobile2) }
    var capital by remember { mutableStateOf(company.capital) }
    var rc by remember { mutableStateOf(company.rc) }
    var patente by remember { mutableStateOf(company.patente) }
    var ifiscal by remember { mutableStateOf(company.ifiscal) }
    var ice by remember { mutableStateOf(company.ice) }
    var banqueRib by remember { mutableStateOf(company.banqueRib) }
    var mail by remember { mutableStateOf(company.mail) }
    var web by remember { mutableStateOf(company.web) }
    var prefixeDevis by remember { mutableStateOf(company.prefixeDevis) }
    var prefixeBL by remember { mutableStateOf(company.prefixeBL) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Paramètres Société", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) { Text("Retour") }
            }
        }

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
                    Text("Identité Juridique DYNIC", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DynicTealPrimary)

                    OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Raison Sociale") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = slogan, onValueChange = { slogan = it }, label = { Text("Slogan") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = adresse, onValueChange = { adresse = it }, label = { Text("Adresse Siège") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ville, onValueChange = { ville = it }, label = { Text("Ville") }, modifier = Modifier.fillMaxWidth())

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = ice, onValueChange = { ice = it }, label = { Text("ICE") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = rc, onValueChange = { rc = it }, label = { Text("RC") }, modifier = Modifier.weight(1f))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = patente, onValueChange = { patente = it }, label = { Text("Patente") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = ifiscal, onValueChange = { ifiscal = it }, label = { Text("IF") }, modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(value = capital, onValueChange = { capital = it }, label = { Text("Capital Social") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = banqueRib, onValueChange = { banqueRib = it }, label = { Text("Coordonnées Bancaires & RIB") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fixe, onValueChange = { fixe = it }, label = { Text("Téléphone Fixe") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mobile1, onValueChange = { mobile1 = it }, label = { Text("Mobile 1") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mobile2, onValueChange = { mobile2 = it }, label = { Text("Mobile 2") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mail, onValueChange = { mail = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = web, onValueChange = { web = it }, label = { Text("Site Web") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

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
                    Text("Numérotation & Séries", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DynicTealPrimary)

                    OutlinedTextField(value = prefixeDevis, onValueChange = { prefixeDevis = it }, label = { Text("Préfixe Devis (ex: TP-27)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = prefixeBL, onValueChange = { prefixeBL = it }, label = { Text("Préfixe BL (ex: TD-26)") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        item {
            Button(
                onClick = {
                    val updated = company.copy(
                        nom = nom,
                        slogan = slogan,
                        adresse = adresse,
                        ville = ville,
                        fixe = fixe,
                        mobile1 = mobile1,
                        mobile2 = mobile2,
                        capital = capital,
                        rc = rc,
                        patente = patente,
                        ifiscal = ifiscal,
                        ice = ice,
                        banqueRib = banqueRib,
                        mail = mail,
                        web = web,
                        prefixeDevis = prefixeDevis,
                        prefixeBL = prefixeBL
                    )
                    onSaveCompany(updated)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DynicTealPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enregistrer les modifications", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
