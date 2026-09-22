package com.toolbox.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object IdCardUtil {

    private val provinces = mapOf(
        "11" to "北京", "12" to "天津", "13" to "河北", "14" to "山西", "15" to "内蒙古",
        "21" to "辽宁", "22" to "吉林", "23" to "黑龙江",
        "31" to "上海", "32" to "江苏", "33" to "浙江", "34" to "安徽", "35" to "福建", "36" to "江西", "37" to "山东",
        "41" to "河南", "42" to "湖北", "43" to "湖南", "44" to "广东", "45" to "广西", "46" to "海南",
        "50" to "重庆", "51" to "四川", "52" to "贵州", "53" to "云南", "54" to "西藏",
        "61" to "陕西", "62" to "甘肃", "63" to "青海", "64" to "宁夏", "65" to "新疆",
        "71" to "台湾", "81" to "香港", "82" to "澳门", "91" to "国外"
    )

    data class IdCardInfo(
        val idNumber: String,
        val province: String,
        val birthday: LocalDate,
        val age: Int,
        val gender: String,
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    fun parseIdCard(idNumber: String): IdCardInfo {
        val trimmed = idNumber.trim()

        if (trimmed.length != 18) {
            return IdCardInfo(
                idNumber = trimmed,
                province = "",
                birthday = LocalDate.now(),
                age = 0,
                gender = "",
                isValid = false,
                errorMessage = "身份证号码必须为18位"
            )
        }

        if (!trimmed.matches(Regex("^[0-9Xx]+$"))) {
            return IdCardInfo(
                idNumber = trimmed,
                province = "",
                birthday = LocalDate.now(),
                age = 0,
                gender = "",
                isValid = false,
                errorMessage = "身份证号码格式错误，只能包含数字和X"
            )
        }

        // 验证校验码
        if (!validateCheckCode(trimmed)) {
            return IdCardInfo(
                idNumber = trimmed,
                province = "",
                birthday = LocalDate.now(),
                age = 0,
                gender = "",
                isValid = false,
                errorMessage = "身份证号码校验码错误"
            )
        }

        // 解析省份
        val provinceCode = trimmed.substring(0, 2)
        val province = provinces[provinceCode] ?: "未知"

        // 解析出生日期
        val birthdayStr = trimmed.substring(6, 14)
        val birthday = try {
            LocalDate.parse(birthdayStr, DateTimeFormatter.ofPattern("yyyyMMdd"))
        } catch (e: Exception) {
            return IdCardInfo(
                idNumber = trimmed,
                province = province,
                birthday = LocalDate.now(),
                age = 0,
                gender = "",
                isValid = false,
                errorMessage = "出生日期无效"
            )
        }

        // 验证出生日期
        if (birthday.isAfter(LocalDate.now()) || birthday.year < 1900) {
            return IdCardInfo(
                idNumber = trimmed,
                province = province,
                birthday = birthday,
                age = 0,
                gender = "",
                isValid = false,
                errorMessage = "出生日期不合理"
            )
        }

        // 计算年龄（需判断生日是否已过）
        val today = LocalDate.now()
        var age = today.year - birthday.year
        if (today.dayOfYear < birthday.dayOfYear) {
            age--
        }

        // 解析性别（第17位，奇数为男，偶数为女）
        val genderCode = trimmed[16].digitToInt()
        val gender = if (genderCode % 2 == 1) "男" else "女"

        return IdCardInfo(
            idNumber = trimmed,
            province = province,
            birthday = birthday,
            age = age,
            gender = gender,
            isValid = true
        )
    }

    private fun validateCheckCode(idNumber: String): Boolean {
        val weights = intArrayOf(7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
        val checkCodes = charArrayOf('1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2')

        var sum = 0
        for (i in 0..16) {
            sum += idNumber[i].digitToInt() * weights[i]
        }

        val checkCode = checkCodes[sum % 11]
        return idNumber[17].uppercaseChar() == checkCode
    }

    fun formatIdNumber(idNumber: String): String {
        val trimmed = idNumber.trim()
        if (trimmed.length != 18) return trimmed
        return "${trimmed.substring(0, 6)}********${trimmed.substring(14)}"
    }
}
