package com.example.ui.screens.characters

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entities.CharacterEntity
import com.example.data.local.entities.PlaylistEntity
import com.example.dice.DiceSystem
import com.example.ui.viewmodel.RoleplayViewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(
    viewModel: RoleplayViewModel,
    modifier: Modifier = Modifier
) {
    val npcs by viewModel.npcs.collectAsState()
    val userCharacters by viewModel.userCharacters.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState()
    val selectedUserChar by viewModel.selectedUserCharacter.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Bots (NPCs), 1: Player Personas (USER)
    var characterToEdit by remember { mutableStateOf<CharacterEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bots IA (${npcs.size})")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mis Personas (${userCharacters.size})")
                        }
                    }
                )
            }

            val listToShow = if (selectedTab == 0) npcs else userCharacters

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(10.dp)) }

                if (listToShow.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Default.SmartToy else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (selectedTab == 0) "No hay Bots creados" else "No has creado ninguna Persona",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (selectedTab == 0)
                                        "Crea tu primer Bot estilo Character.ai con saludo y definición ilimitada."
                                    else
                                        "Crea tu Persona de jugador con descripción ilimitada y estadísticas RPG.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(listToShow, key = { it.id }) { char ->
                    CaiCharacterCard(
                        character = char,
                        isSelectedUser = char.id == selectedUserChar?.id,
                        playlists = playlists,
                        onSelect = { viewModel.selectUserCharacter(char.id) },
                        onEdit = { characterToEdit = char },
                        onDelete = { viewModel.deleteCharacter(char.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { isCreatingNew = true },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_character_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Crear Personaje",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    // Sheet for create / edit Character.ai 1 a 1
    if (isCreatingNew || characterToEdit != null) {
        val initialChar = characterToEdit
        CharacterAiEditSheet(
            initialChar = initialChar,
            defaultRole = if (selectedTab == 0) "NPC" else "USER",
            playlists = playlists,
            onDismiss = {
                isCreatingNew = false
                characterToEdit = null
            },
            onSave = { id, name, role, pers, bg, dial, greet, avatar, stats, pl ->
                viewModel.saveCharacter(id, name, role, pers, bg, dial, greet, avatar, stats, pl)
                isCreatingNew = false
                characterToEdit = null
            }
        )
    }
}

/**
 * Character card styled 1 to 1 with Character.ai feed aesthetic
 */
@Composable
fun CaiCharacterCard(
    character: CharacterEntity,
    isSelectedUser: Boolean,
    playlists: List<PlaylistEntity>,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelectedUser)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelectedUser) MaterialTheme.colorScheme.primary else Color(0xFF27272A)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Image from Gallery or Initials Fallback (56.dp circular)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, Color(0xFF3F3F46), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (character.avatarUri != null) {
                            AsyncImage(
                                model = character.avatarUri,
                                contentDescription = character.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = character.name.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = character.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelectedUser) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Activo",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Tagline / Lema
                        if (character.personality.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = character.personality,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Action buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Preview Greeting (if NPC) or Definition summary
            if (character.initialGreeting.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${character.initialGreeting}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // If USER persona: Stats pill breakdown
            if (character.role == "USER" && character.statsJson.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                CaiUserStatsRow(statsJson = character.statsJson)
            }

            // Select active player persona button
            if (character.role == "USER" && !isSelectedUser) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Interpretar esta Persona",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CaiUserStatsRow(statsJson: String) {
    val stats = remember(statsJson) {
        try {
            val obj = JSONObject(statsJson)
            listOf(
                Triple("FUE", obj.optInt("STR", 10), true),
                Triple("DES", obj.optInt("DEX", 10), true),
                Triple("CON", obj.optInt("CON", 10), true),
                Triple("INT", obj.optInt("INT", 10), true),
                Triple("SAB", obj.optInt("WIS", 10), true),
                Triple("CAR", obj.optInt("CHA", 10), true),
                Triple("HP", obj.optInt("HP", 20), false)
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    if (stats.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF18181B))
                .border(1.dp, Color(0xFF27272A), RoundedCornerShape(10.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            stats.forEach { (label, value, hasMod) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 9.sp
                    )
                    Text(
                        text = "$value",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp
                    )
                    if (hasMod) {
                        val mod = DiceSystem.calculateModifier(value)
                        val sign = if (mod >= 0) "+$mod" else "$mod"
                        Text(
                            text = sign,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 1-to-1 Character.ai Creation Sheet:
 * - Unlimited characters on all fields
 * - For Bots: Name, Tagline, Greeting, Full Definition
 * - For Player Personas: Name, Persona Description (About Me), RPG Stats with chat edit notification
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterAiEditSheet(
    initialChar: CharacterEntity?,
    defaultRole: String,
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, String, String, String, String?, String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var role by remember { mutableStateOf(initialChar?.role ?: defaultRole) }
    var name by remember { mutableStateOf(initialChar?.name ?: "") }
    var tagline by remember { mutableStateOf(initialChar?.personality ?: "") }
    var greeting by remember { mutableStateOf(initialChar?.initialGreeting ?: "") }
    var definition by remember { mutableStateOf(initialChar?.background ?: "") }
    var personaDescription by remember { mutableStateOf(initialChar?.personality ?: "") }
    var avatarUri by remember { mutableStateOf(initialChar?.avatarUri) }
    var selectedPlaylistIds by remember {
        mutableStateOf(
            initialChar?.linkedPlaylistIds?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        )
    }

    // RPG Stats for Player Personas
    var strVal by remember { mutableStateOf("14") }
    var dexVal by remember { mutableStateOf("14") }
    var conVal by remember { mutableStateOf("12") }
    var intVal by remember { mutableStateOf("10") }
    var wisVal by remember { mutableStateOf("10") }
    var chaVal by remember { mutableStateOf("12") }
    var hpVal by remember { mutableStateOf("25") }

    remember(initialChar) {
        if (initialChar?.statsJson != null && initialChar.statsJson.isNotBlank()) {
            try {
                val o = JSONObject(initialChar.statsJson)
                strVal = o.optInt("STR", 14).toString()
                dexVal = o.optInt("DEX", 14).toString()
                conVal = o.optInt("CON", 12).toString()
                intVal = o.optInt("INT", 10).toString()
                wisVal = o.optInt("WIS", 10).toString()
                chaVal = o.optInt("CHA", 12).toString()
                hpVal = o.optInt("HP", 25).toString()
            } catch (_: Exception) {}
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUri = uri.toString()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (role == "NPC") {
                        if (initialChar == null) "Crear Bot IA" else "Editar Bot"
                    } else {
                        if (initialChar == null) "Crear Persona de Jugador" else "Editar Persona"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Ilimitado ∞",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Role selector chips (Bot vs Persona)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = role == "NPC",
                    onClick = { role = "NPC" },
                    label = { Text("Bot IA (Narrador)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
                FilterChip(
                    selected = role == "USER",
                    onClick = { role = "USER" },
                    label = { Text("Persona de Jugador") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar picker
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, Color(0xFF3F3F46), CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Elegir foto",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Subir Avatar")
                    }
                    Text(
                        text = "Imagen de perfil para Character.ai",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (role == "NPC") {
                // ==================== BOT CREATION FORM (1 A 1 CHARACTER.AI) ====================
                // 1. Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Bot") },
                    placeholder = { Text("Ej: Raiden, Gandalf, Guía de Aventura") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Tagline / Short Description (Unlimited)
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("Lema / Descripción Corta (Tagline)") },
                    placeholder = { Text("¿En pocas palabras, quién es este bot?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Initial Greeting / First Message (Unlimited)
                OutlinedTextField(
                    value = greeting,
                    onValueChange = { greeting = it },
                    label = { Text("Saludo Inicial (Primer Mensaje)") },
                    placeholder = { Text("¿Qué dice el personaje para comenzar la conversación?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Character Definition / Prompt / Lore (Unlimited)
                OutlinedTextField(
                    value = definition,
                    onValueChange = { definition = it },
                    label = { Text("Definición Completa del Personaje (Ilimitada ∞)") },
                    placeholder = {
                        Text("Escribe toda la historia, personalidad, motivaciones y ejemplos de diálogo ({{char}}: ... {{user}}: ...). Puedes escribir tanto como desees sin ningún límite de caracteres.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    maxLines = 14
                )

                // Optional Linked Playlists
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Música y Ocasiones Ambientales (Opcional)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                playlists.forEach { pl ->
                    val isLinked = selectedPlaylistIds.contains(pl.id.toString())
                    FilterChip(
                        selected = isLinked,
                        onClick = {
                            val current = selectedPlaylistIds.toMutableSet()
                            if (isLinked) current.remove(pl.id.toString())
                            else current.add(pl.id.toString())
                            selectedPlaylistIds = current
                        },
                        label = { Text("${pl.name} [${pl.ambientType}]") },
                        modifier = Modifier.padding(end = 6.dp, bottom = 4.dp)
                    )
                }

            } else {
                // ==================== PLAYER PERSONA FORM (1 A 1 CHARACTER.AI) ====================
                // 1. Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la Persona / Jugador") },
                    placeholder = { Text("Ej: Alexander, El Pícaro Sombrío, Explorador") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Persona Description / About Me (Unlimited)
                OutlinedTextField(
                    value = personaDescription,
                    onValueChange = { personaDescription = it },
                    label = { Text("Descripción de tu Persona (About Me - Ilimitada ∞)") },
                    placeholder = {
                        Text("Describe a tu personaje, su trasfondo, apariencia, tono y cómo deben tratarte los bots. Longitud completamente ilimitada.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 12
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. RPG Stats with Chat-Customization Notice
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ESTADÍSTICAS RPG PERSONALIZABLES POR CHAT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Puedes cambiar tus atributos en cualquier momento durante la partida escribiendo en el chat: /stat FUE 18, /stat DES +2, /hp 30 o consultar con /stats.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Valores Iniciales de Atributos:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = strVal, onValueChange = { strVal = it }, label = { Text("FUE") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dexVal, onValueChange = { dexVal = it }, label = { Text("DES") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = conVal, onValueChange = { conVal = it }, label = { Text("CON") }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = intVal, onValueChange = { intVal = it }, label = { Text("INT") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = wisVal, onValueChange = { wisVal = it }, label = { Text("SAB") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = chaVal, onValueChange = { chaVal = it }, label = { Text("CAR") }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = hpVal,
                    onValueChange = { hpVal = it },
                    label = { Text("Puntos de Golpe Iniciales (HP)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val statsObj = JSONObject().apply {
                            put("STR", strVal.toIntOrNull() ?: 10)
                            put("DEX", dexVal.toIntOrNull() ?: 10)
                            put("CON", conVal.toIntOrNull() ?: 10)
                            put("INT", intVal.toIntOrNull() ?: 10)
                            put("WIS", wisVal.toIntOrNull() ?: 10)
                            put("CHA", chaVal.toIntOrNull() ?: 10)
                            put("HP", hpVal.toIntOrNull() ?: 20)
                            put("MAX_HP", hpVal.toIntOrNull() ?: 20)
                            put("LVL", 1)
                        }

                        val savedPersonality = if (role == "NPC") tagline else personaDescription
                        val savedBackground = if (role == "NPC") definition else ""

                        onSave(
                            initialChar?.id ?: 0L,
                            name,
                            role,
                            savedPersonality,
                            savedBackground,
                            "", // dialogue
                            greeting,
                            avatarUri,
                            statsObj.toString(),
                            selectedPlaylistIds.joinToString(",")
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_character_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (role == "NPC") "Guardar Bot IA" else "Guardar Persona de Jugador",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
