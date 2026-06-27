package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ChatEntity
import com.example.data.entity.UserEntity
import com.example.ui.theme.MessengerBlue
import com.example.ui.theme.MessengerGreenOnline
import com.example.ui.theme.MessengerLightGray
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    viewModel: ChatViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToGroupCreate: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsState()
    val allChats by viewModel.allChats.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Filter tabs: "all", "favorites", "archived"
    var activeFilter by remember { mutableStateOf("all") }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    // Navigation drawer wrapper
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                SidebarContent(
                    currentUser = currentUser,
                    activeFilter = activeFilter,
                    onFilterSelected = { filter ->
                        activeFilter = filter
                        scope.launch { drawerState.close() }
                    },
                    onNavigateToFriends = {
                        scope.launch { drawerState.close() }
                        onNavigateToFriends()
                    },
                    onNavigateToProfile = {
                        scope.launch { drawerState.close() }
                        onNavigateToProfile()
                    },
                    onNavigateToSettings = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    onLogout = {
                        scope.launch { drawerState.close() }
                        viewModel.logoutUser()
                        onLogout()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    // Header (☰ Title, Notification, Pencil)
                    TopAppBar(
                        title = {
                            Text(
                                text = when (activeFilter) {
                                    "favorites" -> "المفضلة ⭐"
                                    "archived" -> "الأرشيف 📁"
                                    else -> "المحادثات"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "القائمة",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            // Notifications bell
                            Box {
                                IconButton(onClick = { showNotificationsDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "الإشعارات",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                val unreadNotifCount = notifications.count { !it.isRead }
                                if (unreadNotifCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 8.dp, end = 8.dp)
                                            .size(16.dp)
                                            .background(Color.Red, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = unreadNotifCount.toString(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Pencil icon to create group
                            IconButton(
                                onClick = onNavigateToGroupCreate,
                                modifier = Modifier.testTag("create_group_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "مجموعة جديدة",
                                    tint = MessengerBlue
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("ابحث في المحادثات...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = Color.Gray
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "مسح",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(52.dp)
                            .testTag("chats_search_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(26.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MessengerBlue,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    // Quick filter categories (All, Favorites, Archives)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = activeFilter == "all",
                            onClick = { activeFilter = "all" },
                            label = { Text("الكل", fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MessengerBlue.copy(alpha = 0.15f),
                                selectedLabelColor = MessengerBlue
                            )
                        )
                        FilterChip(
                            selected = activeFilter == "favorites",
                            onClick = { activeFilter = "favorites" },
                            label = { Text("المفضلة ⭐", fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MessengerBlue.copy(alpha = 0.15f),
                                selectedLabelColor = MessengerBlue
                            )
                        )
                        FilterChip(
                            selected = activeFilter == "archived",
                            onClick = { activeFilter = "archived" },
                            label = { Text("الأرشيف 📁", fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MessengerBlue.copy(alpha = 0.15f),
                                selectedLabelColor = MessengerBlue
                            )
                        )
                    }
                }
            },
            modifier = modifier
        ) { innerPadding ->
            // Filter chats
            val filteredChats = allChats.filter { chat ->
                // Apply Search Query
                val matchesSearch = chat.name.contains(searchQuery, ignoreCase = true) ||
                        (chat.lastMessage?.contains(searchQuery, ignoreCase = true) ?: false)

                // Apply Tabs Filters
                val matchesFilter = when (activeFilter) {
                    "favorites" -> chat.isFavorite
                    "archived" -> chat.isArchived
                    else -> !chat.isArchived // Hide archived chats by default in 'all'
                }

                matchesSearch && matchesFilter
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (filteredChats.isEmpty()) {
                    // Empty list state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "💬",
                            fontSize = 64.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "لا توجد محادثات",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ابحث عن أصدقائك لبدء الدردشة الآن!",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onNavigateToFriends,
                            colors = ButtonDefaults.buttonColors(containerColor = MessengerBlue)
                        ) {
                            Text("البحث عن أصدقاء 👥", color = Color.White)
                        }
                    }
                } else {
                    // Lazy List of Chats
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("chats_list")
                    ) {
                        items(filteredChats) { chat ->
                            val isOnline = if (chat.isGroup) false else {
                                // Find other participant status
                                val otherId = chat.getMemberIds().firstOrNull { it != currentUser?.userId }
                                allUsers.firstOrNull { it.userId == otherId }?.isOnline ?: false
                            }

                            ChatListItem(
                                chat = chat,
                                isOnline = isOnline,
                                onClick = {
                                    viewModel.setActiveChat(chat.chatId)
                                    onNavigateToChat(chat.chatId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Live notifications dialog overlay (Bell click)
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("مركز الإشعارات 🔔", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (notifications.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearNotifications() }) {
                            Text("مسح الكل", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                    if (notifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد إشعارات جديدة", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(notifications) { notif ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.markNotificationAsRead(notif.notificationId)
                                            if (notif.chatId != null) {
                                                viewModel.setActiveChat(notif.chatId)
                                                onNavigateToChat(notif.chatId)
                                                showNotificationsDialog = false
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface else MessengerBlue.copy(alpha = 0.08f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = notif.message,
                                            fontSize = 13.sp,
                                            fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (notif.isRead) "مقروء" else "جديد",
                                                fontSize = 10.sp,
                                                color = if (notif.isRead) Color.Gray else MessengerBlue,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(notif.timestamp)),
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("إغلاق", color = MessengerBlue)
                }
            }
        )
    }
}

@Composable
fun SidebarContent(
    currentUser: UserEntity?,
    activeFilter: String,
    onFilterSelected: (String) -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // User Profile Info
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big dynamic letters Avatar
            val initial = currentUser?.name?.firstOrNull()?.toString() ?: "U"
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MessengerBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = currentUser?.name ?: "مستخدم ماسنجر",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = currentUser?.email ?: "user@example.com",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onNavigateToProfile,
                colors = ButtonDefaults.buttonColors(containerColor = MessengerBlue.copy(alpha = 0.1f)),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("edit_profile_drawer_button")
            ) {
                Text("تعديل الملف الشخصي ✏️", color = MessengerBlue, fontSize = 12.sp)
            }
        }

        Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

        // Navigation drawer elements
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DrawerItem(
                icon = Icons.Default.Chat,
                label = "المحادثات 💬",
                selected = activeFilter == "all",
                onClick = { onFilterSelected("all") }
            )
            DrawerItem(
                icon = Icons.Default.People,
                label = "الأصدقاء 👥",
                selected = false,
                onClick = onNavigateToFriends
            )
            DrawerItem(
                icon = Icons.Default.Star,
                label = "المفضلة ⭐",
                selected = activeFilter == "favorites",
                onClick = { onFilterSelected("favorites") }
            )
            DrawerItem(
                icon = Icons.Default.Archive,
                label = "الأرشيف 📁",
                selected = activeFilter == "archived",
                onClick = { onFilterSelected("archived") }
            )
            DrawerItem(
                icon = Icons.Default.Settings,
                label = "الإعدادات ⚙️",
                selected = false,
                onClick = onNavigateToSettings
            )
        }

        // Logout
        Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
        DrawerItem(
            icon = Icons.Default.ExitToApp,
            label = "تسجيل الخروج 🚪",
            selected = false,
            onClick = onLogout,
            tint = Color.Red
        )
    }
}

@Composable
fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MessengerBlue.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MessengerBlue else tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MessengerBlue else tint
        )
    }
}

@Composable
fun ChatListItem(
    chat: ChatEntity,
    isOnline: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            val initial = chat.name.firstOrNull()?.toString() ?: "C"
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (chat.isGroup) Color(0xFF673AB7) else MessengerBlue.copy(alpha = 0.8f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Green online badge status indicator
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MessengerGreenOnline)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Chat Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = chat.name,
                fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = chat.lastMessage ?: "لا توجد رسائل",
                fontSize = 13.sp,
                fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.onSurface else Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Time and Badges unread count indicator
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            // Time string representation
            val dateStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(chat.lastMessageTime))
            Text(
                text = dateStr,
                fontSize = 11.sp,
                color = if (chat.unreadCount > 0) MessengerBlue else Color.Gray,
                fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.Red),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (chat.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "مفضلة",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
