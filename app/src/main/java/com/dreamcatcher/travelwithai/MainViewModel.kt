package com.dreamcatcher.travelwithai

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val locationRepository: LocationRepository,
    private val imagesRepository: ImagesRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val generativeModelRepository: GenerativeModelRepository,
) : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        initializeGenerativeModel()
    }

    private fun initializeGenerativeModel() {
        remoteConfigRepository.fetchApiKey(
            onSuccess = { apiKey -> generativeModelRepository.initializeModel(apiKey) },
            onError = { _uiState.value = UiState.Error("Problem with the server.") },
        )
    }

    fun getAIGeneratedImages(): Array<Int> {
        return imagesRepository.getAIGeneratedImages()
    }

    fun sendPrompt(messageType: MessageType, prompt: String? = null, photo: Bitmap? = null) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val location = locationRepository.getCurrentLocation()
//                val location = locationRepository.getFakeLocation() // For screenshots
                if (location == null) {
                    _uiState.value = UiState.Error("Location not found.")
                    return@launch
                }
                if (messageType == MessageType.PHOTO && photo == null) {
                    _uiState.value = UiState.Error("Picture taking error.")
                    return@launch
                }
                val enhancedPrompt = enhancePrompt(messageType, location, prompt)
                val response = generativeModelRepository.generateResponse(enhancedPrompt, photo)
                if (response != null) _uiState.value = UiState.Success(response)
                else _uiState.value = UiState.Error("Error (received prompt is empty).")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Sending prompt error.")
            }
        }
    }

    private fun enhancePrompt(messageType: MessageType, location: Location, prompt: String?) =
        messageType.getMessage(location, prompt ?: "").replace("**", "")
}

class BakingViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                LocationRepository(context),
                ImagesRepository(),
                RemoteConfigRepository(),
                GenerativeModelRepository(),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
