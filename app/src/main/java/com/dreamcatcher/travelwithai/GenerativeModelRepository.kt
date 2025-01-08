package com.dreamcatcher.travelwithai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.asTextOrNull
import com.google.ai.client.generativeai.type.content

object ModelNames {
    const val GEMINI_1_5_FLASH = "gemini-1.5-flash"
    const val GEMINI_2_0_FLASH_EXP = "gemini-2.0-flash-exp"
}

class GenerativeModelRepository() {
    private var generativeModel: GenerativeModel? = null

    fun initializeModel(apiKey: String) {
        generativeModel = GenerativeModel(
            modelName = ModelNames.GEMINI_2_0_FLASH_EXP,
            apiKey = apiKey
        )
    }

    suspend fun generateResponse(prompt: String, image: Bitmap? = null): String? {
        return try {
            val response = generativeModel?.generateContent(content {
                text(prompt)
                image?.let { image(it) }
            })
            response?.candidates?.first()?.content?.parts?.first()?.asTextOrNull()
        } catch (e: Exception) {
            null // Handle error as needed
        }
    }
}
