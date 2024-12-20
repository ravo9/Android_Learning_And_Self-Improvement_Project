package com.dreamcatcher.travelwithai.ui.theme

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.ktx.Firebase

class RemoteConfigRepository {
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // Adjust based on your needs
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    fun fetchApiKey(onComplete: (String?) -> Unit) {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val apiKey = remoteConfig.getString("api_key")
                    onComplete(apiKey)
                } else {
                    onComplete(null) // Fetch failed
                }
            }
    }
}
