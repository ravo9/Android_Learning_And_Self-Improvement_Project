package com.dreamcatcher.travelwithai

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.ktx.Firebase

class RemoteConfigRepository {
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings { minimumFetchIntervalInSeconds = 3600 }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    fun fetchApiKey(onSuccess: (String) -> Unit, onError: (Throwable) -> Unit) {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val apiKey = remoteConfig.getString("api_key")
                    onSuccess(apiKey)
                } else {
                    onError(Throwable()) // Todo
                }
            }
    }
}
