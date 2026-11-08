package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM medical_shifts ORDER BY sortPriority ASC, timestamp DESC")
    fun getAllShifts(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM medical_shifts WHERE shiftStatus = :status ORDER BY sortPriority ASC, timestamp DESC")
    fun getShiftsByStatus(status: String): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM medical_shifts WHERE firebaseDocId = :firebaseDocId LIMIT 1")
    suspend fun getShiftByFirebaseDocId(firebaseDocId: String): ShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shifts: List<ShiftEntity>)

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Query("DELETE FROM medical_shifts WHERE id = :id")
    suspend fun deleteShiftById(id: Int)

    @Query("SELECT COUNT(*) FROM medical_shifts")
    suspend fun getShiftCount(): Int
}
