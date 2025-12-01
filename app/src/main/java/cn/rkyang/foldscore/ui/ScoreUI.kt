package cn.rkyang.foldscore.ui


import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cn.rkyang.foldscore.data.ScoreRecord
import cn.rkyang.foldscore.viewModel.ScoreViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.ui.platform.LocalContext
// 确保包含 ScreenRotation 图标
import androidx.compose.material.icons.filled.ScreenRotation

// --- 组件：单个队伍的计分面板 ---
@Composable
fun TeamPanel(
    modifier: Modifier,
    name: String,
    score: Int,
    bgColor: Color,
    onScoreClick: () -> Unit,
    onNameChange: (String) -> Unit, // 简化版，实际可用Dialog修改
    onColorChange: () -> Unit // 简化版
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(bgColor)
            .clickable { onScoreClick() }, // 点击整个区域加分
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "$score", fontSize = 120.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onScoreClick) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Text(" 加分")
        }
    }
}

// --- 核心：主界面 (内屏控制) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainControlScreen(
    viewModel: ScoreViewModel,
    onCastClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onEditConfig: (Boolean) -> Unit // Boolean: isLeft
) {
    // 1. 获取当前上下文并转换为 Activity
    val context = LocalContext.current
    val activity = context as? Activity
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fold 计分板") },
                actions = {
                    IconButton(onClick = {
                        if (activity != null) {
                            // 判断当前是否是横屏，如果是则切回竖屏(或自动)，否则强制横屏
                            if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                // 设为 UNSPECIFIED 让系统根据传感器或折叠状态决定，或者用 SCREEN_ORIENTATION_PORTRAIT 强制竖屏
                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            } else {
                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        }
                    }) {
                        // 旋转图标
                        Icon(Icons.Default.ScreenRotation, "Rotate Screen")
                    }
                    IconButton(onClick = { viewModel.resetScore() }) {
                        Icon(Icons.Default.Refresh, "Reset")
                    }
                    IconButton(onClick = onCastClick) {
                        Icon(Icons.Default.Cast, "Cast to Outer Screen")
                    }
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Default.History, "History")
                    }
                    IconButton(onClick = { viewModel.saveRecord() }) {
                        Icon(Icons.Default.Save, "Save")
                    }
                }
            )
        }
    ) { padding ->
        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            // 左队
            TeamPanel(
                modifier = Modifier.weight(1f),
                name = viewModel.leftName,
                score = viewModel.leftScore,
                bgColor = viewModel.leftColor,
                onScoreClick = { viewModel.incrementLeft() },
                onNameChange = { /* 触发修改逻辑 */ },
                onColorChange = { /* 触发修改逻辑 */ }
            )
            // 分割线
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
            // 右队
            TeamPanel(
                modifier = Modifier.weight(1f),
                name = viewModel.rightName,
                score = viewModel.rightScore,
                bgColor = viewModel.rightColor,
                onScoreClick = { viewModel.incrementRight() },
                onNameChange = {},
                onColorChange = {}
            )
        }
    }
}

// --- 弹窗：配置名字和颜色 (简化实现) ---
@Composable
fun ConfigDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Color) -> Unit
) {
    var text by remember { mutableStateOf(initialName) }
    // 简单预设几个颜色
    val colors = listOf(Color(0xFFE3F2FD), Color(0xFFFFEBEE), Color(0xFFE8F5E9), Color(0xFFFFF3E0))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置队伍信息") },
        text = {
            Column {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("队名") })
                Spacer(modifier = Modifier.height(16.dp))
                Text("选择背景色:")
                Row {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(4.dp)
                                .background(color)
                                .clickable { onConfirm(text, color); onDismiss() }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

// --- 弹窗：历史记录 ---
@Composable
fun HistoryDialog(history: List<ScoreRecord>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, modifier = Modifier.height(500.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("最近30场记录", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(history) { record ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("${record.leftName} vs ${record.rightName}", fontWeight = FontWeight.Bold)
                                    Text(SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(record.timestamp)))
                                }
                                Text("${record.leftScore} : ${record.rightScore}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 外屏展示逻辑 (Presentation) ---
// 这是一个特殊的类，用于在第二块屏幕上绘制 UI
//class ScorePresentation(
//    context: Context,
//    display: Display,
//    private val viewModel: ScoreViewModel,
//    themeFoldscorePresentation: Int
//) : Presentation(context, display) {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        // Presentation 也是一个 Dialog，可以直接setContent使用Compose
//        setContentView(ComposeView(context).apply {
//            setContent {
//                MaterialTheme {
//                    // 外屏只展示，不可交互，字号特大
//                    Row(modifier = Modifier.fillMaxSize()) {
//                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(viewModel.leftColor), contentAlignment = Alignment.Center) {
//                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                                Text(viewModel.leftName, fontSize = 30.sp)
//                                Text("${viewModel.leftScore}", fontSize = 200.sp, fontWeight = FontWeight.Black)
//                            }
//                        }
//                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(viewModel.rightColor), contentAlignment = Alignment.Center) {
//                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                                Text(viewModel.rightName, fontSize = 30.sp)
//                                Text("${viewModel.rightScore}", fontSize = 200.sp, fontWeight = FontWeight.Black)
//                            }
//                        }
//                    }
//                }
//            }
//        })
//    }
//}