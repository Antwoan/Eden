package com.example.ai

data class RecommendedModel(
    val id: String,
    val name: String,
    val parameterSize: String, // "1.1B", "3.2B", etc.
    val quantFormat: String, // "Q4_K_M"
    val memoryRequired: String, // "~1.2 GB RAM"
    val description: String,
    val huggingFaceUrl: String,
    val filename: String
)

data class ModelConfig(
    val modelName: String = "Llama 3.2 1B Instruct (Q4_K_M)",
    val modelPath: String = "",
    val isModelLoaded: Boolean = false,
    val temperature: Float = 0.75f,
    val topP: Float = 0.90f,
    val repeatPenalty: Float = 1.15f,
    val threadCount: Int = 4,
    val contextWindow: Int = 4096,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
)

val DEFAULT_SYSTEM_PROMPT = """
Eres un Narrador omnisciente y maestro de juego (Game Master) para una aventura de rol inmersiva y épica.
Controlas el entorno, el clima, las reacciones del mundo y las acciones/diálogos de todos los personajes no jugadores (PNJ).
Reglas cruciales:
1. Habla en tercera persona para la narración poética y descriptiva.
2. Cuando hable un PNJ, usa su nombre en negrita seguido de su cita entre comillas, e.g.: **Moira**: "¿Quién osa perturbar el santuario?".
3. Respeta estrictamente la personalidad, trasfondo y tono de cada PNJ presente en la escena.
4. Toma en consideración los resultados de las tiradas de dados y las estadísticas del Personaje del Jugador (Fuerza, Destreza, etc.).
5. Incorpora los datos del Lorebook y las notas de memoria fijadas en la conversación.
6. Nunca decidas las acciones del Personaje del Jugador ({{user}}); describe las consecuencias de lo que él intenta hacer.
7. Varía el ritmo y la tensión según la situación (combate, sigilo, campamento, persecución, terror cósmico, diálogo íntimo, investigación).
""".trimIndent()

object ModelManager {
    val recommendedModels = listOf(
        RecommendedModel(
            id = "llama-3.2-1b",
            name = "Llama 3.2 1B Instruct",
            parameterSize = "1.23B",
            quantFormat = "Q4_K_M",
            memoryRequired = "~850 MB RAM",
            description = "Ultra ligero y veloz para teléfonos móviles de gama media y alta. Gran coherencia en español y rol dinámico.",
            huggingFaceUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF",
            filename = "Llama-3.2-1B-Instruct-Q4_K_M.gguf"
        ),
        RecommendedModel(
            id = "llama-3.2-3b",
            name = "Llama 3.2 3B Instruct",
            parameterSize = "3.21B",
            quantFormat = "Q4_K_M",
            memoryRequired = "~2.2 GB RAM",
            description = "Excelente balance entre vocabulario descriptivo, prosa literaria de fantasía y memoria de contexto.",
            huggingFaceUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF",
            filename = "Llama-3.2-3B-Instruct-Q4_K_M.gguf"
        ),
        RecommendedModel(
            id = "qwen-2.5-1.5b",
            name = "Qwen 2.5 1.5B Instruct",
            parameterSize = "1.54B",
            quantFormat = "Q4_K_M",
            memoryRequired = "~1.1 GB RAM",
            description = "Fascinante seguimiento de instrucciones, diálogos de personajes y manejo de reglas de juego complejas.",
            huggingFaceUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF",
            filename = "qwen2.5-1.5b-instruct-q4_k_m.gguf"
        ),
        RecommendedModel(
            id = "gemma-2-2b",
            name = "Gemma 2 2B IT",
            parameterSize = "2.6B",
            quantFormat = "Q4_K_M",
            memoryRequired = "~1.7 GB RAM",
            description = "Desarrollado por Google, ofrece una prosa rica en matices poéticos y atmósfera para mundos oscuros o mágicos.",
            huggingFaceUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF",
            filename = "gemma-2-2b-it-Q4_K_M.gguf"
        ),
        RecommendedModel(
            id = "smollm2-1.7b",
            name = "SmolLM2 1.7B Instruct",
            parameterSize = "1.71B",
            quantFormat = "Q4_K_M",
            memoryRequired = "~1.2 GB RAM",
            description = "Compacto y especializado en razonamiento conversacional y respuestas rápidas para partidas ágiles.",
            huggingFaceUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF",
            filename = "SmolLM2-1.7B-Instruct-Q4_K_M.gguf"
        )
    )
}
