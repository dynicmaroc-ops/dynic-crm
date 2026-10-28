package com.example.data

import android.content.Context
import com.example.model.Client
import com.example.model.Company
import com.example.model.DocumentEntity
import com.example.model.DocumentLineEntity
import com.example.model.DocumentStatus
import com.example.model.DocumentType
import com.example.model.DocumentWithLines
import com.example.model.ExpenseEntity
import com.example.model.FormatUtils
import com.example.model.PaymentEntity
import com.example.model.Product
import com.example.model.Supplier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CrmRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val companyDao = db.companyDao()
    private val clientDao = db.clientDao()
    private val supplierDao = db.supplierDao()
    private val productDao = db.productDao()
    private val documentDao = db.documentDao()
    private val documentLineDao = db.documentLineDao()
    private val expenseDao = db.expenseDao()
    private val paymentDao = db.paymentDao()

    init {
        // Initialize default seed data if database is fresh
        CoroutineScope(Dispatchers.IO).launch {
            seedDefaultDataIfNeeded()
        }
    }

    // Company
    fun getCompany(): Flow<Company?> = companyDao.getCompany()
    suspend fun getCompanyDirect(): Company = companyDao.getCompanyDirect() ?: Company()
    suspend fun updateCompany(company: Company) = companyDao.insertOrUpdate(company)

    // Clients
    fun getClients(): Flow<List<Client>> = clientDao.getAll()
    suspend fun getClient(id: Long) = clientDao.getById(id)
    suspend fun saveClient(client: Client) = clientDao.insert(client)
    suspend fun updateClient(client: Client) = clientDao.update(client)
    suspend fun deleteClient(client: Client) = clientDao.delete(client)

    // Suppliers
    fun getSuppliers(): Flow<List<Supplier>> = supplierDao.getAll()
    suspend fun getSupplier(id: Long) = supplierDao.getById(id)
    suspend fun findSupplierByIceOrNom(ice: String, nom: String) = supplierDao.findByIceOrNom(ice, nom)
    suspend fun saveSupplier(supplier: Supplier) = supplierDao.insert(supplier)
    suspend fun updateSupplier(supplier: Supplier) = supplierDao.update(supplier)
    suspend fun deleteSupplier(supplier: Supplier) = supplierDao.delete(supplier)

    // Products
    fun getProducts(): Flow<List<Product>> = productDao.getAll()
    suspend fun getProduct(id: Long) = productDao.getById(id)
    suspend fun findProductByRefOrName(ref: String, query: String) = productDao.findByRefOrName(ref, query)
    suspend fun saveProduct(product: Product) = productDao.insert(product)
    suspend fun updateProduct(product: Product) = productDao.update(product)
    suspend fun deleteProduct(product: Product) = productDao.delete(product)

    // Documents
    fun getDocuments(): Flow<List<DocumentEntity>> = documentDao.getAll()
    fun getDocumentsByType(type: DocumentType): Flow<List<DocumentEntity>> = documentDao.getByType(type)
    suspend fun getDocument(id: Long): DocumentEntity? = documentDao.getById(id)
    fun getDocumentLines(documentId: Long): Flow<List<DocumentLineEntity>> = documentLineDao.getLinesForDocument(documentId)
    suspend fun getDocumentLinesDirect(documentId: Long): List<DocumentLineEntity> = documentLineDao.getLinesForDocumentDirect(documentId)

    suspend fun getDocumentWithLines(documentId: Long): DocumentWithLines? {
        val doc = documentDao.getById(documentId) ?: return null
        val lines = documentLineDao.getLinesForDocumentDirect(documentId)
        return DocumentWithLines(doc, lines)
    }

    suspend fun saveDocument(doc: DocumentEntity, lines: List<DocumentLineEntity>): Long = withContext(Dispatchers.IO) {
        // Recalculate totals
        var totalHT = 0.0
        val preparedLines = lines.mapIndexed { index, line ->
            val lineTotal = (line.quantite * line.prixUnitaireHT) - line.remise
            totalHT += lineTotal
            line.copy(
                totalHT = lineTotal,
                ordre = index
            )
        }
        val tvaRate = doc.tvaTaux
        val totalTVA = (totalHT * (tvaRate / 100.0))
        val totalTTC = totalHT + totalTVA

        val updatedDoc = doc.copy(
            totalHT = totalHT,
            totalTVA = totalTVA,
            totalTTC = totalTTC
        )

        val docId = documentDao.insert(updatedDoc)
        documentLineDao.deleteLinesForDocument(docId)
        val linesWithDocId = preparedLines.map { it.copy(documentId = docId) }
        documentLineDao.insertLines(linesWithDocId)
        docId
    }

    suspend fun deleteDocument(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        documentLineDao.deleteLinesForDocument(doc.id)
        documentDao.delete(doc)
    }

    suspend fun generateNextNumber(type: DocumentType): String = withContext(Dispatchers.IO) {
        val company = getCompanyDirect()
        val count = documentDao.countByType(type)
        when (type) {
            DocumentType.DEVIS -> {
                val next = company.serieDevis + count
                "${company.prefixeDevis}-$next" // e.g. TP-27-0100
            }
            DocumentType.BON_LIVRAISON -> {
                val next = company.serieBL + count
                "${company.prefixeBL}-${String.format(java.util.Locale.US, "%05d", next)}" // e.g. TD-26-00027
            }
            DocumentType.FACTURE -> {
                val next = company.serieFacture + count
                "${company.prefixeFacture}-${String.format(java.util.Locale.US, "%04d", next)}"
            }
            DocumentType.BON_COMMANDE -> {
                val next = company.serieCommande + count
                "${company.prefixeCommande}-${String.format(java.util.Locale.US, "%04d", next)}"
            }
        }
    }

    suspend fun convertDocument(sourceDocId: Long, targetType: DocumentType): Long = withContext(Dispatchers.IO) {
        val source = getDocumentWithLines(sourceDocId) ?: return@withContext 0L
        val nextNumber = generateNextNumber(targetType)

        val newDoc = source.document.copy(
            id = 0L,
            type = targetType,
            numero = nextNumber,
            date = FormatUtils.currentDateFrench(),
            sourceDocumentId = sourceDocId,
            status = if (targetType == DocumentType.BON_LIVRAISON) DocumentStatus.VALIDE else DocumentStatus.BROUILLON,
            createdAt = System.currentTimeMillis()
        )
        val newLines = source.lines.map { it.copy(id = 0L, documentId = 0L) }
        saveDocument(newDoc, newLines)
    }

    suspend fun duplicateDocument(docId: Long): Long = withContext(Dispatchers.IO) {
        val source = getDocumentWithLines(docId) ?: return@withContext 0L
        val nextNumber = generateNextNumber(source.document.type)
        val newDoc = source.document.copy(
            id = 0L,
            numero = nextNumber,
            date = FormatUtils.currentDateFrench(),
            status = DocumentStatus.BROUILLON,
            createdAt = System.currentTimeMillis()
        )
        val newLines = source.lines.map { it.copy(id = 0L, documentId = 0L) }
        saveDocument(newDoc, newLines)
    }

    // Payments
    fun getPayments(): Flow<List<PaymentEntity>> = paymentDao.getAll()
    fun getPaymentsForDocument(docId: Long): Flow<List<PaymentEntity>> = paymentDao.getByDocument(docId)

    suspend fun addPayment(payment: PaymentEntity) = withContext(Dispatchers.IO) {
        paymentDao.insert(payment)
        // Update document paidAmount and status
        val doc = documentDao.getById(payment.documentId)
        if (doc != null) {
            val newPaid = doc.montantPaye + payment.montant
            val newStatus = if (newPaid >= doc.totalTTC - 0.01) {
                DocumentStatus.PAYE
            } else if (newPaid > 0) {
                DocumentStatus.PAYE_PARTIEL
            } else {
                doc.status
            }
            documentDao.update(doc.copy(montantPaye = newPaid, status = newStatus))
        }
    }

    // Expenses
    fun getExpenses(): Flow<List<ExpenseEntity>> = expenseDao.getAll()
    suspend fun checkExpenseDuplicate(ref: String, supplierNom: String): ExpenseEntity? {
        if (ref.isBlank()) return null
        return expenseDao.findDuplicate(ref, supplierNom)
    }
    suspend fun saveExpense(expense: ExpenseEntity): Long = expenseDao.insert(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.delete(expense)

    private suspend fun seedDefaultDataIfNeeded() = withContext(Dispatchers.IO) {
        val existingCompany = companyDao.getCompanyDirect()
        if (existingCompany == null) {
            val company = Company()
            companyDao.insertOrUpdate(company)
        }

        // Check if demo data exists
        if (productDao.findByRefOrName("MK-01", "Minaret") == null) {
            // Seed 11 reference products from DYNIC specifications
            val products = listOf(
                Product(ref = "MK-01", designation = "Minaret Koutoubia", details = "Fabrication artisanale et finition soignée", um = "U", prixAchatHT = 1600.0, prixVenteHT = 2500.0, stock = 5.0),
                Product(ref = "FX-NUM", designation = "Numéros en Forex, découpe + peinture", details = "Forex haute densité avec peinture laquée", um = "U", prixAchatHT = 300.0, prixVenteHT = 500.0, stock = 20.0),
                Product(ref = "DEC-LET", designation = "Découpe laser lettres", details = "Découpe de précision grand format", um = "U", prixAchatHT = 0.0, prixVenteHT = 0.0, stock = 100.0),
                Product(ref = "TR-PLEX", designation = "Trophée en plexiglas 10 mm + support bois", details = "Plexiglas transparent biseauté, socle chêne verni", um = "U", prixAchatHT = 140.0, prixVenteHT = 240.0, stock = 35.0),
                Product(ref = "BOX-ROSE", designation = "Boîte en plexiglas « Octobre Rose »", details = "Urne transparente sérigraphiée", um = "U", prixAchatHT = 95.0, prixVenteHT = 170.0, stock = 15.0),
                Product(ref = "FX-150", designation = "Découpe Forex 150x150", details = "Panneaux signalétiques 150x150 cm", um = "U", prixAchatHT = 180.0, prixVenteHT = 300.0, stock = 18.0),
                Product(ref = "DEC-PLX", designation = "Découpe plexiglas", details = "Usinage CNC et polissage flamme", um = "U", prixAchatHT = 170.0, prixVenteHT = 300.0, stock = 25.0),
                Product(ref = "DEC-CP", designation = "Découpe carton plume", details = "Carton plume 10 mm léger", um = "U", prixAchatHT = 25.0, prixVenteHT = 50.0, stock = 50.0),
                Product(ref = "FX-5MM", designation = "Forex 5 mm 1/4 + découpe", details = "Format quart de feuille 5 mm", um = "U", prixAchatHT = 110.0, prixVenteHT = 200.0, stock = 12.0),
                Product(ref = "TR-ALU", designation = "Trophée en Aluco + Isorel", details = "Aluminium composite et support isorel gravé", um = "U", prixAchatHT = 9.5, prixVenteHT = 17.0, stock = 150.0),
                Product(ref = "BDG-ABS", designation = "Badge ABS", details = "Badge nominatif gravé avec attache aimantée", um = "U", prixAchatHT = 3.2, prixVenteHT = 6.0, stock = 200.0)
            )
            for (p in products) {
                productDao.insert(p)
            }

            // Seed clients
            val clientId1 = clientDao.insert(
                Client(
                    nom = "AGENCE HORIZON DESIGN",
                    adresse = "Av. Mohammed V, Rabat",
                    ville = "Rabat",
                    ice = "001234567890001",
                    telephone = "06 61 22 33 44",
                    email = "contact@horizondesign.ma",
                    contact = "M. Karim Alami",
                    soldeDu = 0.0
                )
            )
            clientDao.insert(
                Client(
                    nom = "MAROC SIGNALISATION & EVENT",
                    adresse = "Zone Industrielle Technopolis, Salé",
                    ville = "Salé",
                    ice = "002345678901234",
                    telephone = "06 63 55 66 77",
                    email = "info@marocsignal.ma",
                    contact = "Mme Fatima Bennani"
                )
            )

            // Seed suppliers
            val suppId = supplierDao.insert(
                Supplier(
                    nom = "PLASTICORP MAROC",
                    adresse = "Zone Industrielle Oukacha, Casablanca",
                    ice = "000555888999111",
                    telephone = "05 22 44 88 00",
                    email = "commandes@plasticorp.ma",
                    contact = "M. Hamid Tazi"
                )
            )

            // Seed expenses
            expenseDao.insert(
                ExpenseEntity(
                    date = FormatUtils.currentDateFrench(),
                    supplierId = suppId,
                    supplierNom = "PLASTICORP MAROC",
                    categorie = "Matière première",
                    montantHT = 4500.0,
                    tvaTaux = 20.0,
                    montantTVA = 900.0,
                    montantTTC = 5400.0,
                    modePaiement = "Virement",
                    referenceFacture = "FC-PLAST-2026-89",
                    statut = "PAYEE"
                )
            )

            // Seed sample reference Devis: TP-27-0100 (exactly matching test criteria in Section 9 & 15)
            val demoDoc = DocumentEntity(
                type = DocumentType.DEVIS,
                numero = "TP-27-0100",
                date = "26/10/2026",
                clientId = clientId1,
                tiersNom = "AGENCE HORIZON DESIGN",
                tiersAdresse = "Av. Mohammed V, Rabat",
                tiersIce = "001234567890001",
                status = DocumentStatus.VALIDE,
                tvaTaux = 20.0,
                totalHT = 15572.0,
                totalTVA = 3114.40,
                totalTTC = 18686.40,
                avecCachet = true,
                notes = "Validité de l'offre : 30 jours. Modalités : 50% à la commande, solde à la livraison."
            )
            val docId = documentDao.insert(demoDoc)

            val demoLines = listOf(
                DocumentLineEntity(documentId = docId, ref = "MK-01", designation = "Minaret Koutoubia", um = "U", quantite = 1.00, prixUnitaireHT = 2500.00, totalHT = 2500.00, ordre = 0),
                DocumentLineEntity(documentId = docId, ref = "FX-NUM", designation = "Numéros en Forex, découpe + peinture", um = "U", quantite = 1.00, prixUnitaireHT = 500.00, totalHT = 500.00, ordre = 1),
                DocumentLineEntity(documentId = docId, ref = "DEC-LET", designation = "Découpe laser lettres", um = "U", quantite = 1.00, prixUnitaireHT = 0.00, totalHT = 0.00, ordre = 2),
                DocumentLineEntity(documentId = docId, ref = "TR-PLEX", designation = "Trophée en plexiglas 10 mm + support bois", um = "U", quantite = 18.00, prixUnitaireHT = 240.00, totalHT = 4320.00, ordre = 3),
                DocumentLineEntity(documentId = docId, ref = "BOX-ROSE", designation = "Boîte en plexiglas « Octobre Rose »", um = "U", quantite = 10.00, prixUnitaireHT = 170.00, totalHT = 1700.00, ordre = 4),
                DocumentLineEntity(documentId = docId, ref = "FX-150", designation = "Découpe Forex 150x150", um = "U", quantite = 9.00, prixUnitaireHT = 300.00, totalHT = 2700.00, ordre = 5),
                DocumentLineEntity(documentId = docId, ref = "DEC-PLX", designation = "Découpe plexiglas", um = "U", quantite = 3.00, prixUnitaireHT = 300.00, totalHT = 900.00, ordre = 6),
                DocumentLineEntity(documentId = docId, ref = "DEC-CP", designation = "Découpe carton plume", um = "U", quantite = 17.00, prixUnitaireHT = 50.00, totalHT = 850.00, ordre = 7),
                DocumentLineEntity(documentId = docId, ref = "FX-5MM", designation = "Forex 5 mm 1/4 + découpe", um = "U", quantite = 1.00, prixUnitaireHT = 200.00, totalHT = 200.00, ordre = 8),
                DocumentLineEntity(documentId = docId, ref = "TR-ALU", designation = "Trophée en Aluco + Isorel", um = "U", quantite = 90.00, prixUnitaireHT = 17.00, totalHT = 1530.00, ordre = 9),
                DocumentLineEntity(documentId = docId, ref = "BDG-ABS", designation = "Badge ABS", um = "U", quantite = 62.00, prixUnitaireHT = 6.00, totalHT = 372.00, ordre = 10)
            )
            documentLineDao.insertLines(demoLines)

            // Also seed sample Bon de livraison: TD-26-00027
            val blDoc = DocumentEntity(
                type = DocumentType.BON_LIVRAISON,
                numero = "TD-26-00027",
                date = "26/10/2026",
                clientId = clientId1,
                tiersNom = "AGENCE HORIZON DESIGN",
                tiersAdresse = "Av. Mohammed V, Rabat",
                tiersIce = "001234567890001",
                status = DocumentStatus.LIVRE,
                sourceDocumentId = docId,
                totalHT = 0.0,
                totalTVA = 0.0,
                totalTTC = 0.0,
                avecCachet = true,
                notes = "Livraison conforme effectuée au siège client."
            )
            val blId = documentDao.insert(blDoc)
            val blLines = demoLines.take(5).mapIndexed { i, l ->
                l.copy(id = 0L, documentId = blId, ordre = i)
            }
            documentLineDao.insertLines(blLines)
        }
    }
}
