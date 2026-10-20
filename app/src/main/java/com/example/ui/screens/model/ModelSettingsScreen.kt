package com.example.ui.screens.model

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.DEFAULT_SYSTEM_PROMPT
import com.example.ai.ModelManager
import com.example.ai.NativeLlama
import com.example.ui.theme.ArcaneGold
import com.example.ui.theme.CaiAccentBlue
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.OledEmerald
import com.example.ui.theme.ParchmentCopper
import com.example.ui.viewmodel.AppThemeSetting
import com.example.ui.viewmodel.RoleplayViewModel

@Composable
fun ModelSettingsScreen(
    viewModel: RoleplayViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val modelConfig by viewModel.modelConfig.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()

    var temp by remember(modelConfig.temperature) { mutableFloatStateOf(modelConfig.temperature) }
    var topP by remember(modelConfig.topP) { mutableFloatStateOf(modelConfig.topP) }
    var repPenalty by remember(modelConfig.repeatPenalty) { mutableFloatStateOf(modelConfig.repeatPenalty) }
    var threads by remember(modelConfig.threadCount) { mutableIntStateOf(modelConfig.threadCount) }
    var contextSize by remember(modelConfig.contextWindow) { mutableIntStateOf(modelConfig.contextWindow) }
    var systemPrompt by remember(modelConfig.systemPrompt) { mutableStateOf(modelConfig.systemPrompt) }

    // GGUF file picker
    val ggufPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "modelo_personalizado.gguf"
            viewModel.updateModelConfig(
                modelName = fileName,
                modelPath = uri.toString()
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Motor de IA Local (llama.cpp)",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "100% privado y local en tu dispositivo. Compatible con modelos cuantizados GGUF (1B a 4B Q4).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Current Model Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Modelo Activo",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val isNative = NativeLlama.isLibraryLoaded && modelConfig.isModelLoaded
                        Text(
                            text = if (isNative) "llama.cpp JNI Activo" else "Motor Local Integrado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = modelConfig.modelName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { ggufPickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("load_gguf_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cargar Modelo GGUF (.gguf)", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        // Appearance & Themes
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tema & Apariencia",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ThemeOptionBox(
                            name = "C.AI Dark",
                            color = CaiAccentBlue,
                            isSelected = appTheme == AppThemeSetting.CHARACTER_AI_DARK,
                            onClick = { viewModel.setTheme(AppThemeSetting.CHARACTER_AI_DARK) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionBox(
                            name = "Arcano",
                            color = ArcaneGold,
                            isSelected = appTheme == AppThemeSetting.DARK_FANTASY,
                            onClick = { viewModel.setTheme(AppThemeSetting.DARK_FANTASY) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionBox(
                            name = "Pergamino",
                            color = ParchmentCopper,
                            isSelected = appTheme == AppThemeSetting.PARCHMENT,
                            onClick = { viewModel.setTheme(AppThemeSetting.PARCHMENT) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionBox(
                            name = "Cyberpunk",
                            color = CyberCyan,
                            isSelected = appTheme == AppThemeSetting.CYBERPUNK,
                            onClick = { viewModel.setTheme(AppThemeSetting.CYBERPUNK) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionBox(
                            name = "OLED",
                            color = OledEmerald,
                            isSelected = appTheme == AppThemeSetting.OLED_BLACK,
                            onClick = { viewModel.setTheme(AppThemeSetting.OLED_BLACK) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Model Parameters (Temperature, TopP, Threads, etc.)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Parámetros de Inferencia",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Temperature
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Temperatura (Creatividad):", style = MaterialTheme.typography.bodySmall)
                        Text(text = String.format("%.2f", temp), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = temp,
                        onValueChange = {
                            temp = it
                            viewModel.updateModelConfig(temperature = it)
                        },
                        valueRange = 0.1f..1.5f
                    )

                    // Top-P
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Top-P (Nucleus Sampling):", style = MaterialTheme.typography.bodySmall)
                        Text(text = String.format("%.2f", topP), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = topP,
                        onValueChange = {
                            topP = it
                            viewModel.updateModelConfig(topP = it)
                        },
                        valueRange = 0.1f..1.0f
                    )

                    // Repetition Penalty
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Penalización de Repetición:", style = MaterialTheme.typography.bodySmall)
                        Text(text = String.format("%.2f", repPenalty), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = repPenalty,
                        onValueChange = {
                            repPenalty = it
                            viewModel.updateModelConfig(repeatPenalty = it)
                        },
                        valueRange = 1.0f..1.4f
                    )

                    // Threads
                    Text(text = "Hilos de CPU para Inferencia:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 4, 6, 8).forEach { t ->
                            FilterChip(
                                selected = threads == t,
                                onClick = {
                                    threads = t
                                    viewModel.updateModelConfig(threads = t)
                                },
                                label = { Text("$t hilos") }
                            )
                        }
                    }
                }
            }
        }

        // Editable System Prompt (up to 50,000 chars)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Prompt del Sistema (${systemPrompt.length} / 50.000)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = {
                            systemPrompt = DEFAULT_SYSTEM_PROMPT
                            viewModel.updateModelConfig(systemPrompt = DEFAULT_SYSTEM_PROMPT)
                        }) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = "Restablecer")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Variables disponibles: {{user}}, {{world}}, {{char}}, {{lore}}, {{pinned}}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = {
                            if (it.length <= 50000) {
                                systemPrompt = it
                                viewModel.updateModelConfig(systemPrompt = it)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("system_prompt_input"),
                        minLines = 6,
                        maxLines = 14
                    )
                }
            }
        }

        // Recommended Models
        item {
            Text(
                text = "Modelos GGUF Recomendados (1B a 4B)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(ModelManager.recommendedModels) { model ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = model.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${model.parameterSize} • ${model.quantFormat}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Memoria RAM requerida: ${model.memoryRequired}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(model.huggingFaceUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver en HuggingFace y Descargar GGUF")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun ThemeOptionBox(
    name: String,
    color: androidx.compose.ui.graphics.Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                2.dp,
                if (isSelected) color else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
