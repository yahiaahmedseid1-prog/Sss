package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.MessageEntity
import com.example.data.entity.UserEntity
import com.example.ui.theme.BubbleReceivedDark
import com.example.ui.theme.BubbleReceivedLight
import com.example.ui.theme.BubbleSentDark
import com.example.ui.theme.BubbleSentLight
import com.example.ui.theme.MessengerBlue
import com.example.ui.theme.MessengerGreenOnline
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val currentUser by viewModel.currentUser.collectAsState()
    val activeChat by viewModel.activeChat.collectAsState()
    val messages by viewModel.activeChatMessages.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Dialogs & Sheets states
    var selectedMessageForOptions by remember { mutableStateOf<MessageEntity?>(null) }
    var showForwardDialog by remember { mutableStateOf(false) }
    var showAttachmentDialog by remember { mutableStateOf(false) }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (activeChat == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MessengerBlue)
        }
        return
    }

    // Identify participant info for 1-on-1 chats
    val otherUser = remember(activeChat, allUsers) {
        val otherId = activeChat?.getMemberIds()?.firstOrNull { it != currentUser?.userId }
        allUsers.firstOrNull { it.userId == otherId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Small avatar with status dot
                        Box(contentAlignment = Alignment.BottomEnd) {
                            val initial = activeChat?.name?.firstOrNull()?.toString() ?: "C"
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (activeChat!!.isGroup) Color(0xFF673AB7) else MessengerBlue.copy(alpha = 0.8f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (otherUser?.isOnline == true && !activeChat!!.isGroup) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MessengerGreenOnline)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Name + Status
                        Column {
                            Text(
                                text = activeChat?.name ?: "محادثة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (activeChat!!.isGroup) {
                                    "${activeChat?.getMemberIds()?.size ?: 0} أعضاء"
                                } else {
                                    if (otherUser?.isOnline == true) "متصل الآن" else "غير متصل"
                                },
                                fontSize = 11.sp,
                                color = if (otherUser?.isOnline == true && !activeChat!!.isGroup) MessengerGreenOnline else Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "عودة"
                        )
                    }
                },
                actions = {
                    // Favorite Star
                    IconButton(onClick = { viewModel.toggleFavorite(activeChat!!.chatId) }) {
                        Icon(
                            imageVector = if (activeChat!!.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "مفضلة",
                            tint = if (activeChat!!.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // More Menu ⋮
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "المزيد"
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (activeChat!!.isArchived) "إلغاء الأرشفة" else "أرشفة المحادثة 📁") },
                                onClick = {
                                    viewModel.toggleArchived(activeChat!!.chatId)
                                    showMoreMenu = false
                                    Toast.makeText(context, "تم التحديث", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف المحادثة 🗑️", color = Color.Red) },
                                onClick = {
                                    viewModel.deleteChat(activeChat!!.chatId)
                                    showMoreMenu = false
                                    onNavigateBack()
                                    Toast.makeText(context, "تم حذف المحادثة", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color(0xFFF0F2F5)
                )
        ) {
            // Messages Area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("messages_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
            ) {
                items(messages) { message ->
                    val isOutgoing = message.senderId == currentUser?.userId
                    MessageBubbleRow(
                        message = message,
                        isOutgoing = isOutgoing,
                        onLongClick = {
                            selectedMessageForOptions = message
                        }
                    )
                }
            }

            // Quoted message reply preview
            AnimatedVisibility(
                visible = replyingToMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                replyingToMessage?.let { original ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .background(MessengerBlue, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الرد على ${original.senderName}",
                                fontSize = 12.sp,
                                color = MessengerBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = original.text,
                                fontSize = 13.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { replyingToMessage = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إلغاء الرد",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Input Bar (microphone, attachment, emoji, input field, send button)
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment button 📎
                    IconButton(onClick = { showAttachmentDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "مرفقات",
                            tint = MessengerBlue
                        )
                    }

                    // Voice recording 🎤 trigger button
                    IconButton(
                        onClick = {
                            // Simulate recording a voice message
                            viewModel.sendMessage(
                                text = "🎤 رسالة صوتية (0:12)",
                                type = "audio",
                                replyToId = replyingToMessage?.messageId,
                                replyToText = replyingToMessage?.text,
                                replyToSenderName = replyingToMessage?.senderName
                            )
                            replyingToMessage = null
                            Toast.makeText(context, "تم إرسال رسالة صوتية تجريبية 🎙️", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "تسجيل صوتي",
                            tint = MessengerBlue
                        )
                    }

                    // Emoji insert button 😊
                    IconButton(onClick = {
                        textInput += "😊"
                    }) {
                        Icon(
                            imageVector = Icons.Default.SentimentSatisfiedAlt,
                            contentDescription = "إيموجي",
                            tint = MessengerBlue
                        )
                    }

                    // Input Text Field
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("اكتب رسالة...", fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 120.dp)
                            .testTag("message_input_field"),
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedBorderColor = Color.LightGray,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Send airplane icon ✈️
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(
                                    text = textInput,
                                    type = "text",
                                    replyToId = replyingToMessage?.messageId,
                                    replyToText = replyingToMessage?.text,
                                    replyToSenderName = replyingToMessage?.senderName
                                )
                                textInput = ""
                                replyingToMessage = null
                            }
                        },
                        enabled = textInput.isNotBlank(),
                        modifier = Modifier.testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "إرسال",
                            tint = if (textInput.isNotBlank()) MessengerBlue else Color.Gray
                        )
                    }
                }
            }
        }
    }

    // Selected Message Options Sheet/Dialog (long press)
    if (selectedMessageForOptions != null) {
        val msg = selectedMessageForOptions!!
        val isMine = msg.senderId == currentUser?.userId

        AlertDialog(
            onDismissRequest = { selectedMessageForOptions = null },
            title = {
                // Reactions row 👍, ❤️, 😂, 😮, 😢, 😡
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("👍", "❤️", "😂", "😮", "😢", "😡").forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray.copy(alpha = 0.3f))
                                .clickable {
                                    viewModel.toggleReaction(msg.messageId, emoji)
                                    selectedMessageForOptions = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "\"${msg.text}\"",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Divider(color = Color.LightGray.copy(alpha = 0.5f))

                    // Reply option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                replyingToMessage = msg
                                selectedMessageForOptions = null
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = "رد", tint = MessengerBlue)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("رد على الرسالة 💬", fontSize = 14.sp)
                    }

                    // Copy option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("message", msg.text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ النص إلى الحافظة", Toast.LENGTH_SHORT).show()
                                selectedMessageForOptions = null
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = Color.Gray)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("نسخ النص 📋", fontSize = 14.sp)
                    }

                    // Forward Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showForwardDialog = true
                                selectedMessageForOptions = null
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "إعادة توجيه", tint = Color.Gray)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("إعادة توجيه الرسالة ➡️", fontSize = 14.sp)
                    }

                    // Delete options
                    if (isMine) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.deleteMessage(msg.messageId, forEveryone = true)
                                    selectedMessageForOptions = null
                                    Toast.makeText(context, "تم حذف الرسالة للجميع", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "حذف للجميع", tint = Color.Red)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("حذف للجميع 🗑️", fontSize = 14.sp, color = Color.Red)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.deleteMessage(msg.messageId, forEveryone = false)
                                selectedMessageForOptions = null
                                Toast.makeText(context, "تم حذف الرسالة لديك", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف لي", tint = Color.Red)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("حذف لدي فقط 🗑️", fontSize = 14.sp, color = Color.Red)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForOptions = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    // Attachment Picker Simulation Dialog (Attachment button click)
    if (showAttachmentDialog) {
        AlertDialog(
            onDismissRequest = { showAttachmentDialog = false },
            title = { Text("إرفاق ملف أو صورة 📎") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.sendMessage(
                                    text = "🖼️ صورة مرفقة (صورة_ماسنجر.jpg)",
                                    type = "image"
                                )
                                showAttachmentDialog = false
                                Toast.makeText(context, "تم إرسال صورة تجريبية", Toast.LENGTH_SHORT).show()
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "صورة", tint = MessengerBlue)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("إرسال صورة معرض الصور 🖼️", fontSize = 14.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.sendMessage(
                                    text = "📁 مستند مرفق (تقرير_العمل.pdf)",
                                    type = "file"
                                )
                                showAttachmentDialog = false
                                Toast.makeText(context, "تم إرسال ملف مستند تجريبي", Toast.LENGTH_SHORT).show()
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, contentDescription = "ملف", tint = Color.Gray)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("إرفاق ملف مستند (PDF, DOC) 📁", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAttachmentDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    // Forward Message Dialog overlay
    if (showForwardDialog && selectedMessageForOptions != null) {
        val forwardMsg = selectedMessageForOptions!!
        val chatsList by viewModel.allChats.collectAsState()

        AlertDialog(
            onDismissRequest = { showForwardDialog = false },
            title = { Text("إعادة توجيه الرسالة إلى:") },
            text = {
                Box(modifier = Modifier.heightIn(max = 280.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(chatsList) { chat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.sendMessage(
                                            text = "➡️ إعادة توجيه: ${forwardMsg.text}",
                                            type = forwardMsg.type
                                        )
                                        showForwardDialog = false
                                        Toast.makeText(context, "تمت إعادة توجيه الرسالة بنجاح!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MessengerBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chat.name.firstOrNull()?.toString() ?: "C",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = chat.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showForwardDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleRow(
    message: MessageEntity,
    isOutgoing: Boolean,
    onLongClick: () -> Unit
) {
    val layoutAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val bubbleColor = if (isOutgoing) {
        if (isDark) BubbleSentDark else BubbleSentLight
    } else {
        if (isDark) BubbleReceivedDark else BubbleReceivedLight
    }
    val textColor = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface

    // Customize bubble corners (Messenger custom styling)
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 2.dp // Sharp bottom right corner
        )
    } else {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 2.dp, // Sharp bottom left corner
            bottomEnd = 18.dp
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = layoutAlignment
    ) {
        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            // Show Sender Name if in group and incoming
            if (!isOutgoing && message.chatId.startsWith("group_") && message.senderId != "system") {
                Text(
                    text = message.senderName,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Quoted message reply bubble representation
            if (message.replyToId != null) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = message.replyToSenderName ?: "مستخدم",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MessengerBlue
                        )
                        Text(
                            text = message.replyToText ?: "",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Main Message bubble container
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(if (message.senderId == "system") Color.Transparent else bubbleColor)
                    .combinedClickable(
                        onClick = { /* Tap */ },
                        onLongClick = { if (message.senderId != "system") onLongClick() }
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        color = if (message.senderId == "system") Color.Gray else textColor,
                        fontSize = 14.sp,
                        fontWeight = if (message.senderId == "system") FontWeight.Medium else FontWeight.Normal,
                        modifier = if (message.senderId == "system") Modifier.fillMaxWidth() else Modifier,
                        style = if (message.senderId == "system") {
                            LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp)
                        } else LocalTextStyle.current
                    )
                }
            }

            // Message Reactions badge row overlay
            val reactions = message.getReactionsMap()
            if (reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 2.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    reactions.forEach { (emoji, users) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "$emoji ${users.size}", fontSize = 10.sp)
                        }
                    }
                }
            }

            // Timestamp + Status ticks beneath bubble
            if (message.senderId != "system") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                ) {
                    val dateStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )

                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        // Tick states: ✔️, ✔️✔️, ✔️✔️🔵
                        val tickIcon = when (message.status) {
                            "seen" -> "✔️✔️🔵"
                            "delivered" -> "✔️✔️"
                            else -> "✔️"
                        }
                        Text(
                            text = tickIcon,
                            fontSize = 10.sp,
                            color = if (message.status == "seen") MessengerBlue else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
