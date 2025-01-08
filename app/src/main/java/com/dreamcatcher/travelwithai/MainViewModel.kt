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
    remoteConfigRepository: RemoteConfigRepository,
    private val generativeModelRepository: GenerativeModelRepository,
) : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        remoteConfigRepository.fetchApiKey(
            onSuccess = { apiKey ->
                generativeModelRepository.initializeModel(apiKey)
            },
            onError = {
                _uiState.value = UiState.Error("Problem with the server.")
            },
        )
    }

    fun sendPrompt(messageType: MessageType, prompt: String? = null, photo: Bitmap? = null) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val location = locationRepository.getCurrentLocation()
//                val location = locationRepository.getFakeLocation()
                if (location == null) {
                    _uiState.value = UiState.Error("Location not found.")
                    return@launch
                }

                if (messageType == MessageType.PHOTO && photo == null) {
                    _uiState.value = UiState.Error("Picture taking error.")
                    return@launch
                }

                val enhancedPrompt = enhancePrompt(messageType, location, prompt)
                generativeModelRepository.generateResponse(enhancedPrompt, photo)?.let {
                    cleanResponseText(it).let {
                        _uiState.value = UiState.Success(it)
                    }
                }
                // Todo: handle null state
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }

    private fun cleanResponseText(originalText: String) = originalText.replace("**", "")

    private fun enhancePrompt(messageType: MessageType, location: Location, prompt: String?) =
        messageType.getMessage(location, prompt ?: "")

    fun getAIGeneratedImages(): Array<Int> {
        return imagesRepository.getAIGeneratedImages()
    }
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
