package com.example.ui.screens.stories

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.R
import com.example.data.local.entities.StoryGenres
import com.example.data.local.entities.WorldEntity
import com.example.ui.viewmodel.RoleplayViewModel

data class StoryTemplate(
    val genreKey: String,
    val genreLabel: String,
    val iconEmoji: String,
    val defaultTitle: String,
    val defaultDescription: String,
    val defaultLore: String,
    val defaultAmbient: String,
    val defaultNpcName: String,
    val defaultNpcPersonality: String,
    val defaultGreeting: String,
    val defaultCoverRes: Int?
)

val STORY_TEMPLATES = listOf(
    StoryTemplate(
        genreKey = StoryGenres.DARK_FANTASY,
        genreLabel = "Fantasía Oscura",
        iconEmoji = "⚔️",
        defaultTitle = "Ecos de Sangre y Magia",
        defaultDescription = "Reino medieval gótico asediado por pactos prohibidos y órdenes inquisitoriales.",
        defaultLore = "Las tierras del norte están consumidas por una bruma espectral. Los supervivientes se refugian en ciudadelas fortificadas mientras brujos proscritos buscan reliquias del Velo.",
        defaultAmbient = "Descanso en Campamento",
        defaultNpcName = "Moira la Guerrera",
        defaultNpcPersonality = "Protectora, curtida en mil batallas, astuta y observadora.",
        defaultGreeting = "Si buscas paz, toma asiento junto al fuego; si buscas pelea, la puerta está detrás de ti.",
        defaultCoverRes = R.drawable.bg_fantasy_camp_1792455550994
    ),
    StoryTemplate(
        genreKey = StoryGenres.SCI_FI,
        genreLabel = "Espacial / Sci-Fi",
        iconEmoji = "🚀",
        defaultTitle = "Odisea Nova: Sector Orión",
        defaultDescription = "Exploración interestelar en los límites de la galaxia y contacto alienígena.",
        defaultLore = "Año 2389. La nave Odisea Nova navega un sector olvidado donde los restos de una civilización precursora emiten señales gravitacionales anómalas.",
        defaultAmbient = "Odisea Espacial",
        defaultNpcName = "ARIA (IA de Nave)",
        defaultNpcPersonality = "Analítica, formal, con sarcasmo sutil y lealtad absoluta.",
        defaultGreeting = "Comandante, sensores detectan una fluctuación energética en el hangar abandonado. ¿Cuáles son sus órdenes?",
        defaultCoverRes = R.drawable.img_genre_scifi_1792454034452
    ),
    StoryTemplate(
        genreKey = StoryGenres.MODERN_NOIR,
        genreLabel = "Moderna / Noir",
        iconEmoji = "🕵️",
        defaultTitle = "Distrito 9: Lluvia y Neón",
        defaultDescription = "Investigación detectivesca en una metrópolis moderna lluviosa y corrupta.",
        defaultLore = "En New Haven la policía trabaja para los cárteles corporativos. Como detective privado, solo te queda tu instinto y tu pistola para resolver el crimen del siglo.",
        defaultAmbient = "Investigación & Misterio",
        defaultNpcName = "Detective Miller",
        defaultNpcPersonality = "Cínico, cansado, tenaz, adicto al café amargo.",
        defaultGreeting = "Cierra la persiana. Si los matones de Thorne nos ven hablando, no pasamos de esta noche.",
        defaultCoverRes = R.drawable.img_genre_modern_1792454047373
    ),
    StoryTemplate(
        genreKey = StoryGenres.CYBERPUNK,
        genreLabel = "Cyberpunk",
        iconEmoji = "⚡",
        defaultTitle = "Protocolo Cero: Neo-Kyoto",
        defaultDescription = "Distopía cibernética de megacorporaciones, cromo y hackers rebeldes.",
        defaultLore = "Bajo las torres holográficas de las megacorporaciones, los mercenarios callejeros sobreviven pirateando neuro-implantes y traficando con memorias robadas.",
        defaultAmbient = "Ciudad Cyberpunk",
        defaultNpcName = "Nyx (Fixer Callejera)",
        defaultNpcPersonality = "Eléctrica, rápida con la navaja y los teclados cibernéticos.",
        defaultGreeting = "Tengo el chip de datos que me pediste... pero el ICE de Arasaka ya nos tiene en el radar.",
        defaultCoverRes = null
    ),
    StoryTemplate(
        genreKey = StoryGenres.POST_APOCALYPTIC,
        genreLabel = "Apocalipsis",
        iconEmoji = "☢️",
        defaultTitle = "Cenizas del Éxodo",
        defaultDescription = "Supervivencia brutal en un páramo desértico tras el colapso global.",
        defaultLore = "Hace cincuenta años cayeron las bombas de pulso. Ahora el agua limpia vale más que el oro y los clanes de saqueadores patrullan las autopistas en ruinas.",
        defaultAmbient = "Persecución & Fuga",
        defaultNpcName = "Rusty el Chatarrero",
        defaultNpcPersonality = "Desconfiado, práctico, experto en reparar motores a punto de reventar.",
        defaultGreeting = "Guarda el arma o te la quito. Si tienes filtros de agua o munición de 9mm, podemos negociar.",
        defaultCoverRes = null
    ),
    StoryTemplate(
        genreKey = StoryGenres.COSMIC_HORROR,
        genreLabel = "Terror Cósmico",
        iconEmoji = "🐙",
        defaultTitle = "El Susurro de las Mareas",
        defaultDescription = "Misterio lovecraftiano en un pueblo costero azotado por la locura.",
        defaultLore = "Blackwater Reef parece un pueblo pesquero en decadencia, pero los ancianos celebran extraños ritos nocturnos cuando la niebla espesa devora el muelle.",
        defaultAmbient = "Terror Cósmico",
        defaultNpcName = "Padre Jeremías",
        defaultNpcPersonality = "Fanático, febril, de mirada extraviada y voz temblorosa.",
        defaultGreeting = "No debiste venir durante la luna de estío... las profundidades reclaman lo que es suyo.",
        defaultCoverRes = null
    ),
    StoryTemplate(
        genreKey = StoryGenres.CUSTOM,
        genreLabel = "Personalizada",
        iconEmoji = "✨",
        defaultTitle = "Mi Nueva Historia",
        defaultDescription = "Crea un mundo completamente a tu medida.",
        defaultLore = "Describe el universo, sus reglas y sus secretos...",
        defaultAmbient = "Descanso en Campamento",
        defaultNpcName = "Guía del Destino",
        defaultNpcPersonality = "Misterioso y elocuente.",
        defaultGreeting = "Bienvenido a este nuevo comienzo. ¿Qué camino elegiremos hoy?",
        defaultCoverRes = null
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoriesHubScreen(
    viewModel: RoleplayViewModel,
    onPlayStory: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allWorlds by viewModel.allWorlds.collectAsState()
    val activeWorld by viewModel.activeWorld.collectAsState()

    var selectedGenreFilter by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Dialogs for Restart and Duplicate
    var storyToRestart by remember { mutableStateOf<WorldEntity?>(null) }
    var storyToDuplicate by remember { mutableStateOf<WorldEntity?>(null) }
    var duplicateTitle by remember { mutableStateOf("") }
    var storyToDelete by remember { mutableStateOf<WorldEntity?>(null) }

    val filteredStories = remember(allWorlds, selectedGenreFilter) {
        if (selectedGenreFilter == null) allWorlds
        else allWorlds.filter { it.genre == selectedGenreFilter }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = "Historias & Campañas",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Crea historias ilimitadas, cambia de ambientación (espacial, moderna, fantasía) y vuelve a jugarlas cuando quieras.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Genre Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedGenreFilter == null,
                        onClick = { selectedGenreFilter = null },
                        label = { Text("Todas (${allWorlds.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                items(STORY_TEMPLATES) { template ->
                    val count = allWorlds.count { it.genre == template.genreKey }
                    FilterChip(
                        selected = selectedGenreFilter == template.genreKey,
                        onClick = { selectedGenreFilter = template.genreKey },
                        label = { Text("${template.iconEmoji} ${template.genreLabel} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stories List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredStories, key = { it.id }) { story ->
                    StoryCard(
                        story = story,
                        isActive = story.id == activeWorld?.id,
                        onPlay = {
                            viewModel.selectStory(story.id)
                            onPlayStory(story.id)
                        },
                        onRestart = { storyToRestart = story },
                        onDuplicate = {
                            storyToDuplicate = story
                            duplicateTitle = "${story.name} (Línea Alternativa)"
                        },
                        onDelete = { storyToDelete = story }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Floating Action Button to create a new Story
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_story_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Nueva Historia",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    // Create Story Sheet
    if (showCreateDialog) {
        CreateStorySheet(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, genre, desc, lore, sysPrompt, ambient, cover, chatBg, npcName, npcPers, greet ->
                viewModel.createNewStory(
                    title = title,
                    genre = genre,
                    description = desc,
                    lore = lore,
                    systemPrompt = sysPrompt,
                    ambientType = ambient,
                    coverUri = cover,
                    chatBgUri = chatBg,
                    starterNpcName = npcName,
                    starterNpcPersonality = npcPers,
                    starterGreeting = greet
                )
                showCreateDialog = false
                viewModel.activeWorld.value?.id?.let { onPlayStory(it) }
            }
        )
    }

    // Confirm Restart Dialog
    if (storyToRestart != null) {
        val st = storyToRestart!!
        AlertDialog(
            onDismissRequest = { storyToRestart = null },
            title = { Text("¿Volver a jugar esta historia?") },
            text = {
                Text(
                    "Se reiniciarán los mensajes de '${st.name}' desde el prólogo inicial. Las fichas de personajes y el Lorebook se mantendrán intactos para que puedas vivir una nueva partida con diferentes elecciones."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restartStory(st.id)
                        viewModel.selectStory(st.id)
                        storyToRestart = null
                        onPlayStory(st.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Reiniciar y Jugar")
                }
            },
            dismissButton = {
                TextButton(onClick = { storyToRestart = null }) { Text("Cancelar") }
            }
        )
    }

    // Duplicate Dialog
    if (storyToDuplicate != null) {
        val st = storyToDuplicate!!
        AlertDialog(
            onDismissRequest = { storyToDuplicate = null },
            title = { Text("Duplicar Historia (Línea Alternativa)") },
            text = {
                Column {
                    Text("Crea una copia exacta del mundo y lore para explorar un camino narrativo diferente.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = duplicateTitle,
                        onValueChange = { duplicateTitle = it },
                        label = { Text("Título de la copia") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (duplicateTitle.isNotBlank()) {
                            viewModel.duplicateStory(st.id, duplicateTitle)
                            storyToDuplicate = null
                        }
                    }
                ) {
                    Text("Duplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { storyToDuplicate = null }) { Text("Cancelar") }
            }
        )
    }

    // Delete Story Dialog
    if (storyToDelete != null) {
        val st = storyToDelete!!
        AlertDialog(
            onDismissRequest = { storyToDelete = null },
            title = { Text("¿Eliminar '${st.name}'?") },
            text = { Text("Se borrarán todos sus mensajes, crónica y lugares asociados. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStory(st.id)
                        storyToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { storyToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun StoryCard(
    story: WorldEntity,
    isActive: Boolean,
    onPlay: () -> Unit,
    onRestart: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val template = STORY_TEMPLATES.firstOrNull { it.genreKey == story.genre }
    val genreLabel = template?.genreLabel ?: story.genre
    val genreEmoji = template?.iconEmoji ?: "📖"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("story_card_${story.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isActive) 2.dp else 1.dp,
            if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Story Cover Image Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                if (story.coverImageUri != null) {
                    AsyncImage(
                        model = story.coverImageUri,
                        contentDescription = story.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val fallbackRes = template?.defaultCoverRes ?: R.drawable.bg_fantasy_camp_1792455550994
                    AsyncImage(
                        model = fallbackRes,
                        contentDescription = story.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Gradient darkness overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )

                // Genre and Ambient Badges
                Row(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$genreEmoji $genreLabel",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = story.defaultAmbientType,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }

                // Active Story Indicator
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "EN JUEGO",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }
                }
            }

            // Body
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = story.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opciones")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Volver a Jugar (Reiniciar)") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onRestart()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicar Historia") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onDuplicate()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Play / Continue Button
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.AutoStories else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isActive) "Continuar Aventura Actual" else "Jugar esta Historia",
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStorySheet(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, String, String?, String?, String, String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTemplateIndex by remember { mutableStateOf(0) }
    val currentTemplate = STORY_TEMPLATES[selectedTemplateIndex]

    var title by remember { mutableStateOf(currentTemplate.defaultTitle) }
    var description by remember { mutableStateOf(currentTemplate.defaultDescription) }
    var lore by remember { mutableStateOf(currentTemplate.defaultLore) }
    var ambientType by remember { mutableStateOf(currentTemplate.defaultAmbient) }
    var npcName by remember { mutableStateOf(currentTemplate.defaultNpcName) }
    var npcPersonality by remember { mutableStateOf(currentTemplate.defaultNpcPersonality) }
    var greeting by remember { mutableStateOf(currentTemplate.defaultGreeting) }
    var coverUri by remember { mutableStateOf<String?>(null) }
    var chatBgUri by remember { mutableStateOf<String?>(null) }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) coverUri = uri.toString()
    }

    val chatBgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) chatBgUri = uri.toString()
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
            Text(
                text = "Crear Nueva Historia / Campaña",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Selecciona una plantilla de ambientación o personalízala desde cero.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Template Selector Row
            Text(
                text = "Plantilla de Género y Ambientación",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(STORY_TEMPLATES.indices.toList()) { index ->
                    val t = STORY_TEMPLATES[index]
                    val isSelected = index == selectedTemplateIndex
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTemplateIndex = index
                            title = t.defaultTitle
                            description = t.defaultDescription
                            lore = t.defaultLore
                            ambientType = t.defaultAmbient
                            npcName = t.defaultNpcName
                            npcPersonality = t.defaultNpcPersonality
                            greeting = t.defaultGreeting
                        },
                        label = { Text("${t.iconEmoji} ${t.genreLabel}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título de la Historia") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description / Synopsis
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Sinopsis del Escenario") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // World Lore
            OutlinedTextField(
                value = lore,
                onValueChange = { lore = it },
                label = { Text("Trasfondo del Mundo y Reglas del Entorno") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Default Ambient Soundscape
            Text(
                text = "Música / Ambientación Sonora Predeterminada",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(com.example.audio.AmbientOccasions.ALL) { occ ->
                    FilterChip(
                        selected = ambientType == occ.name,
                        onClick = { ambientType = occ.name },
                        label = { Text("${occ.emoji} ${occ.name}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Starter NPC Companion
            Text(
                text = "Personaje Inicial (PNJ que recibe al jugador)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = npcName,
                onValueChange = { npcName = it },
                label = { Text("Nombre del PNJ (ej: Moira, ARIA, Miller)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = npcPersonality,
                onValueChange = { npcPersonality = it },
                label = { Text("Personalidad del PNJ") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = greeting,
                onValueChange = { greeting = it },
                label = { Text("Frase de Saludo Inicial") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Image Customization
            Text(
                text = "Imágenes de la Historia (Galería)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (coverUri == null) "Carátula" else "Carátula ✓", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        chatBgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (chatBgUri == null) "Fondo Chat" else "Fondo Chat ✓", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(
                            title,
                            currentTemplate.genreKey,
                            description,
                            lore,
                            "", // System prompt template
                            ambientType,
                            coverUri,
                            chatBgUri,
                            npcName,
                            npcPersonality,
                            greeting
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_create_story_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Crear e Iniciar Historia", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
