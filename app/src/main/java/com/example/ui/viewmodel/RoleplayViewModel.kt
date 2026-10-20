package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiService
import com.example.ai.GenerationContext
import com.example.ai.LocalLlamaEngine
import com.example.ai.ModelConfig
import com.example.ai.ModelManager
import com.example.audio.AmbientAudioManager
import com.example.audio.AudioTrackItem
import com.example.data.local.AppDatabase
import com.example.data.local.entities.CharacterEntity
import com.example.data.local.entities.LorebookEntryEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.local.entities.PinnedMemoryEntity
import com.example.data.local.entities.PlaylistEntity
import com.example.data.local.entities.StoryLogEntity
import com.example.data.local.entities.WorldEntity
import com.example.data.local.entities.WorldPlaceEntity
import com.example.data.repository.RoleplayRepository
import com.example.dice.DiceSystem
import com.example.dice.DiceType
import com.example.dice.RollResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

enum class AppThemeSetting {
    CHARACTER_AI_DARK,
    DARK_FANTASY,
    PARCHMENT,
    CYBERPUNK,
    OLED_BLACK
}

data class MessageNode(
    val message: MessageEntity,
    val branchIndex: Int,
    val totalBranches: Int,
    val parentId: Long?
)

class RoleplayViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = RoleplayRepository(db)
    val llamaEngine = LocalLlamaEngine()
    val audioManager = AmbientAudioManager(application)
    val geminiService = GeminiService(application)

    // Current State
    val activeWorld: StateFlow<WorldEntity?> = repository.activeWorld
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allWorlds: StateFlow<List<WorldEntity>> = repository.allWorlds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val npcs: StateFlow<List<CharacterEntity>> = repository.npcs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userCharacters: StateFlow<List<CharacterEntity>> = repository.userCharacters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCharacters: StateFlow<List<CharacterEntity>> = combine(npcs, userCharacters) { n, u -> n + u }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedUserCharacter: StateFlow<CharacterEntity?> = repository.selectedUserCharacter
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _allMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val allMessages: StateFlow<List<MessageEntity>> = _allMessages.asStateFlow()

    private val _pinnedMemories = MutableStateFlow<List<PinnedMemoryEntity>>(emptyList())
    val pinnedMemories: StateFlow<List<PinnedMemoryEntity>> = _pinnedMemories.asStateFlow()

    private val _lorebook = MutableStateFlow<List<LorebookEntryEntity>>(emptyList())
    val lorebook: StateFlow<List<LorebookEntryEntity>> = _lorebook.asStateFlow()

    private val _storyLogs = MutableStateFlow<List<StoryLogEntity>>(emptyList())
    val storyLogs: StateFlow<List<StoryLogEntity>> = _storyLogs.asStateFlow()

    private val _places = MutableStateFlow<List<WorldPlaceEntity>>(emptyList())
    val places: StateFlow<List<WorldPlaceEntity>> = _places.asStateFlow()

    // Branch selection map: parentId -> selected branchIndex
    private val _branchSelections = MutableStateFlow<Map<Long?, Int>>(emptyMap())
    val branchSelections: StateFlow<Map<Long?, Int>> = _branchSelections.asStateFlow()

    // Streaming state
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _streamingMessageText = MutableStateFlow("")
    val streamingMessageText: StateFlow<String> = _streamingMessageText.asStateFlow()

    // Length mode
    private val _lengthPreference = MutableStateFlow("MEDIUM")
    val lengthPreference: StateFlow<String> = _lengthPreference.asStateFlow()

    // Model settings
    private val _modelConfig = MutableStateFlow(ModelConfig())
    val modelConfig: StateFlow<ModelConfig> = _modelConfig.asStateFlow()

    // Theme & Appearance
    private val _appTheme = MutableStateFlow(AppThemeSetting.CHARACTER_AI_DARK)
    val appTheme: StateFlow<AppThemeSetting> = _appTheme.asStateFlow()

    private val _chatBackgroundUri = MutableStateFlow<String?>(null)
    val chatBackgroundUri: StateFlow<String?> = _chatBackgroundUri.asStateFlow()

    // Auto-save notification
    private val _autoSaveNotice = MutableStateFlow<String?>(null)
    val autoSaveNotice: StateFlow<String?> = _autoSaveNotice.asStateFlow()

    private var messageCounterSinceSave = 0

    // Last dice roll
    private val _lastRollResult = MutableStateFlow<RollResult?>(null)
    val lastRollResult: StateFlow<RollResult?> = _lastRollResult.asStateFlow()

    // Gemini Media Generation State
    private val _isGeneratingMedia = MutableStateFlow(false)
    val isGeneratingMedia: StateFlow<Boolean> = _isGeneratingMedia.asStateFlow()

    private val _mediaStatus = MutableStateFlow<String?>(null)
    val mediaStatus: StateFlow<String?> = _mediaStatus.asStateFlow()

    // Active conversation tree path
    val currentConversationPath: StateFlow<List<MessageNode>> = combine(
        _allMessages,
        _branchSelections
    ) { messages, selections ->
        buildActiveConversationPath(messages, selections)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedInitialDataIfNeeded()
            observeWorldData()
        }
    }

    private fun observeWorldData() {
        viewModelScope.launch {
            activeWorld.collect { world ->
                if (world != null) {
                    launch { repository.getMessagesForWorld(world.id).collect { _allMessages.value = it } }
                    launch { repository.getPinnedMemories(world.id).collect { _pinnedMemories.value = it } }
                    launch { repository.getLorebookForWorld(world.id).collect { _lorebook.value = it } }
                    launch { repository.getStoryLogs(world.id).collect { _storyLogs.value = it } }
                    launch { repository.getPlacesForWorld(world.id).collect { _places.value = it } }
                }
            }
        }
    }

    private fun buildActiveConversationPath(
        messages: List<MessageEntity>,
        selections: Map<Long?, Int>
    ): List<MessageNode> {
        if (messages.isEmpty()) return emptyList()

        val groupedByParent = messages.groupBy { it.parentId }
        val path = mutableListOf<MessageNode>()

        var currentParentId: Long? = null
        while (true) {
            val siblings = groupedByParent[currentParentId]?.sortedBy { it.branchIndex }
            if (siblings.isNullOrEmpty()) break

            val selectedIndex = selections[currentParentId] ?: 0
            val chosen = siblings.getOrNull(selectedIndex) ?: siblings.first()

            path.add(
                MessageNode(
                    message = chosen,
                    branchIndex = siblings.indexOf(chosen),
                    totalBranches = siblings.size,
                    parentId = currentParentId
                )
            )
            currentParentId = chosen.id
        }
        return path
    }

    fun setLengthPreference(pref: String) {
        _lengthPreference.value = pref
    }

    fun setTheme(theme: AppThemeSetting) {
        _appTheme.value = theme
    }

    fun setChatBackground(uri: String?) {
        _chatBackgroundUri.value = uri
    }

    fun switchBranch(parentId: Long?, targetIndex: Int) {
        val map = _branchSelections.value.toMutableMap()
        map[parentId] = targetIndex
        _branchSelections.value = map
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isStreaming.value) return
        val world = activeWorld.value ?: return
        val userChar = selectedUserCharacter.value
        val userName = userChar?.name ?: "Tú"

        // Handle slash commands
        if (trimmed.startsWith("/")) {
            val parts = trimmed.split(" ", limit = 2)
            val command = parts[0].lowercase()
            val argument = parts.getOrNull(1)?.trim() ?: ""

            when (command) {
                "/mapa", "/map" -> {
                    val prompt = argument.ifBlank { "Plano táctico y estratégico de la zona actual en ${world.name}" }
                    generateGeminiMedia(prompt, isMap = true)
                    return
                }
                "/imagen", "/img", "/foto" -> {
                    val prompt = argument.ifBlank { "Escena dramática en ${world.name} con ambiente de rol" }
                    generateGeminiMedia(prompt, isMap = false)
                    return
                }
                "/roll", "/dado" -> {
                    val action = argument.ifBlank { "Acción de personaje" }
                    rollDiceWithAction(actionText = action, diceType = DiceType.D20)
                    return
                }
                "/stats", "/atributos" -> {
                    val userC = selectedUserCharacter.value
                    if (userC == null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            insertSystemNotice(world.id, "⚠️ No tienes ningún Personaje Jugador seleccionado actualmente.")
                        }
                        return
                    }
                    val msg = formatCharacterStats(userC)
                    viewModelScope.launch(Dispatchers.IO) {
                        insertSystemNotice(world.id, msg)
                    }
                    return
                }
                "/stat", "/atributo" -> {
                    val userC = selectedUserCharacter.value
                    if (userC == null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            insertSystemNotice(world.id, "⚠️ Selecciona un Personaje Jugador antes de modificar atributos.")
                        }
                        return
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        val msg = applyStatChange(userC, argument)
                        insertSystemNotice(world.id, msg)
                    }
                    return
                }
                "/hp", "/vida" -> {
                    val userC = selectedUserCharacter.value
                    if (userC == null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            insertSystemNotice(world.id, "⚠️ Selecciona un Personaje Jugador para modificar puntos de vida.")
                        }
                        return
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        val msg = applyStatChange(userC, "HP $argument")
                        insertSystemNotice(world.id, msg)
                    }
                    return
                }
                "/ayuda", "/help", "/?" -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        val currentLeaf = currentConversationPath.value.lastOrNull()?.message
                        val helpMsg = MessageEntity(
                            worldId = world.id,
                            parentId = currentLeaf?.id,
                            senderType = "SYSTEM_EVENT",
                            senderName = "Guía de Comandos y Atajos",
                            content = """
                                📜 **Atajos y Comandos de Mythos Rol**:
                                • `/mapa [zona]` : Genera un mapa táctico interactivo con Gemini.
                                • `/imagen [escena]` : Genera una ilustración visual con Gemini.
                                • `/roll [acción]` : Lanza un dado d20 vinculado a tu acción escrita.
                                • `/stats` : Consulta la ficha de atributos RPG de tu personaje.
                                • `/stat [FUE/DES/CON/INT/SAB/CAR] [valor]` : Modifica un atributo en vivo (ej: `/stat FUE 18` o `/stat DES +2`).
                                • `/hp [vida]` : Modifica tus puntos de vida (ej: `/hp 25`).
                                • `/ooc [pregunta]` : Habla fuera de rol con el Game Master.
                                • Botón `✨` : Despliega el menú con comillas, fuera de rol, dados e imágenes.
                            """.trimIndent(),
                            branchIndex = 0
                        )
                        repository.insertMessage(helpMsg)
                    }
                    return
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val currentLeaf = currentConversationPath.value.lastOrNull()?.message
            val parentIdForUser = currentLeaf?.id

            // Insert User Message into tree
            val userMsg = MessageEntity(
                worldId = world.id,
                parentId = parentIdForUser,
                senderType = "USER",
                senderName = userName,
                content = trimmed,
                branchIndex = 0,
                lengthPreference = _lengthPreference.value
            )
            val userMsgId = repository.insertMessage(userMsg)

            checkAutoSave()
            generateAiResponse(userMsgId, trimmed)
        }
    }

    /**
     * Generates a tactical map or artistic scene using Gemini and saves it directly to the story tree.
     */
    fun generateGeminiMedia(prompt: String, isMap: Boolean) {
        if (_isGeneratingMedia.value) return
        val world = activeWorld.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingMedia.value = true
            _mediaStatus.value = if (isMap) "Generando mapa táctico con Gemini..." else "Generando imagen con Gemini..."
            try {
                val result = geminiService.generateMedia(
                    rawPrompt = prompt,
                    isMap = isMap,
                    currentLocation = world.name,
                    worldGenre = world.genre
                )

                val currentLeaf = currentConversationPath.value.lastOrNull()?.message
                val mediaMsg = MessageEntity(
                    worldId = world.id,
                    parentId = currentLeaf?.id,
                    senderType = if (isMap) "GEMINI_MAP" else "GEMINI_IMAGE",
                    senderName = if (isMap) "Cartografía Gemini" else "Ilustración Gemini",
                    content = "${result.title}\n\n${result.description}",
                    branchIndex = 0,
                    imageUri = result.filePath,
                    mediaType = if (isMap) "MAP" else "IMAGE"
                )
                repository.insertMessage(mediaMsg)
                checkAutoSave()
            } catch (e: Exception) {
                // handle error
            } finally {
                _isGeneratingMedia.value = false
                _mediaStatus.value = null
            }
        }
    }

    /**
     * Executes a dice roll together with a player action and lets the AI narrator
     * immediately describe the consequences of the roll outcome.
     */
    fun rollDiceWithAction(
        actionText: String,
        diceType: DiceType = DiceType.D20,
        modifierName: String = "Ninguno",
        modifierValue: Int = 0,
        difficultyClass: Int? = null,
        precomputedRollResult: com.example.dice.RollResult? = null
    ) {
        if (_isStreaming.value) return
        val world = activeWorld.value ?: return
        val userChar = selectedUserCharacter.value
        val userName = userChar?.name ?: "Tú"

        val rollResult = precomputedRollResult ?: DiceSystem.roll(
            diceType = diceType,
            modifierName = modifierName,
            modifierValue = modifierValue,
            difficultyClass = difficultyClass,
            context = getApplication()
        )
        _lastRollResult.value = rollResult

        viewModelScope.launch(Dispatchers.IO) {
            val currentLeaf = currentConversationPath.value.lastOrNull()?.message
            val parentIdForUser = currentLeaf?.id

            val cleanAction = actionText.trim().ifBlank { "Intento realizar la acción" }
            val combinedUserContent = buildString {
                append(cleanAction)
                append("\n\n🎲 ")
                append(rollResult.description)
            }

            val userMsg = MessageEntity(
                worldId = world.id,
                parentId = parentIdForUser,
                senderType = "USER",
                senderName = userName,
                content = combinedUserContent,
                branchIndex = 0,
                lengthPreference = _lengthPreference.value
            )
            val userMsgId = repository.insertMessage(userMsg)

            checkAutoSave()
            generateAiResponse(userMsgId, combinedUserContent)
        }
    }

    fun regenerateMessage(narratorMessageId: Long) {
        if (_isStreaming.value) return
        val world = activeWorld.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val targetMsg = _allMessages.value.firstOrNull { it.id == narratorMessageId } ?: return@launch
            val parentId = targetMsg.parentId ?: return@launch
            val siblings = repository.getSiblingsSync(parentId)
            val newBranchIndex = siblings.size

            // Update branch selection to the new branch
            val map = _branchSelections.value.toMutableMap()
            map[parentId] = newBranchIndex
            _branchSelections.value = map

            // Find user prompt that initiated this
            val parentUserMsg = _allMessages.value.firstOrNull { it.id == parentId }
            val promptText = parentUserMsg?.content ?: ""

            generateAiResponse(parentId, promptText, branchIndex = newBranchIndex)
        }
    }

    fun editMessage(messageId: Long, newText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val msg = _allMessages.value.firstOrNull { it.id == messageId } ?: return@launch
            repository.updateMessage(msg.copy(content = newText))
        }
    }

    fun rewindToMessage(messageId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val msg = _allMessages.value.firstOrNull { it.id == messageId } ?: return@launch
            // To rewind, set the branch selection so this message is the tail
            val parentId = msg.parentId
            val map = _branchSelections.value.toMutableMap()
            val siblings = repository.getSiblingsSync(parentId ?: 0L)
            val idx = siblings.indexOfFirst { it.id == messageId }
            if (idx >= 0) {
                map[parentId] = idx
                // Clear any child selections
                val toRemove = map.keys.filter { it == messageId }
                toRemove.forEach { map.remove(it) }
                _branchSelections.value = map
            }
        }
    }

    private suspend fun generateAiResponse(
        parentMessageId: Long,
        userPrompt: String,
        branchIndex: Int = 0
    ) {
        val world = activeWorld.value ?: return
        val userChar = selectedUserCharacter.value
        val npcsList = npcs.value
        val memories = _pinnedMemories.value
        val lore = _lorebook.value
        val recentPath = currentConversationPath.value.map { it.message }

        _isStreaming.value = true
        _streamingMessageText.value = ""

        val context = GenerationContext(
            world = world,
            userCharacter = userChar,
            npcs = npcsList,
            pinnedMemories = memories,
            lorebookEntries = lore,
            recentMessages = recentPath,
            userInput = userPrompt,
            lengthPreference = _lengthPreference.value,
            modelConfig = _modelConfig.value,
            lastDiceRoll = _lastRollResult.value?.description
        )

        val accumulated = StringBuilder()
        llamaEngine.streamResponse(context).collect { token ->
            accumulated.append(token)
            _streamingMessageText.value = accumulated.toString()
        }

        val completedNarrative = accumulated.toString()
        val narratorMsg = MessageEntity(
            worldId = world.id,
            parentId = parentMessageId,
            senderType = "NARRATOR",
            senderName = "Narrador",
            content = completedNarrative,
            branchIndex = branchIndex,
            lengthPreference = _lengthPreference.value
        )
        repository.insertMessage(narratorMsg)

        _isStreaming.value = false
        _streamingMessageText.value = ""
        checkAutoSave()
    }

    private fun checkAutoSave() {
        messageCounterSinceSave++
        if (messageCounterSinceSave >= 8) {
            messageCounterSinceSave = 0
            _autoSaveNotice.value = "Árbol de historia sincronizado y guardado en Room automáticamente."
            viewModelScope.launch {
                kotlinx.coroutines.delay(4000)
                _autoSaveNotice.value = null
            }
        }
    }

    fun dismissAutoSaveNotice() {
        _autoSaveNotice.value = null
    }

    // Dice System
    fun rollDice(
        diceType: DiceType,
        modifierName: String = "Ninguno",
        modifierValue: Int = 0,
        difficultyClass: Int? = null,
        precomputedRollResult: com.example.dice.RollResult? = null
    ) {
        val result = precomputedRollResult ?: DiceSystem.roll(
            diceType = diceType,
            modifierName = modifierName,
            modifierValue = modifierValue,
            difficultyClass = difficultyClass,
            context = getApplication()
        )
        _lastRollResult.value = result

        // Inscribe roll into message tree as a system card
        val world = activeWorld.value ?: return
        val currentLeaf = currentConversationPath.value.lastOrNull()?.message

        viewModelScope.launch(Dispatchers.IO) {
            val diceMsg = MessageEntity(
                worldId = world.id,
                parentId = currentLeaf?.id,
                senderType = "DICE_ROLL",
                senderName = "Sistema de Dados",
                content = result.description,
                branchIndex = 0
            )
            repository.insertMessage(diceMsg)
        }
    }

    // Character Management (Character.ai style with unlimited length)
    fun saveCharacter(
        id: Long = 0,
        name: String,
        role: String,
        personality: String,
        background: String,
        dialogue: String,
        greeting: String,
        avatarUri: String?,
        statsJson: String,
        linkedPlaylists: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val charEntity = CharacterEntity(
                id = id,
                name = name.trim(),
                role = role,
                personality = personality,
                background = background,
                dialogueExamples = dialogue,
                initialGreeting = greeting,
                avatarUri = avatarUri,
                statsJson = statsJson,
                linkedPlaylistIds = linkedPlaylists,
                worldId = activeWorld.value?.id,
                isNarratorControlled = role == "NPC",
                isSelectedUserCharacter = role == "USER"
            )
            if (id == 0L) {
                repository.insertCharacter(charEntity)
            } else {
                repository.updateCharacter(charEntity)
            }
        }
    }

    private suspend fun insertSystemNotice(worldId: Long, content: String) {
        val currentLeaf = currentConversationPath.value.lastOrNull()?.message
        val msg = MessageEntity(
            worldId = worldId,
            parentId = currentLeaf?.id,
            senderType = "SYSTEM_EVENT",
            senderName = "Sistema de Atributos",
            content = content,
            branchIndex = 0
        )
        repository.insertMessage(msg)
    }

    private fun formatCharacterStats(character: CharacterEntity): String {
        val json = try { JSONObject(character.statsJson) } catch (_: Exception) { JSONObject() }
        val str = json.optInt("STR", 10)
        val dex = json.optInt("DEX", 10)
        val con = json.optInt("CON", 10)
        val intVal = json.optInt("INT", 10)
        val wis = json.optInt("WIS", 10)
        val cha = json.optInt("CHA", 10)
        val hp = json.optInt("HP", 20)
        val maxHp = json.optInt("MAX_HP", hp)

        fun modStr(v: Int): String {
            val m = DiceSystem.calculateModifier(v)
            return if (m >= 0) "+$m" else "$m"
        }

        return buildString {
            append("📊 **Ficha de Atributos de ${character.name}**:\n")
            append("• **Fuerza (STR)**: $str (${modStr(str)})\n")
            append("• **Destreza (DEX)**: $dex (${modStr(dex)})\n")
            append("• **Constitución (CON)**: $con (${modStr(con)})\n")
            append("• **Inteligencia (INT)**: $intVal (${modStr(intVal)})\n")
            append("• **Sabiduría (WIS)**: $wis (${modStr(wis)})\n")
            append("• **Carisma (CHA)**: $cha (${modStr(cha)})\n")
            append("• **Puntos de Vida (HP)**: $hp / $maxHp\n\n")
            append("_💡 Puedes cambiar cualquier estadística escribiendo `/stat FUE 18` o `/stat DES +2` o `/hp 25`._")
        }
    }

    private suspend fun applyStatChange(character: CharacterEntity, argument: String): String {
        val trimmed = argument.trim()
        if (trimmed.isBlank()) {
            return "⚠️ Uso del comando: `/stat [FUE/DES/CON/INT/SAB/CAR/HP] [valor o +X/-X]`\nEjemplo: `/stat FUE 18` o `/stat DES +2`"
        }

        val parts = trimmed.split(Regex("\\s+"), limit = 2)
        val statKeyInput = parts[0].lowercase()
        val valueInput = parts.getOrNull(1)?.trim() ?: ""

        if (valueInput.isBlank()) {
            return "⚠️ Debes especificar el valor o delta. Ejemplo: `/stat FUE 18` o `/stat DES +2`"
        }

        val (canonicalKey, statName) = when (statKeyInput) {
            "fue", "fuerza", "str", "strength" -> "STR" to "Fuerza (STR)"
            "des", "destreza", "dex", "dexterity", "agl", "agilidad" -> "DEX" to "Destreza (DEX)"
            "con", "constitucion", "constitución", "constitution", "res", "resistencia" -> "CON" to "Constitución (CON)"
            "int", "inteligencia", "intelligence" -> "INT" to "Inteligencia (INT)"
            "sab", "sabiduria", "sabiduría", "wis", "wisdom", "per", "percepcion" -> "WIS" to "Sabiduría (WIS)"
            "car", "carisma", "cha", "charisma" -> "CHA" to "Carisma (CHA)"
            "hp", "vida", "salud" -> "HP" to "Puntos de Vida (HP)"
            else -> null to null
        }

        if (canonicalKey == null || statName == null) {
            return "⚠️ Estadística desconocida '$statKeyInput'. Usa: FUE, DES, CON, INT, SAB, CAR o HP."
        }

        val jsonObj = try { JSONObject(character.statsJson) } catch (_: Exception) { JSONObject() }
        val oldValue = jsonObj.optInt(canonicalKey, 10)

        val newValue = when {
            valueInput.startsWith("+") -> {
                val delta = valueInput.removePrefix("+").trim().toIntOrNull() ?: 0
                oldValue + delta
            }
            valueInput.startsWith("-") -> {
                val delta = valueInput.removePrefix("-").trim().toIntOrNull() ?: 0
                oldValue - delta
            }
            else -> {
                valueInput.toIntOrNull() ?: oldValue
            }
        }.coerceAtLeast(1)

        jsonObj.put(canonicalKey, newValue)
        if (canonicalKey == "HP") {
            val maxHp = jsonObj.optInt("MAX_HP", 20)
            if (newValue > maxHp) {
                jsonObj.put("MAX_HP", newValue)
            }
        }

        val updatedChar = character.copy(statsJson = jsonObj.toString())
        repository.updateCharacter(updatedChar)

        val newMod = DiceSystem.calculateModifier(newValue)
        val modStr = if (newMod >= 0) "+$newMod" else "$newMod"

        return if (canonicalKey == "HP") {
            "❤️ **Salud de ${character.name} actualizada**: HP = $newValue (anterior: $oldValue)"
        } else {
            "📊 **Atributo de ${character.name} actualizado**:\n• **$statName**: $oldValue ➔ **$newValue** (Modificador: $modStr)"
        }
    }

    fun selectUserCharacter(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.selectUserCharacter(id)
        }
    }

    fun deleteCharacter(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCharacter(id)
        }
    }

    // Lorebook Management
    fun saveLorebookEntry(id: Long = 0, title: String, keywords: String, content: String) {
        val worldId = activeWorld.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val entry = LorebookEntryEntity(
                id = id,
                worldId = worldId,
                title = title,
                keywords = keywords,
                content = content,
                isEnabled = true
            )
            if (id == 0L) repository.insertLorebookEntry(entry) else repository.updateLorebookEntry(entry)
        }
    }

    fun deleteLorebookEntry(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLorebookEntry(id)
        }
    }

    // Pinned Memories
    fun addPinnedMemory(content: String, importance: Int = 3) {
        val worldId = activeWorld.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPinnedMemory(
                PinnedMemoryEntity(
                    worldId = worldId,
                    content = content.take(10000),
                    importance = importance
                )
            )
        }
    }

    fun deletePinnedMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePinnedMemory(id)
        }
    }

    // Story Chronicle / Bitácora
    fun addStoryLog(title: String, summary: String, isMilestone: Boolean = false) {
        val worldId = activeWorld.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertStoryLog(
                StoryLogEntity(
                    worldId = worldId,
                    title = title,
                    summary = summary,
                    isMilestone = isMilestone
                )
            )
        }
    }

    fun deleteStoryLog(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStoryLog(id)
        }
    }

    // Playlists
    fun savePlaylist(
        id: Long = 0,
        name: String,
        ambientType: String,
        imageUri: String?,
        tracks: List<AudioTrackItem>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = audioManager.serializeTracks(tracks)
            val playlist = PlaylistEntity(
                id = id,
                name = name,
                ambientType = ambientType,
                imageUri = imageUri,
                tracksJson = json
            )
            if (id == 0L) repository.insertPlaylist(playlist) else repository.updatePlaylist(playlist)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlaylist(id)
        }
    }

    fun playPlaylist(playlist: PlaylistEntity) {
        audioManager.playPlaylist(
            playlistId = playlist.id,
            playlistName = playlist.name,
            ambientType = playlist.ambientType,
            tracksJson = playlist.tracksJson
        )
    }

    // Model settings
    fun updateModelConfig(
        modelName: String? = null,
        modelPath: String? = null,
        temperature: Float? = null,
        topP: Float? = null,
        repeatPenalty: Float? = null,
        threads: Int? = null,
        contextWindow: Int? = null,
        systemPrompt: String? = null
    ) {
        val current = _modelConfig.value
        val updated = current.copy(
            modelName = modelName ?: current.modelName,
            modelPath = modelPath ?: current.modelPath,
            temperature = temperature ?: current.temperature,
            topP = topP ?: current.topP,
            repeatPenalty = repeatPenalty ?: current.repeatPenalty,
            threadCount = threads ?: current.threadCount,
            contextWindow = contextWindow ?: current.contextWindow,
            systemPrompt = (systemPrompt ?: current.systemPrompt).take(50000)
        )
        _modelConfig.value = updated

        if (modelPath != null && modelPath.isNotBlank()) {
            val success = llamaEngine.loadNativeModel(modelPath, updated.threadCount, updated.contextWindow)
            _modelConfig.value = updated.copy(isModelLoaded = success)
        }
    }

    fun setCurrentLocation(worldId: Long, placeId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markCurrentLocation(worldId, placeId)
        }
    }

    // Story / World Campaign Operations
    fun selectStory(worldId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setActiveWorld(worldId)
            _branchSelections.value = emptyMap()
            val w = repository.getWorldByIdSync(worldId)
            _chatBackgroundUri.value = w?.chatBackgroundUri
        }
    }

    fun createNewStory(
        title: String,
        genre: String,
        description: String,
        lore: String,
        systemPrompt: String,
        ambientType: String,
        coverUri: String?,
        chatBgUri: String?,
        starterNpcName: String,
        starterNpcPersonality: String,
        starterGreeting: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.createNewStory(
                title = title,
                genre = genre,
                description = description,
                lore = lore,
                systemPrompt = systemPrompt,
                ambientType = ambientType,
                coverUri = coverUri,
                chatBgUri = chatBgUri,
                starterNpcName = starterNpcName,
                starterNpcPersonality = starterNpcPersonality,
                starterGreeting = starterGreeting
            )
            _branchSelections.value = emptyMap()
            _chatBackgroundUri.value = chatBgUri
        }
    }

    fun restartStory(worldId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restartStory(worldId)
            _branchSelections.value = emptyMap()
        }
    }

    fun duplicateStory(worldId: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.duplicateStory(worldId, newName)
            if (newId > 0) {
                repository.setActiveWorld(newId)
                _branchSelections.value = emptyMap()
            }
        }
    }

    fun deleteStory(worldId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWorld(worldId)
            val remaining = repository.allWorlds.firstOrNull()
            if (!remaining.isNullOrEmpty()) {
                val next = remaining.firstOrNull { it.id != worldId } ?: remaining.first()
                repository.setActiveWorld(next.id)
            }
        }
    }

    // Export & Import (JSON)
    fun exportWorldJson(): String {
        val world = activeWorld.value ?: return "{}"
        val npcsList = npcs.value
        val userChars = userCharacters.value
        val lore = _lorebook.value

        val root = JSONObject()
        root.put("worldName", world.name)
        root.put("worldLore", world.lore)
        root.put("worldDescription", world.description)
        root.put("systemPrompt", world.systemPrompt)

        val npcsArray = org.json.JSONArray()
        npcsList.forEach {
            val o = JSONObject()
            o.put("name", it.name)
            o.put("personality", it.personality)
            o.put("background", it.background)
            o.put("dialogueExamples", it.dialogueExamples)
            o.put("initialGreeting", it.initialGreeting)
            npcsArray.put(o)
        }
        root.put("npcs", npcsArray)

        val loreArray = org.json.JSONArray()
        lore.forEach {
            val l = JSONObject()
            l.put("title", it.title)
            l.put("keywords", it.keywords)
            l.put("content", it.content)
            loreArray.put(l)
        }
        root.put("lorebook", loreArray)

        return root.toString(2)
    }

    fun importWorldJson(jsonStr: String): Boolean {
        return try {
            val obj = JSONObject(jsonStr)
            val name = obj.optString("worldName", "Mundo Importado")
            val lore = obj.optString("worldLore", "")
            val desc = obj.optString("worldDescription", "")
            val sysPrompt = obj.optString("systemPrompt", "")

            viewModelScope.launch(Dispatchers.IO) {
                val newWorld = WorldEntity(
                    name = name,
                    description = desc,
                    lore = lore,
                    systemPrompt = sysPrompt,
                    isActive = true
                )
                val newWorldId = repository.insertWorld(newWorld)
                repository.setActiveWorld(newWorldId)

                val npcsArray = obj.optJSONArray("npcs")
                if (npcsArray != null) {
                    for (i in 0 until npcsArray.length()) {
                        val n = npcsArray.getJSONObject(i)
                        repository.insertCharacter(
                            CharacterEntity(
                                name = n.optString("name", "PNJ"),
                                role = "NPC",
                                personality = n.optString("personality", ""),
                                background = n.optString("background", ""),
                                dialogueExamples = n.optString("dialogueExamples", ""),
                                initialGreeting = n.optString("initialGreeting", "Saludos."),
                                worldId = newWorldId,
                                isNarratorControlled = true
                            )
                        )
                    }
                }

                val loreArray = obj.optJSONArray("lorebook")
                if (loreArray != null) {
                    for (i in 0 until loreArray.length()) {
                        val l = loreArray.getJSONObject(i)
                        repository.insertLorebookEntry(
                            LorebookEntryEntity(
                                worldId = newWorldId,
                                title = l.optString("title", "Lore"),
                                keywords = l.optString("keywords", ""),
                                content = l.optString("content", "")
                            )
                        )
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        llamaEngine.unloadNativeModel()
        audioManager.stopAll()
    }
}
