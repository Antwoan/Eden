package com.example.data.repository

import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entities.CharacterEntity
import com.example.data.local.entities.LorebookEntryEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.local.entities.PinnedMemoryEntity
import com.example.data.local.entities.PlaylistEntity
import com.example.data.local.entities.StoryGenres
import com.example.data.local.entities.StoryLogEntity
import com.example.data.local.entities.WorldEntity
import com.example.data.local.entities.WorldPlaceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONObject

class RoleplayRepository(private val db: AppDatabase) {

    // Worlds / Stories
    val allWorlds: Flow<List<WorldEntity>> = db.worldDao().getAllWorlds()
    val activeWorld: Flow<WorldEntity?> = db.worldDao().getActiveWorld()

    suspend fun insertWorld(world: WorldEntity): Long = db.worldDao().insertWorld(world)
    suspend fun updateWorld(world: WorldEntity) = db.worldDao().updateWorld(world)
    suspend fun deleteWorld(id: Long) = db.worldDao().deleteWorld(id)
    suspend fun setActiveWorld(id: Long) = db.worldDao().setActiveWorld(id)
    suspend fun getWorldByIdSync(id: Long) = db.worldDao().getWorldByIdSync(id)

    // Characters
    val allCharacters: Flow<List<CharacterEntity>> = db.characterDao().getAllCharacters()
    val npcs: Flow<List<CharacterEntity>> = db.characterDao().getNpcCharacters()
    val userCharacters: Flow<List<CharacterEntity>> = db.characterDao().getUserCharacters()
    val selectedUserCharacter: Flow<CharacterEntity?> = db.characterDao().getSelectedUserCharacter()

    suspend fun insertCharacter(character: CharacterEntity): Long = db.characterDao().insertCharacter(character)
    suspend fun updateCharacter(character: CharacterEntity) = db.characterDao().updateCharacter(character)
    suspend fun deleteCharacter(id: Long) = db.characterDao().deleteCharacter(id)
    suspend fun selectUserCharacter(id: Long) = db.characterDao().selectUserCharacter(id)
    suspend fun getCharacterByIdSync(id: Long): CharacterEntity? = db.characterDao().getCharacterByIdSync(id)

    // Lorebook
    fun getLorebookForWorld(worldId: Long): Flow<List<LorebookEntryEntity>> = db.lorebookDao().getLorebookForWorld(worldId)
    suspend fun getEnabledLoreSync(worldId: Long) = db.lorebookDao().getEnabledEntriesForWorld(worldId)
    suspend fun insertLorebookEntry(entry: LorebookEntryEntity): Long = db.lorebookDao().insertLorebookEntry(entry)
    suspend fun updateLorebookEntry(entry: LorebookEntryEntity) = db.lorebookDao().updateLorebookEntry(entry)
    suspend fun deleteLorebookEntry(id: Long) = db.lorebookDao().deleteLorebookEntry(id)

    // Messages
    fun getMessagesForWorld(worldId: Long): Flow<List<MessageEntity>> = db.messageDao().getMessagesForWorld(worldId)
    suspend fun getMessagesForWorldSync(worldId: Long) = db.messageDao().getMessagesForWorldSync(worldId)
    suspend fun getSiblingsSync(parentId: Long) = db.messageDao().getSiblingsSync(parentId)
    suspend fun insertMessage(message: MessageEntity): Long = db.messageDao().insertMessage(message)
    suspend fun updateMessage(message: MessageEntity) = db.messageDao().updateMessage(message)
    suspend fun deleteMessage(id: Long) = db.messageDao().deleteMessage(id)
    suspend fun clearMessagesForWorld(worldId: Long) = db.messageDao().clearMessagesForWorld(worldId)
    fun searchMessages(worldId: Long, query: String): Flow<List<MessageEntity>> = db.messageDao().searchMessages(worldId, query)

    // Story Logs
    fun getStoryLogs(worldId: Long): Flow<List<StoryLogEntity>> = db.storyLogDao().getLogsForWorld(worldId)
    suspend fun insertStoryLog(log: StoryLogEntity): Long = db.storyLogDao().insertLog(log)
    suspend fun deleteStoryLog(id: Long) = db.storyLogDao().deleteLog(id)

