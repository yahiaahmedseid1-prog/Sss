package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val profilePic: String = "",
    val status: String = "متاح", // متاح، مشغول، غير متصل
    val lastSeen: Long = System.currentTimeMillis(),
    val isCurrentUser: Boolean = false,
    val isOnline: Boolean = true,
    // Settings columns
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val languageCode: String = "ar", // "ar" or "en"
    val privacyLastSeen: String = "all", // "all", "friends", "none"
    val privacyProfilePic: String = "all",
    val privacyReadReceipts: Boolean = true
)

@Entity(tableName = "friendships")
data class FriendshipEntity(
    @PrimaryKey val friendshipId: String, // "user1_user2"
    val requesterId: String,
    val receiverId: String,
    val status: String, // "pending", "accepted", "rejected"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: String, // "user1_user2" or "group_123"
    val name: String,
    val isGroup: Boolean,
    val groupImage: String = "",
    val adminId: String? = null,
    val memberIdsString: String = "", // Comma-separated user IDs
    val lastMessage: String? = null,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false
) {
    fun getMemberIds(): List<String> {
        if (memberIdsString.isBlank()) return emptyList()
        return memberIdsString.split(",")
    }
}

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "text", // "text", "image", "audio", "file"
    val fileUrl: String? = null,
    val reactionsString: String = "", // Format: "👍:user1,user2|❤️:user3"
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val status: String = "sent", // "sent", "delivered", "seen"
    val isDeleted: Boolean = false
) {
    // Parser for Reactions: Map<Emoji, List<UserIds>>
    fun getReactionsMap(): Map<String, List<String>> {
        if (reactionsString.isBlank()) return emptyMap()
        val map = mutableMapOf<String, List<String>>()
        val parts = reactionsString.split("|")
        for (part in parts) {
            val subParts = part.split(":")
            if (subParts.size == 2) {
                val emoji = subParts[0]
                val users = subParts[1].split(",").filter { it.isNotBlank() }
                if (users.isNotEmpty()) {
                    map[emoji] = users
                }
            }
        }
        return map
    }

    fun withToggledReaction(emoji: String, userId: String): MessageEntity {
        val currentMap = getReactionsMap().toMutableMap()
        val currentUsers = currentMap[emoji]?.toMutableList() ?: mutableListOf()
        if (currentUsers.contains(userId)) {
            currentUsers.remove(userId)
        } else {
            currentUsers.add(userId)
        }
        if (currentUsers.isEmpty()) {
            currentMap.remove(emoji)
        } else {
            currentMap[emoji] = currentUsers
        }

        // Re-serialize
        val serialized = currentMap.entries.joinToString("|") { entry ->
            "${entry.key}:${entry.value.joinToString(",")}"
        }
        return this.copy(reactionsString = serialized)
    }
}

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val notificationId: String,
    val userId: String, // Receiver
    val type: String, // "new_message", "friend_request", "request_accepted", "added_to_group"
    val senderId: String,
    val senderName: String,
    val message: String,
    val chatId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
