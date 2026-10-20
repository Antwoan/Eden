package com.example.audio

data class AmbientOccasion(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String
)

object AmbientOccasions {
    val ALL = listOf(
        AmbientOccasion(
            id = "COMBAT",
            name = "Combate Épico",
            emoji = "⚔️",
            description = "Percusión rítmica, pulsos graves y tensión marcial para batallas."
        ),
        AmbientOccasion(
            id = "STEALTH",
            name = "Sigilo & Infiltración",
            emoji = "🥷",
            description = "Texturas contenidas, susurros sónicos y silencios calculados."
        ),
        AmbientOccasion(
            id = "CHASE",
            name = "Persecución & Fuga",
            emoji = "⚡",
            description = "Adrenalina acelerada, secuencias urgentes y latidos rápidos."
        ),
        AmbientOccasion(
            id = "MYSTERY",
            name = "Investigación & Misterio",
            emoji = "🕵️",
            description = "Atmósfera noir, notas disonantes e intriga detectivesca."
        ),
        AmbientOccasion(
            id = "COSMIC_HORROR",
            name = "Terror Cósmico",
            emoji = "🐙",
            description = "Subgraves desconcertantes, ecos oscuros y locura primordial."
        ),
        AmbientOccasion(
            id = "ROMANCE",
            name = "Romance & Intimidad",
            emoji = "💖",
            description = "Armonías cálidas, notas suaves y cercanía emocional."
        ),
        AmbientOccasion(
            id = "MELANCHOLY",
            name = "Melancolía & Despedida",
            emoji = "🌧️",
            description = "Tonos nostálgicos, lluvia sutil y reflexiones profundas."
        ),
        AmbientOccasion(
            id = "CAMP_REST",
            name = "Descanso en Campamento",
            emoji = "🏕️",
            description = "Respiración serena, calma nocturna y recuperación."
        ),
        AmbientOccasion(
            id = "SPACE_ODYSSEY",
            name = "Odisea Espacial",
            emoji = "🚀",
            description = "Vacío estelar, zumbido cuántico y paisajes cósmicos."
        ),
        AmbientOccasion(
            id = "CYBERPUNK_CITY",
            name = "Ciudad Cyberpunk",
            emoji = "🏙️",
            description = "Pulsos de neón, lluvia sintética y resonancia de asfalto."
        )
    )

    val NAMES = ALL.map { it.name }

    fun findByName(name: String): AmbientOccasion? {
        return ALL.firstOrNull { it.name.equals(name, ignoreCase = true) || it.id.equals(name, ignoreCase = true) }
    }
}
