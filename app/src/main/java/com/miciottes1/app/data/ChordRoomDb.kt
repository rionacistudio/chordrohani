package com.miciottes1.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tb_chord", primaryKeys = ["judul", "penyanyi"])
data class SongEntity(
    @ColumnInfo(name = "judul") val judul: String,
    @ColumnInfo(name = "penyanyi") val penyanyi: String,
    @ColumnInfo(name = "base_key") val base_key: String,
    @ColumnInfo(name = "album") val album: String = "",
    @ColumnInfo(name = "album_image") val album_image: String = "",
    @ColumnInfo(name = "lastmod") val lastmod: String = "",
    @ColumnInfo(name = "isi_chord") val isi_chord: String = "",
    @ColumnInfo(name = "language") val language: String = "",
)

@Dao
interface ChordDao {

    @Query("SELECT judul, penyanyi, base_key, album, album_image, lastmod, language, '' AS isi_chord FROM tb_chord ORDER BY judul COLLATE NOCASE")
    fun getAllSongsFlow(): Flow<List<SongEntity>>

    @Query("SELECT judul, penyanyi, base_key, album, album_image, lastmod, language, '' AS isi_chord FROM tb_chord ORDER BY judul COLLATE NOCASE")
    suspend fun getAllSongs(): List<SongEntity>

    @Query("SELECT COUNT(*) FROM tb_chord")
    suspend fun count(): Int

    @Query("SELECT MAX(lastmod) FROM tb_chord")
    suspend fun getLastSync(): String?

    @Query("SELECT * FROM tb_chord WHERE judul = :judul AND penyanyi = :penyanyi LIMIT 1")
    suspend fun getDetail(judul: String, penyanyi: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("DELETE FROM tb_chord")
    suspend fun clear()
}

@Database(entities = [SongEntity::class], version = 3, exportSchema = false)
abstract class ChordRoomDb : RoomDatabase() {
    abstract fun chordDao(): ChordDao

    companion object {
        @Volatile
        private var INSTANCE: ChordRoomDb? = null

        fun get(context: Context): ChordRoomDb {
            return INSTANCE ?: synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    ChordRoomDb::class.java,
                    "chordku.db",
                ).fallbackToDestructiveMigration().build()
                INSTANCE = inst
                inst
            }
        }
    }
}
