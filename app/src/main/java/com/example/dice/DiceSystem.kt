package com.example.dice

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.random.Random

enum class DiceType(val sides: Int, val label: String) {
    D4(4, "d4"),
    D6(6, "d6"),
    D8(8, "d8"),
    D10(10, "d10"),
    D12(12, "d12"),
    D20(20, "d20"),
    D100(100, "d100")
}

data class RollResult(
    val diceType: DiceType,
    val naturalRoll: Int,
    val modifierName: String,
    val modifierValue: Int,
    val total: Int,
    val difficultyClass: Int?,
    val isCriticalSuccess: Boolean,
    val isCriticalFailure: Boolean,
    val isSuccess: Boolean?,
    val description: String
)

object DiceSystem {

    fun calculateModifier(statValue: Int): Int {
        // Standard D&D / RPG formula: (stat - 10) / 2
        return (statValue - 10) / 2
    }

    fun roll(
        diceType: DiceType,
        modifierName: String = "Ninguno",
        modifierValue: Int = 0,
        difficultyClass: Int? = null,
        context: Context? = null
    ): RollResult {
        triggerHaptic(context)
        val nat = Random.nextInt(1, diceType.sides + 1)
        val total = nat + modifierValue
        val isCritSuccess = diceType == DiceType.D20 && nat == 20
        val isCritFailure = diceType == DiceType.D20 && nat == 1

        val isSuccess = difficultyClass?.let { dc ->
            when {
                isCritSuccess -> true
                isCritFailure -> false
                else -> total >= dc
            }
        }

        val desc = buildString {
            append("Tirada ${diceType.label}: [ $nat ]")
            if (modifierValue != 0) {
                val sign = if (modifierValue >= 0) "+$modifierValue" else "$modifierValue"
                append(" $sign ($modifierName) = Total $total")
            } else {
                append(" = Total $total")
            }
            if (difficultyClass != null) {
                append(" vs DC $difficultyClass")
                when {
                    isCritSuccess -> append(" ⚔️ ¡ÉXITO CRÍTICO!")
                    isCritFailure -> append(" 💀 ¡PIFIA CRÍTICA!")
                    isSuccess == true -> append(" ✅ ¡ÉXITO!")
                    else -> append(" ❌ FALLO")
                }
            } else if (isCritSuccess) {
                append(" ⚔️ ¡20 NATURAL!")
            } else if (isCritFailure) {
                append(" 💀 ¡1 NATURAL!")
            }
        }

        return RollResult(
            diceType = diceType,
            naturalRoll = nat,
            modifierName = modifierName,
            modifierValue = modifierValue,
            total = total,
            difficultyClass = difficultyClass,
            isCriticalSuccess = isCritSuccess,
            isCriticalFailure = isCritFailure,
            isSuccess = isSuccess,
            description = desc
        )
    }

    private fun triggerHaptic(context: Context?) {
        if (context == null) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(45)
            }
        } catch (_: Throwable) {}
    }
}
