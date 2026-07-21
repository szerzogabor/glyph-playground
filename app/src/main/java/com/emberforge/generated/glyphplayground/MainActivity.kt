package com.emberforge.generated.glyphplayground

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
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
import androidx.core.content.FileProvider
import com.emberforge.generated.glyphplayground.ui.GlyphMatrixCanvas
import com.emberforge.generated.glyphplayground.ui.GlyphMatrixPreview
import com.emberforge.generated.glyphplayground.ui.PictureToGlyphScreen
import com.emberforge.generated.glyphplayground.widget.GlyphWidgetDisplayService
import com.emberforge.generated.glyphplayground.widget.GlyphWidgetProvider
import java.io.File

private val NothingBlack = Color(0xFF000000)
private val NothingCard = Color(0xFF161616)
private val NothingBorder = Color(0xFF2A2A2A)
private val NothingWhite = Color(0xFFFFFFFF)
private val NothingDim = Color(0xFF777777)
private val NothingAccent = Color(0xFFD0FD3E)
private val NothingGreen = Color(0xFF00D563)
private val NothingRed = Color(0xFFFF4444)

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

private fun shareGlyph(context: android.content.Context, pattern: GlyphPattern) {
    val cacheDir = File(context.cacheDir, "shared_glyphs").apply { mkdirs() }
    val fileName = pattern.name.replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".glyph"
    val file = File(cacheDir, fileName)
    file.writeText(PatternRepository.toExportJson(pattern))
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Glyph: ${pattern.name}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Glyph"))
}

private const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/ogbar"

private fun openBuyMeACoffee(context: android.content.Context) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(BUY_ME_A_COFFEE_URL))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
        Toast.makeText(context, "Couldn't open browser", Toast.LENGTH_SHORT).show()
    }
}

private enum class Screen { EDITOR, LIBRARY, PICTURE_TO_GLYPH }

