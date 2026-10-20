package com.example.ui.screens.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import java.io.File
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import com.example.dice.DiceType
import com.example.dice.DiceSystem
import com.example.dice.RollResult
import com.example.ui.components.Dice3DOverlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.entities.CharacterEntity
import com.example.data.local.entities.MessageEntity
import com.example.ui.components.DiceRollerSheet
import com.example.ui.viewmodel.MessageNode
import com.example.ui.viewmodel.RoleplayViewModel

@Composable
fun ChatScreen(
    viewModel: RoleplayViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentPath by viewModel.currentConversationPath.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamingText by viewModel.streamingMessageText.collectAsState()
    val lengthPref by viewModel.lengthPreference.collectAsState()
    val autoSaveNotice by viewModel.autoSaveNotice.collectAsState()
    val userChar by viewModel.selectedUserCharacter.collectAsState()
    val customBgUri by viewModel.chatBackgroundUri.collectAsState()
    val characters by viewModel.activeCharacters.collectAsState()

    var inputTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var showActionMenu by remember { mutableStateOf(false) }
    var active3DRoll by remember { mutableStateOf<Pair<RollResult, String?>?>(null) }
    var showDiceSheet by remember { mutableStateOf(false) }
    var showRollActionDialog by remember { mutableStateOf(false) }
    var showGeminiMapDialog by remember { mutableStateOf(false) }
    var showGeminiImageDialog by remember { mutableStateOf(false) }
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }
    val isGeneratingMedia by viewModel.isGeneratingMedia.collectAsState()
    val mediaStatus by viewModel.mediaStatus.collectAsState()

    // Message edit dialog
    var messageToEdit by remember { mutableStateOf<MessageEntity?>(null) }
    var editTextValue by remember { mutableStateOf("") }

    // Photo picker for chat background
    val bgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setChatBackground(uri.toString())
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(currentPath.size, streamingText) {
        if (currentPath.isNotEmpty()) {
            listState.animateScrollToItem(currentPath.size)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val activeWorld by viewModel.activeWorld.collectAsState()
        val defaultBgRes = when (activeWorld?.genre) {
            com.example.data.local.entities.StoryGenres.SCI_FI -> R.drawable.img_genre_scifi_1792454034452
            com.example.data.local.entities.StoryGenres.MODERN_NOIR -> R.drawable.img_genre_modern_1792454047373
            else -> R.drawable.bg_fantasy_camp_1792455550994
        }

        if (customBgUri != null) {
            AsyncImage(
                model = customBgUri,
                contentDescription = "Fondo del chat",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = defaultBgRes,
                contentDescription = "Fondo de la historia",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Deep atmospheric overlay for Character.ai readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121214).copy(alpha = 0.88f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Top Bar: Length Selector & Background Picker (Minimalist C.AI)
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Longitud:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf("SHORT" to "Corta", "MEDIUM" to "Media", "LONG" to "Larga").forEach { (key, label) ->
                            val isSelected = lengthPref == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { viewModel.setLengthPreference(key) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            bgPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Cambiar fondo del chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // Auto-save banner
            AnimatedVisibility(
                visible = autoSaveNotice != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = autoSaveNotice ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissAutoSaveNotice() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Character.ai Conversation Stream List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(6.dp)) }

                items(currentPath, key = { it.message.id }) { node ->
                    val speakerCharacter = characters.firstOrNull { it.id == node.message.speakerCharacterId }
                    CaiMessageBubble(
                        node = node,
                        userCharacter = userChar,
                        speakerCharacter = speakerCharacter,
                        onBranchPrev = {
                            if (node.branchIndex > 0) {
                                viewModel.switchBranch(node.parentId, node.branchIndex - 1)
                            }
                        },
                        onBranchNext = {
                            if (node.branchIndex < node.totalBranches - 1) {
                                viewModel.switchBranch(node.parentId, node.branchIndex + 1)
                            }
                        },
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(node.message.content))
                            Toast.makeText(context, "Mensaje copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        },
                        onRegenerate = { viewModel.regenerateMessage(node.message.id) },
                        onEdit = {
                            messageToEdit = node.message
                            editTextValue = node.message.content
                        },
                        onRewind = { viewModel.rewindToMessage(node.message.id) },
                        onImageClick = { fullScreenImageUri = it }
                    )
                }

                // Streaming Narrator Bubble (Character.ai style)
                if (isStreaming) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Narrador",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "escribiendo...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = streamingText.ifBlank { "..." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }
            }

            // Bottom Input Section (Character.ai style with Gemini media & shortcuts)
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Media Generation Status Pill
                    AnimatedVisibility(
                        visible = isGeneratingMedia,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = mediaStatus ?: "Generando con Gemini...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Slash Command Suggestions
                    AnimatedVisibility(
                        visible = inputTextFieldValue.text.startsWith("/"),
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val slashItems = listOf(
                                "/stats" to "📊 Ficha de atributos",
                                "/stat" to "⚡ Modificar estadística",
                                "/hp" to "❤️ Puntos de vida",
                                "/mapa" to "🗺️ Mapa táctico",
                                "/imagen" to "🎨 Ilustración",
                                "/roll" to "🎲 Tirada d20 con acción",
                                "/ooc" to "(( )) Fuera de rol",
                                "/ayuda" to "📜 Ayuda de comandos"
                            )
                            items(slashItems) { (cmd, label) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        inputTextFieldValue = TextFieldValue(
                                            text = "$cmd ",
                                            selection = TextRange(cmd.length + 1)
                                        )
                                    }
                                ) {
                                    Text(
                                        text = "$cmd • $label",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Floating Vertical Action Menu (anchored over the single action button)
                    AnimatedVisibility(
                        visible = showActionMenu,
                        enter = fadeIn(tween(180)) + expandVertically(expandFrom = Alignment.Bottom),
                        exit = fadeOut(tween(140)) + shrinkVertically(shrinkTowards = Alignment.Bottom)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF18181B).copy(alpha = 0.98f),
                            border = BorderStroke(1.dp, Color(0xFF27272A)),
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .widthIn(max = 320.dp)
                                .testTag("action_vertical_menu")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "HERRAMIENTAS DE ROL",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { showActionMenu = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cerrar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    color = Color(0xFF27272A),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )

                                // 1. Diálogo entre comillas ""
                                ActionVerticalMenuItem(
                                    title = "Diálogo con comillas",
                                    subtitle = "Inserta \"\" con cursor en medio",
                                    icon = Icons.Default.FormatQuote,
                                    accentColor = Color(0xFF38BDF8),
                                    onClick = {
                                        val currentText = inputTextFieldValue.text
                                        val sel = inputTextFieldValue.selection
                                        val start = sel.min.coerceIn(0, currentText.length)
                                        val end = sel.max.coerceIn(0, currentText.length)
                                        if (start != end) {
                                            val selected = currentText.substring(start, end)
                                            val newText = currentText.substring(0, start) + "\"" + selected + "\"" + currentText.substring(end)
                                            inputTextFieldValue = TextFieldValue(text = newText, selection = TextRange(start + 1, end + 1))
                                        } else {
                                            val newText = currentText.substring(0, start) + "\"\"" + currentText.substring(start)
                                            inputTextFieldValue = TextFieldValue(text = newText, selection = TextRange(start + 1))
                                        }
                                        showActionMenu = false
                                    }
                                )

                                // 2. Fuera de rol (( ))
                                ActionVerticalMenuItem(
                                    title = "Fuera de rol (OOC)",
                                    subtitle = "Inserta (( )) con cursor en medio",
                                    icon = Icons.Default.Terminal,
                                    accentColor = Color(0xFFA78BFA),
                                    onClick = {
                                        val currentText = inputTextFieldValue.text
                                        val sel = inputTextFieldValue.selection
                                        val start = sel.min.coerceIn(0, currentText.length)
                                        val end = sel.max.coerceIn(0, currentText.length)
                                        if (start != end) {
                                            val selected = currentText.substring(start, end)
                                            val newText = currentText.substring(0, start) + "(( " + selected + " ))" + currentText.substring(end)
                                            inputTextFieldValue = TextFieldValue(text = newText, selection = TextRange(start + 3, end + 3))
                                        } else {
                                            val newText = currentText.substring(0, start) + "((  ))" + currentText.substring(start)
                                            inputTextFieldValue = TextFieldValue(text = newText, selection = TextRange(start + 3))
                                        }
                                        showActionMenu = false
                                    }
                                )

                                // 3. Comandos de barra /
                                ActionVerticalMenuItem(
                                    title = "Comandos de barra",
                                    subtitle = "Inserta / y activa atajos",
                                    icon = Icons.Default.Terminal,
                                    accentColor = Color(0xFFFBBF24),
                                    onClick = {
                                        val currentText = inputTextFieldValue.text
                                        val newText = if (!currentText.startsWith("/")) "/$currentText" else currentText
                                        inputTextFieldValue = TextFieldValue(text = newText, selection = TextRange(1))
                                        showActionMenu = false
                                    }
                                )

                                // 4. Tirar dados con acción
                                ActionVerticalMenuItem(
                                    title = "Lanzar dados con acción",
                                    subtitle = "Animación 3D y tirada d20",
                                    icon = Icons.Default.Casino,
                                    accentColor = Color(0xFFF43F5E),
                                    onClick = {
                                        showActionMenu = false
                                        showRollActionDialog = true
                                    }
                                )

                                // 5. Generar Ilustración Gemini
                                ActionVerticalMenuItem(
                                    title = "Ilustración con Gemini",
                                    subtitle = "Crea escenas visuales con IA",
                                    icon = Icons.Default.Palette,
                                    accentColor = Color(0xFF818CF8),
                                    onClick = {
                                        showActionMenu = false
                                        showGeminiImageDialog = true
                                    }
                                )

                                // 6. Generar Mapa Táctico Gemini
                                ActionVerticalMenuItem(
                                    title = "Mapa táctico Gemini",
                                    subtitle = "Crea cuadrícula de batalla con IA",
                                    icon = Icons.Default.Map,
                                    accentColor = Color(0xFF34D399),
                                    onClick = {
                                        showActionMenu = false
                                        showGeminiMapDialog = true
                                    }
                                )
                            }
                        }
                    }

                    // Floating Rounded Input Capsule
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Single Action Menu Button (Deploys vertical panel)
                        IconButton(
                            onClick = { showActionMenu = !showActionMenu },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (showActionMenu)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (showActionMenu) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .testTag("roleplay_action_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Herramientas de Rol",
                                tint = if (showActionMenu) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Text input field
                        Box(modifier = Modifier.weight(1f)) {
                            if (inputTextFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Escribe acción, diálogo o pulsa ✨ para opciones...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = inputTextFieldValue,
                                onValueChange = { inputTextFieldValue = it },
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_input_field")
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Quick dice button if empty, or send button
                        val canSend = inputTextFieldValue.text.isNotBlank() && !isStreaming && !isGeneratingMedia
                        if (!canSend) {
                            IconButton(
                                onClick = { showDiceSheet = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                    .testTag("open_dice_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = "Tirar dados",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val text = inputTextFieldValue.text
                                    inputTextFieldValue = TextFieldValue("")
                                    viewModel.sendMessage(text)
                                },
                                enabled = canSend,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("chat_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dice Roller Sheet
    if (showDiceSheet) {
        DiceRollerSheet(
            userCharacter = userChar,
            onDismiss = { showDiceSheet = false },
            onRollAndInscribe = { dt, name, mod, dc ->
                showDiceSheet = false
                val rollRes = DiceSystem.roll(
                    diceType = dt,
                    modifierName = name,
                    modifierValue = mod,
                    difficultyClass = dc,
                    context = context
                )
                active3DRoll = Pair(rollRes, null)
            }
        )
    }

    // Message Edit Dialog
    if (messageToEdit != null) {
        AlertDialog(
            onDismissRequest = { messageToEdit = null },
            title = { Text("Editar Mensaje de la Historia") },
            text = {
                OutlinedTextField(
                    value = editTextValue,
                    onValueChange = { editTextValue = it },
                    label = { Text("Contenido") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 8
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        messageToEdit?.let { viewModel.editMessage(it.id, editTextValue) }
                        messageToEdit = null
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Roll with Action Dialog
    if (showRollActionDialog) {
        RollActionDialog(
            initialAction = inputTextFieldValue.text,
            userCharacter = userChar,
            onDismiss = { showRollActionDialog = false },
            onConfirmRoll = { action, dt, modName, modVal, dc ->
                showRollActionDialog = false
                inputTextFieldValue = TextFieldValue("")
                val rollRes = DiceSystem.roll(
                    diceType = dt,
                    modifierName = modName,
                    modifierValue = modVal,
                    difficultyClass = dc,
                    context = context
                )
                active3DRoll = Pair(rollRes, action)
            }
        )
    }

    // 3D Animated Dice Overlay
    active3DRoll?.let { (rollResult, actionText) ->
        Dice3DOverlay(
            rollResult = rollResult,
            actionText = actionText,
            onConfirm = { confirmedResult ->
                active3DRoll = null
                if (!actionText.isNullOrBlank()) {
                    viewModel.rollDiceWithAction(
                        actionText = actionText,
                        diceType = confirmedResult.diceType,
                        modifierName = confirmedResult.modifierName,
                        modifierValue = confirmedResult.modifierValue,
                        difficultyClass = confirmedResult.difficultyClass,
                        precomputedRollResult = confirmedResult
                    )
                } else {
                    viewModel.rollDice(
                        diceType = confirmedResult.diceType,
                        modifierName = confirmedResult.modifierName,
                        modifierValue = confirmedResult.modifierValue,
                        difficultyClass = confirmedResult.difficultyClass,
                        precomputedRollResult = confirmedResult
                    )
                }
            },
            onDismiss = {
                active3DRoll = null
            }
        )
    }

    // Gemini Map Dialog
    if (showGeminiMapDialog) {
        val activeWorld by viewModel.activeWorld.collectAsState()
        GeminiMediaDialog(
            isMap = true,
            worldName = activeWorld?.name,
            worldGenre = activeWorld?.genre,
            onDismiss = { showGeminiMapDialog = false },
            onGenerate = { prompt ->
                showGeminiMapDialog = false
                viewModel.generateGeminiMedia(prompt, isMap = true)
            }
        )
    }

    // Gemini Image Dialog
    if (showGeminiImageDialog) {
        val activeWorld by viewModel.activeWorld.collectAsState()
        GeminiMediaDialog(
            isMap = false,
            worldName = activeWorld?.name,
            worldGenre = activeWorld?.genre,
            onDismiss = { showGeminiImageDialog = false },
            onGenerate = { prompt ->
                showGeminiImageDialog = false
                viewModel.generateGeminiMedia(prompt, isMap = false)
            }
        )
    }

    // Full Screen Image/Map Viewer Dialog
    fullScreenImageUri?.let { uri ->
        FullScreenImageDialog(
            imageUri = uri,
            onDismiss = { fullScreenImageUri = null }
        )
    }
}

/**
 * Character.ai Minimalist Message Bubble
 * Features left circular avatar, crisp typography, clean subtle bubbles,
 * media cards for Gemini maps & illustrations, and discreet low-contrast action row.
 */
@Composable
fun CaiMessageBubble(
    node: MessageNode,
    userCharacter: CharacterEntity?,
    speakerCharacter: CharacterEntity?,
    onBranchPrev: () -> Unit,
    onBranchNext: () -> Unit,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    onRewind: () -> Unit,
    onImageClick: (String) -> Unit = {}
) {
    val msg = node.message
    val isUser = msg.senderType == "USER"
    val isDice = msg.senderType == "DICE_ROLL"
    val isGeminiMedia = msg.senderType == "GEMINI_MAP" || msg.senderType == "GEMINI_IMAGE" || msg.imageUri != null
    val isSystem = msg.senderType == "SYSTEM_EVENT"

    if (isSystem) {
        CaiSystemBubble(msg = msg)
        return
    }

    if (isDice) {
        // Special Minimalist Dice Roll Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Dado",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = msg.content,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        return
    }

    if (isGeminiMedia) {
        CaiGeminiMediaBubble(
            node = node,
            onBranchPrev = onBranchPrev,
            onBranchNext = onBranchNext,
            onCopy = onCopy,
            onImageClick = onImageClick
        )
        return
    }

    if (isUser) {
        // User Message (Right-aligned, sleek rounded dark pill)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = msg.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }

                // Discreet edit icon on user message
                Row(
                    modifier = Modifier.padding(top = 2.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar mensaje",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // User character avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (userCharacter?.avatarUri != null) {
                    AsyncImage(
                        model = userCharacter.avatarUri,
                        contentDescription = "Avatar de usuario",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = userCharacter?.name?.take(2)?.uppercase() ?: "TÚ",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.sp
                    )
                }
            }
        }
    } else {
        // Narrator / NPC Message (Character.ai style with Left Avatar and Discreet Actions)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left circular avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (speakerCharacter?.avatarUri != null) {
                    AsyncImage(
                        model = speakerCharacter.avatarUri,
                        contentDescription = speakerCharacter.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Header: Speaker Name & Role Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg.senderName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (msg.speakerCharacterId != null) "PNJ" else "Narrador",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bubble Container with subtle background
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = msg.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 23.sp,
                        letterSpacing = 0.2.sp
                    )
                }

                // Discreet Character.ai Action Bar below the message
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Branch Switcher Capsule (< 1 / 3 >)
                    if (node.totalBranches > 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            IconButton(
                                onClick = onBranchPrev,
                                enabled = node.branchIndex > 0,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Rama anterior",
                                    tint = if (node.branchIndex > 0) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "${node.branchIndex + 1}/${node.totalBranches}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                            IconButton(
                                onClick = onBranchNext,
                                enabled = node.branchIndex < node.totalBranches - 1,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Rama siguiente",
                                    tint = if (node.branchIndex < node.totalBranches - 1) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Right: Discreet Action Icons (Copy, Regenerate, Edit, Rewind)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        IconButton(
                            onClick = onRewind,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Rebobinar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bubble for rendering Gemini generated maps and visual scenes
 */
@Composable
fun CaiGeminiMediaBubble(
    node: MessageNode,
    onBranchPrev: () -> Unit,
    onBranchNext: () -> Unit,
    onCopy: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val msg = node.message
    val isMap = msg.mediaType == "MAP" || msg.senderType == "GEMINI_MAP"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar with Map or Palette icon
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (isMap)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                )
                .border(
                    1.dp,
                    if (isMap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isMap) Icons.Default.Map else Icons.Default.Palette,
                contentDescription = null,
                tint = if (isMap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = msg.senderName,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isMap) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isMap) "MAPA TÁCTICO • GEMINI" else "ILUSTRACIÓN • GEMINI",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isMap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Card Container with Image and Description
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Image thumbnail
                    if (msg.imageUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable { onImageClick(msg.imageUri) }
                        ) {
                            AsyncImage(
                                model = File(msg.imageUri),
                                contentDescription = msg.senderName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Tap to zoom badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Ampliar",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Companion text
                    Text(
                        text = msg.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 21.sp
                    )
                }
            }

            // Action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (node.totalBranches > 1) {
                    Text(
                        text = "${node.branchIndex + 1}/${node.totalBranches}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bubble for system announcements and command help
 */
@Composable
fun CaiSystemBubble(msg: MessageEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = msg.senderName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = msg.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * Shortcut chip used in chat input toolbar
 */
@Composable
fun ChatShortcutChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (highlight)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Item for the vertical action menu anchored over the action button
 */
@Composable
fun ActionVerticalMenuItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Dialog to combine a player's written action with a dice roll
 */
@Composable
fun RollActionDialog(
    initialAction: String,
    userCharacter: CharacterEntity?,
    onDismiss: () -> Unit,
    onConfirmRoll: (action: String, diceType: DiceType, modifierName: String, modifierValue: Int, difficultyClass: Int?) -> Unit
) {
    var actionText by remember { mutableStateOf(initialAction.ifBlank { "Intento realizar una maniobra" }) }
    var selectedDice by remember { mutableStateOf(DiceType.D20) }
    var selectedStat by remember { mutableStateOf("Destreza") }
    var selectedDc by remember { mutableStateOf<Int?>(15) }

    val stats = listOf(
        "Fuerza" to 2,
        "Destreza" to 3,
        "Constitución" to 1,
        "Inteligencia" to 2,
        "Sabiduría" to 1,
        "Carisma" to 0,
        "Ninguno" to 0
    )

    val difficulties = listOf(
        "Sin DC" to null,
        "Fácil (DC 10)" to 10,
        "Moderada (DC 15)" to 15,
        "Difícil (DC 18)" to 18,
        "Heroica (DC 22)" to 22
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lanzar Dados con tu Acción")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Acción que intentas en la historia:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                OutlinedTextField(
                    value = actionText,
                    onValueChange = { actionText = it },
                    placeholder = { Text("Ej: Intento forzar la cerradura sigilosamente...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Text(
                    text = "Tipo de Dado:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DiceType.values()) { dt ->
                        val isSel = selectedDice == dt
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedDice = dt }
                        ) {
                            Text(
                                text = dt.label.uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Estadística / Modificador:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(stats) { (name, mod) ->
                        val isSel = selectedStat == name
                        val label = if (mod != 0) "$name (+${mod})" else name
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedStat = name }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Dificultad (DC):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(difficulties) { (label, dcVal) ->
                        val isSel = selectedDc == dcVal
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedDc = dcVal }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSel) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val modVal = stats.firstOrNull { it.first == selectedStat }?.second ?: 0
                    onConfirmRoll(actionText, selectedDice, selectedStat, modVal, selectedDc)
                }
            ) {
                Text("⚔️ Lanzar y Enviar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Dialog to configure Gemini Map or Image generation
 */
@Composable
fun GeminiMediaDialog(
    isMap: Boolean,
    worldName: String?,
    worldGenre: String?,
    onDismiss: () -> Unit,
    onGenerate: (prompt: String) -> Unit
) {
    val defaultPrompt = if (isMap) {
        "Plano táctico de ${worldName ?: "la zona actual"} con cuadrícula de combate, cámaras y accesos"
    } else {
        "Escena dramática en ${worldName ?: "la aventura"} capturando la tensión del momento"
    }
    var prompt by remember { mutableStateOf(defaultPrompt) }

    val presets = if (isMap) {
        listOf(
            "Plano táctico de ${worldName ?: "la zona"}",
            "Mazmorra subterránea con trampas",
            "Estación orbital y esclusas",
            "Callejón cyberpunk bajo lluvia",
            "Ruinas arcanas con altar místico"
        )
    } else {
        listOf(
            "Escena dramática en ${worldName ?: "la aventura"}",
            "Retrato épico de personaje en combate",
            "Paisaje cósmico y nebulosas",
            "Encuentro misterioso con el PNJ",
            "Momento íntimo junto a la fogata"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isMap) Icons.Default.Map else Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isMap) "Generar Mapa con Gemini" else "Generar Imagen con Gemini")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isMap)
                        "Gemini generará un plano cartográfico o táctico con cuadrícula:"
                    else
                        "Gemini ilustrará la escena visualmente según tus instrucciones:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Sugerencias rápidas:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presets) { preset ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { prompt = preset }
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(if (isMap) "Descripción del mapa" else "Descripción de la imagen") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (prompt.isNotBlank()) {
                        onGenerate(prompt.trim())
                    }
                }
            ) {
                Text("✨ Generar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Fullscreen viewer for tactical maps and illustrations
 */
@Composable
fun FullScreenImageDialog(
    imageUri: String,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF101012),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vista de Exploración",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = File(imageUri),
                        contentDescription = "Mapa / Imagen ampliada",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

