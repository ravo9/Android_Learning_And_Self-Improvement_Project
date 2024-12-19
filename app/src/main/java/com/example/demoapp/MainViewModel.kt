package com.example.demoapp

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.asTextOrNull
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object ModelNames {
    const val GEMINI_1_5_FLASH = "gemini-1.5-flash"
    const val GEMINI_2_0_FLASH_EXP = "gemini-2.0-flash-exp"
}

class BakingViewModel(
    private val locationRepository: LocationRepository,
    private val generativeModel: GenerativeModel = GenerativeModel(
        modelName = ModelNames.GEMINI_2_0_FLASH_EXP,
        apiKey = BuildConfig.apiKey
    )
) : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun sendLocationBasedPrompt() {
        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val location: Location? = locationRepository.getCurrentLocation()
                if (location != null) {
                    val prompt = createLocationPrompt(location)
                    sendPrompt(prompt = prompt)
                } else {
                    _uiState.value = UiState.Error("Location not found.")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error fetching location: ${e.localizedMessage}")
            }
        }
    }

    private fun createLocationPrompt(location: Location): String {
        return "Tell me interesting things about this location: " +
                "Latitude: ${location.latitude}, Longitude: ${location.longitude}" +
                "Do not mention these values in response. Tell me about tourist spots, restaurants etc."
    }

    fun sendPrompt(
        bitmap: Bitmap? = null,
        prompt: String
    ) {
        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                var enhancedPrompt = prompt
                locationRepository.getCurrentLocation()?.let {
                    enhancedPrompt += ". Please answer in relation to the place $it but do not mention these values in answer."
                }
                val response = generativeModel.generateContent(content { text(enhancedPrompt) })
                response.candidates.first().content.parts.first().asTextOrNull()?.let {
                    _uiState.value = UiState.Success(it)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }
}

class BakingViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BakingViewModel::class.java)) {
            return BakingViewModel(LocationRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
