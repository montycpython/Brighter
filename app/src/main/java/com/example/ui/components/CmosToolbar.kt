package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark

@Composable
fun CmosToolbar(
    onApplySmartQuotes: () -> Unit,
    onApplyEmDash: () -> Unit,
    onApplyEnDash: () -> Unit,
    onApplyHeadlineCase: () -> Unit,
    onApplyOxfordComma: () -> Unit,
    onInsertBlockQuote: () -> Unit,
    onInsertFootnote: () -> Unit,
    onFullPolish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Full Polish Chip
            AssistChip(
                onClick = onFullPolish,
                label = { Text("CMOS Polish", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Polish",
                        tint = BookGoldDark
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("btn_cmos_polish")
            )

            // Smart Quotes
            AssistChip(
                onClick = onApplySmartQuotes,
                label = { Text("“ ” Curly Quotes", fontSize = 12.sp, fontFamily = FontFamily.Serif) },
                modifier = Modifier.testTag("btn_smart_quotes")
            )

            // Em-Dash
            AssistChip(
                onClick = onApplyEmDash,
                label = { Text("— Em-Dash", fontSize = 12.sp, fontFamily = FontFamily.Serif) },
                modifier = Modifier.testTag("btn_em_dash")
            )

            // En-Dash
            AssistChip(
                onClick = onApplyEnDash,
                label = { Text("– En-Dash (Ranges)", fontSize = 12.sp, fontFamily = FontFamily.Serif) },
                modifier = Modifier.testTag("btn_en_dash")
            )

            // Title Case
            AssistChip(
                onClick = onApplyHeadlineCase,
                label = { Text("CMOS Title Case", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Title Case",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.testTag("btn_title_case")
            )

            // Oxford Comma
            AssistChip(
                onClick = onApplyOxfordComma,
                label = { Text("+ Serial (Oxford) Comma", fontSize = 12.sp) },
                modifier = Modifier.testTag("btn_oxford_comma")
            )

            // Block Quote
            AssistChip(
                onClick = onInsertBlockQuote,
                label = { Text("❝ Block Quote", fontSize = 12.sp) },
                modifier = Modifier.testTag("btn_block_quote")
            )

            // Footnote
            AssistChip(
                onClick = onInsertFootnote,
                label = { Text("[^n] Footnote Citation", fontSize = 12.sp) },
                modifier = Modifier.testTag("btn_footnote")
            )
        }
    }
}
