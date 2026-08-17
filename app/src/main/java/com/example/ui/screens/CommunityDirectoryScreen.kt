package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.GlobalBookIndexEntry
import com.example.model.UserProfile
import com.example.model.WorkRole
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.BookGoldLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDirectoryScreen(
    currentUser: UserProfile,
    books: List<GlobalBookIndexEntry>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenReader: (Long) -> Unit = {},
    onOpenManuscript: (Long) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val filteredBooks = remember(books, searchQuery, currentUser) {
        if (searchQuery.isBlank()) {
            books
        } else {
            books.filter {
                val authorName = if (it.authorEmail.equals(currentUser.email, ignoreCase = true)) currentUser.displayName else it.authorName
                it.title.contains(searchQuery, ignoreCase = true) ||
                authorName.contains(searchQuery, ignoreCase = true) ||
                it.workType.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BookGoldDark.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, BookGoldDark),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "B",
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = BookGoldLight
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Global Book Registry",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp,
                                color = Color(0xFFF3EFE6)
                            )
                            Text(
                                text = "Decentralized Master Library & Publications",
                                fontSize = 11.sp,
                                color = BookGoldLight
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_community_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF3EFE6))
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, modifier = Modifier.testTag("btn_community_refresh")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BookGoldLight)
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
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search title, author, or genre...", color = Color.Gray, fontSize = 12.5.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth().testTag("input_search_community"),
                textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFFF3EFE6)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BookGold,
                    unfocusedBorderColor = Color(0xFF2C2C38),
                    focusedContainerColor = Color(0xFF14141C),
                    unfocusedContainerColor = Color(0xFF121218)
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Info banner explaining Registry click-to-read & collaboration
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF14141C),
                border = BorderStroke(1.dp, Color(0xFF2C2C38)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Browse the Global Registry. Tap any book to read in two-page leaf spreads. To propose revisions on other authors' books, switch to Editor or Contributor mode.",
                        fontSize = 11.sp,
                        color = Color(0xFFB0A89C),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Book List
            if (filteredBooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No books found in the Global Registry.",
                            color = Color.Gray,
                            fontSize = 13.5.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBooks, key = { it.fileId }) { book ->
                        val isMyBook = book.authorEmail.equals(currentUser.email, ignoreCase = true)
                        val isSuperuserBook = book.authorName.equals("Author", ignoreCase = true) || book.authorEmail.equals(com.example.data.GoogleDriveSyncService.EDITOR_IN_CHIEF_EMAIL, ignoreCase = true)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenReader(book.manuscriptId) }
                                .testTag("community_book_${book.fileId}"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                            border = BorderStroke(1.dp, if (isMyBook) BookGoldDark else Color(0xFF2C2C38)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = book.title,
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFFF3EFE6)
                                        )
                                        if (book.subtitle.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = book.subtitle,
                                                fontSize = 12.sp,
                                                color = Color(0xFFB0A89C)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSuperuserBook) BookGoldDark.copy(alpha = 0.25f) else Color(0xFF242432),
                                        border = BorderStroke(0.5.dp, if (isSuperuserBook) BookGold else Color(0xFF444455))
                                    ) {
                                        Text(
                                            text = if (isSuperuserBook) "Superuser Master" else book.workType.replace("_", " "),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSuperuserBook) BookGoldLight else Color(0xFFCCCCCC),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    val displayAuthor = if (isMyBook) currentUser.displayName else book.authorName
                                    Text(
                                        text = "By $displayAuthor",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFE2DDD5)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "• ${book.workType.replace("_", " ")}",
                                        fontSize = 11.5.sp,
                                        color = BookGoldLight
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${book.wordCount} words • ~${book.totalLeaves} leaves",
                                        fontSize = 11.sp,
                                        color = Color(0xFFB0A89C)
                                    )
                                    Text(
                                        text = "Synced: ${dateFormat.format(Date(book.lastSyncedTimestamp))}",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF7E7E8E)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick Action Buttons: Read Book & Propose Revisions / Draft
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onOpenReader(book.manuscriptId) },
                                        modifier = Modifier.weight(1f).testTag("btn_read_book_${book.fileId}"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BookGoldDark,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Read Book", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { onOpenManuscript(book.manuscriptId) },
                                        modifier = Modifier.weight(1f).testTag("btn_collaborate_book_${book.fileId}"),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = BookGoldLight
                                        ),
                                        border = BorderStroke(1.dp, BookGoldDark),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isMyBook || currentUser.role != WorkRole.AUTHOR) "Draft / Edit" else "Review & Inspect",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
