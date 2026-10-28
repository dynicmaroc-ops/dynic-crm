package com.example.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.CrmRepository
import com.example.model.Company
import com.example.model.DocumentEntity
import com.example.model.DocumentLineEntity
import com.example.model.DocumentStatus
import com.example.model.DocumentType
import com.example.model.DocumentWithLines
import com.example.model.FormatUtils
import com.example.model.Product
import com.example.pdf.PdfGenerator
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ExtractedLine(
    val id: String = UUID.randomUUID().toString(),
    var ref: String = "",
    var designation: String = "",
    var um: String = "U",
    var quantite: Double = 1.0,
    var prixUnitaireHT: Double = 0.0,
    var totalHT: Double = 0.0,
    var action: LineAction = LineAction.CREATE_PRODUCT,
    var matchedProductId: Long? = null
)

enum class LineAction {
    LINK_EXISTING,
    CREATE_PRODUCT,
    IGNORE
}

enum class DetectedDocType(val label: String, val badgeColorHex: String) {
    FACTURE("Facture Fournisseur", "#0288D1"),
    BON_LIVRAISON("Bon de Livraison (BL)", "#00796B"),
    DEVIS("Devis / Offre de prix", "#1DB094")
}

data class ScanExtractionResult(
    var detectedType: DetectedDocType,
    val imageUri: Uri?,
    var typeDetectionConfidence: Float = 0.95f,
    var supplierNom: String = "",
    var supplierIce: String = "",
    var supplierAdresse: String = "",
    var numero: String = "",
    var date: String = FormatUtils.currentDateFrench(),
    var echeance: String = "",
    var categorie: String = "Matière première",
    var totalHT: Double = 0.0,
    var tvaTaux: Double = 20.0,
    var totalTVA: Double = 0.0,
    var totalTTC: Double = 0.0,
    var lines: MutableList<ExtractedLine> = mutableListOf(),
    var confidenceScore: Float = 0.90f,
    var isDuplicate: Boolean = false,
    var duplicateMessage: String = "",
    var rawTextSummary: String = "",
    var isRealOcrExtraction: Boolean = false
)

