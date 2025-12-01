package cn.rkyang.foldscore.ui

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import cn.rkyang.foldscore.viewModel.ScoreViewModel

/**
 * 专用于在外屏（Rear Display）展示比分的 Presentation。
 *
 * 关键点：
 * 1. 必须手动实现 LifecycleOwner 和 SavedStateRegistryOwner，
 * 否则 ComposeView 无法在 Presentation (Dialog) 中正常工作。
 * 2. 构造函数接收 themeResId，用于去除系统默认的 Dialog 浮窗样式。
 */
class ScorePresentation(
    context: Context,
    display: Display,
    private val viewModel: ScoreViewModel,
    themeResId: Int // 接收来自 MainActivity 的专用主题 ID
) : Presentation(context, display, themeResId), LifecycleOwner, SavedStateRegistryOwner {

    // --- 生命周期管理核心组件 ---
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 恢复生命周期状态
        savedStateRegistryController.performRestore(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        // 2. 设置 ComposeView
        // 我们不使用 setContentView(R.layout.xxx)，直接用 ComposeView
        val composeView = ComposeView(context).apply {
            // 关键：将 View 树与当前的生命周期拥有者绑定
            // 如果不写这两行，Compose 会报 "LifecycleOwner not found" 错误并崩溃
            setViewTreeLifecycleOwner(this@ScorePresentation)
            setViewTreeSavedStateRegistryOwner(this@ScorePresentation)

            // 销毁策略：当 View 从窗口分离时销毁 Composition
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                MaterialTheme {
                    // 监听 ViewModel 数据
                    val leftScore = viewModel.leftScore // 假设你的ViewModel里这是StateFlow，如果是MutableState直接用
                    val rightScore = viewModel.rightScore
                    // 注意：如果你的 ViewModel 用的是 mutableIntStateOf，则直接访问 viewModel.leftScore 即可
                    // 这里为了兼容性，假设是 StateFlow。如果是 Compose State，请去掉 .collectAsState()

                    // 下面演示直接读取 Compose State 的写法 (对应你之前提供的 ViewModel)
                    val lScore = viewModel.leftScore
                    val rScore = viewModel.rightScore
                    val lName = viewModel.leftName
                    val rName = viewModel.rightName
                    val lColor = viewModel.leftColor
                    val rColor = viewModel.rightColor

                    // 外屏 UI 布局：全屏、大字号、只读
                    Row(modifier = Modifier.fillMaxSize()) {
                        // 左队展示区
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(lColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = lName,
                                    fontSize = 40.sp, // 外屏名字稍微小点
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "$lScore",
                                    fontSize = 180.sp, // 超大比分
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }
                        }

                        // 中间分割线 (可选)
                        Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(Color.Black))

                        // 右队展示区
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(rColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = rName,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "$rScore",
                                    fontSize = 180.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // 设置内容视图
        setContentView(composeView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
    }

    // --- 必须手动分发生命周期事件 ---

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onStop() {
        super.onStop()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onSaveInstanceState() = super.onSaveInstanceState().also {
        savedStateRegistryController.performSave(it)
    }

    // --- 接口实现 ---

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
}