package com.example.data.repository

import com.example.data.database.TaskDao
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    fun getTasksForDate(date: String): Flow<List<Task>> = taskDao.getTasksForDate(date)

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    suspend fun getUnfinishedTasks(): List<Task> = taskDao.getUnfinishedTasks()

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)
}
