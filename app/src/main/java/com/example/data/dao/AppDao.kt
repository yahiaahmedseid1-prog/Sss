package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Users ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun getUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserOneShot(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUserOneShot(): UserEntity?

    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY name ASC")
    suspend fun getAllUsersOneShot(): List<UserEntity>

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUser(userId: String)


    // --- Friendships ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriendship(friendship: FriendshipEntity)

    @Query("SELECT * FROM friendships")
    fun getFriendships(): Flow<List<FriendshipEntity>>

    @Query("UPDATE friendships SET status = :status WHERE friendshipId = :friendshipId")
    suspend fun updateFriendshipStatus(friendshipId: String, status: String)

    @Query("DELETE FROM friendships WHERE friendshipId = :friendshipId")
    suspend fun deleteFriendship(friendshipId: String)


    // --- Chats & Groups ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("SELECT * FROM chats WHERE chatId = :chatId LIMIT 1")
    fun getChat(chatId: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE chatId = :chatId LIMIT 1")
    suspend fun getChatOneShot(chatId: String): ChatEntity?

    @Query("SELECT * FROM chats ORDER BY lastMessageTime DESC")
    fun getChats(): Flow<List<ChatEntity>>

    @Query("UPDATE chats SET lastMessage = :lastMessage, lastMessageTime = :lastMessageTime, unreadCount = unreadCount + :unreadIncrement WHERE chatId = :chatId")
    suspend fun updateChatLastMessage(chatId: String, lastMessage: String?, lastMessageTime: Long, unreadIncrement: Int = 1)

    @Query("UPDATE chats SET lastMessage = :lastMessage, lastMessageTime = :lastMessageTime, unreadCount = :unreadCount WHERE chatId = :chatId")
    suspend fun updateChatDetails(chatId: String, lastMessage: String?, lastMessageTime: Long, unreadCount: Int)

    @Query("UPDATE chats SET unreadCount = 0 WHERE chatId = :chatId")
    suspend fun clearChatUnreadCount(chatId: String)

    @Query("UPDATE chats SET isFavorite = :isFavorite WHERE chatId = :chatId")
    suspend fun updateChatFavorite(chatId: String, isFavorite: Boolean)

    @Query("UPDATE chats SET isArchived = :isArchived WHERE chatId = :chatId")
    suspend fun updateChatArchived(chatId: String, isArchived: Boolean)

    @Query("DELETE FROM chats WHERE chatId = :chatId")
    suspend fun deleteChat(chatId: String)


    // --- Messages ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessage(messageId: String): MessageEntity?

    @Query("UPDATE messages SET isDeleted = 1, text = 'تم حذف هذه الرسالة' WHERE messageId = :messageId")
    suspend fun softDeleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: String)


    // --- Notifications ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: String): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET isRead = 1 WHERE notificationId = :notificationId")
    suspend fun markNotificationAsRead(notificationId: String)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun clearAllNotifications(userId: String)
}
