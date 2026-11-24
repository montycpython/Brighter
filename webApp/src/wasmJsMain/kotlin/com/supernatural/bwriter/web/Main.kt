package com.supernatural.bwriter.web

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
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
      var openMyBook by remember { mutableStateOf<MyBook?>(null) }
      val book = openBook
      val myBook = openMyBook
      when {
        email == null -> SignInScreen()
        book != null -> ReaderScreen(book, onBack = { openBook = null })
        myBook != null -> MyBookReaderScreen(myBook, onBack = { openMyBook = null })
        else -> LibraryScreen(
          email,
          onOpen = { openBook = it },
          onOpenMine = { openMyBook = it }
        )
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
private fun LibraryScreen(
  email: String,
  onOpen: (RegistryBook) -> Unit,
  onOpenMine: (MyBook) -> Unit
) {
  var books by remember { mutableStateOf<List<RegistryBook>?>(null) }
  var myBooks by remember { mutableStateOf<List<MyBook>?>(null) }
  var mail by remember { mutableStateOf<List<MailMessage>?>(null) }
  var mailFailed by remember { mutableStateOf(false) }
  var openMail by remember { mutableStateOf<MailMessage?>(null) }
  var wallet by remember { mutableStateOf<WalletData?>(null) }
  var walletLoaded by remember { mutableStateOf(false) }
  var walletFailed by remember { mutableStateOf(false) }
  var tab by remember { mutableStateOf(0) }
  LaunchedEffect(email) {
    FirestoreBooks.fetchRegistry { result -> books = result }
    FirestoreBooks.fetchMyBooks(email) { result -> myBooks = result }
    FirestoreBooks.fetchMail(email) { result ->
      mail = result
      mailFailed = result == null
    }
    FirestoreBooks.fetchWallet(email) { result ->
      wallet = result
      walletFailed = result == null
      walletLoaded = true
    }
  }
  val openedMail = openMail
  if (openedMail != null) {
    MailReaderScreen(message = openedMail, onBack = { openMail = null })
    return
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
    Row(
      modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
    ) {
      Surface(
        color = Color(0xFFE8E8ED),
        shape = RoundedCornerShape(10.dp)
      ) {
        Row(modifier = Modifier.padding(2.dp)) {
          SegmentButton("Library", tab == 0) { tab = 0 }
          SegmentButton("My Books", tab == 1) { tab = 1 }
          SegmentButton("Wallet", tab == 2) { tab = 2 }
          val unread = mail?.count { !it.isRead } ?: 0
          SegmentButton(
            if (unread > 0) "Mail ($unread)" else "Mail",
            tab == 3
          ) { tab = 3 }
        }
      }
    }
    when (tab) {
      1 -> MyBooksList(myBooks, onOpenMine)
      2 -> WalletView(wallet, walletLoaded, walletFailed)
      3 -> MailList(mail, mailFailed) { message ->
        openMail = message
        if (!message.isRead) {
          FirestoreBooks.markMailRead(message.id)
          mail = mail?.map {
            if (it.id == message.id) it.copy(isRead = true) else it
          }
        }
      }
      else -> RegistryList(books, onOpen)
    }
  }
}

@Composable
private fun RegistryList(
  books: List<RegistryBook>?,
  onOpen: (RegistryBook) -> Unit
) {
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
          .padding(horizontal = 28.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        items(current) { book -> BookCard(book, onOpen) }
      }
    }
  }
}

@Composable
private fun WalletView(wallet: WalletData?, loaded: Boolean, failed: Boolean) {
  when {
    !loaded -> {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(color = BwriterGold)
      }
    }
    failed || wallet == null -> {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Your wallet could not be loaded.",
          fontSize = 16.sp,
          color = AppleSecondary
        )
      }
    }
    else -> {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 28.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        item {
          Surface(
            color = AppleSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Text(
                text = "CREDIT BALANCE",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppleSecondary
              )
              Text(
                text = wallet.balance.toString(),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = AppleInk,
                modifier = Modifier.padding(top = 4.dp)
              )
              Text(
                text = "Claimed ${wallet.claimed} · Received ${wallet.received}" +
                  " · Sent ${wallet.sent} · Spent ${wallet.spent}",
                fontSize = 12.sp,
                color = AppleSecondary,
                modifier = Modifier.padding(top = 6.dp)
              )
            }
          }
        }
        if (wallet.unclaimed.isNotEmpty()) {
          item {
            Surface(
              color = AppleSurface,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(20.dp)) {
                Text(
                  text = "UNCLAIMED CREDIT",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = AppleSecondary
                )
                Text(
                  text = wallet.unclaimed.sumOf { it.amount }.toString(),
                  fontSize = 22.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = AppleInk,
                  modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                  text = "${wallet.unclaimed.size} issuances waiting to be mined",
                  fontSize = 12.sp,
                  color = AppleSecondary,
                  modifier = Modifier.padding(top = 4.dp)
                )
              }
            }
          }
        }
        item {
          Surface(
            color = AppleSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Text(
                text = "SHARE UNITS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppleSecondary
              )
              Text(
                text = wallet.units.size.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppleInk,
                modifier = Modifier.padding(top = 4.dp)
              )
              val vested = wallet.units.count { it.vested }
              Text(
                text = "Vested $vested · Awaiting vesting ${wallet.units.size - vested}",
                fontSize = 12.sp,
                color = AppleSecondary,
                modifier = Modifier.padding(top = 4.dp)
              )
              val groups = wallet.units
                .groupBy { it.seasonYear to it.role }
                .entries
                .sortedWith(compareBy({ it.key.first }, { it.key.second }))
              for ((key, group) in groups) {
                Text(
                  text = "Season ${key.first} · ${key.second} · ${group.size}",
                  fontSize = 13.sp,
                  color = AppleInk,
                  modifier = Modifier.padding(top = 8.dp)
                )
              }
            }
          }
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