class SmartScanManager(
    private val context: Context,
    private val repository: CrmRepository
) {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Analyse et traite la photo prise par la caméra avec ML Kit Text Recognition réel.
     * Détermine automatiquement le type de document (BL, Facture, Devis) et extrait
     * les véritables informations présentes sur le document.
     */
    suspend fun analyzeAndExtract(
        uri: Uri?,
        manualPresetSelection: Int? = null
    ): ScanExtractionResult = withContext(Dispatchers.IO) {
        var imageBitmap: Bitmap? = null

        if (uri != null) {
            try {
                val input: InputStream? = context.contentResolver.openInputStream(uri)
                imageBitmap = BitmapFactory.decodeStream(input)
                input?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Si l'utilisateur utilise un exemple de test rapide (preset)
        if (manualPresetSelection != null) {
            return@withContext getPresetResult(manualPresetSelection, uri)
        }

        // Exécution de l'OCR réel sur l'image capturée
        if (imageBitmap != null) {
            try {
                val inputImage = InputImage.fromBitmap(imageBitmap, 0)
                val visionText: Text = suspendCancellableCoroutine { continuation ->
                    recognizer.process(inputImage)
                        .addOnSuccessListener { text -> continuation.resume(text) }
                        .addOnFailureListener { error -> continuation.resumeWithException(error) }
                }

                val fullText = visionText.text
                if (fullText.isNotBlank()) {
                    return@withContext parseRealDocumentText(fullText, uri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback si la photo n'a pas pu être décodée ou est complètement vierge
        return@withContext ScanExtractionResult(
            detectedType = DetectedDocType.FACTURE,
            imageUri = uri,
            typeDetectionConfidence = 0.70f,
            supplierNom = "",
            supplierIce = "",
            numero = "",
            date = FormatUtils.currentDateFrench(),
            totalHT = 0.0,
            tvaTaux = 20.0,
            totalTVA = 0.0,
            totalTTC = 0.0,
            lines = mutableListOf(),
            confidenceScore = 0.50f,
            rawTextSummary = "Aucun texte lisible détecté. Vous pouvez renseigner les champs ci-dessous ou reprendre une photo plus nette.",
            isRealOcrExtraction = false
        )
    }

    /**
     * Analyse le texte réel extrait par ML Kit depuis la photo du document.
     */
    private suspend fun parseRealDocumentText(rawText: String, uri: Uri?): ScanExtractionResult {
        val uppercaseText = rawText.uppercase(Locale.FRENCH)
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Détermination automatique du type de document
        val isFacture = uppercaseText.contains("FACTURE") || uppercaseText.contains("INVOICE") ||
                uppercaseText.contains("TOTAL TTC") || uppercaseText.contains("REGLEMENT") || uppercaseText.contains("DOIT")
        val isBL = uppercaseText.contains("BON DE LIVRAISON") || uppercaseText.contains("LIVRAISON") ||
                uppercaseText.contains("B.L") || uppercaseText.contains("BL N") || uppercaseText.contains("RECEPTION")
        val isDevis = uppercaseText.contains("DEVIS") || uppercaseText.contains("PROFORMA") ||
                uppercaseText.contains("OFFRE DE PRIX") || uppercaseText.contains("DEVIS ESTIMATIF")

        val detectedType = when {
            isBL && !isFacture -> DetectedDocType.BON_LIVRAISON
            isDevis && !isFacture -> DetectedDocType.DEVIS
            else -> DetectedDocType.FACTURE
        }

        // 2. Extraction du tiers / fournisseur
        var extractedSupplier = ""
        // Recherche des lignes avec SARL, SA, STE, ETS ou la première ligne significative
        for (line in lines.take(8)) {
            val upper = line.uppercase()
            if (upper.contains("SARL") || upper.contains("STE") || upper.contains("SOCIETE") ||
                upper.contains("ETS") || upper.contains("COMPANY") || upper.contains("MAROC")
            ) {
                extractedSupplier = line.replace("FOURNISSEUR", "", ignoreCase = true)
                    .replace("CLIENT", "", ignoreCase = true)
                    .replace(":", "").trim()
                break
            }
        }
        if (extractedSupplier.isBlank() && lines.isNotEmpty()) {
            extractedSupplier = lines.firstOrNull { it.length > 3 && !it.contains("http", ignoreCase = true) } ?: ""
        }

        // 3. Extraction de l'ICE (15 chiffres marocains)
        var extractedIce = ""
        val iceRegex = Pattern.compile("ICE\\s*[:\\-\\s]?\\s*(\\d{15})|\\b(\\d{15})\\b")
        val iceMatcher = iceRegex.matcher(rawText)
        if (iceMatcher.find()) {
            extractedIce = iceMatcher.group(1) ?: iceMatcher.group(2) ?: ""
        }

        // 4. Extraction du numéro de document
        var extractedNumber = ""
        val numRegex = Pattern.compile("(?:N[°o]|Num[ée]ro|Facture|BL|Devis|BC|Ref)\\s*[:#\\-\\s]?\\s*([A-Za-z0-9\\-_/]{3,25})", Pattern.CASE_INSENSITIVE)
        val numMatcher = numRegex.matcher(rawText)
        if (numMatcher.find()) {
            extractedNumber = numMatcher.group(1)?.trim() ?: ""
        }

        // 5. Extraction de la date
        var extractedDate = FormatUtils.currentDateFrench()
        val dateRegex = Pattern.compile("\\b(\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4})\\b")
        val dateMatcher = dateRegex.matcher(rawText)
        if (dateMatcher.find()) {
            extractedDate = dateMatcher.group(1) ?: FormatUtils.currentDateFrench()
        }

        // 6. Extraction des montants (HT, TVA, TTC)
        var totalHT = 0.0
        var totalTVA = 0.0
        var totalTTC = 0.0

        for (line in lines) {
            val upper = line.uppercase()
            val numbersInLine = extractDecimalNumbers(line)

            if (upper.contains("TOTAL TTC") || upper.contains("NET A PAYER") || upper.contains("MONTANT TTC")) {
                numbersInLine.lastOrNull()?.let { totalTTC = it }
            } else if (upper.contains("TOTAL HT") || (upper.contains("HT") && !upper.contains("TVA"))) {
                numbersInLine.lastOrNull()?.let { totalHT = it }
            } else if (upper.contains("TVA") && !upper.contains("SANS TVA")) {
                numbersInLine.lastOrNull()?.let { totalTVA = it }
            }
        }

        // Cohérence automatique des montants si certains manquent
        if (totalTTC > 0.0 && totalHT == 0.0) {
            totalHT = totalTTC / 1.20
            totalTVA = totalTTC - totalHT
        } else if (totalHT > 0.0 && totalTTC == 0.0) {
            totalTVA = totalHT * 0.20
            totalTTC = totalHT + totalTVA
        }

        // 7. Extraction des lignes d'articles
        val extractedLines = mutableListOf<ExtractedLine>()
        for (line in lines) {
            // Une ligne d'article contient typiquement du texte et au moins 1 ou 2 nombres (quantité et prix)
            val numbers = extractDecimalNumbers(line)
            if (numbers.size >= 2 && line.length > 5 &&
                !line.contains("TOTAL", ignoreCase = true) &&
                !line.contains("TVA", ignoreCase = true) &&
                !line.contains("ICE", ignoreCase = true) &&
                !line.contains("TEL", ignoreCase = true) &&
                !line.contains("BANQUE", ignoreCase = true)
            ) {
                val qte = numbers.first()
                val pu = numbers[1]
                val designation = line.replace(Regex("[0-9]+[.,]?[0-9]*"), "").trim()
                if (designation.length >= 3) {
                    val lineTotal = if (numbers.size >= 3) numbers[2] else qte * pu
                    extractedLines.add(
                        ExtractedLine(
                            designation = designation,
                            um = if (line.contains("M²", ignoreCase = true)) "M²" else if (line.contains("ML", ignoreCase = true)) "ML" else "U",
                            quantite = qte,
                            prixUnitaireHT = pu,
                            totalHT = lineTotal
                        )
                    )
                }
            }
        }

        // Reconcile avec les produits du catalogue DYNIC
        for (item in extractedLines) {
            val matched = repository.findProductByRefOrName(item.ref, item.designation)
            if (matched != null) {
                item.action = LineAction.LINK_EXISTING
                item.matchedProductId = matched.id
            } else {
                item.action = LineAction.CREATE_PRODUCT
            }
        }

        // Vérification de doublon
        val duplicate = repository.checkExpenseDuplicate(extractedNumber, extractedSupplier)
        val isDup = duplicate != null
        val dupMsg = if (isDup) "Doublon détecté : une facture avec le n° $extractedNumber pour $extractedSupplier existe déjà." else ""

        return ScanExtractionResult(
            detectedType = detectedType,
            imageUri = uri,
            typeDetectionConfidence = 0.94f,
            supplierNom = extractedSupplier,
            supplierIce = extractedIce,
            numero = extractedNumber,
            date = extractedDate,
            totalHT = totalHT,
            tvaTaux = 20.0,
            totalTVA = totalTVA,
            totalTTC = totalTTC,
            lines = extractedLines,
            confidenceScore = 0.92f,
            isDuplicate = isDup,
            duplicateMessage = dupMsg,
            rawTextSummary = rawText,
            isRealOcrExtraction = true
        )
    }

    private fun extractDecimalNumbers(line: String): List<Double> {
        val list = mutableListOf<Double>()
        // Ex: 1 500,00 ou 1500.00 ou 250,50
        val matcher = Pattern.compile("(\\d+(?:[ ]\\d{3})*(?:[.,]\\d{1,2})?)").matcher(line)
        while (matcher.find()) {
            val str = matcher.group(1)?.replace(" ", "")?.replace(",", ".") ?: continue
            val num = str.toDoubleOrNull()
            if (num != null && num > 0) {
                list.add(num)
            }
        }
        return list
    }

    private suspend fun getPresetResult(presetId: Int, uri: Uri?): ScanExtractionResult {
        return when (presetId) {
            2 -> {
                val rawLines = listOf(
                    ExtractedLine(ref = "PLX-3MM", designation = "Plaque Plexiglas Transparent 3mm", um = "M²", quantite = 10.0, prixUnitaireHT = 125.0, totalHT = 1250.0),
                    ExtractedLine(ref = "FX-3MM", designation = "Panneau Forex Blanc 3mm", um = "U", quantite = 15.0, prixUnitaireHT = 95.0, totalHT = 1425.0)
                )
                ScanExtractionResult(
                    detectedType = DetectedDocType.BON_LIVRAISON,
                    imageUri = uri,
                    typeDetectionConfidence = 0.96f,
                    supplierNom = "PLASTICORP MAROC",
                    supplierIce = "000555888999111",
                    numero = "BL-FOURN-2026-441",
                    date = FormatUtils.currentDateFrench(),
                    totalHT = 2675.0,
                    tvaTaux = 20.0,
                    totalTVA = 535.0,
                    totalTTC = 3210.0,
                    lines = rawLines.toMutableList(),
                    confidenceScore = 0.95f,
                    rawTextSummary = "EXEMPLE BL RECEPTION FOURNISSEUR",
                    isRealOcrExtraction = false
                )
            }
            else -> {
                ScanExtractionResult(
                    detectedType = DetectedDocType.FACTURE,
                    imageUri = uri,
                    typeDetectionConfidence = 0.98f,
                    supplierNom = "POLYCOLOR SARL",
                    supplierIce = "001928374650001",
                    numero = "FAC-PC-2026-114",
                    date = FormatUtils.currentDateFrench(),
                    totalHT = 3200.0,
                    tvaTaux = 20.0,
                    totalTVA = 640.0,
                    totalTTC = 3840.0,
                    confidenceScore = 0.98f,
                    rawTextSummary = "EXEMPLE FACTURE ACHAT FOURNISSEUR",
                    isRealOcrExtraction = false
                )
            }
        }
    }

    /**
     * Génère un récapitulatif textuel complet prêt pour partage WhatsApp / Email / Copier.
     */
    fun generateExportText(result: ScanExtractionResult): String {
        val sb = StringBuilder()
        sb.append("📋 *DOCUMENT NUMÉRISÉ - DYNIC CRM LITE*\n")
        sb.append("------------------------------------\n")
        sb.append("Type détecté : ${result.detectedType.label}\n")
        sb.append("Numéro : ${result.numero.ifBlank { "N/A" }}\n")
        sb.append("Date : ${result.date}\n")
        sb.append("Tiers / Fournisseur : ${result.supplierNom.ifBlank { "Non spécifié" }}\n")
        if (result.supplierIce.isNotBlank()) sb.append("ICE : ${result.supplierIce}\n")
        sb.append("\n*Articles / Lignes :*\n")
        if (result.lines.isEmpty()) {
            sb.append("Aucun article détaillé extrait.\n")
        } else {
            result.lines.forEachIndexed { i, line ->
                sb.append("${i + 1}. ${line.designation} (${FormatUtils.formatMoney(line.quantite)} ${line.um})")
                if (line.prixUnitaireHT > 0) {
                    sb.append(" - PU: ${FormatUtils.formatMoneyWithCurrency(line.prixUnitaireHT)}")
                }
                sb.append("\n")
            }
        }
        sb.append("------------------------------------\n")
        sb.append("Total HT : ${FormatUtils.formatMoneyWithCurrency(result.totalHT)}\n")
        sb.append("TVA (${result.tvaTaux.toInt()}%) : ${FormatUtils.formatMoneyWithCurrency(result.totalTVA)}\n")
        sb.append("TOTAL TTC : ${FormatUtils.formatMoneyWithCurrency(result.totalTTC)}\n")
        return sb.toString()
    }

    /**
     * Exporte un fichier PDF officiel fidèle à partir des données réelles extraites du scan.
     */
    suspend fun generateScanPdf(result: ScanExtractionResult, company: Company): File {
        val docType = when (result.detectedType) {
            DetectedDocType.FACTURE -> DocumentType.FACTURE
            DetectedDocType.BON_LIVRAISON -> DocumentType.BON_LIVRAISON
            DetectedDocType.DEVIS -> DocumentType.DEVIS
        }

        val docEntity = DocumentEntity(
            type = docType,
            numero = result.numero.ifBlank { "SCAN-${System.currentTimeMillis() % 10000}" },
            date = result.date,
            tiersNom = result.supplierNom.ifBlank { "Fournisseur / Client" },
            tiersAdresse = result.supplierAdresse,
            tiersIce = result.supplierIce,
            status = DocumentStatus.VALIDE,
            totalHT = result.totalHT,
            totalTVA = result.totalTVA,
            totalTTC = result.totalTTC,
            avecCachet = true,
            notes = "Document numérisé par appareil photo dans DYNIC CRM Lite."
        )

        val lines = if (result.lines.isNotEmpty()) {
            result.lines.mapIndexed { index, l ->
                DocumentLineEntity(
                    documentId = 0L,
                    ref = l.ref,
                    designation = l.designation,
                    um = l.um,
                    quantite = l.quantite,
                    prixUnitaireHT = l.prixUnitaireHT,
                    totalHT = l.totalHT,
                    ordre = index
                )
            }
        } else {
            listOf(
                DocumentLineEntity(
                    documentId = 0L,
                    designation = "Achat / Prestation selon document numérisé",
                    um = "Forfait",
                    quantite = 1.0,
                    prixUnitaireHT = result.totalHT,
                    totalHT = result.totalHT,
                    ordre = 0
                )
            )
        }

        val docWithLines = DocumentWithLines(docEntity, lines)
        return PdfGenerator.generatePdfFile(context, docWithLines, company)
    }
}