@Composable
private fun GlyphPlaygroundApp(repo: PatternRepository) {
    var screen by remember { mutableStateOf(Screen.EDITOR) }
    var activeLeds by remember { mutableStateOf(setOf<Int>()) }
    var ledBrightness by remember { mutableStateOf(mapOf<Int, Int>()) }
    var patterns by remember { mutableStateOf(repo.loadAll()) }
    var editingPattern by remember { mutableStateOf<GlyphPattern?>(null) }

    val context = LocalContext.current
    val refreshPatterns = {
        patterns = repo.loadAll()
        GlyphWidgetProvider.refreshAll(context)
    }

    var patternToExport by remember { mutableStateOf<GlyphPattern?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            patternToExport?.let { pattern ->
                try {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(PatternRepository.toExportJson(pattern).toByteArray())
                    }
                    Toast.makeText(context, "Pattern exported!", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
        patternToExport = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                val pattern = text?.let { PatternRepository.fromImportJson(it) }
                if (pattern != null) {
                    repo.save(pattern)
                    refreshPatterns()
                    Toast.makeText(context, "\"${pattern.name}\" imported!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Invalid glyph file", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
    ) {
        when (screen) {
            Screen.EDITOR -> EditorScreen(
                activeLeds = activeLeds,
                ledBrightness = ledBrightness,
                onLedsChanged = { leds ->
                    activeLeds = leds
                    ledBrightness = emptyMap()
                },
                onOpenLibrary = { screen = Screen.LIBRARY },
                onOpenPictureToGlyph = { screen = Screen.PICTURE_TO_GLYPH },
                repo = repo,
                editingPattern = editingPattern,
                onPatternSaved = {
                    editingPattern = null
                    refreshPatterns()
                },
                onCancelEdit = { editingPattern = null },
                onShare = { leds ->
                    shareGlyph(context, GlyphPattern(name = "Glyph", activeLeds = leds, ledBrightness = ledBrightness))
                }
            )
            Screen.LIBRARY -> LibraryScreen(
                patterns = patterns,
                onBack = { screen = Screen.EDITOR },
                onSelect = { pattern ->
                    activeLeds = pattern.activeLeds
                    ledBrightness = pattern.ledBrightness
                    editingPattern = null
                    screen = Screen.EDITOR
                },
                onModify = { pattern ->
                    activeLeds = pattern.activeLeds
                    ledBrightness = pattern.ledBrightness
                    editingPattern = pattern
                    screen = Screen.EDITOR
                },
                onDelete = { pattern ->
                    repo.delete(pattern.id)
                    refreshPatterns()
                },
                onImport = {
                    importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                },
                onShare = { pattern -> shareGlyph(context, pattern) },
                onExport = { pattern ->
                    patternToExport = pattern
                    val fileName = pattern.name.replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".glyph"
                    exportLauncher.launch(fileName)
                }
            )
            Screen.PICTURE_TO_GLYPH -> PictureToGlyphScreen(
                onBack = { screen = Screen.EDITOR },
                onApply = { leds, brightness ->
                    activeLeds = leds
                    ledBrightness = brightness
                    editingPattern = null
                    screen = Screen.EDITOR
                }
            )
        }
    }
}

@Composable
private fun EditorScreen(
    activeLeds: Set<Int>,
    ledBrightness: Map<Int, Int>,
    onLedsChanged: (Set<Int>) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPictureToGlyph: () -> Unit,
    repo: PatternRepository,
    editingPattern: GlyphPattern?,
    onPatternSaved: () -> Unit,
    onCancelEdit: () -> Unit,
    onShare: (Set<Int>) -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var glyphOn by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarPadding.calculateTopPadding())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

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
                    text = if (editingPattern != null) "EDITING · ${editingPattern.name}" else "PLAYGROUND",
                    color = if (editingPattern != null) NothingAccent else NothingDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = if (editingPattern != null) 2.sp else 6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { openBuyMeACoffee(context) }) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NothingCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Coffee,
                            contentDescription = "Buy me a coffee",
                            tint = NothingAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = onOpenPictureToGlyph) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NothingCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Picture to Glyph",
                            tint = NothingWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = {
                    if (activeLeds.isNotEmpty()) {
                        onShare(activeLeds)
                    } else {
                        Toast.makeText(context, "Draw something first!", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NothingCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = NothingWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = onOpenLibrary) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NothingCard),
                        contentAlignment = Alignment.Center
                    ) {
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

        Spacer(Modifier.height(4.dp))

        Text(
            text = "${activeLeds.size} / ${GlyphLayout.VALID_LED_COUNT} LEDs",
            color = NothingDim,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(Modifier.height(8.dp))

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
                .fillMaxWidth()
                .aspectRatio(1f),
            ledBrightness = ledBrightness
        )

        Spacer(Modifier.height(12.dp))

        PresetRow(onSelect = onLedsChanged)

        Spacer(Modifier.weight(1f))

        if (editingPattern != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onLedsChanged(emptySet())
                        onCancelEdit()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingDim),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text("Cancel", fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        repo.save(editingPattern.copy(activeLeds = activeLeds, ledBrightness = ledBrightness))
                        onPatternSaved()
                        Toast.makeText(context, "Pattern updated!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NothingAccent,
                        contentColor = NothingBlack
                    ),
                    enabled = activeLeds.isNotEmpty(),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Update", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

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
                    if (glyphOn) {
                        GlyphWidgetDisplayService.hide(context)
                        glyphOn = false
                    } else {
                        if (activeLeds.isNotEmpty()) {
                            GlyphWidgetDisplayService.show(context, activeLeds, ledBrightness)
                            glyphOn = true
                        } else {
                            Toast.makeText(context, "Draw something first!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (glyphOn) NothingRed else NothingGreen,
                    contentColor = NothingBlack
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = if (glyphOn) "Glyph Off" else "Glyph",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showSaveDialog) {
        SaveDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                repo.save(GlyphPattern(name = name, activeLeds = activeLeds, ledBrightness = ledBrightness))
                onPatternSaved()
                showSaveDialog = false
                Toast.makeText(context, "Pattern saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun PresetRow(onSelect: (Set<Int>) -> Unit) {
    Column {
        Text(
            text = "PRESETS",
            color = NothingDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 3.sp
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PredefinedGlyphs.ALL.forEach { glyph ->
                PresetChip(glyph = glyph, onClick = { onSelect(glyph.activeLeds) })
            }
        }
    }
}

@Composable
private fun PresetChip(glyph: PredefinedGlyph, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(NothingCard)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(NothingBlack),
            contentAlignment = Alignment.Center
        ) {
            GlyphMatrixPreview(
                activeLeds = glyph.activeLeds,
                modifier = Modifier.fillMaxSize().padding(2.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = glyph.name,
            color = NothingWhite,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
    onModify: (GlyphPattern) -> Unit,
    onDelete: (GlyphPattern) -> Unit,
    onImport: () -> Unit,
    onShare: (GlyphPattern) -> Unit,
    onExport: (GlyphPattern) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarPadding.calculateTopPadding())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            Button(
                onClick = onImport,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NothingAccent,
                    contentColor = NothingBlack
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Import", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
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
                        onModify = { onModify(pattern) },
                        onDelete = { onDelete(pattern) },
                        onShare = { onShare(pattern) },
                        onExport = { onExport(pattern) }
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
    onModify: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }

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
                    .aspectRatio(1f)
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
                    onClick = onModify,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Modify",
                        tint = NothingAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { showActionsMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = NothingWhite,
                        modifier = Modifier.size(16.dp)
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

    if (showActionsMenu) {
        AlertDialog(
            onDismissRequest = { showActionsMenu = false },
            containerColor = NothingCard,
            title = {
                Text(pattern.name, color = NothingWhite, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showActionsMenu = false; onShare() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NothingAccent,
                            contentColor = NothingBlack
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { showActionsMenu = false; onExport() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingWhite),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("Export to file", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showActionsMenu = false }) {
                    Text("Cancel", color = NothingDim)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = NothingCard,
            title = { Text("Delete pattern?", color = NothingWhite) },
            text = { Text("\"${pattern.name}\" will be permanently deleted.", color = NothingDim) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Delete", color = NothingRed)
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
