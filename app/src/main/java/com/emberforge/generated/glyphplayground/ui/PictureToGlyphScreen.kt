package com.emberforge.generated.glyphplayground.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emberforge.generated.glyphplayground.GlyphLayout
import com.emberforge.generated.glyphplayground.ImageToGlyph

private val NothingBlack = Color(0xFF000000)
private val NothingCard = Color(0xFF161616)
private val NothingBorder = Color(0xFF2A2A2A)
private val NothingWhite = Color(0xFFFFFFFF)
private val NothingDim = Color(0xFF777777)
private val NothingAccent = Color(0xFFD0FD3E)

@Composable
fun PictureToGlyphScreen(
    onBack: () -> Unit,
    onApply: (Set<Int>) -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var threshold by remember { mutableFloatStateOf(0.5f) }
    var invert by remember { mutableStateOf(true) }
    var previewLeds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                bitmap = bmp
                previewLeds = ImageToGlyph.convert(bmp, threshold, invert)
            } catch (_: Exception) {
                bitmap = null
                previewLeds = emptySet()
            }
        }
    }

    fun recalculate() {
        bitmap?.let { bmp ->
            previewLeds = ImageToGlyph.convert(bmp, threshold, invert)
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
            .padding(top = statusBarPadding.calculateTopPadding())
            .padding(horizontal = 16.dp)
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
                text = "PICTURE TO GLYPH",
                color = NothingWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        if (bitmap == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NothingCard),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No image selected", color = NothingDim, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { pickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NothingAccent,
                            contentColor = NothingBlack
                        )
                    ) {
                        Text("Choose Picture", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NothingCard),
                    contentAlignment = Alignment.Center
                ) {
                    bitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Source image",
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NothingBlack),
                    contentAlignment = Alignment.Center
                ) {
                    GlyphMatrixPreview(
                        activeLeds = previewLeds,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "${previewLeds.size} / ${GlyphLayout.VALID_LED_COUNT} LEDs",
                color = NothingDim,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "THRESHOLD",
                color = NothingDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = threshold,
                    onValueChange = {
                        threshold = it
                        recalculate()
                    },
                    valueRange = 0.05f..0.95f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = NothingAccent,
                        activeTrackColor = NothingAccent,
                        inactiveTrackColor = NothingBorder
                    )
                )
                Text(
                    text = "${(threshold * 100).toInt()}%",
                    color = NothingWhite,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(44.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "INVERT",
                    color = NothingDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 3.sp
                )
                Switch(
                    checked = invert,
                    onCheckedChange = {
                        invert = it
                        recalculate()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NothingBlack,
                        checkedTrackColor = NothingAccent,
                        uncheckedThumbColor = NothingDim,
                        uncheckedTrackColor = NothingCard
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { pickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingWhite),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Change Picture", fontSize = 14.sp)
            }
        }

        Spacer(Modifier.weight(1f))

        if (bitmap != null) {
            Button(
                onClick = { onApply(previewLeds) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NothingAccent,
                    contentColor = NothingBlack
                ),
                enabled = previewLeds.isNotEmpty(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Apply to Editor", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
