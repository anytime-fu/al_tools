package com.toolbox.ai.tools

import com.google.gson.annotations.SerializedName

// Function Calling 工具定义
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: ToolParameters
)

data class ToolParameters(
    val type: String = "object",
    val properties: Map<String, ToolProperty>,
    val required: List<String> = emptyList()
)

data class ToolProperty(
    val type: String,
    val description: String,
    val enum: List<String>? = null
)

// 工具调用请求
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)

data class ToolCallFunction(
    val name: String,
    val arguments: String
)

// 工具执行结果
data class ToolResult(
    val toolCallId: String,
    val functionName: String,
    val result: String,
    val isError: Boolean = false
)

// 工具定义集合
object ToolRegistry {

    // 笔记相关工具
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

    // 记账相关工具
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

    // 习惯打卡相关工具
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

    // 待办事项工具
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

    // 密码管理工具
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
        )
    )

    // 计算工具
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

    // 所有工具集合
    val allTools: List<ToolDefinition> = noteTools + transactionTools + habitTools + todoTools + passwordTools + calculatorTools

    // 按类别获取工具
    fun getToolsByCategory(category: String): List<ToolDefinition> {
        return when (category) {
            "note" -> noteTools
            "transaction" -> transactionTools
            "habit" -> habitTools
            "todo" -> todoTools
            "password" -> passwordTools
            "calculator" -> calculatorTools
            else -> emptyList()
        }
    }

    // 根据工具名查找工具定义
    fun getToolByName(name: String): ToolDefinition? {
        return allTools.find { it.name == name }
    }
}
