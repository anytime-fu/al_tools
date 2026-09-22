package com.toolbox.data.repository

import com.toolbox.data.local.dao.FolderDao
import com.toolbox.data.local.entity.Folder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(
    private val folderDao: FolderDao
) {
    fun getAllFolders(): Flow<List<Folder>> = folderDao.getAllFolders()

    suspend fun getFolderById(id: Long): Folder? = folderDao.getFolderById(id)

    suspend fun insertFolder(folder: Folder): Long = folderDao.insertFolder(folder)

    suspend fun updateFolder(folder: Folder) = folderDao.updateFolder(folder)

    suspend fun deleteFolder(folder: Folder) = folderDao.deleteFolder(folder)
}
