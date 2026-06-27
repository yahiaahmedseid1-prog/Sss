package com.example.data.repository

import com.example.data.dao.AppDao
import com.example.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(private val appDao: AppDao) {

    // Users
    val currentUser: Flow<UserEntity?> = appDao.getCurrentUser()
    val allUsers: Flow<List<UserEntity>> = appDao.getAllUsers()

    suspend fun getCurrentUserOneShot(): UserEntity? = withContext(Dispatchers.IO) {
        appDao.getCurrentUserOneShot()
    }

    suspend fun getUserOneShot(userId: String): UserEntity? = withContext(Dispatchers.IO) {
        appDao.getUserOneShot(userId)
    }

    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        appDao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) = withContext(Dispatchers.IO) {
        appDao.updateUser(user)
    }

    suspend fun deleteUser(userId: String) = withContext(Dispatchers.IO) {
        appDao.deleteUser(userId)
    }

    // Friendships
    val friendships: Flow<List<FriendshipEntity>> = appDao.getFriendships()

    suspend fun addFriend(requesterId: String, receiverId: String) = withContext(Dispatchers.IO) {
        val friendshipId = if (requesterId < receiverId) "${requesterId}_${receiverId}" else "${receiverId}_${requesterId}"
        val friendship = FriendshipEntity(
            friendshipId = friendshipId,
            requesterId = requesterId,
            receiverId = receiverId,
            status = "pending",
            timestamp = System.currentTimeMillis()
        )
        appDao.insertFriendship(friendship)

        // Add a notification for receiver
        val requesterName = appDao.getUserOneShot(requesterId)?.name ?: "مستخدم جديد"
        val notification = NotificationEntity(
            notificationId = UUID.randomUUID().toString(),
            userId = receiverId,
            type = "friend_request",
            senderId = requesterId,
            senderName = requesterName,
            message = "أرسل لك $requesterName طلب صداقة جديد.",
            timestamp = System.currentTimeMillis()
        )
        appDao.insertNotification(notification)
    }

    suspend fun acceptFriendRequest(friendshipId: String) = withContext(Dispatchers.IO) {
        appDao.updateFriendshipStatus(friendshipId, "accepted")
    }

    suspend fun rejectFriendRequest(friendshipId: String) = withContext(Dispatchers.IO) {
        appDao.deleteFriendship(friendshipId)
    }

    // Chats & Groups
    val chats: Flow<List<ChatEntity>> = appDao.getChats()

    fun getChat(chatId: String): Flow<ChatEntity?> = appDao.getChat(chatId)

    suspend fun createOneOnOneChat(currentUserId: String, otherUserId: String): String = withContext(Dispatchers.IO) {
        val chatId = if (currentUserId < otherUserId) "${currentUserId}_${otherUserId}" else "${otherUserId}_${currentUserId}"
        val otherUser = appDao.getUserOneShot(otherUserId) ?: return@withContext chatId
        val existing = appDao.getChatOneShot(chatId)
        if (existing == null) {
            val chat = ChatEntity(
                chatId = chatId,
                name = otherUser.name,
                isGroup = false,
                memberIdsString = "$currentUserId,$otherUserId",
                lastMessage = "بدأت المحادثة للتو",
                lastMessageTime = System.currentTimeMillis()
            )
            appDao.insertChat(chat)
        }
        chatId
    }

    suspend fun createGroupChat(name: String, memberIds: List<String>, adminId: String): String = withContext(Dispatchers.IO) {
        val chatId = "group_${UUID.randomUUID()}"
        val chat = ChatEntity(
            chatId = chatId,
            name = name,
            isGroup = true,
            adminId = adminId,
            memberIdsString = memberIds.joinToString(","),
            lastMessage = "تم إنشاء المجموعة بواسطة المسؤول",
            lastMessageTime = System.currentTimeMillis()
        )
        appDao.insertChat(chat)

        // Insert system messages
        val adminName = appDao.getUserOneShot(adminId)?.name ?: "المسؤول"
        val systemMsg = MessageEntity(
            messageId = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = "system",
            senderName = "النظام",
            text = "قام $adminName بإنشاء المجموعة '$name'",
            timestamp = System.currentTimeMillis()
        )
        appDao.insertMessage(systemMsg)

        // Send notification to all members except admin
        memberIds.forEach { memberId ->
            if (memberId != adminId) {
                appDao.insertNotification(
                    NotificationEntity(
                        notificationId = UUID.randomUUID().toString(),
                        userId = memberId,
                        type = "added_to_group",
                        senderId = adminId,
                        senderName = adminName,
                        message = "تمت إضافتك إلى المجموعة '$name' بواسطة $adminName.",
                        chatId = chatId,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        chatId
    }

    suspend fun clearUnread(chatId: String) = withContext(Dispatchers.IO) {
        appDao.clearChatUnreadCount(chatId)
    }

    suspend fun toggleFavorite(chatId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        appDao.updateChatFavorite(chatId, isFavorite)
    }

    suspend fun toggleArchived(chatId: String, isArchived: Boolean) = withContext(Dispatchers.IO) {
        appDao.updateChatArchived(chatId, isArchived)
    }

    suspend fun deleteChat(chatId: String) = withContext(Dispatchers.IO) {
        appDao.deleteChat(chatId)
    }

    // Messages
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = appDao.getMessagesForChat(chatId)

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String,
        type: String = "text",
        fileUrl: String? = null,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSenderName: String? = null
    ) = withContext(Dispatchers.IO) {
        val message = MessageEntity(
            messageId = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis(),
            type = type,
            fileUrl = fileUrl,
            replyToId = replyToId,
            replyToText = replyToText,
            replyToSenderName = replyToSenderName,
            status = "sent"
        )
        appDao.insertMessage(message)

        // Update chat last message
        appDao.updateChatDetails(chatId, text, System.currentTimeMillis(), 0)

        // Simulate other participants seen and response if chatting with a mock friend
        simulateMockResponse(chatId, senderId, text)
    }

    private suspend fun simulateMockResponse(chatId: String, senderId: String, lastText: String) {
        if (chatId.startsWith("group_")) {
            // Group chat mock responses
            val group = appDao.getChatOneShot(chatId) ?: return
            val members = group.getMemberIds()
            val responderId = members.firstOrNull { it != senderId && it != "system" } ?: return
            val responder = appDao.getUserOneShot(responderId) ?: return

            // Wait a little, and respond
            kotlinx.coroutines.delay(1000)
            val responseText = "أنا متفق معك تماماً! 👍"
            val responseMsg = MessageEntity(
                messageId = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = responderId,
                senderName = responder.name,
                text = responseText,
                timestamp = System.currentTimeMillis(),
                status = "seen"
            )
            appDao.insertMessage(responseMsg)
            appDao.updateChatDetails(chatId, responseText, System.currentTimeMillis(), 1)
        } else {
            // 1-on-1 mock responses
            val otherUserId = chatId.split("_").firstOrNull { it != senderId } ?: return
            val otherUser = appDao.getUserOneShot(otherUserId) ?: return

            kotlinx.coroutines.delay(1200)

            // Select reply based on what was typed
            val replyText = when {
                lastText.contains("السلام") || lastText.contains("مرحبا") || lastText.contains("أهلاً") ->
                    "وعليكم السلام يا صديقي! مرحباً بك في أي وقت 😊"
                lastText.contains("كيف") ->
                    "الحمد لله أنا بخير وبأحسن حال، أتمنى أن تكون أنت كذلك بصحة جيدة!"
                lastText.contains("شاشة") || lastText.contains("تصميم") ->
                    "يبدو التصميم مذهلاً ومتقناً للغاية! فخور جداً بما تنجزه 🚀"
                else ->
                    "رائع جداً! واصل هذا العمل المتميز 👍 وسأقوم بالرد عليك فور تفرغي."
            }

            val responseMsg = MessageEntity(
                messageId = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = otherUserId,
                senderName = otherUser.name,
                text = replyText,
                timestamp = System.currentTimeMillis(),
                status = "seen"
            )
            appDao.insertMessage(responseMsg)
            appDao.updateChatDetails(chatId, replyText, System.currentTimeMillis(), 1)
        }
    }

    suspend fun toggleMessageReaction(messageId: String, emoji: String, userId: String) = withContext(Dispatchers.IO) {
        val msg = appDao.getMessage(messageId) ?: return@withContext
        val updated = msg.withToggledReaction(emoji, userId)
        appDao.insertMessage(updated)
    }

    suspend fun deleteMessageForEveryone(messageId: String) = withContext(Dispatchers.IO) {
        appDao.softDeleteMessage(messageId)
    }

    suspend fun deleteMessageForMe(messageId: String) = withContext(Dispatchers.IO) {
        appDao.deleteMessage(messageId)
    }

    // Notifications
    fun getNotifications(userId: String): Flow<List<NotificationEntity>> = appDao.getNotificationsForUser(userId)

    suspend fun markNotificationAsRead(notificationId: String) = withContext(Dispatchers.IO) {
        appDao.markNotificationAsRead(notificationId)
    }

    suspend fun clearNotifications(userId: String) = withContext(Dispatchers.IO) {
        appDao.clearAllNotifications(userId)
    }

    // Seeding database with beautiful, realistic initial data
    suspend fun seedDatabase() = withContext(Dispatchers.IO) {
        val usersCount = appDao.getAllUsersOneShot().size
        if (usersCount > 0) return@withContext // Database is already seeded or holds users

        // 1. Create Default Current User
        val currentUser = UserEntity(
            userId = "current_user_id",
            name = "يحيى أحمد",
            email = "yahiaahmedseid1@gmail.com",
            profilePic = "",
            status = "متاح للحديث والدردشة 💬",
            isCurrentUser = true,
            isOnline = true,
            notificationsEnabled = true,
            darkModeEnabled = false,
            languageCode = "ar",
            privacyLastSeen = "all",
            privacyProfilePic = "all",
            privacyReadReceipts = true
        )
        appDao.insertUser(currentUser)

        // 2. Create Friend Users
        val friend1 = UserEntity(
            userId = "user_ahmed",
            name = "أحمد محمد",
            email = "ahmed@example.com",
            profilePic = "",
            status = "متاح ومستعد للحديث 🚀",
            isCurrentUser = false,
            isOnline = true
        )
        val friend2 = UserEntity(
            userId = "user_sara",
            name = "سارة أحمد",
            email = "sara@example.com",
            profilePic = "",
            status = "في العمل ☕ لا تتردد في مراسلتي",
            isCurrentUser = false,
            isOnline = true
        )
        val friend3 = UserEntity(
            userId = "user_ali",
            name = "محمد علي",
            email = "ali@example.com",
            profilePic = "",
            status = "مشغول في البرمجة 🔕",
            isCurrentUser = false,
            isOnline = false
        )
        val friend4 = UserEntity(
            userId = "user_layla",
            name = "ليلى حسن",
            email = "layla@example.com",
            profilePic = "",
            status = "خارج التغطية ✈️ في إجازة",
            isCurrentUser = false,
            isOnline = false
        )
        val friend5 = UserEntity(
            userId = "user_fatima",
            name = "فاطمة عمر",
            email = "fatima@example.com",
            profilePic = "",
            status = "أهلاً بالجميع!",
            isCurrentUser = false,
            isOnline = true
        )
        appDao.insertUsers(listOf(friend1, friend2, friend3, friend4, friend5))

        // 3. Insert Friendships (Accepted & Requests)
        appDao.insertFriendship(FriendshipEntity("friendship_ahmed", "current_user_id", "user_ahmed", "accepted"))
        appDao.insertFriendship(FriendshipEntity("friendship_sara", "current_user_id", "user_sara", "accepted"))
        appDao.insertFriendship(FriendshipEntity("friendship_ali", "current_user_id", "user_ali", "accepted"))
        appDao.insertFriendship(FriendshipEntity("friendship_layla", "current_user_id", "user_layla", "accepted"))
        appDao.insertFriendship(FriendshipEntity("friendship_fatima", "user_fatima", "current_user_id", "pending"))

        // 4. Create Chats
        val chatAhmed = ChatEntity(
            chatId = "current_user_id_user_ahmed",
            name = "أحمد محمد",
            isGroup = false,
            memberIdsString = "current_user_id,user_ahmed",
            lastMessage = "رائع جداً! أرسل لي لقطة شاشة عندما تستطيع.",
            lastMessageTime = System.currentTimeMillis() - 60000 * 5, // 5 minutes ago
            unreadCount = 2,
            isFavorite = false
        )
        val chatSara = ChatEntity(
            chatId = "current_user_id_user_sara",
            name = "سارة أحمد",
            isGroup = false,
            memberIdsString = "current_user_id,user_sara",
            lastMessage = "حسناً، سأكون جاهزة في الوقت المحدد 👍",
            lastMessageTime = System.currentTimeMillis() - 60000 * 30, // 30 minutes ago
            unreadCount = 0,
            isFavorite = true
        )
        val chatGroup = ChatEntity(
            chatId = "group_work_friends",
            name = "مجموعة العمل 👥",
            isGroup = true,
            adminId = "user_ahmed",
            memberIdsString = "current_user_id,user_ahmed,user_sara,user_ali",
            lastMessage = "أهلاً يا شباب، هل بدأنا العمل على الكود الجديد؟",
            lastMessageTime = System.currentTimeMillis() - 60000 * 120, // 2 hours ago
            unreadCount = 0,
            isFavorite = false
        )
        appDao.insertChats(listOf(chatAhmed, chatSara, chatGroup))

        // 5. Insert Message Histories
        // Ahmed messages
        appDao.insertMessage(MessageEntity("msg_ahmed_1", "current_user_id_user_ahmed", "user_ahmed", "أحمد محمد", "السلام عليكم يا يحيى! كيف حالك؟", System.currentTimeMillis() - 60000 * 60))
        appDao.insertMessage(MessageEntity("msg_ahmed_2", "current_user_id_user_ahmed", "current_user_id", "يحيى أحمد", "وعليكم السلام يا أحمد! الحمد لله بخير، كيف أمورك؟", System.currentTimeMillis() - 60000 * 50))
        appDao.insertMessage(MessageEntity("msg_ahmed_3", "current_user_id_user_ahmed", "user_ahmed", "أحمد محمد", "الحمد لله كله تمام. هل انتهيت من تصميم واجهة ماسنجر الجديدة؟", System.currentTimeMillis() - 60000 * 40))
        appDao.insertMessage(MessageEntity("msg_ahmed_4", "current_user_id_user_ahmed", "current_user_id", "يحيى أحمد", "نعم، لقد انتهيت منها للتو! تبدو رائعة وتدعم اللغة العربية بالكامل والوضع الداكن.", System.currentTimeMillis() - 60000 * 30))
        appDao.insertMessage(MessageEntity("msg_ahmed_5", "current_user_id_user_ahmed", "user_ahmed", "أحمد محمد", "رائع جداً! أرسل لي لقطة شاشة عندما تستطيع.", System.currentTimeMillis() - 60000 * 5, reactionsString = "👍:current_user_id"))

        // Sara messages
        appDao.insertMessage(MessageEntity("msg_sara_1", "current_user_id_user_sara", "user_sara", "سارة أحمد", "مرحباً يحيى، هل هناك اجتماع اليوم لمناقشة التحديثات؟", System.currentTimeMillis() - 60000 * 120))
        appDao.insertMessage(MessageEntity("msg_sara_2", "current_user_id_user_sara", "current_user_id", "يحيى أحمد", "مرحباً سارة، نعم الاجتماع في تمام الساعة الواحدة ظهراً.", System.currentTimeMillis() - 60000 * 100))
        appDao.insertMessage(MessageEntity("msg_sara_3", "current_user_id_user_sara", "user_sara", "سارة أحمد", "حسناً، سأكون جاهزة في الوقت المحدد 👍", System.currentTimeMillis() - 60000 * 30, reactionsString = "❤️:current_user_id"))

        // Group messages
        appDao.insertMessage(MessageEntity("msg_group_1", "group_work_friends", "user_ali", "محمد علي", "مرحباً بالجميع! أتمنى لكم يوماً سعيداً.", System.currentTimeMillis() - 60000 * 240))
        appDao.insertMessage(MessageEntity("msg_group_2", "group_work_friends", "user_ahmed", "أحمد محمد", "أهلاً علي! يوماً سعيداً للجميع.", System.currentTimeMillis() - 60000 * 200))
        appDao.insertMessage(MessageEntity("msg_group_3", "group_work_friends", "user_sara", "سارة أحمد", "أهلاً يا شباب، هل بدأنا العمل على الكود الجديد؟", System.currentTimeMillis() - 60000 * 120))

        // 6. Insert Notifications
        appDao.insertNotification(NotificationEntity(
            notificationId = "notif_1",
            userId = "current_user_id",
            type = "friend_request",
            senderId = "user_fatima",
            senderName = "فاطمة عمر",
            message = "أرسلت لك فاطمة عمر طلب صداقة جديد.",
            timestamp = System.currentTimeMillis() - 60000 * 10
        ))
        appDao.insertNotification(NotificationEntity(
            notificationId = "notif_2",
            userId = "current_user_id",
            type = "new_message",
            senderId = "user_ahmed",
            senderName = "أحمد محمد",
            message = "أرسل لك أحمد محمد رسالة جديدة: 'رائع جداً! أرسل لي لقطة...'",
            chatId = "current_user_id_user_ahmed",
            timestamp = System.currentTimeMillis() - 60000 * 5
        ))
    }
}
