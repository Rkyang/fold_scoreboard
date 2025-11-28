package cn.rkyang.foldscore

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import androidx.window.area.WindowAreaSession
import androidx.window.area.WindowAreaPresentationSessionCallback
import androidx.window.area.WindowAreaSessionPresenter
import androidx.window.core.ExperimentalWindowApi
import cn.rkyang.foldscore.ui.ConfigDialog
import cn.rkyang.foldscore.ui.HistoryDialog
import cn.rkyang.foldscore.ui.MainControlScreen
import cn.rkyang.foldscore.ui.ScorePresentation
import cn.rkyang.foldscore.viewModel.ScoreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

// 导入你的 UI 组件

class MainActivity : ComponentActivity(), WindowAreaPresentationSessionCallback {

    private val viewModel: ScoreViewModel by viewModels()
    private lateinit var windowAreaController: WindowAreaController

    // 👇 修正：我们要保存的是 Info 对象，因为需要它的 token
    private var rearDisplayInfo: WindowAreaInfo? = null

    private var currentSession: WindowAreaSession? = null
    private var scorePresentation: ScorePresentation? = null

    @OptIn(ExperimentalWindowApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        windowAreaController = WindowAreaController.getOrCreate()

        lifecycleScope.launch(Dispatchers.Main) {
            windowAreaController.windowAreaInfos
                .onEach { infoList ->
                    // 👇 修正：使用正确的常量 OPERATION_PRESENT_ON_AREA
                    // 这个常量代表：在第二个区域显示内容（Rear Display Presentation）
                    val possibleInfo = infoList.firstOrNull { info ->
                        info.getCapability(WindowAreaCapability.Operation.OPERATION_PRESENT_ON_AREA) != null
                    }
                    rearDisplayInfo = possibleInfo
                }
                .collect()
        }

        setContent {
            MaterialTheme {
                var showHistory by remember { mutableStateOf(false) }
                var showConfig by remember { mutableStateOf(false) }
                var isEditingLeft by remember { mutableStateOf(true) }

                val history by viewModel.historyList.collectAsState()

                MainControlScreen(
                    viewModel = viewModel,
                    onCastClick = { toggleRearDisplayMode() },
                    onHistoryClick = { showHistory = true },
                    onEditConfig = { isLeft ->
                        isEditingLeft = isLeft
                        showConfig = true
                    }
                )

                if (showHistory) {
                    HistoryDialog(history = history) { showHistory = false }
                }

                if (showConfig) {
                    ConfigDialog(
                        initialName = if(isEditingLeft) viewModel.leftName else viewModel.rightName,
                        onDismiss = { showConfig = false },
                        onConfirm = { name, color ->
                            if(isEditingLeft) {
                                viewModel.leftName = name
                                viewModel.leftColor = color
                            } else {
                                viewModel.rightName = name
                                viewModel.rightColor = color
                            }
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalWindowApi::class)
    private fun toggleRearDisplayMode() {
        if (currentSession != null) {
            currentSession?.close()
            currentSession = null
            return
        }

        // 👇 修正：使用 info 中的 token 来启动
        val info = rearDisplayInfo
        if (info != null) {
            Toast.makeText(this, "正在请求外屏权限...", Toast.LENGTH_SHORT).show()

            // 获取 token
            val token = info.token

            // 使用标准的 presentContentOnWindowArea 方法
            windowAreaController.presentContentOnWindowArea(
                token,             // 1. token (IBinder)
                this,              // 2. activity (Activity)
                mainExecutor,      // 3. executor (Executor)
                this               // 4. callback (WindowAreaSessionCallback)
            )
        } else {
            Toast.makeText(this, "当前状态不可用（请确保手机展开且支持后置显示）", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSessionStarted(session: WindowAreaSessionPresenter) {
        currentSession = session
        val rearDisplay = (session as? WindowAreaSessionPresenter)?.context?.display
            ?: session.context.display

        if (rearDisplay != null) {
            scorePresentation = ScorePresentation(this, rearDisplay, viewModel)
            scorePresentation?.show()
            Toast.makeText(this, "外屏已点亮", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onContainerVisibilityChanged(isVisible: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onSessionEnded(t: Throwable?) {
        scorePresentation?.dismiss()
        scorePresentation = null
        currentSession = null

        // 👇 增强：如果 t 不为空，说明是非正常终止
        if (t != null) {
            Log.e("RearDisplayError", "Window Area Session terminated unexpectedly!", t)
            Toast.makeText(this, "外屏连接中断或失败: ${t.localizedMessage ?: t.javaClass.simpleName}", Toast.LENGTH_LONG).show()
        }
    }
}