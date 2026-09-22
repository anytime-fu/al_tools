package com.toolbox.data.repository

import com.toolbox.data.local.dao.PasswordDao
import com.toolbox.data.local.entity.PasswordEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordRepository @Inject constructor(
    private val passwordDao: PasswordDao
) {
    fun getAllPasswords(): Flow<List<PasswordEntry>> = passwordDao.getAllPasswords()

    fun searchPasswords(query: String): Flow<List<PasswordEntry>> = passwordDao.searchPasswords(query)

    suspend fun getPasswordById(id: Long): PasswordEntry? = passwordDao.getPasswordById(id)

    suspend fun insertPassword(password: PasswordEntry): Long = passwordDao.insertPassword(password)

    suspend fun updatePassword(password: PasswordEntry) = passwordDao.updatePassword(password)

    suspend fun deletePassword(password: PasswordEntry) = passwordDao.deletePassword(password)
}
