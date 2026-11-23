package com.supernatural.bwriter.data

data class StudioSystemConfig(
    val geminiApiKey: String = "",
    val geminiModel: String = DEFAULT_MODEL,
    val updatedAtMillis: Long = 0L
) {
    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }
}

object SystemAiConfig {
    @Volatile
    var apiKey: String = ""
        private set

    @Volatile
    var model: String = StudioSystemConfig.DEFAULT_MODEL
        private set

    fun update(config: StudioSystemConfig) {
        apiKey = config.geminiApiKey
        model = config.geminiModel.ifBlank { StudioSystemConfig.DEFAULT_MODEL }
    }
}