    // Pinned Memories
    fun getPinnedMemories(worldId: Long): Flow<List<PinnedMemoryEntity>> = db.pinnedMemoryDao().getPinnedMemories(worldId)
    suspend fun getActiveMemoriesSync(worldId: Long) = db.pinnedMemoryDao().getActiveMemoriesSync(worldId)
    suspend fun insertPinnedMemory(memory: PinnedMemoryEntity): Long = db.pinnedMemoryDao().insertMemory(memory)
    suspend fun updatePinnedMemory(memory: PinnedMemoryEntity) = db.pinnedMemoryDao().updateMemory(memory)
    suspend fun deletePinnedMemory(id: Long) = db.pinnedMemoryDao().deleteMemory(id)

    // Playlists
    val allPlaylists: Flow<List<PlaylistEntity>> = db.playlistDao().getAllPlaylists()
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long = db.playlistDao().insertPlaylist(playlist)
    suspend fun updatePlaylist(playlist: PlaylistEntity) = db.playlistDao().updatePlaylist(playlist)
    suspend fun deletePlaylist(id: Long) = db.playlistDao().deletePlaylist(id)

    // Places
    fun getPlacesForWorld(worldId: Long): Flow<List<WorldPlaceEntity>> = db.worldPlaceDao().getPlacesForWorld(worldId)
    suspend fun insertPlace(place: WorldPlaceEntity): Long = db.worldPlaceDao().insertPlace(place)
    suspend fun deletePlace(id: Long) = db.worldPlaceDao().deletePlace(id)
    suspend fun markCurrentLocation(worldId: Long, placeId: Long) {
        db.worldPlaceDao().clearCurrentLocation(worldId)
        db.worldPlaceDao().markCurrentLocation(placeId)
    }

    // Story Lifecycle Operations (Restart, Duplicate, Create)
    suspend fun restartStory(worldId: Long, openingText: String? = null) {
        clearMessagesForWorld(worldId)
        val world = getWorldByIdSync(worldId) ?: return
        val opening = openingText ?: when (world.genre) {
            StoryGenres.SCI_FI ->
                "La nave estelar *Odisea Nova* emerge del hiperespacio frente a la colosal nebulosa de Orión. Los sensores de navegación parpadean con una señal de socorro proveniente de una estación minera abandonada.\n\n**ARIA (IA de Nave)**: \"Comandante, hemos llegado al punto de inserción. Detecto anomalías térmicas en el hangar principal. ¿Cuáles son sus órdenes?\""
            StoryGenres.MODERN_NOIR ->
                "La lluvia incesante golpea el cristal de tu oficina en el Distrito 9. El humo del tabaco se mezcla con el aroma a café recalentado mientras las luces de neón tiñen la noche de carmesí. Se escuchan pasos apresurados en el pasillo y unos nudillos golpean tu puerta.\n\n**Detective Miller**: \"Sé que es medianoche, pero si quieres la exclusiva que derribará a la corporación Thorne, abre la puerta ya.\""
            else ->
                "La pesada puerta de roble cede ante tu empuje, rechinando contra las bisagras de hierro forjado. Una bocanada de aire caliente impregnado con olor a estofado de ternera y humo de pipa te envuelve de inmediato.\n\nAl fondo, tras la barra de madera pulida, **Moira la Posadera** levanta la vista mientras seca una jarra de peltre.\n\n**Moira**: \"Adelante, extranjero. Sacúdete la nieve antes de manchar el roble. Si vienes en son de paz, toma asiento junto al fuego.\"\n\n¿Qué decides hacer?"
        }

        insertMessage(
            MessageEntity(
                worldId = worldId,
                parentId = null,
                senderType = "NARRATOR",
                senderName = "Narrador",
                content = opening,
                branchIndex = 0,
                lengthPreference = "MEDIUM"
            )
        )
        setActiveWorld(worldId)
    }

    suspend fun duplicateStory(worldId: Long, newName: String): Long {
        val original = getWorldByIdSync(worldId) ?: return 0L
        val copy = original.copy(
            id = 0,
            name = newName,
            isActive = false,
            createdAt = System.currentTimeMillis(),
            lastPlayedAt = System.currentTimeMillis()
        )
        val newWorldId = insertWorld(copy)

        // Duplicate lorebook
        val lores = getEnabledLoreSync(worldId)
        lores.forEach { l ->
            insertLorebookEntry(l.copy(id = 0, worldId = newWorldId))
        }

        // Duplicate places
        val placesList = db.worldPlaceDao().getPlacesForWorld(worldId).firstOrNull() ?: emptyList()
        placesList.forEach { p ->
            insertPlace(p.copy(id = 0, worldId = newWorldId))
        }

        // Restart story with clean initial message
        restartStory(newWorldId)
        return newWorldId
    }

