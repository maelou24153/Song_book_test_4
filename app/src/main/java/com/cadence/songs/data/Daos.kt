package com.cadence.songs.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :id")
    fun observe(id: Long): Flow<Song?>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun get(id: Long): Song?

    @Query("SELECT * FROM songs")
    suspend fun all(): List<Song>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int

    @Insert
    suspend fun insert(song: Song): Long

    @Update
    suspend fun update(song: Song)

    @Delete
    suspend fun delete(song: Song)
}

@Dao
interface SetListDao {
    @Query("SELECT * FROM setlists ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SetList>>

    @Query("SELECT * FROM setlists WHERE id = :id")
    fun observe(id: Long): Flow<SetList?>

    @Query(
        "SELECT s.*, e.id AS entryId, e.position AS position " +
            "FROM songs s INNER JOIN set_entries e ON e.songId = s.id " +
            "WHERE e.setListId = :setListId ORDER BY e.position"
    )
    fun observeEntries(setListId: Long): Flow<List<EntryWithSong>>

    @Query("SELECT setListId, COUNT(*) AS count FROM set_entries GROUP BY setListId")
    fun observeCounts(): Flow<List<SetCount>>

    @Insert
    suspend fun insert(setList: SetList): Long

    @Update
    suspend fun update(setList: SetList)

    @Delete
    suspend fun delete(setList: SetList)

    @Insert
    suspend fun insertEntries(entries: List<SetEntry>)

    @Query("DELETE FROM set_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("SELECT * FROM set_entries WHERE setListId = :setListId ORDER BY position")
    suspend fun entries(setListId: Long): List<SetEntry>

    @Query("UPDATE set_entries SET position = :position WHERE id = :entryId")
    suspend fun setPosition(entryId: Long, position: Int)

    @Query("SELECT COALESCE(MAX(position), -1) FROM set_entries WHERE setListId = :setListId")
    suspend fun maxPosition(setListId: Long): Int

    @Query("SELECT * FROM setlists")
    suspend fun allSetLists(): List<SetList>
}
