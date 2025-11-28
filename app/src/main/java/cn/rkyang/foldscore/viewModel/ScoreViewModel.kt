package cn.rkyang.foldscore.viewModel

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.rkyang.foldscore.data.AppDatabase
import cn.rkyang.foldscore.data.ScoreRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 颜色转换辅助
fun Color.toHex(): Long = value.toLong()

class ScoreViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).scoreDao()

    // 实时状态
    var leftScore by mutableIntStateOf(0)
    var rightScore by mutableIntStateOf(0)
    var leftName by mutableStateOf("Team A")
    var rightName by mutableStateOf("Team B")

    // 默认颜色
    var leftColor by mutableStateOf(Color(0xFFE3F2FD)) // 淡蓝
    var rightColor by mutableStateOf(Color(0xFFFFEBEE)) // 淡红

    // 历史记录流
    val historyList = dao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun incrementLeft() { leftScore++ }
    fun incrementRight() { rightScore++ }
    fun resetScore() { leftScore = 0; rightScore = 0 }

    // 保存当前比分到历史
    fun saveRecord() {
        viewModelScope.launch {
            dao.addAndClean(
                ScoreRecord(
                    leftName = leftName, rightName = rightName,
                    leftScore = leftScore, rightScore = rightScore
                )
            )
        }
    }
}