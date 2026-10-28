package com.example

import com.example.model.DocumentLineEntity
import com.example.model.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testDynicReferenceQuoteCalculations() {
        // 11 test lines from Section 9 & 15 of Cahier des charges
        val lines = listOf(
            DocumentLineEntity(documentId = 1, designation = "Minaret Koutoubia", um = "U", quantite = 1.0, prixUnitaireHT = 2500.0),
            DocumentLineEntity(documentId = 1, designation = "Numéros en Forex, découpe + peinture", um = "U", quantite = 1.0, prixUnitaireHT = 500.0),
            DocumentLineEntity(documentId = 1, designation = "Découpe laser lettres", um = "U", quantite = 1.0, prixUnitaireHT = 0.0),
            DocumentLineEntity(documentId = 1, designation = "Trophée en plexiglas 10 mm + support bois", um = "U", quantite = 18.0, prixUnitaireHT = 240.0),
            DocumentLineEntity(documentId = 1, designation = "Boîte en plexiglas « Octobre Rose »", um = "U", quantite = 10.0, prixUnitaireHT = 170.0),
            DocumentLineEntity(documentId = 1, designation = "Découpe Forex 150x150", um = "U", quantite = 9.0, prixUnitaireHT = 300.0),
            DocumentLineEntity(documentId = 1, designation = "Découpe plexiglas", um = "U", quantite = 3.0, prixUnitaireHT = 300.0),
            DocumentLineEntity(documentId = 1, designation = "Découpe carton plume", um = "U", quantite = 17.0, prixUnitaireHT = 50.0),
            DocumentLineEntity(documentId = 1, designation = "Forex 5 mm 1/4 + découpe", um = "U", quantite = 1.0, prixUnitaireHT = 200.0),
            DocumentLineEntity(documentId = 1, designation = "Trophée en Aluco + Isorel", um = "U", quantite = 90.0, prixUnitaireHT = 17.0),
            DocumentLineEntity(documentId = 1, designation = "Badge ABS", um = "U", quantite = 62.0, prixUnitaireHT = 6.0)
        )

        val totalHT = lines.sumOf { it.quantite * it.prixUnitaireHT }
        val tvaRate = 20.0
        val totalTVA = totalHT * (tvaRate / 100.0)
        val totalTTC = totalHT + totalTVA

        assertEquals(15572.0, totalHT, 0.001)
        assertEquals(3114.40, totalTVA, 0.001)
        assertEquals(18686.40, totalTTC, 0.001)

        val formattedHT = FormatUtils.formatMoney(totalHT)
        val formattedTVA = FormatUtils.formatMoney(totalTVA)
        val formattedTTC = FormatUtils.formatMoney(totalTTC)

        assertEquals("15 572,00", formattedHT)
        assertEquals("3 114,40", formattedTVA)
        assertEquals("18 686,40", formattedTTC)
    }
}
