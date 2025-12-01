package cn.rkyang.foldscore

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat // 确保引入这个
import androidx.lifecycle.lifecycleScope
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo // 关键引入
import androidx.window.area.WindowAreaPresentationSessionCallback
import androidx.window.area.WindowAreaSessionPresenter
import androidx.window.core.ExperimentalWindowApi
import cn.rkyang.foldscore.ui.ConfigDialog
import cn.rkyang.foldscore.ui.HistoryDialog
import cn.rkyang.foldscore.ui.MainControlScreen
import cn.rkyang.foldscore.ui.ScorePresentation
import cn.rkyang.foldscore.viewModel.ScoreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class MainActivity : ComponentActivity(), WindowAreaPresentationSessionCallback {

    private val viewModel: ScoreViewModel by viewModels()
    private lateinit var windowAreaController: WindowAreaController
    private lateinit var displayExecutor: Executor

    // 👇 修正 1: 变量类型改为 WindowAreaInfo (它里面才有 token)
    private var rearDisplayInfo: WindowAreaInfo? = null

    private var currentSession: WindowAreaSessionPresenter? = null
    private var scorePresentation: ScorePresentation? = null

    @OptIn(ExperimentalWindowApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayExecutor = ContextCompat.getMainExecutor(this)
        windowAreaController = WindowAreaController.getOrCreate()

        lifecycleScope.launch(Dispatchers.Main) {
            windowAreaController.windowAreaInfos
                .map { infoList ->
                    // 👇 修正 2: 寻找包含 "后置显示能力" 的那个 Info 对象
                    infoList.firstOrNull { info ->
                        info.getCapability(WindowAreaCapability.Operation.OPERATION_PRESENT_ON_AREA) != null
                    }
                }
                .distinctUntilChanged()
                .onEach { info ->
                    // 👇 修正 3: 直接保存 Info 对象
                    rearDisplayInfo = info
                    // 调试日志：确认是否获取到了 Info
                    if (info != null) {
                        Log.d("FoldScore", "检测到后置显示能力，Token: ${info.token}")
                    }
                }
                .collect { }
        }

        setContent {
            MaterialTheme {
                var showHistory by remember { mutableStateOf(false) }
                var showConfig by remember { mutableStateOf(false) }
                var isEditingLeft by remember { mutableStateOf(true) }

                val history by viewModel.historyList.collectAsState()

                MainControlScreen(
                    viewModel = viewModel,
                    onCastClick = { toggleDualScreenMode() },
                    onHistoryClick = { showHistory = true },
                    onEditConfig = { isLeft ->
                        isEditingLeft = isLeft
                        showConfig = true
                    }
                )

                // 假设历史记录弹窗的显示状态是 showHistory
                if (showHistory) {
                    HistoryDialog(
                        history = history,
                        onDismiss = { showHistory = false },
                        // 👇 传入 ViewModel 的删除函数
                        onDelete = { recordId ->
                            viewModel.deleteRecordById(recordId)
                        }
                    )
                }

                if (showConfig) {
                    ConfigDialog(
                        // 👇 传入当前颜色
                        initialName = if(isEditingLeft) viewModel.leftName else viewModel.rightName,
                        initialColor = if(isEditingLeft) viewModel.leftColor else viewModel.rightColor,
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
    private fun toggleDualScreenMode() {
        if (currentSession != null) {
            currentSession?.close()
            currentSession = null
            return
        }

        // 👇 修正 4: 使用 rearDisplayInfo
        val info = rearDisplayInfo
        if (info != null) {
            Toast.makeText(this, "正在请求外屏权限...", Toast.LENGTH_SHORT).show()
            try {
                // 👇 修正 5: 从 Info 对象中获取 token
                windowAreaController.presentContentOnWindowArea(
                    info.token,
                    this,
                    displayExecutor,
                    this
                )
            } catch (e: Exception) {
                Log.e("FoldScore", "API 调用失败", e)
                Toast.makeText(this, "调用失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "未检测到外屏可用状态 (请确保手机展开)", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSessionStarted(session: WindowAreaSessionPresenter) {
        currentSession = session
        val rearContext = session.context
        val rearDisplay = rearContext.display // 从 Context 获取 Display

        if (rearDisplay != null) {
            // 务必确保 R.style.Theme_FoldScore_Presentation 在 themes.xml 中已定义
            scorePresentation = ScorePresentation(
                rearContext,
                rearDisplay,
                viewModel,
                R.style.Theme_FoldScore_Presentation
            )

            try {
                scorePresentation?.show()
                Toast.makeText(this, "外屏已点亮", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("FoldScore", "Presentation 显示错误", e)
                session.close()
            }
        }
    }

    override fun onSessionEnded(t: Throwable?) {
        scorePresentation?.dismiss()
        scorePresentation = null
        currentSession = null
        if (t != null) {
            Log.e("FoldScore", "会话异常结束: ${t.message}", t)
            Toast.makeText(this, "会话结束: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onContainerVisibilityChanged(isVisible: Boolean) {
        Log.d("FoldScore", "外屏可见性: $isVisible")
    }
}