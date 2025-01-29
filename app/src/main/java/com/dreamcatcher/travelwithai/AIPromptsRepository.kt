package com.dreamcatcher.travelwithai

import android.location.Location

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
        "Tell me about restaurants and interesting food spots in a walking distance from this " +
                "location: " +
                "Latitude: {latitude}, Longitude: {longitude}. " +
                "Do not mention these values in response. Don't confirm you understand me. " +
                "Mention restaurants' names!"
    ),
    TOURIST_SPOTS(
        "Tell me about 5-6 most famous and important tourist spots/ attractions around this " +
                "location that are worth to visit: " +
                "Latitude: {latitude}, Longitude: {longitude}. " +
                "Do not mention these values in response. Don't confirm you understand me. " +
                "Behave like a tourist guide."
    ),
    SAFETY(
        "Tell me about risks I should be careful on, and behaviours should avoid as a tourist to " +
                "stay safe in this location. Be specific. You can tell me also what behaviours " +
                "should I avoid not to offend locals. Refer to this place specifically: " +
                "Latitude: {latitude}, Longitude: {longitude}. " +
                "Do not mention these values in response. Don't confirm you understand me. " +
                "Behave like a tourist guide."
    ),
    CUSTOM(
        "{prompt}. " +
                "Please answer in relation to the place: " +
                "Latitude: {latitude}, Longitude: {longitude}. " +
                "Do not mention these values in response. Don't confirm you understand me."
    ),
    PHOTO(
        "{prompt}. " +
                "Please tell me what is in the picture." +
                "Please answer in relation to the place:" +
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