package com.supernatural.bwriter.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supernatural.bwriter.ui.theme.BookGoldLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OriginOfWritingScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "The Origin of Writing",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFFF3EFE6)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFF3EFE6)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0E0E14),
                    titleContentColor = Color(0xFFF3EFE6)
                )
            )
        },
        containerColor = Color(0xFF0A0A0E)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            OriginSection(
                title = "Before the Book",
                body = "Before there were books, there was the fear of forgetting. A voice dies on the wind. A mark in clay does not. Everything in this studio descends from the first hand that pressed a sign into earth and meant: this was so. Remember it."
            )

            OriginSection(
                title = "The Clay of Sumer",
                body = "Writing was born in Sumer, over five thousand years ago, when reed styluses pressed wedge-shaped marks into wet clay. The first signs counted grain, goats, and debts. Then, slowly, the marks began to carry sounds, and sounds became words, and words became the first stories ever kept. The scribe was the first keeper of memory, and his tablet the first page."
            )

            OriginSection(
                title = "The Words of the Gods",
                body = "In Egypt the signs were called medu netjer: words of the gods. Thoth, ibis-headed lord of wisdom, was said to have invented writing itself and to stand in the hall of judgment recording the weighing of every heart. To write a name was to give it a second life. To erase it was a second death. The Egyptians carved accordingly: for eternity."
            )

            OriginSection(
                title = "The House of the Letter",
                body = "On the coast of Lebanon stood Byblos, the city that gave the book its name. There the Phoenicians shaped a small alphabet of signs that any hand could learn, and among them was Bet: the house. The letter B is a house drawn in a few strokes, shelter for a sound. Every book is still that — a house built of letters, where a voice can live after the speaker has gone."
            )

            OriginSection(
                title = "The Spell of the Written Word",
                body = "Every old culture knew writing was a kind of magic. Odin hung nine nights on the world tree to seize the runes. The priests of Ptah taught that the world was spoken into being, thought in the heart and named by the tongue. A written word is a spoken word that refuses to die. That is the whole mystery, and it has never been improved upon."
            )

            OriginSection(
                title = "Your Mark",
                body = "The tools have changed. The act has not. When you begin a book here, you join the oldest line of makers there is: the ones who would not let the voice vanish. Make your mark."
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OriginSection(
    title: String,
    body: String
) {
    Spacer(modifier = Modifier.height(14.dp))
    Text(
        text = title,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = BookGoldLight
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = body,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = Color(0xFFC8C2B7)
    )
}
