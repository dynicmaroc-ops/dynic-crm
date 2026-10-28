package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "company")
data class Company(
    @PrimaryKey val id: Long = 1L,
    val nom: String = "DYNIC Sarl",
    val slogan: String = "Dynamisé votre succès avec notre expertise !",
    val adresse: String = "Ss 425-Lot. Sidi Abdellah, Salé",
    val fixe: String = "05 30 04 37 40",
    val mobile1: String = "06 10 023 021",
    val mobile2: String = "06 60 79 30 67",
    val capital: String = "100.000,00 DHS",
    val rc: String = "38229",
    val patente: String = "29407626",
    val ifiscal: String = "53935383",
    val ice: String = "003332326000033",
    val banqueRib: String = "011 815 0000222100004565 29",
    val banqueAgence: String = "BMCE Ag,Sale Hay al Amal",
    val mail: String = "contact@dynic.ma",
    val web: String = "www.dynic.ma",
    val ville: String = "Salé",
    val devise: String = "DH",
    val tvaParDefaut: Double = 20.0,
    val prefixeDevis: String = "TP-27",
    val serieDevis: Int = 100,
    val prefixeBL: String = "TD-26",
    val serieBL: Int = 27,
    val prefixeFacture: String = "FAC-26",
    val serieFacture: Int = 1,
    val prefixeCommande: String = "BC-26",
    val serieCommande: Int = 1
)

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nom: String,
    val adresse: String = "",
    val ville: String = "Salé",
    val ice: String = "",
    val telephone: String = "",
    val email: String = "",
    val contact: String = "",
    val notes: String = "",
    val soldeDu: Double = 0.0
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nom: String,
    val adresse: String = "",
    val ville: String = "",
    val ice: String = "",
    val telephone: String = "",
    val email: String = "",
    val contact: String = "",
    val conditionsPaiement: String = "À réception"
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ref: String = "",
    val designation: String,
    val details: String = "", // Delimited by newlines
    val um: String = "U", // U, M², ML, kg, Forfait
    val prixAchatHT: Double = 0.0,
    val prixVenteHT: Double = 0.0,
    val tvaTaux: Double = 20.0,
    val categorie: String = "Général",
    val stock: Double = 0.0,
    val actif: Boolean = true
) {
    val margePourcentage: Double
        get() = if (prixAchatHT > 0) ((prixVenteHT - prixAchatHT) / prixAchatHT) * 100.0 else 0.0
}

enum class DocumentType(val label: String, val code: String) {
    DEVIS("Devis", "TP"),
    BON_LIVRAISON("Bon de livraison", "BL"),
    FACTURE("Facture", "FAC"),
    BON_COMMANDE("Bon de commande", "BC")
}

enum class DocumentStatus(val label: String) {
    BROUILLON("Brouillon"),
    VALIDE("Validé"),
    ENVOYE("Envoyé"),
    ACCEPTE("Accepté"),
    REFUSE("Refusé"),
    LIVRE("Livré"),
    PAYE_PARTIEL("Payé partiel"),
    PAYE("Payé"),
    EN_RETARD("En retard"),
    ANNULE("Annulé")
}

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: DocumentType,
    val numero: String,
    val date: String, // JJ/MM/AAAA or AAAA-MM-JJ
    val echeance: String = "",
    val clientId: Long? = null,
    val supplierId: Long? = null,
    val tiersNom: String = "",
    val tiersAdresse: String = "",
    val tiersIce: String = "",
    val status: DocumentStatus = DocumentStatus.BROUILLON,
    val sourceDocumentId: Long? = null,
    val tvaTaux: Double = 20.0,
    val remiseGlobale: Double = 0.0,
    val totalHT: Double = 0.0,
    val totalTVA: Double = 0.0,
    val totalTTC: Double = 0.0,
    val montantPaye: Double = 0.0,
    val avecCachet: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val resteAPayer: Double
        get() = (totalTTC - montantPaye).coerceAtLeast(0.0)
}

@Entity(tableName = "document_lines")
data class DocumentLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val documentId: Long,
    val productId: Long? = null,
    val ref: String = "",
    val designation: String,
    val details: String = "", // multiline details
    val um: String = "U",
    val quantite: Double = 1.0,
    val prixUnitaireHT: Double = 0.0,
    val remise: Double = 0.0,
    val tvaTaux: Double = 20.0,
    val totalHT: Double = 0.0,
    val ordre: Int = 0
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val supplierId: Long? = null,
    val supplierNom: String,
    val categorie: String = "Matière première", // Matière, Transport, Loyer, Énergie, etc.
    val montantHT: Double = 0.0,
    val tvaTaux: Double = 20.0,
    val montantTVA: Double = 0.0,
    val montantTTC: Double = 0.0,
    val modePaiement: String = "Virement",
    val statut: String = "PAYEE", // PAYEE / A_PAYER
    val referenceFacture: String = "",
    val photoUri: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val documentId: Long,
    val documentNumero: String,
    val clientNom: String,
    val date: String,
    val montant: Double,
    val modePaiement: String = "Espèces", // Espèces, Virement, Chèque, Effet
    val reference: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class DocumentWithLines(
    val document: DocumentEntity,
    val lines: List<DocumentLineEntity>
)

object FormatUtils {
    private val symbols = DecimalFormatSymbols(Locale.FRENCH).apply {
        groupingSeparator = ' '
        decimalSeparator = ','
    }
    private val moneyFormat = DecimalFormat("#,##0.00", symbols)

    fun formatMoney(amount: Double): String {
        return moneyFormat.format(amount)
    }

    fun formatMoneyWithCurrency(amount: Double, currency: String = "DH"): String {
        return "${moneyFormat.format(amount)} $currency"
    }

    fun currentDateFrench(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
        return sdf.format(Date())
    }

    fun parseAmount(text: String): Double {
        return try {
            text.replace(" ", "").replace(",", ".").replace("DH", "").trim().toDouble()
        } catch (_: Exception) {
            0.0
        }
    }
}
