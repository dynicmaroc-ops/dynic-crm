package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.model.Client
import com.example.model.Company
import com.example.model.DocumentEntity
import com.example.model.DocumentLineEntity
import com.example.model.DocumentType
import com.example.model.ExpenseEntity
import com.example.model.PaymentEntity
import com.example.model.Product
import com.example.model.Supplier
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM company WHERE id = 1 LIMIT 1")
    fun getCompany(): Flow<Company?>

    @Query("SELECT * FROM company WHERE id = 1 LIMIT 1")
    suspend fun getCompanyDirect(): Company?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(company: Company)
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY nom ASC")
    fun getAll(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client): Long

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY nom ASC")
    fun getAll(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Supplier?

    @Query("SELECT * FROM suppliers WHERE ice = :ice OR nom = :nom LIMIT 1")
    suspend fun findByIceOrNom(ice: String, nom: String): Supplier?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(supplier: Supplier): Long

    @Update
    suspend fun update(supplier: Supplier)

    @Delete
    suspend fun delete(supplier: Supplier)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY designation ASC")
    fun getAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products WHERE ref = :ref OR designation LIKE :query LIMIT 1")
    suspend fun findByRefOrName(ref: String, query: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY id DESC")
    fun getAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE type = :type ORDER BY id DESC")
    fun getByType(type: DocumentType): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE clientId = :clientId ORDER BY id DESC")
    fun getByClient(clientId: Long): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity): Long

    @Update
    suspend fun update(document: DocumentEntity)

    @Delete
    suspend fun delete(document: DocumentEntity)

    @Query("SELECT COUNT(*) FROM documents WHERE type = :type")
    suspend fun countByType(type: DocumentType): Int
}

@Dao
interface DocumentLineDao {
    @Query("SELECT * FROM document_lines WHERE documentId = :documentId ORDER BY ordre ASC, id ASC")
    fun getLinesForDocument(documentId: Long): Flow<List<DocumentLineEntity>>

    @Query("SELECT * FROM document_lines WHERE documentId = :documentId ORDER BY ordre ASC, id ASC")
    suspend fun getLinesForDocumentDirect(documentId: Long): List<DocumentLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<DocumentLineEntity>)

    @Query("DELETE FROM document_lines WHERE documentId = :documentId")
    suspend fun deleteLinesForDocument(documentId: Long)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE referenceFacture = :ref AND supplierNom = :supplierNom LIMIT 1")
    suspend fun findDuplicate(ref: String, supplierNom: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY date DESC, id DESC")
    fun getAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE documentId = :documentId ORDER BY date DESC")
    fun getByDocument(documentId: Long): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Delete
    suspend fun delete(payment: PaymentEntity)
}

@Database(
    entities = [
        Company::class,
        Client::class,
        Supplier::class,
        Product::class,
        DocumentEntity::class,
        DocumentLineEntity::class,
        ExpenseEntity::class,
        PaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao
    abstract fun clientDao(): ClientDao
    abstract fun supplierDao(): SupplierDao
    abstract fun productDao(): ProductDao
    abstract fun documentDao(): DocumentDao
    abstract fun documentLineDao(): DocumentLineDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dynic_crm.db"
                ).build()
                instance = newInstance
                newInstance
            }
        }
    }
}
