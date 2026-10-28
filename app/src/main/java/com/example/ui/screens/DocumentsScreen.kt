package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DocumentEntity
import com.example.model.DocumentStatus
import com.example.model.DocumentType
import com.example.ui.components.SearchBar
import com.example.ui.theme.DynicTealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    documents: List<DocumentEntity>,
    onNavigateToNewDoc: (DocumentType) -> Unit,
    onNavigateToDocDetail: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedStatusFilter by remember { mutableStateOf<DocumentStatus?>(null) }

    val tabTitles = listOf("Tous", "Devis", "BL", "Factures", "Commandes")

    // Filter documents
    val filteredDocs = documents.filter { doc ->
        val matchesTab = when (selectedTabIndex) {
            1 -> doc.type == DocumentType.DEVIS
            2 -> doc.type == DocumentType.BON_LIVRAISON
            3 -> doc.type == DocumentType.FACTURE
            4 -> doc.type == DocumentType.BON_COMMANDE
            else -> true
        }
        val matchesStatus = selectedStatusFilter == null || doc.status == selectedStatusFilter
        val matchesSearch = searchQuery.isBlank() ||
                doc.numero.contains(searchQuery, ignoreCase = true) ||
                doc.tiersNom.contains(searchQuery, ignoreCase = true)

        matchesTab && matchesStatus && matchesSearch
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Text(
                    text = "Documents",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Rechercher par n° de document, client..."
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = DynicTealPrimary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = Color.Gray
                )

                FilterChip(
                    selected = selectedStatusFilter == null,
                    onClick = { selectedStatusFilter = null },
                    label = { Text("Tous les statuts", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DynicTealPrimary.copy(alpha = 0.15f),
                        selectedLabelColor = DynicTealPrimary
                    )
                )

                listOf(
                    DocumentStatus.BROUILLON,
                    DocumentStatus.VALIDE,
                    DocumentStatus.ENVOYE,
                    DocumentStatus.ACCEPTE,
                    DocumentStatus.LIVRE,
                    DocumentStatus.PAYE
                ).forEach { status ->
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == status) null else status
                        },
                        label = { Text(status.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DynicTealPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = DynicTealPrimary
                        )
                    )
                }
            }

            // Documents List
            if (filteredDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun document trouvé pour ce filtre.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    items(filteredDocs) { doc ->
                        DocumentRowCard(
                            doc = doc,
                            onClick = { onNavigateToDocDetail(doc.id) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                val defaultType = when (selectedTabIndex) {
                    2 -> DocumentType.BON_LIVRAISON
                    3 -> DocumentType.FACTURE
                    4 -> DocumentType.BON_COMMANDE
                    else -> DocumentType.DEVIS
                }
                onNavigateToNewDoc(defaultType)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_new_document"),
            containerColor = DynicTealPrimary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nouveau document")
        }
    }
}
