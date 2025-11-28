package cn.rkyang.foldscore.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. 实体类
@Entity(tableName = "scores")
data class ScoreRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val leftName: String,
    val rightName: String,
    val leftScore: Int,
    val rightScore: Int,
    val timestamp: Long = System.currentTimeMillis()
)

// 2. DAO
@Dao
interface ScoreDao {
    @Query("SELECT * FROM scores ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ScoreRecord>>

    @Insert
    suspend fun insert(record: ScoreRecord)

    // 核心逻辑：保留最新的30条，删除其他的
    @Query("DELETE FROM scores WHERE id NOT IN (SELECT id FROM scores ORDER BY timestamp DESC LIMIT 30)")
    suspend fun cleanOldRecords()

    @Transaction
    suspend fun addAndClean(record: ScoreRecord) {
        insert(record)
        cleanOldRecords()
    }
}

// 3. Database
@Database(entities = [ScoreRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "score_db")
                    .build().also { INSTANCE = it }
            }
        }
    }
}