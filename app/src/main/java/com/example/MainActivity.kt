package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.CrmRepository
import com.example.model.Company
import com.example.model.DocumentEntity
import com.example.model.DocumentType
import com.example.model.DocumentWithLines
import com.example.scan.SmartScanManager
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentEditScreen
import com.example.ui.screens.DocumentsScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.MoreSubScreen
import com.example.ui.screens.PdfPreviewScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.theme.DynicTealPrimary
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Accueil", Icons.Default.Home),
    DOCS("Documents", Icons.Default.Receipt),
    SCAN("Scanner", Icons.Default.DocumentScanner),
    CATALOG("Catalogue", Icons.Default.Inventory),
    MORE("Plus", Icons.Default.MoreHoriz)
}

enum class ActiveScreen {
    MAIN_TAB,
    DOC_EDIT,
    DOC_PREVIEW
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val repository = remember { CrmRepository(context) }
    val scanManager = remember { SmartScanManager(context, repository) }
    val coroutineScope = rememberCoroutineScope()

    // State collections from Room
    val company by repository.getCompany().collectAsState(initial = Company())
    val documents by repository.getDocuments().collectAsState(initial = emptyList())
    val products by repository.getProducts().collectAsState(initial = emptyList())
    val clients by repository.getClients().collectAsState(initial = emptyList())
    val suppliers by repository.getSuppliers().collectAsState(initial = emptyList())
    val expenses by repository.getExpenses().collectAsState(initial = emptyList())
    val payments by repository.getPayments().collectAsState(initial = emptyList())

