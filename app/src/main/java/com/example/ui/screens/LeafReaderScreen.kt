package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalculatedLeaf
import com.example.model.LeafDisplayType
import com.example.model.LeafSide
import com.example.model.ManuscriptEntity
import com.example.model.MatterType
import com.example.ui.components.LeafBadge
import com.example.ui.theme.BlankLeafColor
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.BookGoldLight
import com.example.ui.theme.InkBlack
import com.example.ui.theme.InkNavy
import com.example.ui.theme.ParchmentCream
import com.example.ui.theme.ParchmentPaper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeafReaderScreen(
    manuscript: ManuscriptEntity,
    leaves: List<CalculatedLeaf>,
    onBack: () -> Unit,
    onOpenExport: () -> Unit
) {
    var currentSpreadIndex by remember { mutableIntStateOf(0) }
    val totalLeaves = leaves.size
    val totalSpreads = maxOf(1, (totalLeaves + 1) / 2)

    // For spread index i:
    // Left leaf (Verso) is at index (i * 2) - 1 if i > 0, else null
    // Right leaf (Recto) is at index (i * 2)
    val rectoIndex = currentSpreadIndex * 2
    val versoIndex = rectoIndex - 1

    val versoLeaf = if (versoIndex in leaves.indices) leaves[versoIndex] else null
    val rectoLeaf = if (rectoIndex in leaves.indices) leaves[rectoIndex] else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = manuscript.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "Spread ${currentSpreadIndex + 1} of $totalSpreads • Leaf ${rectoIndex + 1} of $totalLeaves",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_reader")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenExport, modifier = Modifier.testTag("btn_reader_export")) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = BookGoldDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (currentSpreadIndex > 0) currentSpreadIndex-- },
                        enabled = currentSpreadIndex > 0,
                        modifier = Modifier.testTag("btn_prev_spread")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Previous Spread")
                    }

                    Text(
                        text = "Chicago 6\"×9\" Leaf Spread",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { if (currentSpreadIndex < totalSpreads - 1) currentSpreadIndex++ },
                        enabled = currentSpreadIndex < totalSpreads - 1,
                        colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                        modifier = Modifier.testTag("btn_next_spread")
                    ) {
                        Text("Next Spread")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF23252E)) // Dark desk backdrop
                .padding(paddingValues)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(12.dp, RoundedCornerShape(4.dp))
                    .background(Color(0xFFFDFCF9), RoundedCornerShape(4.dp)),
                horizontalArrangement = Arrangement.Center
            ) {
                // Left Leaf: VERSO
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 12.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    if (versoLeaf != null) {
                        BookLeafView(leaf = versoLeaf, manuscript = manuscript, isVerso = true)
                    } else {
                        // Inside Cover (Before Leaf 1)
                        InsideCoverView(manuscript = manuscript)
                    }
                }

                // Central Spine Binding Shadow
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Black.copy(alpha = 0.28f),
                                    Color.Black.copy(alpha = 0.12f)
                                )
                            )
                        )
                )

                // Right Leaf: RECTO
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 6.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    if (rectoLeaf != null) {
                        BookLeafView(leaf = rectoLeaf, manuscript = manuscript, isVerso = false)
                    } else {
                        // Empty back endpaper
                        EndpaperView()
                    }
                }
            }
        }
    }
}

@Composable
fun BookLeafView(
    leaf: CalculatedLeaf,
    manuscript: ManuscriptEntity,
    isVerso: Boolean
) {
    val isBlank = leaf.displayType == LeafDisplayType.BLANK_INTENTIONAL
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = if (isVerso) 12.dp else 16.dp, // Gutter margin on inner side
                end = if (isVerso) 16.dp else 12.dp,
                top = 8.dp,
                bottom = 8.dp
            )
    ) {
        // Running Head and Folio (Top)
        if (!leaf.hasBlindFolio) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isVerso) {
                    Text(
                        text = leaf.pageNumberDisplay,
                        fontFamily = FontFamily.Serif,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = leaf.runningHeadVerso.ifBlank { manuscript.title },
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 9.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = leaf.runningHeadRecto.ifBlank { leaf.sectionTitle },
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 9.sp,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = leaf.pageNumberDisplay,
                        fontFamily = FontFamily.Serif,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
            }
            HorizontalDivider(
                color = Color.LightGray.copy(alpha = 0.6f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Leaf Body Render
        when (leaf.displayType) {
            LeafDisplayType.HALF_TITLE -> {
                Spacer(modifier = Modifier.height(60.dp))
                Text(
                    text = manuscript.title.uppercase(),
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LeafDisplayType.TITLE_PAGE -> {
                Spacer(modifier = Modifier.height(30.dp))
                Text(
                    text = manuscript.title,
                    fontFamily = FontFamily.Serif,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (manuscript.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = manuscript.subtitle,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp, 1.dp)
                        .background(BookGoldDark)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "by\n${manuscript.authorName.uppercase()}",
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(60.dp))
                Text(
                    text = "${manuscript.publisher}\n${manuscript.year}",
                    fontFamily = FontFamily.Serif,
                    fontSize = 9.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LeafDisplayType.COPYRIGHT -> {
                Spacer(modifier = Modifier.height(60.dp))
                Text(
                    text = leaf.contentSnippet,
                    fontFamily = FontFamily.Serif,
                    fontSize = 8.5.sp,
                    color = Color.DarkGray,
                    lineHeight = 14.sp
                )
            }
            LeafDisplayType.DEDICATION -> {
                Spacer(modifier = Modifier.height(60.dp))
                Text(
                    text = leaf.contentSnippet,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LeafDisplayType.EPIGRAPH -> {
                Spacer(modifier = Modifier.height(60.dp))
                Text(
                    text = leaf.contentSnippet,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 10.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            LeafDisplayType.BLANK_INTENTIONAL -> {
                Spacer(modifier = Modifier.height(100.dp))
                Text(
                    text = "[ This leaf intentionally left blank ]",
                    fontSize = 9.sp,
                    fontStyle = FontStyle.Italic,
                    color = BlankLeafColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LeafDisplayType.CHAPTER_OPENER, LeafDisplayType.PART_OPENER -> {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = leaf.sectionTitle,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp, 1.dp)
                        .background(BookGoldDark)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = leaf.contentSnippet,
                    fontFamily = FontFamily.Serif,
                    fontSize = 10.sp,
                    lineHeight = 17.sp,
                    color = InkBlack
                )
            }
            else -> {
                Text(
                    text = leaf.contentSnippet,
                    fontFamily = FontFamily.Serif,
                    fontSize = 10.sp,
                    lineHeight = 17.sp,
                    color = InkBlack
                )
            }
        }

        // Drop folio on chapter openers (Centered bottom)
        if (leaf.isOpener && leaf.matterType == MatterType.TEXT_BODY && leaf.pageNumberDisplay.isNotBlank()) {
            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = leaf.pageNumberDisplay,
                fontFamily = FontFamily.Serif,
                fontSize = 9.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun InsideCoverView(manuscript: ManuscriptEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Book,
            contentDescription = null,
            tint = BookGoldDark.copy(alpha = 0.5f),
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "BWRITER MANUSCRIPT",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Color.Gray
        )
        Text(
            text = "Inside Front Cover",
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            color = Color.DarkGray
        )
    }
}

@Composable
fun EndpaperView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "— End of Volume —",
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSize = 10.sp,
            color = Color.LightGray
        )
    }
}
