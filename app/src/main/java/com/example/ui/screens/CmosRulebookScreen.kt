package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cmos.CmosFormatter
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.BookGoldLight
import com.example.ui.theme.InkNavy

data class CmosGuideRule(
    val sectionRef: String,
    val title: String,
    val summary: String,
    val example: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CmosRulebookScreen(
    onBack: () -> Unit
) {
    var playgroundInput by remember {
        mutableStateOf("The author, editor and contributor discussed \"The Obsidian Quill\" -- a book set in Chicago (1890-1910).")
    }
    var playgroundOutput by remember { mutableStateOf("") }

    val rules = listOf(
        CmosGuideRule(
            sectionRef = "CMOS §1.4 – §1.8",
            title = "Recto & Verso Leaves",
            summary = "A leaf consists of two facing pages: Recto (odd page on right) and Verso (even page on left). All major divisions must start on a Recto leaf. Blank verso leaves are placed automatically.",
            example = "Half-title (p. i), Title (p. iii), Chapter 1 (p. 1) always on Recto."
        ),
        CmosGuideRule(
            sectionRef = "CMOS §1.12",
            title = "Running Heads & Folios",
            summary = "Running headers appear at the head of each leaf: Verso carries book title/author; Recto carries chapter title. Opener leaves suppress running heads (blind folio) and use drop folios at foot.",
            example = "Verso: [4] The Obsidian Quill | Recto: Chapter I [5]"
        ),
        CmosGuideRule(
            sectionRef = "CMOS §6.19",
            title = "The Serial (Oxford) Comma",
            summary = "When a conjunction joins the last two elements in a series of three or more, a comma should be used before the conjunction to prevent ambiguity.",
            example = "“authors, editors, and contributors” (Correct)"
        ),
        CmosGuideRule(
            sectionRef = "CMOS §6.85",
            title = "Em-Dashes (No Spaces)",
            summary = "The em-dash (—) sets off parenthetical phrases or sharp interruptions without surrounding spaces.",
            example = "“His fingers moved—with thirty years of rhythm—across the types.”"
        ),
        CmosGuideRule(
            sectionRef = "CMOS §6.78",
            title = "En-Dashes for Ranges",
            summary = "The en-dash (–) connects numbers, dates, or pages representing a continuing span or range.",
            example = "“Silas Dearborn (1842–1918) • pp. 45–62”"
        ),
        CmosGuideRule(
            sectionRef = "CMOS §8.159",
            title = "Headline-Style Capitalization",
            summary = "Capitalize first and last words in titles, nouns, pronouns, verbs, adjectives, adverbs. Lowercase articles (a, an, the) and prepositions under 5 letters.",
            example = "“The Craft of Typography and the Printed Leaf”"
        ),
        CmosGuideRule(
            sectionRef = "CMOS §2.12",
            title = "Paragraph Indentation",
            summary = "Prose paragraphs should be indented by standard 0.5 inch (~3 em spaces), without double spacing between continuous prose paragraphs.",
            example = "Standard first-line indent applied in PDF export."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Chicago Manual of Style",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "17th & 18th Edition Typographical Rules",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_rulebook")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = InkNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Bwriter Style Guide Engine",
                            color = BookGoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Publication-Grade Standards",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Bwriter automates Chicago Manual of Style pagination, leaf placement, foliation, quotes, and punctuation for Authors, Editors, and Contributors.",
                            color = Color(0xFFD0D0D5),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Interactive Playground
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = BookGoldDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live CMOS Polisher & Diagnostic",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = playgroundInput,
                            onValueChange = { playgroundInput = it },
                            label = { Text("Test Draft Text") },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_cmos_tester")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                playgroundOutput = CmosFormatter.polishText(playgroundInput)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_run_cmos_test")
                        ) {
                            Text("Apply Chicago Manual of Style Rules")
                        }

                        if (playgroundOutput.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Formatted Result:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = playgroundOutput,
                                fontFamily = FontFamily.Serif,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Core Chicago Rules Enforced by Bwriter",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(rules) { rule ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = BookGoldDark.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = rule.sectionRef,
                                    color = BookGoldDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = rule.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = rule.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = rule.example,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Serif,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}
