package com.emberforge.generated.glyphplayground

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emberforge.generated.glyphplayground.ui.GlyphMatrixCanvas
import com.emberforge.generated.glyphplayground.ui.GlyphMatrixPreview

private val NothingBlack = Color(0xFF000000)
private val NothingDark = Color(0xFF0D0D0D)
private val NothingCard = Color(0xFF161616)
private val NothingBorder = Color(0xFF2A2A2A)
private val NothingWhite = Color(0xFFFFFFFF)
private val NothingDim = Color(0xFF777777)
private val NothingAccent = Color(0xFFD0FD3E)

private val AppColorScheme = darkColorScheme(
    primary = NothingWhite,
    onPrimary = NothingBlack,
    secondary = NothingAccent,
    background = NothingBlack,
    surface = NothingCard,
    onBackground = NothingWhite,
    onSurface = NothingWhite,
    outline = NothingBorder
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = PatternRepository(this)
        setContent {
            MaterialTheme(colorScheme = AppColorScheme) {
                GlyphPlaygroundApp(repo)
            }
        }
    }
}

private enum class Screen { EDITOR, LIBRARY, PREVIEW }

@Composable
private fun GlyphPlaygroundApp(repo: PatternRepository) {
    var screen by remember { mutableStateOf(Screen.EDITOR) }
    var activeLeds by remember { mutableStateOf(setOf<Int>()) }
    var patterns by remember { mutableStateOf(repo.loadAll()) }

    val refreshPatterns = { patterns = repo.loadAll() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
    ) {
        when (screen) {
            Screen.EDITOR -> EditorScreen(
                activeLeds = activeLeds,
                onLedsChanged = { activeLeds = it },
                onOpenLibrary = { screen = Screen.LIBRARY },
                onPreview = { screen = Screen.PREVIEW },
                repo = repo,
                onPatternSaved = refreshPatterns
            )
            Screen.LIBRARY -> LibraryScreen(
                patterns = patterns,
                onBack = { screen = Screen.EDITOR },
                onSelect = { pattern ->
                    activeLeds = pattern.activeLeds
                    screen = Screen.EDITOR
                },
                onDelete = { pattern ->
                    repo.delete(pattern.id)
                    refreshPatterns()
                }
            )
            Screen.PREVIEW -> PreviewScreen(
                activeLeds = activeLeds,
                onClose = { screen = Screen.EDITOR }
            )
        }
    }
}

@Composable
private fun EditorScreen(
    activeLeds: Set<Int>,
    onLedsChanged: (Set<Int>) -> Unit,
    onOpenLibrary: () -> Unit,
    onPreview: () -> Unit,
    repo: PatternRepository,
    onPatternSaved: () -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarPadding.calculateTopPadding())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GLYPH",
                    color = NothingWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "PLAYGROUND",
                    color = NothingDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 6.sp
                )
            }
            Row {
                IconButton(onClick = onOpenLibrary) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NothingCard),
                        contentAlignment = Alignment.Center
                    ) {
                        // Grid icon using 4 dots
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(5.dp).clip(RoundedCornerShape(1.dp)).background(NothingWhite))
                                Box(Modifier.size(5.dp).clip(RoundedCornerShape(1.dp)).background(NothingWhite))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(5.dp).clip(RoundedCornerShape(1.dp)).background(NothingWhite))
                                Box(Modifier.size(5.dp).clip(RoundedCornerShape(1.dp)).background(NothingWhite))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // LED count indicator
        Text(
            text = "${activeLeds.size} / ${GlyphLayout.totalLeds} LEDs",
            color = NothingDim,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(Modifier.height(8.dp))

        // Glyph Matrix Canvas
        GlyphMatrixCanvas(
            activeLeds = activeLeds,
            onToggle = { idx ->
                onLedsChanged(
                    if (idx in activeLeds) activeLeds - idx else activeLeds + idx
                )
            },
            onDraw = { idx, turnOn ->
                onLedsChanged(
                    if (turnOn) activeLeds + idx else activeLeds - idx
                )
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onLedsChanged(emptySet()) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingWhite),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Clear", fontSize = 14.sp)
            }

            Button(
                onClick = { showSaveDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NothingWhite,
                    contentColor = NothingBlack
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    if (activeLeds.isNotEmpty()) {
                        onPreview()
                    } else {
                        Toast.makeText(context, "Draw something first!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NothingAccent,
                    contentColor = NothingBlack
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Glyph", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showSaveDialog) {
        SaveDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                repo.save(GlyphPattern(name = name, activeLeds = activeLeds))
                onPatternSaved()
                showSaveDialog = false
                Toast.makeText(context, "Pattern saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun SaveDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NothingCard,
        title = {
            Text("Save Pattern", color = NothingWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Pattern name", color = NothingDim) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NothingWhite,
                    unfocusedTextColor = NothingWhite,
                    focusedBorderColor = NothingAccent,
                    unfocusedBorderColor = NothingBorder,
                    cursorColor = NothingAccent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Save", color = if (name.isNotBlank()) NothingAccent else NothingDim)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NothingDim)
            }
        }
    )
}

@Composable
private fun LibraryScreen(
    patterns: List<GlyphPattern>,
    onBack: () -> Unit,
    onSelect: (GlyphPattern) -> Unit,
    onDelete: (GlyphPattern) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarPadding.calculateTopPadding())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NothingWhite)
            }
            Text(
                text = "SAVED PATTERNS",
                color = NothingWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "${patterns.size} patterns",
            color = NothingDim,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 12.dp)
        )

        Spacer(Modifier.height(16.dp))

        if (patterns.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No saved patterns yet", color = NothingDim, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Draw on the Glyph Matrix\nand tap Save",
                        color = NothingBorder,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(patterns, key = { it.id }) { pattern ->
                    PatternCard(
                        pattern = pattern,
                        onClick = { onSelect(pattern) },
                        onDelete = { onDelete(pattern) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PatternCard(
    pattern: GlyphPattern,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NothingCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.5f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NothingBlack)
            ) {
                GlyphMatrixPreview(
                    activeLeds = pattern.activeLeds,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pattern.name,
                        color = NothingWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${pattern.activeLeds.size} LEDs",
                        color = NothingDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = NothingDim,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = NothingCard,
            title = { Text("Delete pattern?", color = NothingWhite) },
            text = { Text("\"${pattern.name}\" will be permanently deleted.", color = NothingDim) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Delete", color = Color(0xFFFF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = NothingDim)
                }
            }
        )
    }
}

@Composable
private fun PreviewScreen(activeLeds: Set<Int>, onClose: () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NothingBlack)
        ) {
            GlyphMatrixPreview(
                activeLeds = activeLeds,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            )

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close preview",
                        tint = NothingWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Label
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GLYPH PREVIEW",
                    color = NothingDim,
                    fontSize = 12.sp,
                    letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${activeLeds.size} LEDs active",
                    color = NothingBorder,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
