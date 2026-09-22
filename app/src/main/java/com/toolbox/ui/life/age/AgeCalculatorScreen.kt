package com.toolbox.ui.life.age

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeCalculatorScreen(
    onBack: () -> Unit
) {
    var birthYear by remember { mutableStateOf("1990") }
    var birthMonth by remember { mutableStateOf("1") }
    var birthDay by remember { mutableStateOf("1") }
    var result by remember { mutableStateOf<AgeResult?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("年龄计算") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Cake,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("出生日期", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = birthYear,
                    onValueChange = { birthYear = it },
                    label = { Text("年") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = birthMonth,
                    onValueChange = { birthMonth = it },
                    label = { Text("月") },
                    modifier = Modifier.width(80.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = birthDay,
                    onValueChange = { birthDay = it },
                    label = { Text("日") },
                    modifier = Modifier.width(80.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val year = birthYear.toIntOrNull() ?: return@Button
                    val month = birthMonth.toIntOrNull() ?: return@Button
                    val day = birthDay.toIntOrNull() ?: return@Button

                    result = calculateAge(year, month, day)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("计算")
            }

            Spacer(modifier = Modifier.height(24.dp))

            result?.let { r ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("计算结果", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        AgeInfoRow("年龄", "${r.years} 岁")
                        AgeInfoRow("月数", "${r.months} 个月")
                        AgeInfoRow("天数", "${r.days} 天")
                        AgeInfoRow("小时", "${r.hours} 小时")
                        AgeInfoRow("分钟", "${r.minutes} 分钟")

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("距离下次生日", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        AgeInfoRow("剩余", "${r.daysUntilBirthday} 天")
                    }
                }
            }
        }
    }
}

@Composable
private fun AgeInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

data class AgeResult(
    val years: Int,
    val months: Int,
    val days: Int,
    val hours: Long,
    val minutes: Long,
    val daysUntilBirthday: Int
)

private fun calculateAge(year: Int, month: Int, day: Int): AgeResult {
    val birth = Calendar.getInstance().apply {
        set(year, month - 1, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val now = Calendar.getInstance()

    var years = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
    var months = now.get(Calendar.MONTH) - birth.get(Calendar.MONTH)
    var days = now.get(Calendar.DAY_OF_MONTH) - birth.get(Calendar.DAY_OF_MONTH)

    if (days < 0) {
        months--
        val prevMonth = now.clone() as Calendar
        prevMonth.add(Calendar.MONTH, -1)
        days += prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    if (months < 0) {
        years--
        months += 12
    }

    val diffMillis = now.timeInMillis - birth.timeInMillis
    val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)

    // Days until next birthday
    val nextBirthday = Calendar.getInstance().apply {
        set(Calendar.YEAR, now.get(Calendar.YEAR))
        set(Calendar.MONTH, birth.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, birth.get(Calendar.DAY_OF_MONTH))
    }
    if (nextBirthday.before(now)) {
        nextBirthday.add(Calendar.YEAR, 1)
    }
    val daysUntilBirthday = ((nextBirthday.timeInMillis - now.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

    return AgeResult(years, months, days, hours, minutes, daysUntilBirthday)
}
