package com.example.data.database

import androidx.room.*
import com.example.data.models.Product
import com.example.data.models.Service
import com.example.data.models.Settings
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {
    @Query("SELECT * FROM cached_services WHERE is_active = 1 ORDER BY price ASC")
    fun getActiveServices(): Flow<List<Service>>

    @Query("SELECT * FROM cached_services ORDER BY id ASC")
    fun getAllServices(): Flow<List<Service>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(services: List<Service>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(service: Service)

    @Query("DELETE FROM cached_services WHERE id = :serviceId")
    suspend fun deleteById(serviceId: Long)

    @Query("DELETE FROM cached_services WHERE id NOT IN (:remoteIds)")
    suspend fun deleteMissing(remoteIds: List<Long>)

    @Query("DELETE FROM cached_services")
    suspend fun deleteAll()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM cached_products WHERE is_active = 1 ORDER BY id DESC")
    fun getActiveProducts(): Flow<List<Product>>

    @Query("SELECT * FROM cached_products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product)

    @Query("DELETE FROM cached_products WHERE id = :productId")
    suspend fun deleteById(productId: Long)

    @Query("DELETE FROM cached_products WHERE id NOT IN (:remoteIds)")
    suspend fun deleteMissing(remoteIds: List<Long>)

    @Query("DELETE FROM cached_products")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM cached_settings")
    fun getAllSettings(): Flow<List<Settings>>

    @Query("SELECT * FROM cached_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingByKey(key: String): Settings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<Settings>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: Settings)

    @Query("DELETE FROM cached_settings")
    suspend fun deleteAll()
}

@Dao
interface ClientNoteDao {
    @Query("SELECT * FROM client_notes_local WHERE client_phone = :phone ORDER BY created_at DESC")
    fun getNotesForClient(phone: String): Flow<List<com.example.data.models.ClientNoteEntity>>

    @Insert
    suspend fun insert(note: com.example.data.models.ClientNoteEntity)

    @Query("SELECT * FROM client_notes_local WHERE synced = 0")
    suspend fun getUnsynced(): List<com.example.data.models.ClientNoteEntity>

    @Query("UPDATE client_notes_local SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}

@Database(
    entities = [Service::class, Product::class, Settings::class, com.example.data.models.ClientNoteEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serviceDao(): ServiceDao
    abstract fun productDao(): ProductDao
    abstract fun settingsDao(): SettingsDao
    abstract fun clientNoteDao(): ClientNoteDao
}
