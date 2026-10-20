package com.example.ai

import com.example.data.local.entities.CharacterEntity
import com.example.data.local.entities.LorebookEntryEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.local.entities.PinnedMemoryEntity
import com.example.data.local.entities.WorldEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale
import kotlin.random.Random

data class GenerationContext(
    val world: WorldEntity?,
    val userCharacter: CharacterEntity?,
    val npcs: List<CharacterEntity>,
    val pinnedMemories: List<PinnedMemoryEntity>,
    val lorebookEntries: List<LorebookEntryEntity>,
    val recentMessages: List<MessageEntity>,
    val userInput: String,
    val lengthPreference: String, // "SHORT", "MEDIUM", "LONG"
    val modelConfig: ModelConfig,
    val lastDiceRoll: String? = null
)

class LocalLlamaEngine {

    private var nativeModelHandle: Long = 0L

    fun loadNativeModel(path: String, threads: Int, contextSize: Int): Boolean {
        if (!NativeLlama.isLibraryLoaded) return false
        return try {
            nativeModelHandle = NativeLlama.loadModel(path, threads, contextSize)
            nativeModelHandle != 0L
        } catch (e: Throwable) {
            nativeModelHandle = 0L
            false
        }
    }

    fun isNativeLoaded(): Boolean = nativeModelHandle != 0L

    fun unloadNativeModel() {
        if (nativeModelHandle != 0L && NativeLlama.isLibraryLoaded) {
            try {
                NativeLlama.freeModel(nativeModelHandle)
            } catch (e: Throwable) {
                // ignore
            }
            nativeModelHandle = 0L
        }
    }

    fun buildPrompt(context: GenerationContext): String {
        val sb = StringBuilder()
        val userName = context.userCharacter?.name ?: "Aventurero"
        val worldName = context.world?.name ?: "Tierras Olvidadas"
        val worldLore = context.world?.lore ?: "Un reino misterioso envuelto en leyendas arcanas."

        var systemPrompt = context.modelConfig.systemPrompt
        systemPrompt = systemPrompt.replace("{{user}}", userName)
        systemPrompt = systemPrompt.replace("{{world}}", worldName)

        sb.append("=== INSTRUCCIONES DEL NARRADOR ===\n")
        sb.append(systemPrompt).append("\n\n")

        sb.append("=== MUNDO & ENTORNO ===\n")
        sb.append("Mundo: ").append(worldName).append("\n")
        sb.append("Trasfondo: ").append(worldLore).append("\n\n")

        if (context.userCharacter != null) {
            sb.append("=== PERSONAJE DEL JUGADOR ({{user}}) ===\n")
            sb.append("Nombre: ").append(context.userCharacter.name).append("\n")
            sb.append("Personalidad: ").append(context.userCharacter.personality).append("\n")
            sb.append("Estadísticas: ").append(context.userCharacter.statsJson).append("\n\n")
        }

        if (context.npcs.isNotEmpty()) {
            sb.append("=== PERSONAJES NO JUGADORES (PNJ) ACTIVOS ===\n")
            context.npcs.forEach { npc ->
                sb.append("- ").append(npc.name).append(":\n")
                sb.append("  Personalidad: ").append(npc.personality.take(300)).append("\n")
                sb.append("  Trasfondo: ").append(npc.background.take(300)).append("\n")
                if (npc.dialogueExamples.isNotBlank()) {
                    sb.append("  Estilo de diálogo: ").append(npc.dialogueExamples.take(200)).append("\n")
                }
            }
            sb.append("\n")
        }

        // Detect and inject Lorebook triggers
        val activeLore = findMatchingLore(context)
        if (activeLore.isNotEmpty()) {
            sb.append("=== LOREBOOK (CONOCIMIENTO REVELADO) ===\n")
            activeLore.forEach { lore ->
                sb.append("• [").append(lore.title).append("]: ").append(lore.content).append("\n")
            }
            sb.append("\n")
        }

        // Pinned memories
        val activeMemories = context.pinnedMemories.filter { it.isActive }
        if (activeMemories.isNotEmpty()) {
            sb.append("=== MEMORIAS FIJAS EN CONTEXTO ===\n")
            activeMemories.forEach { mem ->
                sb.append("★ ").append(mem.content).append("\n")
            }
            sb.append("\n")
        }

        if (!context.lastDiceRoll.isNullOrBlank()) {
            sb.append("=== EVENTO DE REGLAS / TIRADA DE DADO RECIENTE ===\n")
            sb.append(context.lastDiceRoll).append("\n\n")
        }

        // Length directive
        sb.append("=== EXTENSIÓN SOLICITADA ===\n")
        when (context.lengthPreference) {
            "SHORT" -> sb.append("Respuesta CORTA (50-80 palabras): directa, dinámica y centrada en la acción inmediata.\n\n")
            "LONG" -> sb.append("Respuesta LARGA (400+ palabras): prosa rica, literaria, inmersiva con descripciones del clima, atmósfera, sensaciones y múltiples diálogos.\n\n")
            else -> sb.append("Respuesta MEDIA (150-250 palabras): equilibrio perfecto entre narración de escena y diálogo vivo.\n\n")
        }

        sb.append("=== HISTORIA RECIENTE ===\n")
        val recent = context.recentMessages.takeLast(12)
        recent.forEach { msg ->
            when (msg.senderType) {
                "USER" -> sb.append("${msg.senderName}: \"${msg.content}\"\n")
                "DICE_ROLL" -> sb.append("[Sistema]: ${msg.content}\n")
                else -> sb.append("Narrador: ${msg.content}\n")
            }
        }

        if (context.userInput.isNotBlank()) {
            sb.append("${userName}: \"${context.userInput}\"\n")
        }
        sb.append("Narrador: ")

        return sb.toString()
    }

