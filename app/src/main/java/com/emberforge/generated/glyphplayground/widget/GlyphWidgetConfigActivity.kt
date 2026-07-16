package com.emberforge.generated.glyphplayground.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emberforge.generated.glyphplayground.GlyphPattern
import com.emberforge.generated.glyphplayground.PatternRepository
import com.emberforge.generated.glyphplayground.ui.GlyphMatrixPreview

private val NothingBlack = Color(0xFF000000)
private val NothingCard = Color(0xFF161616)
private val NothingBorder = Color(0xFF2A2A2A)
private val NothingWhite = Color(0xFFFFFFFF)
private val NothingDim = Color(0xFF777777)
private val NothingAccent = Color(0xFFD0FD3E)

private val ConfigColorScheme = darkColorScheme(
    primary = NothingWhite,
    onPrimary = NothingBlack,
    secondary = NothingAccent,
    background = NothingBlack,
    surface = NothingCard,
    onBackground = NothingWhite,
    onSurface = NothingWhite,
    outline = NothingBorder
)

/**
 * Lets the user choose which saved glyphs a widget instance shows. Launched
 * automatically when the widget is placed, and again via the launcher's
 * "reconfigure" action. Selecting nothing means "show every saved glyph".
 */
class GlyphWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // If the user backs out, the widget host must not add the widget.
        setResult(RESULT_CANCELED)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val repo = PatternRepository(this)
        val patterns = repo.loadAll()
        val initial = WidgetPrefs.getSelected(this, appWidgetId) ?: patterns.map { it.id }.toSet()

        setContent {
            MaterialTheme(colorScheme = ConfigColorScheme) {
                ConfigScreen(
                    patterns = patterns,
                    initiallySelected = initial,
                    onCancel = { finish() },
                    onConfirm = { selectedIds ->
                        WidgetPrefs.setSelected(this, appWidgetId, selectedIds)
                        GlyphWidgetProvider.renderWidget(
                            this,
                            AppWidgetManager.getInstance(this),
                            appWidgetId
                        )
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun ConfigScreen(
    patterns: List<GlyphPattern>,
    initiallySelected: Set<String>,
    onCancel: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var selected by remember { mutableStateOf(initiallySelected) }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
            .padding(top = topPadding)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Text(
            text = "GLYPH WIDGET",
            color = NothingWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp
        )
        Text(
            text = "Choose the glyphs to show",
            color = NothingDim,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(16.dp))

        if (patterns.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "No saved glyphs yet.\nOpen Glyph Playground and save one first.",
                    color = NothingDim,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(patterns, key = { it.id }) { pattern ->
                    val checked = pattern.id in selected
                    ConfigRow(
                        pattern = pattern,
                        checked = checked,
                        onToggle = {
                            selected = if (checked) selected - pattern.id else selected + pattern.id
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingDim),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Cancel", fontSize = 14.sp)
            }
            Button(
                onClick = { onConfirm(selected) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NothingAccent,
                    contentColor = NothingBlack
                ),
                enabled = patterns.isNotEmpty(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Add widget", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ConfigRow(
    pattern: GlyphPattern,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = NothingCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NothingBlack)
            ) {
                GlyphMatrixPreview(
                    activeLeds = pattern.activeLeds,
                    modifier = Modifier.fillMaxSize().padding(3.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pattern.name,
                    color = NothingWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${pattern.activeLeds.size} LEDs",
                    color = NothingDim,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = NothingAccent,
                    uncheckedColor = NothingBorder,
                    checkmarkColor = NothingBlack
                )
            )
        }
    }
}
