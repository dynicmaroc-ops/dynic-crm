package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DocumentStatus
import com.example.ui.theme.DynicTealPrimary
import com.example.ui.theme.StatusAccepted
import com.example.ui.theme.StatusDelivered
import com.example.ui.theme.StatusDraft
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPartial
import com.example.ui.theme.StatusRefused
import com.example.ui.theme.StatusSent
import com.example.ui.theme.StatusValidated

@Composable
fun StatusBadge(status: DocumentStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status) {
        DocumentStatus.BROUILLON -> Color(0xFFEEEEEE) to StatusDraft
        DocumentStatus.VALIDE -> Color(0xFFE1F5FE) to StatusValidated
        DocumentStatus.ENVOYE -> Color(0xFFEDE7F6) to StatusSent
        DocumentStatus.ACCEPTE -> Color(0xFFE8F5E9) to StatusAccepted
        DocumentStatus.REFUSE -> Color(0xFFFFEBEE) to StatusRefused
        DocumentStatus.LIVRE -> Color(0xFFE0F2F1) to StatusDelivered
        DocumentStatus.PAYE_PARTIEL -> Color(0xFFFFF3E0) to StatusPartial
        DocumentStatus.PAYE -> Color(0xFFE8F5E9) to StatusPaid
        DocumentStatus.EN_RETARD -> Color(0xFFFFEBEE) to StatusLate
        DocumentStatus.ANNULE -> Color(0xFFFFEBEE) to StatusRefused
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(textColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = status.label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Rechercher...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Rechercher",
                tint = DynicTealPrimary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Effacer")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DynicTealPrimary,
            unfocusedBorderColor = Color(0xFFD6E2DF)
        )
    )
}
