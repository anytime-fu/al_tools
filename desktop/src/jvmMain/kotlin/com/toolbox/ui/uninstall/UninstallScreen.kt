package com.toolbox.ui.uninstall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toolbox.ui.components.AppFilterChip
import com.toolbox.util.UninstallGuard

private data class Reason(
    val label: String,
    val hint: String,
    val commentLabel: String
)

private val REASONS = listOf(
    Reason("用得少", "把常用工具置顶、开机自启常驻托盘，也许能让你更常想起我们。", "怎样你会更常用它？"),
    Reason("功能不满足", "告诉我们你最需要的功能，它会进入我们的迭代计划。", "最希望增加的功能是"),
    Reason("占用空间大", "所有数据都在本地数据目录，卸载后可手动删除；也可以清理历史数据后继续使用。", "想先清理哪部分数据？"),
    Reason("遇到了 Bug", "描述一下问题，我们会尽快修复。", "问题描述"),
    Reason("找到了替代品", "能告诉我们替代品的名字吗？我们想学习它的优点。", "替代品是"),
    Reason("其他", "任何想法都欢迎，帮助我们做得更好。", "想说的话")
)

@Composable
fun UninstallScreen(
    onKeep: () -> Unit,
    onUninstallRequested: () -> Unit
) {
    var selected by remember { mutableStateOf<Reason?>(null) }
    var comment by remember { mutableStateOf("") }
    var uninstalling by remember { mutableStateOf(false) }
    var launchFailed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "先别急着走",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "卸载前，能用 30 秒告诉我们原因吗？你的每条反馈都会直接进入产品迭代。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (uninstalling) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (launchFailed) {
                            "未能自动启动卸载程序，请通过 系统设置 → 应用 完成卸载"
                        } else {
                            "感谢你的反馈，正在启动卸载程序…"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@Column
        }

        Text(
            text = "离开的原因是",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        REASONS.chunked(3).forEach { rowReasons ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowReasons.forEach { reason ->
                    AppFilterChip(
                        selected = selected == reason,
                        onClick = {
                            selected = if (selected == reason) null else reason
                        },
                        label = { Text(reason.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        selected?.let { reason ->
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = reason.hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(reason.commentLabel) },
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onKeep,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("先不卸载了，继续使用")
        }

        TextButton(
            onClick = {
                UninstallGuard.saveFeedback(selected?.label ?: "未选择", comment.trim())
                uninstalling = true
                val ok = UninstallGuard.restoreAndRunUninstaller()
                launchFailed = !ok
                onUninstallRequested()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "仍要卸载",
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "反馈仅保存在本地，不会上传",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}