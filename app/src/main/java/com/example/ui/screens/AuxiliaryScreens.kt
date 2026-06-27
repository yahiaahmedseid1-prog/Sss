package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.UserEntity
import com.example.ui.theme.MessengerBlue
import com.example.ui.theme.MessengerGreenOnline
import com.example.ui.viewmodel.ChatViewModel

// ==========================================
// 1. FRIENDS & SEARCH SCREEN (Part 5 & 8)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onStartChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val friendships by viewModel.friendships.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Friends, 1: Requests, 2: Discover
    var searchUserQuery by remember { mutableStateOf("") }

    val pendingRequests = remember(friendships, currentUser) {
        friendships.filter { it.receiverId == currentUser?.userId && it.status == "pending" }
    }

    val friendsList = remember(friendships, allUsers, currentUser) {
        val acceptedFriendships = friendships.filter { it.status == "accepted" }
        val friendIds = acceptedFriendships.map {
            if (it.requesterId == currentUser?.userId) it.receiverId else it.requesterId
        }
        allUsers.filter { friendIds.contains(it.userId) }
    }

    val discoverList = remember(allUsers, friendships, currentUser, searchUserQuery) {
        val nonDiscoverIds = friendships.map {
            listOf(it.requesterId, it.receiverId)
        }.flatten().toSet()

        allUsers.filter {
            it.userId != currentUser?.userId &&
                    !nonDiscoverIds.contains(it.userId) &&
                    it.name.contains(searchUserQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الأصدقاء والبحث 👥", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "عودة")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab row
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("أصدقائي (${friendsList.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        BadgedBox(
                            badge = {
                                if (pendingRequests.isNotEmpty()) {
                                    Badge(containerColor = Color.Red) { Text(pendingRequests.size.toString()) }
                                }
                            }
                        ) {
                            Text("الطلبات", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("اكتشف 🔍", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (activeTab) {
                0 -> {
                    // Friends List
                    if (friendsList.isEmpty()) {
                        EmptyStateView("👥", "لا يوجد أصدقاء مضافين", "انتقل إلى تبويب 'اكتشف' للبحث عن مستخدمين وإضافتهم كأصدقاء.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(friendsList) { friend ->
                                UserRowItem(
                                    user = friend,
                                    actionButtonText = "دردشة 💬",
                                    onActionClick = {
                                        viewModel.startChatWithUser(friend.userId, onStartChat)
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Pending Friend Requests
                    if (pendingRequests.isEmpty()) {
                        EmptyStateView("✉️", "لا توجد طلبات صداقة معلقة", "ستظهر الطلبات المرسلة إليك هنا بمجرد وصولها.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(pendingRequests) { req ->
                                val sender = allUsers.firstOrNull { it.userId == req.requesterId } ?: return@items
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(MessengerBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sender.name.firstOrNull()?.toString() ?: "U",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = sender.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(text = sender.status, fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.acceptFriendRequest(req.friendshipId)
                                                Toast.makeText(context, "تم قبول طلب الصداقة بنجاح 🎉", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MessengerBlue),
                                            shape = RoundedCornerShape(16.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("قبول", color = Color.White, fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.rejectFriendRequest(req.friendshipId)
                                                Toast.makeText(context, "تم رفض الطلب", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("رفض", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Discover Users Screen
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = searchUserQuery,
                            onValueChange = { searchUserQuery = it },
                            placeholder = { Text("البحث عن مستخدمين باسم الحساب...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp)
                        )

                        if (discoverList.isEmpty()) {
                            EmptyStateView("🔍", "لا توجد نتائج بحث", "حاول كتابة اسم مستخدم آخر للبحث.")
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(discoverList) { user ->
                                    UserRowItem(
                                        user = user,
                                        actionButtonText = "إضافة ➕",
                                        onActionClick = {
                                            viewModel.sendFriendRequest(user.userId)
                                            Toast.makeText(context, "تم إرسال طلب الصداقة 🚀", Toast.LENGTH_SHORT).show()
                                        }
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

// ==========================================
// 2. GROUP MANAGEMENT SCREEN (Part 9)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val friendships by viewModel.friendships.collectAsState()

    var groupName by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<String>() }

    val friendsList = remember(friendships, allUsers, currentUser) {
        val acceptedFriendships = friendships.filter { it.status == "accepted" }
        val friendIds = acceptedFriendships.map {
            if (it.requesterId == currentUser?.userId) it.receiverId else it.requesterId
        }
        allUsers.filter { friendIds.contains(it.userId) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إنشاء مجموعة جديدة 👥", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "عودة")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Group name input
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("اسم المجموعة") },
                placeholder = { Text("مثال: أصدقاء العمل، العائلة") },
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = "اسم المجموعة") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("group_name_input"),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "اختر أعضاء المجموعة (${selectedMembers.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Friends Selection checkboxes
            if (friendsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("يجب إضافة أصدقاء أولاً لتتمكن من إنشاء مجموعة.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(friendsList) { friend ->
                        val isChecked = selectedMembers.contains(friend.userId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedMembers.remove(friend.userId) else selectedMembers.add(friend.userId)
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (isChecked) selectedMembers.remove(friend.userId) else selectedMembers.add(friend.userId)
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MessengerBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = friend.name.firstOrNull()?.toString() ?: "U",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = friend.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Create Button
            Button(
                onClick = {
                    if (groupName.isBlank()) {
                        Toast.makeText(context, "الرجاء إدخال اسم المجموعة", Toast.LENGTH_SHORT).show()
                    } else if (selectedMembers.isEmpty()) {
                        Toast.makeText(context, "يجب تحديد عضو واحد على الأقل", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.createGroupChat(groupName, selectedMembers)
                        Toast.makeText(context, "تم إنشاء المجموعة بنجاح 🎉", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_group_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MessengerBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إنشاء المجموعة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ==========================================
// 3. EDIT PROFILE SCREEN (Part 6)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()

    var name by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var status by remember(currentUser) { mutableStateOf(currentUser?.status ?: "") }
    var avatarSelection by remember { mutableStateOf("💬") } // Emoji based avatar selection
    var showAvatarDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تعديل الملف الشخصي ✏️", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "عودة")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big avatar selection button
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MessengerBlue.copy(alpha = 0.15f))
                    .clickable { showAvatarDialog = true }
                    .testTag("edit_avatar_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = avatarSelection, fontSize = 40.sp)
                    Text(text = "تغيير", fontSize = 11.sp, color = MessengerBlue, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم الكامل") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "اسم") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_input"),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status message
            OutlinedTextField(
                value = status,
                onValueChange = { status = it },
                label = { Text("رسالة الحالة الخاصة بك") },
                placeholder = { Text("مثال: متاح، في العمل، مشغول...") },
                leadingIcon = { Icon(Icons.Default.ChatBubble, contentDescription = "حالة") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_status_input"),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email (Non editable)
            OutlinedTextField(
                value = currentUser?.email ?: "user@example.com",
                onValueChange = {},
                enabled = false,
                label = { Text("البريد الإلكتروني (غير قابل للتعديل)") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "البريد") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Save changes button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "الاسم الكامل مطلوب", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.updateProfile(name, status, avatarSelection)
                        Toast.makeText(context, "تم حفظ البيانات وتحديث الملف الشخصي بنجاح! ✅", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("profile_save_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MessengerBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("حفظ التغييرات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Change Password button placeholder
            TextButton(
                onClick = {
                    Toast.makeText(context, "تم إرسال تعليمات إعادة تعيين كلمة المرور إلى البريد الإلكتروني الخاص بك", Toast.LENGTH_LONG).show()
                }
            ) {
                Text("تغيير كلمة المرور 🔑", color = MessengerBlue, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Avatar Selection Dialog overlay
    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = { Text("اختر رمزاً للملف الشخصي") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("💬", "🚀", "☕", "🎮", "🌟", "🔥").forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray.copy(alpha = 0.3f))
                                .clickable {
                                    avatarSelection = emoji
                                    showAvatarDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 24.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarDialog = false }) {
                    Text("إغلاق", color = Color.Gray)
                }
            }
        )
    }
}

// ==========================================
// 4. SETTINGS SCREEN (Part 7)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()

    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات التطبيق ⚙️", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "عودة")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        currentUser?.let { user ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "عام وتفضيلات",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MessengerBlue
                )

                // Push Notifications Switch
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("الإشعارات الفورية", fontWeight = FontWeight.Bold)
                            Text("تلقي تنبيهات عند استلام رسائل أو طلبات جديدة", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = user.notificationsEnabled,
                            onCheckedChange = {
                                viewModel.updateSettings(
                                    darkMode = user.darkModeEnabled,
                                    languageCode = user.languageCode,
                                    notifications = it
                                )
                            }
                        )
                    }
                }

                // Dark Mode Switch
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("الوضع الداكن (Dark Mode) 🌙", fontWeight = FontWeight.Bold)
                            Text("تشغيل المظهر الداكن لتوفير استهلاك البطارية", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = user.darkModeEnabled,
                            onCheckedChange = {
                                viewModel.updateSettings(
                                    darkMode = it,
                                    languageCode = user.languageCode,
                                    notifications = user.notificationsEnabled
                                )
                                Toast.makeText(context, "تم تغيير مظهر التطبيق", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                // Language selector (Arabic/English)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("لغة التطبيق (Language) 🌐", fontWeight = FontWeight.Bold)
                            Text(if (user.languageCode == "ar") "العربية (تخطيط RTL)" else "English (LTR Layout)", fontSize = 11.sp, color = Color.Gray)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.updateSettings(
                                        darkMode = user.darkModeEnabled,
                                        languageCode = "ar",
                                        notifications = user.notificationsEnabled
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (user.languageCode == "ar") MessengerBlue else Color.LightGray
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("عربي", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    viewModel.updateSettings(
                                        darkMode = user.darkModeEnabled,
                                        languageCode = "en",
                                        notifications = user.notificationsEnabled
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (user.languageCode == "en") MessengerBlue else Color.LightGray
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("EN", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }

                Text(
                    text = "الخصوصية والأمان",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MessengerBlue,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Read Receipts Switch
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("مؤشرات قراءة الرسائل (Seen)", fontWeight = FontWeight.Bold)
                            Text("السماح للآخرين بمعرفة ما إذا كنت قد قرأت رسائلهم", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = user.privacyReadReceipts,
                            onCheckedChange = {
                                viewModel.updatePrivacy(
                                    lastSeen = user.privacyLastSeen,
                                    profilePic = user.privacyProfilePic,
                                    readReceipts = it
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Delete Account button
                Button(
                    onClick = { showDeleteConfirmation = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("delete_account_button")
                ) {
                    Text("حذف الحساب نهائياً ⚠️", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Account Deletion Confirmation Dialog overlay
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("تحذير: حذف الحساب نهائياً! ⚠️") },
            text = { Text("هل أنت متأكد من رغبتك في حذف حسابك تماماً من قاعدة البيانات؟ هذه العملية لا يمكن التراجع عنها وسيتم مسح جميع رسائلك ومحادثاتك.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteConfirmation = false
                        onLogout()
                        Toast.makeText(context, "تم حذف الحساب بنجاح", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("نعم، احذف الحساب", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}

// ==========================================
// SHARED HELPER DRAWABLES & COMPOSABLES
// ==========================================
@Composable
fun UserRowItem(
    user: UserEntity,
    actionButtonText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
        Box(contentAlignment = Alignment.BottomEnd) {
            val initial = user.name.firstOrNull()?.toString() ?: "U"
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MessengerBlue.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            if (user.isOnline) {
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

        Spacer(modifier = Modifier.width(16.dp))

        // Info details
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = user.status, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
        }

        // Action Button
        Button(
            onClick = onActionClick,
            colors = ButtonDefaults.buttonColors(containerColor = MessengerBlue.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(text = actionButtonText, color = MessengerBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyStateView(
    icon: String,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 56.sp, modifier = Modifier.padding(bottom = 12.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}
