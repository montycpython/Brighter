package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LeafSide
import com.example.model.MatterType
import com.example.model.SectionStatus
import com.example.model.WorkRole
import com.example.model.WorkType
import com.example.ui.theme.BlankLeafColor
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.CrimsonSeal
import com.example.ui.theme.ForestCloth
import com.example.ui.theme.InkNavy
import com.example.ui.theme.ParchmentCream
import com.example.ui.theme.RectoBadgeColor
import com.example.ui.theme.VersoBadgeColor

@Composable
fun LeafBadge(
    side: LeafSide,
    pageNumber: String = "",
    isBlank: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isBlank -> BlankLeafColor
        side == LeafSide.RECTO -> RectoBadgeColor
        else -> VersoBadgeColor
    }
    val label = when {
        isBlank -> "Blank Verso"
        side == LeafSide.RECTO -> if (pageNumber.isNotBlank()) "Recto (p. $pageNumber)" else "Recto"
        else -> if (pageNumber.isNotBlank()) "Verso (p. $pageNumber)" else "Verso"
    }

    Surface(
        color = bgColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, bgColor.copy(alpha = 0.5f)),
        modifier = modifier.testTag("leaf_badge_${side.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(bgColor)
            )
            Text(
                text = label,
                color = bgColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun MatterBadge(matterType: MatterType, modifier: Modifier = Modifier) {
    val (color, label) = when (matterType) {
        MatterType.FRONT_MATTER -> Pair(BookGoldDark, "Front Matter")
        MatterType.TEXT_BODY -> Pair(ForestCloth, "Text / Body")
        MatterType.BACK_MATTER -> Pair(CrimsonSeal, "Back Matter")
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatusBadge(status: SectionStatus, modifier: Modifier = Modifier) {
    val (color, label) = when (status) {
        SectionStatus.DRAFT -> Pair(Color(0xFF8C7A6B), "Draft")
        SectionStatus.UNDER_REVIEW -> Pair(Color(0xFFC07020), "Under Review")
        SectionStatus.POLISHED -> Pair(Color(0xFF28547C), "Polished")
        SectionStatus.FINAL -> Pair(Color(0xFF246B43), "Final Approved")
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun WorkTypeIcon(workType: WorkType, modifier: Modifier = Modifier) {
    val icon = when (workType) {
        WorkType.NOVEL -> Icons.Default.Book
        WorkType.BIOGRAPHY -> Icons.Default.Person
        WorkType.DOCUMENTARY -> Icons.Default.Article
        WorkType.MANUAL -> Icons.Default.MenuBook
    }
    Icon(
        imageVector = icon,
        contentDescription = workType.displayName,
        modifier = modifier
    )
}

@Composable
fun RoleBadge(role: WorkRole, modifier: Modifier = Modifier) {
    val (color, label) = when (role) {
        WorkRole.AUTHOR -> Pair(BookGoldDark, "Author")
        WorkRole.EDITOR -> Pair(CrimsonSeal, "Editor")
        WorkRole.CONTRIBUTOR -> Pair(ForestCloth, "Contributor")
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier.testTag("role_badge")
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
