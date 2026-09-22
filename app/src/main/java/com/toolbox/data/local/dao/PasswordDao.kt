package com.toolbox.data.local.dao

import androidx.room.*
import com.toolbox.data.local.entity.PasswordEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM password_entries ORDER BY updatedAt DESC")
    fun getAllPasswords(): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM password_entries WHERE id = :id")
    suspend fun getPasswordById(id: Long): PasswordEntry?

    @Query("SELECT * FROM password_entries WHERE appName LIKE '%' || :query || '%' OR account LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchPasswords(query: String): Flow<List<PasswordEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: PasswordEntry): Long

    @Update
    suspend fun updatePassword(password: PasswordEntry)

    @Delete
    suspend fun deletePassword(password: PasswordEntry)
}
