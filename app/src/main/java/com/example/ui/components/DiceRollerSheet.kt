package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.CharacterEntity
import com.example.dice.DiceSystem
import com.example.dice.DiceType
import com.example.dice.RollResult
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceRollerSheet(
    userCharacter: CharacterEntity?,
    onDismiss: () -> Unit,
    onRollAndInscribe: (DiceType, String, Int, Int?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedDice by remember { mutableStateOf(DiceType.D20) }
    var selectedStatModifierName by remember { mutableStateOf("Ninguno") }
    var selectedModifierValue by remember { mutableIntStateOf(0) }
    var selectedDc by remember { mutableStateOf<Int?>(15) }
    var currentRollResult by remember { mutableStateOf<RollResult?>(null) }

    // Parse stats from player character sheet
    val statModifiers = remember(userCharacter?.statsJson) {
        val list = mutableListOf(Pair("Ninguno", 0))
        if (userCharacter != null && userCharacter.statsJson.isNotBlank()) {
            try {
                val obj = JSONObject(userCharacter.statsJson)
                val stats = listOf("STR" to "Fuerza", "DEX" to "Destreza", "CON" to "Constitución", "INT" to "Inteligencia", "WIS" to "Sabiduría", "CHA" to "Carisma")
                for ((key, label) in stats) {
                    val rawVal = obj.optInt(key, 10)
                    val mod = DiceSystem.calculateModifier(rawVal)
                    list.add(Pair("$label ($key $rawVal)", mod))
                }
            } catch (_: Exception) {}
        }
        list
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("dice_roller_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Dados",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sistema de Reglas & Dados",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dice type selector
            Text(
                text = "Seleccionar Dado Poliédrico",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(DiceType.values()) { dt ->
                    val isSelected = dt == selectedDice
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedDice = dt }
                            .testTag("dice_${dt.label}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dt.label.uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat Modifier selector
            Text(
                text = "Modificador de Atributo (${userCharacter?.name ?: "Personaje"})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(statModifiers) { (label, mod) ->
                    val isSelected = selectedStatModifierName == label
                    val sign = if (mod >= 0) "+$mod" else "$mod"
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedStatModifierName = label
                            selectedModifierValue = mod
                        },
                        label = { Text(if (label == "Ninguno") "Sin mod." else "$label [$sign]") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Difficulty Class (DC)
            Text(
                text = "Dificultad de la Tirada (DC / Dificultad)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    null to "Libre",
                    10 to "Fácil (10)",
                    15 to "Media (15)",
                    20 to "Difícil (20)",
                    25 to "Épica (25)"
                ).forEach { (dc, label) ->
                    FilterChip(
                        selected = selectedDc == dc,
                        onClick = { selectedDc = dc },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Roll Result Area
            AnimatedContent(
                targetState = currentRollResult,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "roll_result"
            ) { res ->
                if (res != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                res.isCriticalSuccess -> Color(0xFF143825)
                                res.isCriticalFailure -> Color(0xFF3F1313)
                                res.isSuccess == true -> Color(0xFF1B2E3E)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            when {
                                res.isCriticalSuccess -> Color(0xFF10B981)
                                res.isCriticalFailure -> Color(0xFFEF4444)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = res.description,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    onRollAndInscribe(
                                        res.diceType,
                                        res.modifierName,
                                        res.modifierValue,
                                        res.difficultyClass
                                    )
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth().testTag("inscribe_roll_button")
                            ) {
                                Text("Inscribir en la Historia & Notificar al Narrador", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val res = DiceSystem.roll(
                            diceType = selectedDice,
                            modifierName = selectedStatModifierName,
                            modifierValue = selectedModifierValue,
                            difficultyClass = selectedDc
                        )
                        currentRollResult = res
                    },
                    modifier = Modifier.weight(1f).testTag("roll_preview_button")
                ) {
                    Text("Tirar Dado")
                }

                Button(
                    onClick = {
                        onRollAndInscribe(
                            selectedDice,
                            selectedStatModifierName,
                            selectedModifierValue,
                            selectedDc
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1.5f).testTag("roll_direct_button")
                ) {
                    Text("Tirar e Inscribir", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