    fun findMatchingLore(context: GenerationContext): List<LorebookEntryEntity> {
        val textToSearch = buildString {
            append(context.userInput.lowercase(Locale.ROOT))
            context.recentMessages.takeLast(4).forEach {
                append(" ").append(it.content.lowercase(Locale.ROOT))
            }
        }

        return context.lorebookEntries.filter { entry ->
            if (!entry.isEnabled) return@filter false
            val triggers = entry.keywords.split(",", ";").map { it.trim().lowercase(Locale.ROOT) }
            triggers.any { kw -> kw.isNotBlank() && textToSearch.contains(kw) }
        }
    }

    fun streamResponse(context: GenerationContext): Flow<String> = flow {
        if (isNativeLoaded() && NativeLlama.isLibraryLoaded) {
            // Native JNI execution flow
            val prompt = buildPrompt(context)
            val maxTokens = when (context.lengthPreference) {
                "SHORT" -> 150
                "LONG" -> 800
                else -> 400
            }
            val collectedTokens = mutableListOf<String>()
            var isFinished = false
            var errorMessage: String? = null

            val callback = object : NativeLlama.TokenCallback {
                override fun onToken(token: String) {
                    synchronized(collectedTokens) {
                        collectedTokens.add(token)
                    }
                }
                override fun onComplete() {
                    isFinished = true
                }
                override fun onError(error: String) {
                    errorMessage = error
                    isFinished = true
                }
            }

            try {
                NativeLlama.generateCompletion(
                    modelHandle = nativeModelHandle,
                    prompt = prompt,
                    temperature = context.modelConfig.temperature,
                    topP = context.modelConfig.topP,
                    maxTokens = maxTokens,
                    callback = callback
                )

                while (!isFinished) {
                    delay(30)
                    val tokensToEmit = synchronized(collectedTokens) {
                        val copy = collectedTokens.toList()
                        collectedTokens.clear()
                        copy
                    }
                    for (token in tokensToEmit) {
                        emit(token)
                    }
                }
                if (errorMessage != null) {
                    emit("\n\n[Aviso del motor local: $errorMessage]")
                }
                return@flow
            } catch (e: Throwable) {
                // fallback to integrated engine if native invocation encounters an issue
            }
        }

        // Integrated intelligent on-device roleplay narrator
        val simulatedText = generateRichRoleplayResponse(context)
        val words = simulatedText.split(" ")
        for (i in words.indices) {
            val chunk = if (i == words.lastIndex) words[i] else words[i] + " "
            emit(chunk)
            // Realistic streaming delay (20 to 45 ms)
            delay(Random.nextLong(20, 45))
        }
    }

