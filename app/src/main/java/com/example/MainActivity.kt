package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.components.DiceRollerSheet
import com.example.ui.components.MiniAudioPlayer
import com.example.ui.components.RoleplayBottomNavBar
import com.example.ui.components.RoleplayScreen
import com.example.ui.components.RoleplayTopBar
import com.example.ui.screens.characters.CharactersScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.memory.MemoryScreen
import com.example.ui.screens.model.ModelSettingsScreen
import com.example.ui.screens.music.PlaylistsScreen
import com.example.ui.screens.stories.StoriesHubScreen
import com.example.ui.screens.world.WorldLoreScreen
import com.example.ui.theme.MythosRoleplayTheme
import com.example.ui.viewmodel.RoleplayViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: RoleplayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by viewModel.appTheme.collectAsState()

            MythosRoleplayTheme(appTheme = appTheme) {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: RoleplayViewModel) {
    var currentScreen by remember { mutableStateOf(RoleplayScreen.STORIES) }
    var showQuickDice by remember { mutableStateOf(false) }

    val activeWorld by viewModel.activeWorld.collectAsState()
    val userCharacter by viewModel.selectedUserCharacter.collectAsState()
    val audioPlaybackState by viewModel.audioManager.playbackState.collectAsState()

    // BackHandler: if not on Stories or Chat screen, navigating back takes user to Chat
    BackHandler(enabled = currentScreen != RoleplayScreen.CHAT && currentScreen != RoleplayScreen.STORIES) {
        currentScreen = RoleplayScreen.CHAT
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            RoleplayTopBar(
                activeWorld = activeWorld,
                userCharacter = userCharacter,
                onQuickDice = { showQuickDice = true },
                onOpenStories = { currentScreen = RoleplayScreen.STORIES }
            )
        },
        bottomBar = {
            Column {
                MiniAudioPlayer(
                    playbackState = audioPlaybackState,
                    onTogglePlay = {
                        if (audioPlaybackState.isPlaying) {
                            viewModel.audioManager.pause()
                        } else {
                            viewModel.audioManager.resume()
                        }
                    },
                    onSetVolume = { vol -> viewModel.audioManager.setVolume(vol) }
                )
                RoleplayBottomNavBar(
                    currentScreen = currentScreen,
                    onSelectScreen = { currentScreen = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    RoleplayScreen.STORIES -> StoriesHubScreen(
                        viewModel = viewModel,
                        onPlayStory = { currentScreen = RoleplayScreen.CHAT }
                    )
                    RoleplayScreen.CHAT -> ChatScreen(viewModel = viewModel)
                    RoleplayScreen.CHARACTERS -> CharactersScreen(viewModel = viewModel)
                    RoleplayScreen.WORLD -> WorldLoreScreen(viewModel = viewModel)
                    RoleplayScreen.MUSIC -> PlaylistsScreen(viewModel = viewModel)
                    RoleplayScreen.MODEL -> ModelSettingsScreen(viewModel = viewModel)
                    RoleplayScreen.MEMORY -> MemoryScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Quick Dice Sheet accessible from anywhere in the app
    if (showQuickDice) {
        DiceRollerSheet(
            userCharacter = userCharacter,
            onDismiss = { showQuickDice = false },
            onRollAndInscribe = { dt, name, mod, dc ->
                viewModel.rollDice(dt, name, mod, dc)
                currentScreen = RoleplayScreen.CHAT
            }
        )
    }
}
