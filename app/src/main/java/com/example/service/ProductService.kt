package com.example.service

import com.example.data.models.Product
import com.example.data.models.Service
import com.example.data.models.Settings
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductService {
    private val api = SupabaseClient.api

    // PRODUCTS
    suspend fun getProducts(): List<Product> = withContext(Dispatchers.IO) {
        try {
            api.getProducts()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createProduct(product: Product, imageBytes1: ByteArray? = null, filename1: String? = null, imageBytes2: ByteArray? = null, filename2: String? = null): Result<Product> = withContext(Dispatchers.IO) {
        try {
            var url1 = product.imageUrl1
            var url2 = product.imageUrl2

            if (imageBytes1 != null && filename1 != null) {
                val reqBody = imageBytes1.toRequestBody("image/jpeg".toMediaTypeOrNull())
                try {
                    api.uploadProductImage(filename1, reqBody)
                    url1 = SupabaseClient.getPublicStorageUrl(filename1)
                } catch (e: Exception) {
                    url1 = SupabaseClient.getPublicStorageUrl(filename1)
                }
            }
            if (imageBytes2 != null && filename2 != null) {
                val reqBody = imageBytes2.toRequestBody("image/jpeg".toMediaTypeOrNull())
                try {
                    api.uploadProductImage(filename2, reqBody)
                    url2 = SupabaseClient.getPublicStorageUrl(filename2)
                } catch (e: Exception) {
                    url2 = SupabaseClient.getPublicStorageUrl(filename2)
                }
            }

            val finalProduct = product.copy(imageUrl1 = url1, imageUrl2 = url2)
            try {
                val created = api.createProduct(finalProduct)
                Result.success(created.firstOrNull() ?: finalProduct)
            } catch (e: Exception) {
                Result.success(finalProduct)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(id: Long, updates: Map<String, Any?>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            api.updateProduct("eq.$id", updates)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(id: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            api.deleteProduct("eq.$id")
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // SERVICES
    suspend fun getServices(): List<Service> = withContext(Dispatchers.IO) {
        try {
            api.getServices()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createService(service: Service): Result<Service> = withContext(Dispatchers.IO) {
        try {
            val created = api.createService(service)
            Result.success(created.firstOrNull() ?: service)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateService(id: Long, updates: Map<String, Any?>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            api.updateService("eq.$id", updates)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteService(id: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            api.deleteService("eq.$id")
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // SETTINGS (Manager contact, hours)
    suspend fun getSettings(): List<Settings> = withContext(Dispatchers.IO) {
        try {
            api.getSettings()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveSetting(key: String, value: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            try {
                api.updateSettingByKey("eq.$key", mapOf("value" to value))
            } catch (e: Exception) {
                api.createSetting(Settings(key = key, value = value))
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
