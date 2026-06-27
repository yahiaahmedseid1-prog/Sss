package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.*
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    // --- State Observables ---
    val currentUser: StateFlow<UserEntity?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allChats: StateFlow<List<ChatEntity>> = repository.chats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendships: StateFlow<List<FriendshipEntity>> = repository.friendships
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    // Active Chat State
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    val activeChat: StateFlow<ChatEntity?> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) repository.getChat(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeChatMessages: StateFlow<List<MessageEntity>> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) repository.getMessagesForChat(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI-Specific states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSeedingCompleted = MutableStateFlow(false)
    val isSeedingCompleted = _isSeedingCompleted.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed the database with high-quality sample data if empty
            repository.seedDatabase()
            _isSeedingCompleted.value = true

            // Start observing notifications for logged-in user
            currentUser.collect { user ->
                if (user != null) {
                    repository.getNotifications(user.userId).collect { list ->
                        _notifications.value = list
                    }
                }
            }
        }
    }

    // --- Actions ---

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loginUser(email: String): Boolean {
        var success = false
        viewModelScope.launch {
            val matchedUser = allUsers.value.firstOrNull { it.email.trim().lowercase() == email.trim().lowercase() }
            if (matchedUser != null) {
                // Update isCurrentUser to true
                repository.updateUser(matchedUser.copy(isCurrentUser = true, isOnline = true))
                success = true
            } else {
                // Create a user on the fly if email exists as a mock or input
                val newUser = UserEntity(
                    userId = UUID.randomUUID().toString(),
                    name = email.split("@").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "مستخدم",
                    email = email.trim().lowercase(),
                    isCurrentUser = true,
                    isOnline = true
                )
                repository.insertUser(newUser)
                success = true
            }
        }
        return success
    }

    fun registerUser(name: String, email: String) {
        viewModelScope.launch {
            // Log out previous users first
            allUsers.value.forEach {
                if (it.isCurrentUser) {
                    repository.updateUser(it.copy(isCurrentUser = false, isOnline = false))
                }
            }
            val newUser = UserEntity(
                userId = UUID.randomUUID().toString(),
                name = name,
                email = email.trim().lowercase(),
                isCurrentUser = true,
                isOnline = true,
                languageCode = "ar"
            )
            repository.insertUser(newUser)
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            currentUser.value?.let {
                repository.updateUser(it.copy(isCurrentUser = false, isOnline = false))
            }
            _activeChatId.value = null
        }
    }

    fun setActiveChat(chatId: String?) {
        _activeChatId.value = chatId
        if (chatId != null) {
            viewModelScope.launch {
                repository.clearUnread(chatId)
            }
        }
    }

    fun sendMessage(
        text: String,
        type: String = "text",
        fileUrl: String? = null,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSenderName: String? = null
    ) {
        val chatId = _activeChatId.value ?: return
        val sender = currentUser.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                senderId = sender.userId,
                senderName = sender.name,
                text = text,
                type = type,
                fileUrl = fileUrl,
                replyToId = replyToId,
                replyToText = replyToText,
                replyToSenderName = replyToSenderName
            )
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleMessageReaction(messageId, emoji, user.userId)
        }
    }

    fun deleteMessage(messageId: String, forEveryone: Boolean) {
        viewModelScope.launch {
            if (forEveryone) {
                repository.deleteMessageForEveryone(messageId)
            } else {
                repository.deleteMessageForMe(messageId)
            }
        }
    }

    fun toggleFavorite(chatId: String) {
        val chat = allChats.value.firstOrNull { it.chatId == chatId } ?: return
        viewModelScope.launch {
            repository.toggleFavorite(chatId, !chat.isFavorite)
        }
    }

    fun toggleArchived(chatId: String) {
        val chat = allChats.value.firstOrNull { it.chatId == chatId } ?: return
        viewModelScope.launch {
            repository.toggleArchived(chatId, !chat.isArchived)
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            if (_activeChatId.value == chatId) {
                _activeChatId.value = null
            }
        }
    }

    fun startChatWithUser(otherUserId: String, onCompleted: (String) -> Unit) {
        val sender = currentUser.value ?: return
        viewModelScope.launch {
            val chatId = repository.createOneOnOneChat(sender.userId, otherUserId)
            setActiveChat(chatId)
            onCompleted(chatId)
        }
    }

    fun createGroupChat(name: String, memberIds: List<String>) {
        val admin = currentUser.value ?: return
        val fullMembers = memberIds.toMutableList()
        if (!fullMembers.contains(admin.userId)) {
            fullMembers.add(admin.userId)
        }
        viewModelScope.launch {
            repository.createGroupChat(name, fullMembers, admin.userId)
        }
    }

    fun sendFriendRequest(otherUserId: String) {
        val sender = currentUser.value ?: return
        viewModelScope.launch {
            repository.addFriend(sender.userId, otherUserId)
        }
    }

    fun acceptFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            repository.acceptFriendRequest(friendshipId)
        }
    }

    fun rejectFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            repository.rejectFriendRequest(friendshipId)
        }
    }

    fun updateProfile(name: String, status: String, profilePic: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateUser(
                user.copy(
                    name = name,
                    status = status,
                    profilePic = profilePic
                )
            )
        }
    }

    fun updateSettings(darkMode: Boolean, languageCode: String, notifications: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateUser(
                user.copy(
                    darkModeEnabled = darkMode,
                    languageCode = languageCode,
                    notificationsEnabled = notifications
                )
            )
        }
    }

    fun updatePrivacy(lastSeen: String, profilePic: String, readReceipts: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateUser(
                user.copy(
                    privacyLastSeen = lastSeen,
                    privacyProfilePic = profilePic,
                    privacyReadReceipts = readReceipts
                )
            )
        }
    }

    fun deleteAccount() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteUser(user.userId)
            _activeChatId.value = null
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }

    fun clearNotifications() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.clearNotifications(user.userId)
        }
    }
}

class ChatViewModelFactory(private val repository: ChatRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
