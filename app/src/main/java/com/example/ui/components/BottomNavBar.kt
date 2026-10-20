package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class RoleplayScreen(val title: String, val icon: ImageVector, val tag: String) {
    STORIES("Historias", Icons.Default.Book, "nav_stories"),
    CHAT("Aventura", Icons.Default.AutoStories, "nav_chat"),
    CHARACTERS("Personajes", Icons.Default.People, "nav_characters"),
    WORLD("Mundo", Icons.Default.Map, "nav_world"),
    MUSIC("Ambiente", Icons.Default.GraphicEq, "nav_music"),
    MODEL("Motor IA", Icons.Default.Psychology, "nav_model"),
    MEMORY("Memoria", Icons.Default.Bookmark, "nav_memory")
}

@Composable
fun RoleplayBottomNavBar(
    currentScreen: RoleplayScreen,
    onSelectScreen: (RoleplayScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("roleplay_bottom_nav"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        RoleplayScreen.values().forEach { screen ->
            val isSelected = screen == currentScreen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectScreen(screen) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        modifier = Modifier.size(19.dp)
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                },
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(screen.tag)
            )
        }
    }
}