@Composable
private fun SegmentButton(label: String, selected: Boolean, onClick: () -> Unit) {
  if (selected) {
    Surface(
      color = AppleSurface,
      shape = RoundedCornerShape(8.dp),
      shadowElevation = 1.dp
    ) {
      Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppleInk,
        modifier = Modifier
          .padding(horizontal = 18.dp, vertical = 6.dp)
      )
    }
  } else {
    TextButton(onClick = onClick) {
      Text(text = label, fontSize = 14.sp, color = AppleSecondary)
    }
  }
}

@Composable
private fun MyBooksList(myBooks: List<MyBook>?, onOpenMine: (MyBook) -> Unit) {
  val current = myBooks
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
          text = "No books of yours are in the cloud yet.",
          fontSize = 16.sp,
          color = AppleSecondary
        )
      }
    }
    else -> {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 28.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        items(current) { book -> MyBookCard(book, onOpenMine) }
      }
    }
  }
}

@Composable
private fun MyBookCard(book: MyBook, onOpenMine: (MyBook) -> Unit) {
  Surface(
    onClick = { onOpenMine(book) },
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
        if (book.wordCount > 0) {
          Text(
            text = "${groupThousands(book.wordCount)} words",
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

@Composable
private fun MailList(
  mail: List<MailMessage>?,
  failed: Boolean,
  onOpen: (MailMessage) -> Unit
) {
  when {
    mail == null && !failed -> {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(color = BwriterGold)
      }
    }
    failed -> {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Your mail could not be loaded.",
          fontSize = 16.sp,
          color = AppleSecondary
        )
      }
    }
    mail.isNullOrEmpty() -> {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No mail yet.",
          fontSize = 16.sp,
          color = AppleSecondary
        )
      }
    }
    else -> {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 28.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(mail) { message -> MailRow(message, onOpen) }
      }
    }
  }
}

@Composable
private fun MailRow(message: MailMessage, onOpen: (MailMessage) -> Unit) {
  Surface(
    onClick = { onOpen(message) },
    color = AppleSurface,
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(9.dp)
          .background(
            if (message.isRead) Color.Transparent else BwriterGold,
            CircleShape
          )
      )
      Column(modifier = Modifier.padding(start = 12.dp)) {
        Text(
          text = message.senderName.ifBlank { message.senderEmail },
          fontSize = 15.sp,
          fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.SemiBold,
          color = AppleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = message.subject.ifBlank { "(no subject)" },
          fontSize = 14.sp,
          fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.SemiBold,
          color = AppleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = 2.dp)
        )
        Text(
          text = message.body,
          fontSize = 13.sp,
          color = AppleSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
    }
  }
}

@Composable
private fun MailReaderScreen(message: MailMessage, onBack: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(AppleBackground)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      TextButton(onClick = onBack) {
        Text(text = "‹  Mail", color = BwriterGold, fontSize = 16.sp)
      }
    }
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 28.dp, vertical = 12.dp)
    ) {
      Text(
        text = message.subject.ifBlank { "(no subject)" },
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = AppleInk
      )
      Text(
        text = "From ${message.senderName.ifBlank { message.senderEmail }}",
        fontSize = 14.sp,
        color = AppleSecondary,
        modifier = Modifier.padding(top = 6.dp)
      )
      if (message.manuscriptTitle.isNotBlank()) {
        Text(
          text = "About: ${message.manuscriptTitle}",
          fontSize = 13.sp,
          color = BwriterGold,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = message.body,
        fontFamily = FontFamily.Serif,
        fontSize = 16.sp,
        lineHeight = 25.sp,
        color = AppleInk
      )
      Spacer(modifier = Modifier.height(40.dp))
    }
  }
}