    // First launch permission request state
    var showFirstLaunchPermissionDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        showFirstLaunchPermissionDialog = false
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            showFirstLaunchPermissionDialog = true
        }
    }

    // Navigation state
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var activeScreen by remember { mutableStateOf(ActiveScreen.MAIN_TAB) }
    var selectedDocId by remember { mutableStateOf<Long?>(null) }
    var newDocType by remember { mutableStateOf(DocumentType.DEVIS) }
    var nextGeneratedNumber by remember { mutableStateOf("") }
    var editingDocWithLines by remember { mutableStateOf<DocumentWithLines?>(null) }
    var previewDocWithLines by remember { mutableStateOf<DocumentWithLines?>(null) }
    var moreSubScreen by remember { mutableStateOf(MoreSubScreen.MENU) }

    fun navigateToNewDoc(type: DocumentType) {
        coroutineScope.launch {
            val num = repository.generateNextNumber(type)
            nextGeneratedNumber = num
            newDocType = type
            editingDocWithLines = null
            activeScreen = ActiveScreen.DOC_EDIT
        }
    }

    fun navigateToDocDetail(docId: Long) {
        coroutineScope.launch {
            val docWithLines = repository.getDocumentWithLines(docId)
            if (docWithLines != null) {
                previewDocWithLines = docWithLines
                selectedDocId = docId
                activeScreen = ActiveScreen.DOC_PREVIEW
            }
        }
    }

    fun navigateToDocEdit(docId: Long) {
        coroutineScope.launch {
            val docWithLines = repository.getDocumentWithLines(docId)
            if (docWithLines != null) {
                editingDocWithLines = docWithLines
                newDocType = docWithLines.document.type
                nextGeneratedNumber = docWithLines.document.numero
                activeScreen = ActiveScreen.DOC_EDIT
            }
        }
    }

    // Handle Android system back button
    BackHandler(enabled = activeScreen != ActiveScreen.MAIN_TAB || (currentTab == MainTab.MORE && moreSubScreen != MoreSubScreen.MENU)) {
        if (activeScreen == ActiveScreen.DOC_EDIT) {
            if (previewDocWithLines != null) {
                activeScreen = ActiveScreen.DOC_PREVIEW
            } else {
                activeScreen = ActiveScreen.MAIN_TAB
            }
        } else if (activeScreen == ActiveScreen.DOC_PREVIEW) {
            activeScreen = ActiveScreen.MAIN_TAB
        } else if (currentTab == MainTab.MORE && moreSubScreen != MoreSubScreen.MENU) {
            moreSubScreen = MoreSubScreen.MENU
        } else {
            currentTab = MainTab.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (activeScreen == ActiveScreen.MAIN_TAB) {
                Box {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = DynicTealPrimary,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        MainTab.values().forEach { tab ->
                            if (tab == MainTab.SCAN) {
                                // Empty spacer slot for prominent central floating button
                                NavigationBarItem(
                                    selected = currentTab == MainTab.SCAN,
                                    onClick = { currentTab = MainTab.SCAN },
                                    icon = {
                                        Box(modifier = Modifier.size(24.dp))
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            } else {
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DynicTealPrimary,
                                        selectedTextColor = DynicTealPrimary,
                                        indicatorColor = DynicTealPrimary.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }

                    // Centered Prominent Floating Scanner Button
                    FloatingActionButton(
                        onClick = { currentTab = MainTab.SCAN },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-14).dp)
                            .size(54.dp)
                            .testTag("central_scan_fab"),
                        shape = CircleShape,
                        containerColor = DynicTealPrimary,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Scanner",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeScreen) {
                ActiveScreen.DOC_EDIT -> {
                    DocumentEditScreen(
                        initialDocWithLines = editingDocWithLines,
                        initialType = newDocType,
                        nextGeneratedNumber = nextGeneratedNumber,
                        clients = clients,
                        catalogProducts = products,
                        onSaveDocument = { doc, lines ->
                            coroutineScope.launch {
                                val savedId = repository.saveDocument(doc, lines)
                                val savedDocWithLines = repository.getDocumentWithLines(savedId)
                                if (savedDocWithLines != null) {
                                    previewDocWithLines = savedDocWithLines
                                    selectedDocId = savedId
                                    activeScreen = ActiveScreen.DOC_PREVIEW
                                } else {
                                    activeScreen = ActiveScreen.MAIN_TAB
                                }
                            }
                        },
                        onBack = {
                            if (previewDocWithLines != null) {
                                activeScreen = ActiveScreen.DOC_PREVIEW
                            } else {
                                activeScreen = ActiveScreen.MAIN_TAB
                            }
                        }
                    )
                }

                ActiveScreen.DOC_PREVIEW -> {
                    val preview = previewDocWithLines
                    if (preview != null) {
                        PdfPreviewScreen(
                            documentWithLines = preview,
                            company = company ?: Company(),
                            onEditDocument = { docId ->
                                navigateToDocEdit(docId)
                            },
                            onConvertDocument = { docId, targetType ->
                                coroutineScope.launch {
                                    val newId = repository.convertDocument(docId, targetType)
                                    val newDocWithLines = repository.getDocumentWithLines(newId)
                                    if (newDocWithLines != null) {
                                        previewDocWithLines = newDocWithLines
                                        selectedDocId = newId
                                    }
                                }
                            },
                            onDuplicateDocument = { docId ->
                                coroutineScope.launch {
                                    val dupId = repository.duplicateDocument(docId)
                                    val dupDocWithLines = repository.getDocumentWithLines(dupId)
                                    if (dupDocWithLines != null) {
                                        previewDocWithLines = dupDocWithLines
                                        selectedDocId = dupId
                                    }
                                }
                            },
                            onDeleteDocument = { doc ->
                                coroutineScope.launch {
                                    repository.deleteDocument(doc)
                                    activeScreen = ActiveScreen.MAIN_TAB
                                }
                            },
                            onAddPayment = { payment ->
                                coroutineScope.launch {
                                    repository.addPayment(payment)
                                    selectedDocId?.let { id ->
                                        previewDocWithLines = repository.getDocumentWithLines(id)
                                    }
                                }
                            },
                            onToggleStamp = { stampEnabled ->
                                coroutineScope.launch {
                                    val updatedDoc = preview.document.copy(avecCachet = stampEnabled)
                                    repository.saveDocument(updatedDoc, preview.lines)
                                    previewDocWithLines = preview.copy(document = updatedDoc)
                                }
                            },
                            onBack = { activeScreen = ActiveScreen.MAIN_TAB }
                        )
                    }
                }

                ActiveScreen.MAIN_TAB -> {
                    when (currentTab) {
                        MainTab.HOME -> {
                            DashboardScreen(
                                company = company ?: Company(),
                                documents = documents,
                                expenses = expenses,
                                onNavigateToNewDoc = { type -> navigateToNewDoc(type) },
                                onNavigateToScan = { currentTab = MainTab.SCAN },
                                onNavigateToDocDetail = { docId -> navigateToDocDetail(docId) },
                                onNavigateToDocsList = { currentTab = MainTab.DOCS }
                            )
                        }

                        MainTab.DOCS -> {
                            DocumentsScreen(
                                documents = documents,
                                onNavigateToNewDoc = { type -> navigateToNewDoc(type) },
                                onNavigateToDocDetail = { docId -> navigateToDocDetail(docId) }
                            )
                        }

                        MainTab.SCAN -> {
                            ScanScreen(
                                scanManager = scanManager,
                                company = company ?: Company(),
                                onSaveExpense = { expense, supplier ->
                                    coroutineScope.launch {
                                        if (supplier.nom.isNotBlank()) {
                                            val existing = repository.findSupplierByIceOrNom(supplier.ice, supplier.nom)
                                            val suppId = existing?.id ?: repository.saveSupplier(supplier)
                                            repository.saveExpense(expense.copy(supplierId = suppId))
                                        } else {
                                            repository.saveExpense(expense)
                                        }
                                        currentTab = MainTab.HOME
                                    }
                                },
                                onSaveScannedProducts = { scannedProducts, supplier ->
                                    coroutineScope.launch {
                                        if (supplier.nom.isNotBlank()) {
                                            repository.saveSupplier(supplier)
                                        }
                                        for (p in scannedProducts) {
                                            val existing = repository.findProductByRefOrName(p.ref, p.designation)
                                            if (existing != null) {
                                                repository.updateProduct(existing.copy(stock = existing.stock + p.stock))
                                            } else {
                                                repository.saveProduct(p)
                                            }
                                        }
                                        currentTab = MainTab.CATALOG
                                    }
                                },
                                onBack = { currentTab = MainTab.HOME }
                            )
                        }

                        MainTab.CATALOG -> {
                            CatalogScreen(
                                products = products,
                                onSaveProduct = { p ->
                                    coroutineScope.launch {
                                        if (p.id == 0L) repository.saveProduct(p) else repository.updateProduct(p)
                                    }
                                },
                                onDeleteProduct = { p ->
                                    coroutineScope.launch { repository.deleteProduct(p) }
                                }
                            )
                        }

                        MainTab.MORE -> {
                            MoreHubScreen(
                                currentSubScreen = moreSubScreen,
                                company = company ?: Company(),
                                clients = clients,
                                suppliers = suppliers,
                                expenses = expenses,
                                payments = payments,
                                onSelectSubScreen = { moreSubScreen = it },
                                onSaveClient = { c ->
                                    coroutineScope.launch {
                                        if (c.id == 0L) repository.saveClient(c) else repository.updateClient(c)
                                    }
                                },
                                onDeleteClient = { c ->
                                    coroutineScope.launch { repository.deleteClient(c) }
                                },
                                onSaveSupplier = { s ->
                                    coroutineScope.launch {
                                        if (s.id == 0L) repository.saveSupplier(s) else repository.updateSupplier(s)
                                    }
                                },
                                onDeleteSupplier = { s ->
                                    coroutineScope.launch { repository.deleteSupplier(s) }
                                },
                                onSaveExpense = { exp ->
                                    coroutineScope.launch { repository.saveExpense(exp) }
                                },
                                onDeleteExpense = { exp ->
                                    coroutineScope.launch { repository.deleteExpense(exp) }
                                },
                                onSaveCompany = { comp ->
                                    coroutineScope.launch { repository.updateCompany(comp) }
                                }
                            )
                        }
                    }
                }
            }
        }

        // First Launch Permission Dialog
        if (showFirstLaunchPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showFirstLaunchPermissionDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = DynicTealPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Autorisation de l'appareil photo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "Pour numériser instantanément vos factures d'achat et vos bons de livraison (BL), DYNIC CRM Lite a besoin d'accéder à l'appareil photo de votre téléphone.\n\nL'application déterminera automatiquement le type du document et en extraira toutes les données.",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DynicTealPrimary)
                    ) {
                        Text("Autoriser l'appareil photo", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFirstLaunchPermissionDialog = false }) {
                        Text("Plus tard")
                    }
                }
            )
        }
    }
}
