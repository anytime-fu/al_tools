package com.toolbox.ai.tools

import kotlinx.serialization.Serializable

@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: ToolParameters
)

@Serializable
data class ToolParameters(
    val type: String = "object",
    val properties: Map<String, ToolProperty>,
    val required: List<String> = emptyList()
)

@Serializable
data class ToolProperty(
    val type: String,
    val description: String,
    val enum: List<String>? = null
)

@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)

@Serializable
data class ToolCallFunction(
    val name: String,
    val arguments: String
)

@Serializable
data class ToolResult(
    val toolCallId: String,
    val functionName: String,
    val result: String,
    val isError: Boolean = false
)

object ToolRegistry {
    private val noteTools = listOf(
        ToolDefinition(
            name = "create_note",
            description = "创建新笔记。当用户要求创建笔记、写笔记、记录内容时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "title" to ToolProperty(
                        type = "string",
                        description = "笔记标题"
                    ),
                    "content" to ToolProperty(
                        type = "string",
                        description = "笔记内容，支持Markdown格式"
                    )
                ),
                required = listOf("title", "content")
            )
        ),
        ToolDefinition(
            name = "search_notes",
            description = "搜索笔记。当用户要求查找笔记、搜索笔记时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "query" to ToolProperty(
                        type = "string",
                        description = "搜索关键词"
                    )
                ),
                required = listOf("query")
            )
        ),
        ToolDefinition(
            name = "get_recent_notes",
            description = "获取最近的笔记列表。当用户要求查看最近笔记、列出笔记时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "limit" to ToolProperty(
                        type = "integer",
                        description = "返回笔记数量，默认5条"
                    )
                )
            )
        )
    )

    private val transactionTools = listOf(
        ToolDefinition(
            name = "add_transaction",
            description = "添加记账记录。当用户要求记账、记录收入、记录支出时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "amount" to ToolProperty(
                        type = "number",
                        description = "金额"
                    ),
                    "type" to ToolProperty(
                        type = "string",
                        description = "类型：income(收入) 或 expense(支出)",
                        enum = listOf("income", "expense")
                    ),
                    "category" to ToolProperty(
                        type = "string",
                        description = "分类名称，如：餐饮、交通、工资等"
                    ),
                    "note" to ToolProperty(
                        type = "string",
                        description = "备注说明"
                    )
                ),
                required = listOf("amount", "type", "category")
            )
        ),
        ToolDefinition(
            name = "query_transactions",
            description = "查询记账记录。当用户要求查看账单、查询收支、查看消费时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "type" to ToolProperty(
                        type = "string",
                        description = "查询类型：all(全部)、income(收入)、expense(支出)",
                        enum = listOf("all", "income", "expense")
                    ),
                    "period" to ToolProperty(
                        type = "string",
                        description = "时间范围：today(今天)、week(本周)、month(本月)、year(本年)",
                        enum = listOf("today", "week", "month", "year")
                    )
                )
            )
        ),
        ToolDefinition(
            name = "get_expense_summary",
            description = "获取支出汇总。当用户要求统计消费、查看支出汇总、分析账单时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "period" to ToolProperty(
                        type = "string",
                        description = "时间范围：week(本周)、month(本月)、year(本年)",
                        enum = listOf("week", "month", "year")
                    )
                )
            )
        )
    )

    private val habitTools = listOf(
        ToolDefinition(
            name = "create_habit",
            description = "创建新习惯。当用户要求创建习惯、添加打卡项目时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "name" to ToolProperty(
                        type = "string",
                        description = "习惯名称"
                    ),
                    "icon" to ToolProperty(
                        type = "string",
                        description = "图标emoji，默认为⭐"
                    )
                ),
                required = listOf("name")
            )
        ),
        ToolDefinition(
            name = "check_habit",
            description = "习惯打卡。当用户要求打卡、完成习惯时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "habit_name" to ToolProperty(
                        type = "string",
                        description = "习惯名称"
                    )
                ),
                required = listOf("habit_name")
            )
        ),
        ToolDefinition(
            name = "get_habits",
            description = "获取习惯列表。当用户要求查看习惯、列出所有习惯时使用。",
            parameters = ToolParameters(
                properties = emptyMap()
            )
        )
    )

    private val todoTools = listOf(
        ToolDefinition(
            name = "add_todo",
            description = "添加待办事项。当用户要求添加待办、创建任务时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "title" to ToolProperty(
                        type = "string",
                        description = "待办事项标题"
                    )
                ),
                required = listOf("title")
            )
        ),
        ToolDefinition(
            name = "get_todos",
            description = "获取待办列表。当用户要求查看待办、列出任务时使用。",
            parameters = ToolParameters(
                properties = emptyMap()
            )
        )
    )

    private val passwordTools = listOf(
        ToolDefinition(
            name = "generate_password",
            description = "生成随机密码。当用户要求生成密码、创建密码时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "length" to ToolProperty(
                        type = "integer",
                        description = "密码长度，默认16位"
                    ),
                    "include_symbols" to ToolProperty(
                        type = "boolean",
                        description = "是否包含特殊符号，默认true"
                    )
                )
            )
        ),
        ToolDefinition(
            name = "save_password",
            description = "保存密码到密码管理。当用户要求记住密码、保存密码、把密码存进密码管理器时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "app_name" to ToolProperty(
                        type = "string",
                        description = "应用或网站名称，如：GitHub、邮箱"
                    ),
                    "account" to ToolProperty(
                        type = "string",
                        description = "账号"
                    ),
                    "password" to ToolProperty(
                        type = "string",
                        description = "密码；留空则自动生成一个16位强密码"
                    ),
                    "note" to ToolProperty(
                        type = "string",
                        description = "备注说明"
                    )
                ),
                required = listOf("app_name", "account")
            )
        ),
        ToolDefinition(
            name = "search_passwords",
            description = "搜索密码管理中的条目。当用户查找某个应用或网站的密码记录时使用。只返回应用、账号等信息，不返回明文密码。",
            parameters = ToolParameters(
                properties = mapOf(
                    "query" to ToolProperty(
                        type = "string",
                        description = "搜索关键词（应用名或账号）"
                    )
                ),
                required = listOf("query")
            )
        )
    )

    private val scheduleTools = listOf(
        ToolDefinition(
            name = "create_schedule",
            description = "创建日程。当用户要求安排日程、添加提醒、创建日历时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "title" to ToolProperty(
                        type = "string",
                        description = "日程标题"
                    ),
                    "date" to ToolProperty(
                        type = "string",
                        description = "日期，格式 YYYY-MM-DD，默认今天"
                    ),
                    "time" to ToolProperty(
                        type = "string",
                        description = "时间，格式 HH:mm，如 14:30"
                    ),
                    "description" to ToolProperty(
                        type = "string",
                        description = "日程描述"
                    )
                ),
                required = listOf("title")
            )
        ),
        ToolDefinition(
            name = "get_schedules",
            description = "查询某天的日程。当用户询问今天有什么安排、某天的日程时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "date" to ToolProperty(
                        type = "string",
                        description = "日期，格式 YYYY-MM-DD，默认今天"
                    )
                )
            )
        ),
        ToolDefinition(
            name = "complete_schedule",
            description = "标记日程为已完成。当用户说日程做完了、完成了某个安排时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "title" to ToolProperty(
                        type = "string",
                        description = "日程标题（可部分匹配）"
                    )
                ),
                required = listOf("title")
            )
        )
    )

    private val calculatorTools = listOf(
        ToolDefinition(
            name = "calculate",
            description = "数学计算。当用户要求计算数学表达式时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "expression" to ToolProperty(
                        type = "string",
                        description = "数学表达式，如：2+3*4、sqrt(16)、sin(30)"
                    )
                ),
                required = listOf("expression")
            )
        ),
        ToolDefinition(
            name = "unit_convert",
            description = "单位换算。当用户要求单位转换时使用。",
            parameters = ToolParameters(
                properties = mapOf(
                    "value" to ToolProperty(
                        type = "number",
                        description = "数值"
                    ),
                    "from_unit" to ToolProperty(
                        type = "string",
                        description = "源单位"
                    ),
                    "to_unit" to ToolProperty(
                        type = "string",
                        description = "目标单位"
                    )
                ),
                required = listOf("value", "from_unit", "to_unit")
            )
        )
    )

    val allTools: List<ToolDefinition> = noteTools + transactionTools + habitTools + todoTools + passwordTools + scheduleTools + calculatorTools

    fun getToolsByCategory(category: String): List<ToolDefinition> {
        return when (category) {
            "note" -> noteTools
            "transaction" -> transactionTools
            "habit" -> habitTools
            "todo" -> todoTools
            "password" -> passwordTools
            "schedule" -> scheduleTools
            "calculator" -> calculatorTools
            else -> emptyList()
        }
    }

    fun getToolByName(name: String): ToolDefinition? {
        return allTools.find { it.name == name }
    }
}