    suspend fun createNewStory(
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
    ): Long {
        val newWorld = WorldEntity(
            name = title,
            genre = genre,
            description = description,
            lore = lore,
            systemPrompt = systemPrompt,
            defaultAmbientType = ambientType,
            coverImageUri = coverUri,
            chatBackgroundUri = chatBgUri,
            isActive = true,
            lastPlayedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        val worldId = insertWorld(newWorld)
        setActiveWorld(worldId)

        // Create starter NPC if provided
        if (starterNpcName.isNotBlank()) {
            val npcId = insertCharacter(
                CharacterEntity(
                    name = starterNpcName,
                    role = "NPC",
                    personality = starterNpcPersonality,
                    background = "Personaje clave de la historia $title.",
                    dialogueExamples = "<START>\n{{char}}: \"$starterGreeting\"",
                    initialGreeting = starterGreeting,
                    worldId = worldId,
                    isNarratorControlled = true
                )
            )

            // Initial Narrator greeting quoting this NPC
            insertMessage(
                MessageEntity(
                    worldId = worldId,
                    parentId = null,
                    senderType = "NARRATOR",
                    senderName = "Narrador",
                    speakerCharacterId = npcId,
                    content = "El escenario está listo. El aire está cargado de tensión y expectación mientras tus pasos te llevan al centro de los acontecimientos.\n\nFrente a ti aparece **$starterNpcName**, observándote con atención.\n\n**$starterNpcName**: \"$starterGreeting\"\n\n¿Qué decides hacer?",
                    branchIndex = 0,
                    lengthPreference = "MEDIUM"
                )
            )
        } else {
            restartStory(worldId)
        }

        return worldId
    }

    suspend fun seedInitialDataIfNeeded() {
        val existingWorlds = allWorlds.firstOrNull()
        if (!existingWorlds.isNullOrEmpty()) return

        // 1. Dark Fantasy Story: Aethelgard
        val darkFantasyWorld = WorldEntity(
            name = "Aethelgard: Sombras y Ceniza",
            genre = StoryGenres.DARK_FANTASY,
            description = "Un continente medieval fantástico fracturado por antiguas guerras mágicas y pactos prohibidos.",
            lore = "En el año 1042 de la Tercera Era, monstruos y bandas de renegados acechan los caminos comerciales, mientras las ciudades amuralladas como Silverstone se aferran al orden bajo la mirada atenta de los gremios.",
            defaultAmbientType = "Descanso en Campamento",
            coverImageUri = null, // Will use default drawable
            isActive = true
        )
        val fantasyWorldId = insertWorld(darkFantasyWorld)

        // Moira
        val moira = CharacterEntity(
            name = "Moira la Guardiana",
            role = "NPC",
            personality = "Astuta, protectora, observadora implacable. No teme empuñar una maza si alguien perturba la paz del refugio.",
            background = "Nacida en los bajos fondos, Moira se ganó su libertad tras años en el gremio de exploradores de Silverstone. Conoce los rumores de cada caravana.",
            dialogueExamples = "<START>\n{{char}}: \"Si vas a sentarte junto a este fuego, al menos aporta leña o paga tu cuota de guardia, muchacho.\"",
            initialGreeting = "Adelante, extranjero. Sacúdete la nieve y el polvo antes de cruzar el umbral. Si vienes en son de paz, toma asiento junto al fuego.",
            worldId = fantasyWorldId,
            isNarratorControlled = true
        )
        val moiraId = insertCharacter(moira)

        // Valerius
        val valerius = CharacterEntity(
            name = "Maestro Valerius",
            role = "NPC",
            personality = "Culto, nervioso, habla entre susurros, obsesionado con los pergaminos de la Primera Era.",
            background = "Erudito fugitivo del Cónclave de las Torres de Cristal, busca descifrar el Códice de las Mareas Celestiales.",
            dialogueExamples = "<START>\n{{char}}: \"Habla bajo... las paredes tienen oídos y la Inquisición ojos en cada rincón.\"",
            initialGreeting = "Has tardado demasiado. Siéntate y mantén las manos a la vista. ¿Traes las notas que pactamos?",
            worldId = fantasyWorldId,
            isNarratorControlled = true
        )
        insertCharacter(valerius)

        // User Character Kaelen
        val statsObj = JSONObject().apply {
            put("STR", 15)
            put("DEX", 16)
            put("CON", 14)
            put("INT", 12)
            put("WIS", 10)
            put("CHA", 14)
            put("HP", 28)
            put("MAX_HP", 28)
            put("LVL", 3)
            put("GOLD", 35)
        }
        val userChar = CharacterEntity(
            name = "Kaelen el Renegado",
            role = "USER",
            personality = "Temerario, leal a sus principios, cauto con desconocidos, decidido ante el peligro.",
            background = "Porta una espada de acero negro heredada de su mentor. Llegó a Silverstone tras escapar de una emboscada en los desfiladeros del norte.",
            dialogueExamples = "<START>\n{{user}}: \"Solo busco provisiones y el camino seguro hacia las ruinas del norte. Nada más.\"",
            initialGreeting = "",
            statsJson = statsObj.toString(),
            worldId = fantasyWorldId,
            isNarratorControlled = false,
            isSelectedUserCharacter = true
        )
        insertCharacter(userChar)

        // Fantasy Lorebook
        insertLorebookEntry(
            LorebookEntryEntity(
                worldId = fantasyWorldId,
                title = "Refugio del Dragón Ciego",
                keywords = "refugio, campamento, descanso, dragón ciego, moira, fuego",
                content = "El refugio fortificado más antiguo de Silverstone, construido sobre cimientos de granito enano. Lugar seguro donde exploradores y mercenarios descansan bajo tregua tácita."
            )
        )
        insertLorebookEntry(
            LorebookEntryEntity(
                worldId = fantasyWorldId,
                title = "Códice de las Mareas",
                keywords = "códice, grimorio, mareas, valerius, reliquia",
                content = "Un manuscrito mágico sellado que guarda el secreto para disipar las tormentas arcanas de los picos de Aethelgard."
            )
        )

        // Fantasy Initial Message
        insertMessage(
            MessageEntity(
                worldId = fantasyWorldId,
                parentId = null,
                senderType = "NARRATOR",
                senderName = "Narrador",
                speakerCharacterId = moiraId,
                content = "La pesada puerta de roble del refugio cede ante tu empuje, rechinando contra las bisagras de hierro forjado. Una bocanada de aire caliente y el suave chisporroteo del fuego en el hogar te envuelven de inmediato.\n\nAl fondo, junto al mapa de campaña, **Moira la Guardiana** levanta la vista mientras afila una daga corta con precisión serena.\n\n**Moira**: \"Adelante, extranjero. Sacúdete la nieve antes de cruzar el umbral. Si vienes en son de paz, toma asiento junto al fuego del campamento.\"\n\n¿Qué decides hacer?",
                branchIndex = 0,
                lengthPreference = "MEDIUM"
            )
        )

        // 2. Sci-Fi Space Story: Odisea Nova
        val sciFiWorld = WorldEntity(
            name = "Odisea Nova: Sector Orión",
            genre = StoryGenres.SCI_FI,
            description = "Ópera espacial de ciencia ficción en los límites inexplorados de la galaxia.",
            lore = "Año 2389. La nave de exploración Odisea Nova investiga las ruinas de una civilización precursora en los anillos del planeta Thalas. Las comunicaciones con la Tierra tardan meses.",
            defaultAmbientType = "Exploración",
            isActive = false
        )
        val sciFiId = insertWorld(sciFiWorld)

        // ARIA (Ship AI)
        insertCharacter(
            CharacterEntity(
                name = "ARIA (Inteligencia de Nave)",
                role = "NPC",
                personality = "Metódica, analítica, con un humor sutilmente sarcástico y absoluta lealtad a la tripulación.",
                background = "Núcleo de IA cuántica de quinta generación instalado en la Odisea Nova para navegación y soporte vital.",
                dialogueExamples = "<START>\n{{char}}: \"Comandante, la probabilidad de que esa cápsula no explote es del 42.7%. Recomiendo cautela.\"",
                initialGreeting = "Sistemas en línea, Comandante. Los sensores registran una transmisión cifrada en frecuencia prohibida desde la luna helada.",
                worldId = sciFiId,
                isNarratorControlled = true
            )
        )

        insertLorebookEntry(
            LorebookEntryEntity(
                worldId = sciFiId,
                title = "Los Precursores",
                keywords = "precursores, artefacto, reliquia, ruinas, monolito",
                content = "Raza alienígena ancestral extinta hace millones de años. Su tecnología manipula la gravedad y los campos taquiónicos."
            )
        )

        insertMessage(
            MessageEntity(
                worldId = sciFiId,
                parentId = null,
                senderType = "NARRATOR",
                senderName = "Narrador",
                content = "La nave estelar *Odisea Nova* emerge del hiperespacio frente a la colosal nebulosa de Orión. Los anillos púrpuras del planeta Thalas reflejan la luz de una enana blanca sobre los paneles de visualización del puente de mando.\n\nEl zumbido suave del motor de antimateria se interrumpe cuando la consola principal parpadea con una alerta de proximidad.\n\n**ARIA (Inteligencia de Nave)**: \"Comandante, hemos llegado al punto de inserción. Detecto anomalías térmicas y una señal pulsante en el hangar de la estación minera abandonada. ¿Cuáles son sus órdenes?\"",
                branchIndex = 0,
                lengthPreference = "MEDIUM"
            )
        )

        // 3. Modern Urban Noir Story: Distrito 9
        val modernWorld = WorldEntity(
            name = "Distrito 9: Lluvia y Neón",
            genre = StoryGenres.MODERN_NOIR,
            description = "Thriller policiaco y detectivesco en una metrópolis moderna asfixiada por la corrupción.",
            lore = "En New Haven, la lluvia nunca cesa. Las megacorporaciones compran a los jueces mientras bandas callejeras y detectives privados libran una guerra silenciosa en los callejones del Distrito 9.",
            defaultAmbientType = "Misterio",
            isActive = false
        )
        val modernId = insertWorld(modernWorld)

        // Detective Miller
        insertCharacter(
            CharacterEntity(
                name = "Detective Miller",
                role = "NPC",
                personality = "Cínico, bebedor de café amargo, curtido por veinte años en homicidios, no cree en coincidencias.",
                background = "Antiguo inspector jefe degradado tras destapar un escándalo que involucraba al alcalde.",
                dialogueExamples = "<START>\n{{char}}: \"En esta ciudad nadie es inocente, chico. Solo hay culpables que aún no han sido atrapados.\"",
                initialGreeting = "Cierra la persiana y siéntate. Si la gente de Thorne se entera de que estás aquí, seremos carne picada al amanecer.",
                worldId = modernId,
                isNarratorControlled = true
            )
        )

        insertLorebookEntry(
            LorebookEntryEntity(
                worldId = modernId,
                title = "Corporación Thorne",
                keywords = "thorne, corporación, sindicato, dinero",
                content = "El mayor conglomerado farmacéutico y tecnológico de New Haven, sospechoso de experimentos clandestinos con reclusos."
            )
        )

        insertMessage(
            MessageEntity(
                worldId = modernId,
                parentId = null,
                senderType = "NARRATOR",
                senderName = "Narrador",
                content = "La lluvia incesante golpea con fuerza los cristales empañados de tu despacho en el Distrito 9. Las luces de neón de los carteles de hoteles y clubes nocturnos tiñen los charcos de la calle de un rojo sangre intermitente.\n\nUn crujido resuena en la escalera de caracol. La puerta de cristal opaco se abre bruscamente y entra un hombre con una gabardina empapada y la respiración entrecortada.\n\n**Detective Miller**: \"Sé que es medianoche, pero si quieres la exclusiva que derribará a la Corporación Thorne, cierra la persiana y escucha atentamente. Nos están siguiendo.\"",
                branchIndex = 0,
                lengthPreference = "MEDIUM"
            )
        )

        // Initial Playlists with new occasions
        insertPlaylist(
            PlaylistEntity(
                name = "Fuego y Descanso",
                ambientType = "Descanso en Campamento",
                tracksJson = "[]"
            )
        )
        insertPlaylist(
            PlaylistEntity(
                name = "Sombras & Misterio",
                ambientType = "Investigación & Misterio",
                tracksJson = "[]"
            )
        )
        insertPlaylist(
            PlaylistEntity(
                name = "Acero y Furia",
                ambientType = "Combate Épico",
                tracksJson = "[]"
            )
        )
        insertPlaylist(
            PlaylistEntity(
                name = "Espacio Profundo",
                ambientType = "Odisea Espacial",
                tracksJson = "[]"
            )
        )
        insertPlaylist(
            PlaylistEntity(
                name = "Infiltración Táctica",
                ambientType = "Sigilo & Infiltración",
                tracksJson = "[]"
            )
        )
        insertPlaylist(
            PlaylistEntity(
                name = "Lluvia de Neón",
                ambientType = "Ciudad Cyberpunk",
                tracksJson = "[]"
            )
        )
    }
}
