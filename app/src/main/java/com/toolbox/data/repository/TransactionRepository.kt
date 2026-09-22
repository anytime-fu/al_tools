package com.toolbox.data.repository

import com.toolbox.data.local.dao.CategoryDao
import com.toolbox.data.local.dao.TransactionDao
import com.toolbox.data.local.dao.CategoryTotal
import com.toolbox.data.local.entity.Category
import com.toolbox.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByType(type: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type)

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    fun getTotalByTypeAndDateRange(type: String, startDate: Long, endDate: Long): Flow<Double?> =
        transactionDao.getTotalByTypeAndDateRange(type, startDate, endDate)

    fun getCategoryTotals(type: String, startDate: Long, endDate: Long): Flow<List<CategoryTotal>> =
        transactionDao.getCategoryTotals(type, startDate, endDate)

    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.deleteTransaction(transaction)

    fun getCategoriesByType(type: String): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type)

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)
}
