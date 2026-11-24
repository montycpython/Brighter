package com.supernatural.bwriter.web

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import com.supernatural.bwriter.data.bisnFor
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  FirebaseAuth.start()
  ComposeViewport(document.body!!) {
    App()
  }
}

@Composable
fun App() {
  BwriterTheme {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background
    ) {
      val email = AuthState.email
      var openBook by remember { mutableStateOf<RegistryBook?>(null) }
      val book = openBook
      when {
        email == null -> SignInScreen()
        book != null -> ReaderScreen(book, onBack = { openBook = null })
        else -> LibraryScreen(email, onOpen = { openBook = it })
      }
    }
  }
}

@Composable
private fun SignInScreen() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(32.dp)
    ) {
      Text(
        text = "Bwriter",
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        color = AppleInk
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Phoenicia",
        fontSize = 17.sp,
        color = BwriterGold,
        fontWeight = FontWeight.Medium
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Write a book to completion.",
        fontSize = 15.sp,
        color = AppleSecondary
      )
      Spacer(modifier = Modifier.height(36.dp))
      Button(
        onClick = { FirebaseAuth.signInWithGoogle() },
        colors = ButtonDefaults.buttonColors(
          containerColor = AppleInk,
          contentColor = AppleSurface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.widthIn(min = 260.dp)
      ) {
        Text(
          text = "Sign in with Google",
          fontSize = 16.sp,
          modifier = Modifier.padding(vertical = 6.dp)
        )
      }
    }
  }
}

@Composable
private fun LibraryScreen(email: String, onOpen: (RegistryBook) -> Unit) {
  var books by remember { mutableStateOf<List<RegistryBook>?>(null) }
  LaunchedEffect(email) {
    FirestoreBooks.fetchRegistry { result -> books = result }
  }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(AppleBackground)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 28.dp, vertical = 18.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Library",
          fontSize = 30.sp,
          fontWeight = FontWeight.Bold,
          color = AppleInk
        )
        Text(
          text = email,
          fontSize = 13.sp,
          color = AppleSecondary
        )
      }
      TextButton(onClick = { FirebaseAuth.signOut() }) {
        Text(text = "Sign out", color = BwriterGold, fontSize = 15.sp)
      }
    }
    val current = books
    when {
      current == null -> {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = BwriterGold)
        }
      }
      current.isEmpty() -> {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "No books in the registry yet.",
            fontSize = 16.sp,
            color = AppleSecondary
          )
        }
      }
      else -> {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          items(current) { book -> BookCard(book, onOpen) }
          item { Spacer(modifier = Modifier.height(14.dp)) }
        }
      }
    }
  }
}

@Composable
private fun BookCard(book: RegistryBook, onOpen: (RegistryBook) -> Unit) {
  Surface(
    onClick = { onOpen(book) },
    color = AppleSurface,
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Text(
        text = book.title.ifBlank { "Untitled" },
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppleInk
      )
      if (book.subtitle.isNotBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = book.subtitle,
          fontSize = 15.sp,
          color = AppleSecondary
        )
      }
      if (book.authorName.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "by ${book.authorName}",
          fontSize = 14.sp,
          color = AppleSecondary
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      Row {
        if (book.workType.isNotBlank()) {
          Text(
            text = book.workType,
            fontSize = 12.sp,
            color = AppleSecondary
          )
        }
        if (book.wordCount > 0) {
          Text(
            text = "  ·  ${groupThousands(book.wordCount)} words",
            fontSize = 12.sp,
            color = AppleSecondary
          )
        }
        if (book.birthId.isNotBlank()) {
          Text(
            text = "  ·  BISN ${bisnFor(book.birthId)}",
            fontSize = 12.sp,
            color = BwriterGold
          )
        }
      }
    }
  }
}

/** 1234567 -> "1,234,567". Grouped by hand: no locale formatters in Wasm. */
private fun groupThousands(value: Long): String {
  val digits = value.toString()
  val sb = StringBuilder()
  for (i in digits.indices) {
    if (i > 0 && (digits.length - i) % 3 == 0) sb.append(',')
    sb.append(digits[i])
  }
  return sb.toString()
}
