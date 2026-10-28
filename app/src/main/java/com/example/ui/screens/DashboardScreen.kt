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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Company
import com.example.model.DocumentEntity
import com.example.model.DocumentStatus
import com.example.model.DocumentType
import com.example.model.ExpenseEntity
import com.example.model.FormatUtils
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DynicTealContainer
import com.example.ui.theme.DynicTealDark
import com.example.ui.theme.DynicTealPrimary

@Composable
fun DashboardScreen(
    company: Company,
    documents: List<DocumentEntity>,
    expenses: List<ExpenseEntity>,
    onNavigateToNewDoc: (DocumentType) -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToDocDetail: (Long) -> Unit,
    onNavigateToDocsList: () -> Unit
) {
    // Computations
    val totalSales = documents
        .filter { it.type == DocumentType.DEVIS || it.type == DocumentType.FACTURE }
        .filter { it.status == DocumentStatus.ACCEPTE || it.status == DocumentStatus.PAYE || it.status == DocumentStatus.VALIDE }
        .sumOf { it.totalTTC }

    val unpaidInvoices = documents
        .filter { it.type == DocumentType.FACTURE && it.status != DocumentStatus.PAYE }

    val unpaidAmount = unpaidInvoices.sumOf { it.resteAPayer }

    val totalExpenses = expenses.sumOf { it.montantTTC }

    val pendingQuotesCount = documents
        .count { it.type == DocumentType.DEVIS && (it.status == DocumentStatus.ENVOYE || it.status == DocumentStatus.VALIDE) }

    val unbilledDeliveriesCount = documents
        .count { it.type == DocumentType.BON_LIVRAISON && it.status != DocumentStatus.LIVRE }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // DYNIC Header Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = DynicTealPrimary,
                shadowElevation = 3.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(DynicTealPrimary, DynicTealDark)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = company.nom,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${company.ville} • Maroc",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "CRM LITE",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = company.slogan,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Actions Bar
            Text(
                text = "Actions rapides",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    label = "+ Devis",
                    icon = Icons.Default.Receipt,
                    color = DynicTealPrimary,
                    modifier = Modifier.weight(1f).testTag("action_new_devis"),
                    onClick = { onNavigateToNewDoc(DocumentType.DEVIS) }
                )
                QuickActionItem(
                    label = "+ BL",
                    icon = Icons.Default.LocalShipping,
                    color = Color(0xFF00796B),
                    modifier = Modifier.weight(1f).testTag("action_new_bl"),
                    onClick = { onNavigateToNewDoc(DocumentType.BON_LIVRAISON) }
                )
                QuickActionItem(
                    label = "+ Facture",
                    icon = Icons.Default.Add,
                    color = Color(0xFF0288D1),
                    modifier = Modifier.weight(1f).testTag("action_new_facture"),
                    onClick = { onNavigateToNewDoc(DocumentType.FACTURE) }
                )
                QuickActionItem(
                    label = "Scanner",
                    icon = Icons.Default.DocumentScanner,
                    color = Color(0xFFE65100),
                    modifier = Modifier.weight(1f).testTag("action_scanner"),
                    onClick = onNavigateToScan
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // KPI Grid
            Text(
                text = "Vue d'ensemble",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Chiffre d'Affaires",
                    value = FormatUtils.formatMoneyWithCurrency(totalSales),
                    subtitle = "Ventes validées / facturées",
                    icon = Icons.Default.TrendingUp,
                    accentColor = DynicTealPrimary,
                    modifier = Modifier.weight(1f)
                )

                KpiCard(
                    title = "Impayés Clients",
                    value = FormatUtils.formatMoneyWithCurrency(unpaidAmount),
                    subtitle = "${unpaidInvoices.size} factures à recouvrer",
                    icon = Icons.Default.TrendingDown,
                    accentColor = Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Dépenses du mois",
                    value = FormatUtils.formatMoneyWithCurrency(totalExpenses),
                    subtitle = "${expenses.size} achats enregistrés",
                    icon = Icons.Default.Receipt,
                    accentColor = Color(0xFFF57C00),
                    modifier = Modifier.weight(1f)
                )

                KpiCard(
                    title = "En attente",
                    value = "$pendingQuotesCount devis",
                    subtitle = "$unbilledDeliveriesCount BL non facturés",
                    icon = Icons.Default.LocalShipping,
                    accentColor = Color(0xFF5E35B1),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Documents Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Derniers documents",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Voir tout",
                    color = DynicTealPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onNavigateToDocsList() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Recent Documents List (Take first 6)
        val recentDocs = documents.take(6)
        if (recentDocs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun document créé. Cliquez sur + Devis pour commencer !",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(recentDocs) { doc ->
                DocumentRowCard(
                    doc = doc,
                    onClick = { onNavigateToDocDetail(doc.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun QuickActionItem(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun DocumentRowCard(
    doc: DocumentEntity,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        when (doc.type) {
                            DocumentType.DEVIS -> DynicTealPrimary.copy(alpha = 0.15f)
                            DocumentType.BON_LIVRAISON -> Color(0xFF00796B).copy(alpha = 0.15f)
                            DocumentType.FACTURE -> Color(0xFF0288D1).copy(alpha = 0.15f)
                            DocumentType.BON_COMMANDE -> Color(0xFF673AB7).copy(alpha = 0.15f)
                        },
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (doc.type) {
                        DocumentType.DEVIS -> Icons.Default.Receipt
                        DocumentType.BON_LIVRAISON -> Icons.Default.LocalShipping
                        DocumentType.FACTURE -> Icons.Default.PictureAsPdf
                        DocumentType.BON_COMMANDE -> Icons.Default.Receipt
                    },
                    contentDescription = null,
                    tint = when (doc.type) {
                        DocumentType.DEVIS -> DynicTealPrimary
                        DocumentType.BON_LIVRAISON -> Color(0xFF00796B)
                        DocumentType.FACTURE -> Color(0xFF0288D1)
                        DocumentType.BON_COMMANDE -> Color(0xFF673AB7)
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = doc.numero,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = doc.status)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = doc.tiersNom.ifBlank { "Client Divers" },
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )

                Text(
                    text = doc.date,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (doc.type != DocumentType.BON_LIVRAISON) {
                    Text(
                        text = FormatUtils.formatMoneyWithCurrency(doc.totalTTC),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DynicTealPrimary
                    )
                } else {
                    Text(
                        text = "Livré",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color(0xFF00796B)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Ouvrir",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
