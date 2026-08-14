package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CalculatedLeaf
import com.example.model.LeafDisplayType
import com.example.model.LeafSide
import com.example.model.ManuscriptEntity
import com.example.model.MatterType
import com.example.ui.theme.BlankLeafColor
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.InkBlack
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
                            text = "Spread ${currentSpreadIndex + 1} of $totalSpreads • Leaves ${maxOf(1, versoIndex + 1)}–${rectoIndex + 1} of $totalLeaves",
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
                    IconButton(
                        onClick = { if (currentSpreadIndex > 0) currentSpreadIndex-- },
                        enabled = currentSpreadIndex > 0,
                        modifier = Modifier.testTag("btn_top_prev_spread")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Spread")
                    }
                    IconButton(
                        onClick = { if (currentSpreadIndex < totalSpreads - 1) currentSpreadIndex++ },
                        enabled = currentSpreadIndex < totalSpreads - 1,
                        modifier = Modifier.testTag("btn_top_next_spread")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Spread")
                    }
                    IconButton(onClick = onOpenExport, modifier = Modifier.testTag("btn_reader_export")) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = BookGoldDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF1E1E24))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sleek Spread Navigation Bar
            Surface(
                color = Color(0xFF2A2A32),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (currentSpreadIndex > 0) currentSpreadIndex-- },
                        enabled = currentSpreadIndex > 0,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("btn_prev_spread")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Previous Spread", fontSize = 12.sp)
                    }

                    Text(
                        text = "Spread ${currentSpreadIndex + 1} / $totalSpreads",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Button(
                        onClick = { if (currentSpreadIndex < totalSpreads - 1) currentSpreadIndex++ },
                        enabled = currentSpreadIndex < totalSpreads - 1,
                        colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("btn_next_spread")
                    ) {
                        Text("Next Spread", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Book spread container
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(6.dp)),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = ParchmentPaper),
                border = BorderStroke(1.dp, Color(0xFF4A4036))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // LEFT LEAF (Verso - Even page)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
                    ) {
                        if (versoLeaf != null) {
                            SingleLeafView(
                                leaf = versoLeaf,
                                manuscript = manuscript,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            InsideCoverView(manuscript)
                        }
                    }

                    // Book Spine / Gutter shadow
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(14.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0x22000000),
                                        Color(0x66000000),
                                        Color(0x11000000)
                                    )
                                )
                            )
                    )

                    // RIGHT LEAF (Recto - Odd page)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
                    ) {
                        if (rectoLeaf != null) {
                            SingleLeafView(
                                leaf = rectoLeaf,
                                manuscript = manuscript,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            EndpaperView()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SingleLeafView(
    leaf: CalculatedLeaf,
    manuscript: ManuscriptEntity,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(ParchmentCream, RoundedCornerShape(3.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Running Header & Folio
        if (!leaf.hasBlindFolio) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leaf.side == LeafSide.VERSO) {
                    Text(
                        text = leaf.pageNumberDisplay,
                        fontFamily = FontFamily.Serif,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                    Text(
                        text = leaf.runningHeadVerso.ifBlank { manuscript.title },
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 8.5.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = leaf.runningHeadRecto.ifBlank { leaf.sectionTitle },
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 8.5.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                    Text(
                        text = leaf.pageNumberDisplay,
                        fontFamily = FontFamily.Serif,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                thickness = 0.5.dp,
                color = Color(0xFFD4CBBF)
            )
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Leaf Body Content by Type
        when (leaf.displayType) {
            LeafDisplayType.HALF_TITLE -> {
                Spacer(modifier = Modifier.height(60.dp))
                Text(
                    text = leaf.contentSnippet.uppercase(),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LeafDisplayType.TITLE_PAGE -> {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = manuscript.title,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
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
                Spacer(modifier = Modifier.height(30.dp))
                Text(
                    text = "by\n${manuscript.effectiveAuthorByline.uppercase()}",
                    fontFamily = FontFamily.Serif,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(40.dp))
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
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = leaf.contentSnippet,
                    fontFamily = FontFamily.Serif,
                    fontSize = 8.5.sp,
                    color = Color.DarkGray,
                    lineHeight = 14.sp
                )
            }
            LeafDisplayType.DEDICATION -> {
                Spacer(modifier = Modifier.height(40.dp))
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
                Spacer(modifier = Modifier.height(40.dp))
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
                Spacer(modifier = Modifier.height(80.dp))
                Text(
                    text = "[ This leaf intentionally left blank ]",
                    fontSize = 8.5.sp,
                    fontStyle = FontStyle.Italic,
                    color = BlankLeafColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LeafDisplayType.TABLE_OF_CONTENTS -> {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "CONTENTS",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp, 1.dp)
                        .background(BookGoldDark)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(14.dp))

                val lines = leaf.contentSnippet.lines().filter { it.isNotBlank() && it.trim() != "CONTENTS" }
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    for (line in lines) {
                        Text(
                            text = line,
                            fontFamily = FontFamily.Serif,
                            fontSize = 9.sp,
                            lineHeight = 14.5.sp,
                            color = InkBlack,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            LeafDisplayType.CHAPTER_OPENER, LeafDisplayType.PART_OPENER -> {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = leaf.sectionTitle,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (leaf.sectionSubtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = leaf.sectionSubtitle,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp, 1.dp)
                        .background(BookGoldDark)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Chapter Head Illustration
                if (leaf.headerIllustrationUri.isNotBlank()) {
                    IllustrationPreview(
                        uriString = leaf.headerIllustrationUri,
                        caption = leaf.headerIllustrationCaption,
                        maxHeight = 90
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                ProseParagraphsView(text = leaf.contentSnippet)
            }
            else -> {
                ProseParagraphsView(text = leaf.contentSnippet)

                // Chapter Tailpiece (if closer)
                if (leaf.isCloser && leaf.tailIllustrationUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    IllustrationPreview(
                        uriString = leaf.tailIllustrationUri,
                        caption = leaf.tailIllustrationCaption,
                        maxHeight = 55
                    )
                }
            }
        }

        // Drop folio on chapter openers (Centered bottom)
        if (leaf.isOpener && leaf.matterType == MatterType.TEXT_BODY && leaf.pageNumberDisplay.isNotBlank()) {
            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(14.dp))
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
fun ProseParagraphsView(text: String) {
    val paragraphs = text.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (para in paragraphs) {
            if (para.isNotBlank()) {
                val indent = "    " // Always indent every paragraph
                val formatted = com.example.cmos.CmosFormatter.toAnnotatedString("$indent$para")
                Text(
                    text = formatted,
                    fontFamily = FontFamily.Serif,
                    fontSize = 10.sp,
                    lineHeight = 16.5.sp,
                    color = InkBlack
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun IllustrationPreview(
    uriString: String,
    caption: String,
    maxHeight: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            uriString == "drawable:head_engraving" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_chapter_head_engraving_1786743429212),
                    contentDescription = caption.ifBlank { "Chapter Illustration" },
                    modifier = Modifier
                        .height(maxHeight.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            uriString == "drawable:tailpiece" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_chapter_tailpiece_1786743439495),
                    contentDescription = caption.ifBlank { "Chapter Tailpiece" },
                    modifier = Modifier
                        .height(maxHeight.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            else -> {
                AsyncImage(
                    model = uriString,
                    contentDescription = caption.ifBlank { "Chapter Illustration" },
                    modifier = Modifier
                        .height(maxHeight.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }

        if (caption.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = caption,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 8.sp,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
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