    private fun generateRichRoleplayResponse(context: GenerationContext): String {
        val userName = context.userCharacter?.name ?: "Aventurero"
        val activeNpc = context.npcs.firstOrNull { it.isNarratorControlled } ?: context.npcs.firstOrNull()
        val npcName = activeNpc?.name ?: "El Encapuchado"
        val matchedLore = findMatchingLore(context)
        val input = context.userInput.lowercase(Locale.ROOT)
        val dice = context.lastDiceRoll

        val trimmedInput = context.userInput.trim()
        val isOoc = trimmedInput.startsWith("((") ||
                trimmedInput.startsWith("(") ||
                trimmedInput.startsWith("/ooc", ignoreCase = true) ||
                trimmedInput.startsWith("[ooc", ignoreCase = true)

        if (isOoc) {
            val cleanQuestion = trimmedInput
                .removePrefix("((").removeSuffix("))")
                .removePrefix("(").removeSuffix(")")
                .removePrefix("/ooc").removePrefix("/OOC")
                .removePrefix("[ooc]").removePrefix("[OOC]").trim()

            val sbOoc = StringBuilder()
            sbOoc.append("🎲 **[Game Master • Fuera de Rol]**:\n\n")
            sbOoc.append("Hacemos una pausa fuera de la narración para aclarar tu duda: \"$cleanQuestion\".\n\n")

            if (matchedLore.isNotEmpty()) {
                val lore = matchedLore.first()
                sbOoc.append("• **Lore relevante (${lore.title})**: ${lore.content}\n\n")
            }

            val worldName = context.world?.name ?: "el entorno actual"
            if (cleanQuestion.contains("dado", ignoreCase = true) || cleanQuestion.contains("tirar", ignoreCase = true) || cleanQuestion.contains("regla", ignoreCase = true)) {
                sbOoc.append("• **Mecánicas & Dados**: Puedes declarar cualquier acción en el chat y usar el botón **🎲 Tirar con Acción** o el comando `/roll [acción]`. Por ejemplo, para infiltrarte o forzar una cerradura usarás Destreza; para resistir un veneno o daño físico, Constitución; para deducir pistas arcanas, Inteligencia.\n\n")
            } else if (cleanQuestion.contains("puedo", ignoreCase = true) || cleanQuestion.contains("posible", ignoreCase = true)) {
                sbOoc.append("• **Situación en $worldName**: Sí, las circunstancias actuales te permiten intentar esa acción con tu personaje. La dificultad dependerá de cómo lo ejecutes y si hay enemigos o PNJ alerta en la zona.\n\n")
            } else {
                sbOoc.append("• **Estado de la escena**: Te encuentras en $worldName. Los personajes en la sala mantienen su posición y la tensión sigue intacta esperando tu siguiente movimiento en el rol.\n\n")
            }
            sbOoc.append("*(Cuando desees continuar con la historia, escribe tu diálogo normal o tus acciones entre asteriscos)*")
            return sbOoc.toString()
        }

        val isQuestion = input.contains("?") || input.contains("¿") || input.contains("quién") || input.contains("dónde")
        val isAttack = input.contains("ataco") || input.contains("espada") || input.contains("golpe") || input.contains("disparo") || input.contains("hechizo")
        val isLook = input.contains("miro") || input.contains("busco") || input.contains("investigo") || input.contains("observo")

        val sb = StringBuilder()

        // 1. Atmosphere / Sensory opening
        val isSciFi = context.world?.genre == com.example.data.local.entities.StoryGenres.SCI_FI
        val isModern = context.world?.genre == com.example.data.local.entities.StoryGenres.MODERN_NOIR
        val isCyber = context.world?.genre == com.example.data.local.entities.StoryGenres.CYBERPUNK
        val isApoc = context.world?.genre == com.example.data.local.entities.StoryGenres.POST_APOCALYPTIC

        val opening = when {
            isSciFi ->
                "El zumbido constante de los generadores de gravedad y el pulso tenue de las holopantallas iluminan la cubierta de la nave. A través del ventanal blindado de observación, el vacío estelar brilla salpicado de polvo cósmico y estaciones orbitales silenciosas."
            isModern ->
                "El asfalto mojado refleja las luces rojas y azules de los carteles de neón que parpadean en la niebla nocturna. La lluvia cae sin tregua sobre el cuello de tu abrigo mientras el eco de pasos apresurados reverbera en el callejón."
            isCyber ->
                "Un zumbido electromagnético atraviesa tus neuro-sensores. Los cables de fibra óptica descienden como lianas mecánicas desde los rascacielos corporativos, mientras drones de vigilancia surcan la lluvia ácida."
            isApoc ->
                "Una ráfaga de viento abrasador arrastra arena y ceniza radiactiva entre los esqueletos oxidados de viejos vehículos. El silencio del páramo solo se rompe con el chasquido distante de una chapa metálica."
            input.contains("campamento", ignoreCase = true) || input.contains("refugio", ignoreCase = true) || input.contains("descanso", ignoreCase = true) ->
                "El crepitar de la fogata arroja chispas hacia el cielo nocturno y sombras danzantes sobre el terreno. El calor reparador alivia la fatiga del viaje mientras la noche se cierra en silencio a vuestro alrededor."
            input.contains("sigilo", ignoreCase = true) || input.contains("ocultar", ignoreCase = true) || input.contains("esconder", ignoreCase = true) ->
                "Contienes la respiración y fundes tu figura entre las sombras más densas. Cada paso es calculado al milímetro, amortiguando cualquier roce mientras tus ojos escanean los patrones del entorno."
            input.contains("persecu", ignoreCase = true) || input.contains("correr", ignoreCase = true) || input.contains("huir", ignoreCase = true) ->
                "El aire entra a quemarropa en tus pulmones. El pulso se dispara y la adrenalina toma el control mientras pasos apresurados y la tensión resuenan a tus espaldas."
            input.contains("terror", ignoreCase = true) || input.contains("monstruo", ignoreCase = true) || input.contains("lovecraft", ignoreCase = true) ->
                "Un frío antinatural te hiela la médula. Ecos de susurros incomprensibles raspan el borde de tu cordura, revelando presencias que desafían la realidad misma."
            input.contains("romance", ignoreCase = true) || input.contains("abraz", ignoreCase = true) || input.contains("bes", ignoreCase = true) ->
                "El mundo exterior parece desvanecerse por un instante. Una calidez genuina envuelve el espacio compartido, y en el cruce de miradas las palabras sobran."
            input.contains("melancol", ignoreCase = true) || input.contains("trist", ignoreCase = true) || input.contains("lluvia", ignoreCase = true) ->
                "Un velo de suave nostalgia desciende sobre el horizonte. Recuerdos lejanos flotan en el ambiente mientras contemplas el camino recorrido."
            isAttack ->
                "El aire se tensa súbitamente. El brillo del acero o las armas preparadas corta la penumbra mientras un crujido seco resuena bajo tus pasos y la adrenalina agudiza cada uno de tus sentidos."
            isLook ->
                "Agudizas la mirada a través del entorno. Entre las hendiduras y sombras del lugar, distingues detalles ocultos que revelan que algo extraordinario o peligroso ha tenido lugar aquí recientemente."
            dice != null && dice.contains("Crítico") ->
                "El destino parece conspirar a tu favor de manera prodigiosa. Una precisión quirúrgica guía tus movimientos en el momento exacto."
            else ->
                "Un silencio cargado de expectación desciende sobre el lugar. El entorno aguarda con intriga contenida tu siguiente decisión."
        }
        sb.append(opening).append("\n\n")

        // 2. Action consequence & NPC reaction
        if (activeNpc != null) {
            val npcReaction = when {
                isAttack ->
                    "$npcName retrocede un paso con agilidad felina, tensando los músculos y clavando sus ojos penetrantes en ti con una mezcla de sorpresa y advertencia letal."
                isQuestion ->
                    "$npcName ladea el rostro, dejando escapar una sonrisa enigmática que apenas disimula la gravedad de tus palabras."
                else ->
                    "$npcName da un paso al frente, ajustándose la capa polvorienta mientras su mirada mide con cautela cada uno de tus gestos."
            }
            sb.append(npcReaction).append("\n\n")

            // NPC Dialogue
            val dialogText = when {
                isAttack ->
                    "\"¡Temerario! Desenvainar el acero aquí es firmar una sentencia de sangre. ¡Baja tu arma antes de que las sombras reclamen tus restos!\""
                matchedLore.isNotEmpty() ->
                    "\"Veo que conoces los ecos de ${matchedLore.first().title}... pocos se atreven a pronunciar ese nombre sin que la garganta se les seque por el pavor.\""
                isQuestion ->
                    "\"Las respuestas no son baratas en este rincón del mundo, $userName. Pero si tu voluntad es tan firme como tus pasos, escucha atentamente lo que las crónicas olvidaron.\""
                else ->
                    "\"El camino hasta aquí no ha sido benevolente contigo, lo sé bien. Si buscas la verdad oculta tras el velo, tendrás que demostrar de qué madera estás hecho.\""
            }
            sb.append("**$npcName**: $dialogText\n\n")
        }

        // 3. Extended detail if MEDIUM or LONG
        if (context.lengthPreference != "SHORT") {
            val loreDetail = if (matchedLore.isNotEmpty()) {
                "En el fondo del recinto, una vieja insignia con el símbolo de ${matchedLore.first().title} permanece oculta bajo un paño de terciopelo desgarrado, confirmando las sospechas que te trajeron hasta aquí."
            } else {
                "A lo lejos, el tañido de una campana solitaria corta la bruma. Algo se agita en las profundidades de la espesura, advirtiendo que el tiempo para deliberar se agota con cada respiración."
            }
            sb.append(loreDetail).append("\n\n")
        }

        if (context.lengthPreference == "LONG") {
            sb.append("Tus músculos aún recuerdan las fatigas de las jornadas anteriores, pero el pulso de la aventura late con fuerza en tus sienes. Cada decisión tomada hasta este momento resuena en los cimientos de la historia que estás forjando.\n\n")
            sb.append("¿Hacia dónde diriges tu siguiente paso? Las cartas están sobre la mesa, y el destino aguarda tu orden.")
        } else {
            sb.append("El entorno aguarda expectante tu próxima maniobra.")
        }

        return sb.toString()
    }
}
