package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RatingDao {
    @Query("SELECT * FROM ratings ORDER BY timestamp DESC")
    fun getAllRatings(): Flow<List<RatingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRating(rating: RatingEntity)

    @Query("DELETE FROM ratings WHERE id = :id")
    suspend fun deleteRatingById(id: Int)

    @Query("DELETE FROM ratings")
    suspend fun clearAllRatings()
}
