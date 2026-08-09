package com.example.data.repository

import com.example.data.database.SettingsDao
import com.example.data.models.Settings
import com.example.service.ProductService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

class SettingsRepository(private val settingsDao: SettingsDao) {
    private val productService = ProductService()

    val settings: Flow<List<Settings>> = settingsDao.getAllSettings().onStart {
        refreshSettings()
    }

    suspend fun refreshSettings() {
        try {
            val remote = productService.getSettings()
            if (remote.isNotEmpty()) {
                settingsDao.insertAll(remote)
            }
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    suspend fun getSettingValue(key: String, defaultValue: String): String {
        val cached = settingsDao.getSettingByKey(key)
        return cached?.value ?: defaultValue
    }

    suspend fun saveSetting(key: String, value: String): Result<Boolean> {
        val res = productService.saveSetting(key, value)
        if (res.isSuccess) {
            val setting = Settings(id = key.hashCode().toLong(), key = key, value = value)
            settingsDao.insert(setting)
        }
        return res
    }
}
