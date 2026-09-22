package com.toolbox.data.repository

import com.toolbox.data.local.dao.ChatDao
import com.toolbox.data.local.entity.ChatSession
import com.toolbox.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao
) {
    fun getAllSessions(): Flow<List<ChatSession>> = chatDao.getAllSessions()

    suspend fun getSessionById(id: Long): ChatSession? = chatDao.getSessionById(id)

    suspend fun insertSession(session: ChatSession): Long = chatDao.insertSession(session)

    suspend fun updateSession(session: ChatSession) = chatDao.updateSession(session)

    suspend fun deleteSession(session: ChatSession) = chatDao.deleteSession(session)

    suspend fun deleteSessionById(id: Long) = chatDao.deleteSessionById(id)

    fun getMessagesBySessionId(sessionId: Long): Flow<List<ChatMessageEntity>> = 
        chatDao.getMessagesBySessionId(sessionId)

    suspend fun insertMessage(message: ChatMessageEntity): Long = chatDao.insertMessage(message)

    suspend fun deleteMessage(message: ChatMessageEntity) = chatDao.deleteMessage(message)

    suspend fun deleteMessagesBySessionId(sessionId: Long) = chatDao.deleteMessagesBySessionId(sessionId)
}
