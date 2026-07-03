package com.example.data.repository

import com.example.data.database.RatingDao
import com.example.data.database.RatingEntity
import kotlinx.coroutines.flow.Flow

class RatingRepository(private val ratingDao: RatingDao) {
    val allRatings: Flow<List<RatingEntity>> = ratingDao.getAllRatings()

    suspend fun insert(rating: RatingEntity) {
        ratingDao.insertRating(rating)
    }

    suspend fun deleteById(id: Int) {
        ratingDao.deleteRatingById(id)
    }

    suspend fun clearAll() {
        ratingDao.clearAllRatings()
    }
}
