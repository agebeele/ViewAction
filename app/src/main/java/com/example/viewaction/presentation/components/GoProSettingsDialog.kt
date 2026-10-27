package com.example.viewaction.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.example.viewaction.domain.GOPRO_PRESETS_LIST
import com.example.viewaction.domain.GoProPreset
import com.example.viewaction.domain.GoProState
import com.example.viewaction.domain.LensType
import com.example.viewaction.domain.VideoResolution
import com.example.viewaction.presentation.theme.GoProColors

@Composable
fun GoProSettingsDialog(
    state: GoProState,
    onPresetSelected: (GoProPreset) -> Unit,
    onResolutionSelected: (VideoResolution, String) -> Unit,
    onLensSelected: (LensType) -> Unit,
    onClose: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GoProColors.DarkBackground)
    ) {
        ScreenScaffold(
            scrollState = columnState
        ) { contentPadding ->
            TransformingLazyColumn(
                state = columnState,
                contentPadding = contentPadding
            ) {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Text(
                            text = "Ajustes y Presets",
                            color = GoProColors.Blue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Presets GoPro
                item {
                    Text(
                        text = "Modos de Captura GoPro",
                        color = GoProColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
                    )
                }

                GOPRO_PRESETS_LIST.forEach { preset ->
                    item {
                        val isSelected = state.activePreset.id == preset.id
                        Button(
                            onClick = {
                                onPresetSelected(preset)
                                onClose()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) GoProColors.Blue else GoProColors.Surface,
                                contentColor = if (isSelected) GoProColors.DarkBackground else GoProColors.TextWhite
                            )
                        ) {
                            Text(
                                text = "${preset.icon} ${preset.name}",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Resolution & FPS Section
                item {
                    Text(
                        text = "Resolución y FPS",
                        color = GoProColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
                    )
                }

                VideoResolution.entries.forEach { res ->
                    res.availableFps.forEach { fps ->
                        item {
                            val isSelected = state.resolution == res && state.fps == fps
                            Button(
                                onClick = {
                                    onResolutionSelected(res, fps)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec),
                                transformation = SurfaceTransformation(transformationSpec),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) GoProColors.Blue else GoProColors.Surface,
                                    contentColor = if (isSelected) GoProColors.DarkBackground else GoProColors.TextWhite
                                )
                            ) {
                                Text(
                                    text = "${res.displayName} • ${fps} FPS",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Lens Section
                item {
                    Text(
                        text = "Lente / Campo de Visión",
                        color = GoProColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
                    )
                }

                LensType.entries.forEach { lens ->
                    item {
                        val isSelected = state.lens == lens
                        Button(
                            onClick = {
                                onLensSelected(lens)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) GoProColors.Blue else GoProColors.Surface,
                                contentColor = if (isSelected) GoProColors.DarkBackground else GoProColors.TextWhite
                            )
                        ) {
                            Text(
                                text = lens.displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Close Button
                item {
                    Button(
                        onClick = onClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoProColors.Card,
                            contentColor = GoProColors.TextWhite
                        )
                    ) {
                        Text("Volver")
                    }
                }
            }
        }
    }
}
