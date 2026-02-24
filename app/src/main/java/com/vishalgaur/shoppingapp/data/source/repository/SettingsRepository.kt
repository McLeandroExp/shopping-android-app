package com.vishalgaur.shoppingapp.data.source.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.vishalgaur.shoppingapp.data.AppConfig
import com.vishalgaur.shoppingapp.data.CountryConfig
import com.vishalgaur.shoppingapp.data.Result
import kotlinx.coroutines.tasks.await

class SettingsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val settingsRef = firestore.collection("settings").document("global_config")

    suspend fun getAppConfig(): Result<AppConfig> {
        return try {
            val snapshot = settingsRef.get().await()
            if (snapshot.exists()) {
                val appConfig = snapshot.toObject(AppConfig::class.java)
                if (appConfig != null) {
                    Result.Success(appConfig)
                } else {
                    Result.Success(AppConfig())
                }
            } else {
                Result.Success(AppConfig())
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun saveAppConfig(config: AppConfig): Result<Boolean> {
        return try {
            settingsRef.set(config).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
