package com.dreamcatcher.travelwithai

import android.content.Context
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
        remoteConfigRepository.fetchApiKey { apiKey ->
            // Todo: Handle null state
            generativeModelRepository.initializeModel(apiKey!!)
        }
    }

    fun sendPrompt(messageType: MessageType, prompt: String? = null) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val location = locationRepository.getCurrentLocation()
//                val location = locationRepository.getFakeLocation()
                if (location == null) { _uiState.value = UiState.Error("Location not found.") }
                val enhancedPrompt = messageType.getMessage(location!!, prompt ?: "")
                generativeModelRepository.generateResponse(enhancedPrompt)?.let {
                    _uiState.value = UiState.Success(it)
                }
                // Todo: handle null state
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }

    fun getAIGeneratedImages(): Array<Int> {
        return imagesRepository.getAIGeneratedImages()
    }
}

enum class MessageType(private val template: String) {
    INITIAL(
        "Tell me interesting things about this location: " +
                "Latitude: {latitude}, Longitude: {longitude}. " +
                "Do not mention these values in response. Don't confirm you understand me. " +
                "Behave like a tourist guide. Tell me about history, tourist spots, restaurants, etc."
    ),
    HISTORY(
        "Tell me about history of this location: " +
                "Latitude: {latitude}, Longitude: {longitude}. " +
                "Do not mention these values in response. Don't confirm you understand me. " +
                "Behave like a tourist guide."
    ),
    RESTAURANTS(
        "Tell me about restaurants and interesting food spots in a walking distance from this location: " +
                "Latitude: {latitude}, Longitude: {longitude}. " +
                "Do not mention these values in response. Don't confirm you understand me. " +
                "Mention restaurants' names!"
    ),
    TOURIST_SPOTS(
        "Tell me about 5-6 most famous and important tourist spots/ attractions around this location that are worth to visit: " +
                "Latitude: {latitude}, Longitude: {longitude}. " +
                "Do not mention these values in response. Don't confirm you understand me. " +
                "Behave like a tourist guide."
    ),
    CUSTOM(
        "{prompt}. " +
                "Please answer in relation to the place: " +
                "Latitude: {latitude}, Longitude: {longitude}. " +
                "Do not mention these values in response. Don't confirm you understand me."
    );

    fun getMessage(location: Location, prompt: String = ""): String {
        return template
            .replace("{latitude}", location.latitude.toString())
            .replace("{longitude}", location.longitude.toString())
            .replace("{prompt}", prompt)
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
