package com.toolbox.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object TestDataGen {

    enum class Gender { MALE, FEMALE, RANDOM }

    private val AREA_CODES = listOf(
        "110101", "110102", "110105", "110108",
        "310101", "310104", "310115",
        "440106", "440305", "440604",
        "330106", "330203",
        "320102", "320505",
        "510104", "510107",
        "420106", "420111",
        "610103", "610113",
        "120101", "120104",
        "500101", "500103",
        "210102", "210202",
        "370102", "370202"
    )

    private val SURNAMES = listOf(
        "王", "李", "张", "刘", "陈", "杨", "黄", "赵", "吴", "周",
        "徐", "孙", "马", "朱", "胡", "郭", "何", "高", "林", "罗",
        "郑", "梁", "谢", "宋", "唐", "许", "邓", "冯", "韩", "曹"
    )

    private val MALE_GIVEN = listOf(
        "伟", "强", "磊", "军", "洋", "勇", "杰", "涛", "明", "超",
        "刚", "平", "辉", "健", "广", "志", "兴", "良", "海", "山",
        "俊", "浩", "宇", "轩", "睿", "泽", "博", "文", "昊", "然"
    )

    private val FEMALE_GIVEN = listOf(
        "芳", "娜", "敏", "静", "丽", "艳", "娟", "霞", "萍", "燕",
        "彩", "春", "菊", "兰", "凤", "洁", "梅", "琳", "雪", "爱",
        "妹", "香", "月", "莺", "媛", "佳", "嘉", "琼", "珍", "莉"
    )

    private val PHONE_PREFIXES = listOf(
        "130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
        "150", "151", "152", "155", "156", "157", "158", "159",
        "176", "177", "178", "180", "181", "182", "183", "185", "186", "187", "188", "189",
        "191", "198", "199"
    )

    private val EMAIL_DOMAINS = listOf(
        "163.com", "126.com", "qq.com", "gmail.com", "outlook.com", "example.com"
    )

    private val EMAIL_NAME_CHARS = "abcdefghijklmnopqrstuvwxyz"

    private val LOREM_SENTENCES = listOf(
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris.",
        "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum.",
        "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia.",
        "Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit.",
        "Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet.",
        "Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit.",
        "Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil.",
        "At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis."
    )

    private val CHINESE_SENTENCES = listOf(
        "这是一段用于排版测试的占位文本，内容没有实际含义。",
        "在设计与开发过程中，假文可以帮助评估版面的视觉平衡。",
        "当真实文案尚未准备好时，占位文字能够支撑页面结构的推进。",
        "请勿将此处的文字用于正式发布内容，它仅用于演示与测试。",
        "合理的行长与段落间距，能显著提升长文本的阅读体验。",
        "设计师通常使用假文来检查字体、字号与行高的搭配效果。",
        "测试数据的意义在于模拟真实场景，同时避免泄露敏感信息。",
        "该段文字由生成器随机组合产生，每次生成的结果都不相同。",
        "界面在不同语言下的表现可能存在差异，需要分别进行验证。",
        "良好的占位内容应接近真实文案的长度与语义结构。"
    )

    private val STREET_NAMES = listOf(
        "人民路", "解放大道", "中山路", "建设街", "和平路", "文化路", "光明街", "幸福路"
    )

    private val ID_WEIGHTS = intArrayOf(7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
    private const val ID_CHECK_CODES = "10X98765432"

    private val random = Random.Default

    fun placeholderText(paragraphs: Int, chinese: Boolean): String {
        val count = paragraphs.coerceIn(1, 50)
        val pool = if (chinese) CHINESE_SENTENCES else LOREM_SENTENCES
        return (1..count).joinToString("\n\n") {
            val sentenceCount = random.nextInt(3, 6)
            (1..sentenceCount)
                .map { pool[random.nextInt(pool.size)] }
                .joinToString(" ")
        }
    }

    fun randomBirthDate(minAge: Int = 18, maxAge: Int = 60): LocalDate {
        val today = LocalDate.now()
        val min = today.minusYears(maxAge.toLong())
        val max = today.minusYears(minAge.toLong())
        val days = java.time.temporal.ChronoUnit.DAYS.between(min, max)
        return min.plusDays(random.nextLong(days + 1))
    }

    fun idCardNumber(birthDate: LocalDate, gender: Gender): String {
        val area = AREA_CODES[random.nextInt(AREA_CODES.size)]
        val birth = birthDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        var sequence = random.nextInt(1000)
        val resolved = when (gender) {
            Gender.MALE -> Gender.MALE
            Gender.FEMALE -> Gender.FEMALE
            Gender.RANDOM -> if (random.nextBoolean()) Gender.MALE else Gender.FEMALE
        }
        sequence = when (resolved) {
            Gender.MALE -> if (sequence % 2 == 0) sequence + 1 else sequence
            else -> if (sequence % 2 == 1) sequence + 1 else sequence
        }
        if (sequence > 999) sequence -= 2
        val body = area + birth + "%03d".format(sequence)
        return body + idCheckDigit(body)
    }

    fun idCheckDigit(body17: String): Char {
        var sum = 0
        for (i in 0 until 17) {
            sum += (body17[i] - '0') * ID_WEIGHTS[i]
        }
        return ID_CHECK_CODES[sum % 11]
    }

    fun chineseName(gender: Gender): String {
        val resolved = when (gender) {
            Gender.MALE -> Gender.MALE
            Gender.FEMALE -> Gender.FEMALE
            Gender.RANDOM -> if (random.nextBoolean()) Gender.MALE else Gender.FEMALE
        }
        val givenPool = if (resolved == Gender.MALE) MALE_GIVEN else FEMALE_GIVEN
        val surname = SURNAMES[random.nextInt(SURNAMES.size)]
        val givenLength = if (random.nextBoolean()) 1 else 2
        val given = (1..givenLength).joinToString("") { givenPool[random.nextInt(givenPool.size)] }
        return surname + given
    }

    fun phoneNumber(): String {
        val prefix = PHONE_PREFIXES[random.nextInt(PHONE_PREFIXES.size)]
        val rest = (1..8).joinToString("") { random.nextInt(10).toString() }
        return prefix + rest
    }

    fun email(): String {
        val nameLength = random.nextInt(5, 10)
        val name = (1..nameLength).joinToString("") { EMAIL_NAME_CHARS[random.nextInt(EMAIL_NAME_CHARS.length)].toString() }
        val domain = EMAIL_DOMAINS[random.nextInt(EMAIL_DOMAINS.size)]
        return "$name${random.nextInt(100, 999)}@$domain"
    }

    fun bankCard(): String {
        val prefix = "622202"
        val body = prefix + (1..9).joinToString("") { random.nextInt(10).toString() }
        return body + luhnCheckDigit(body)
    }

    fun luhnCheckDigit(body: String): Char {
        var sum = 0
        var double = true
        for (i in body.length - 1 downTo 0) {
            var d = body[i] - '0'
            if (double) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            double = !double
        }
        return ((10 - sum % 10) % 10).digitToChar()
    }

    fun randomDigits(length: Int): String {
        val n = length.coerceIn(1, 256)
        return (1..n).joinToString("") { random.nextInt(10).toString() }
    }

    fun address(): String {
        val province = listOf("北京市", "上海市", "广东省", "浙江省", "江苏省", "四川省", "湖北省", "陕西省")[random.nextInt(8)]
        val district = listOf("朝阳区", "浦东新区", "天河区", "西湖区", "鼓楼区", "锦江区", "武侯区", "雁塔区")[random.nextInt(8)]
        return province + district + STREET_NAMES[random.nextInt(STREET_NAMES.size)] + (random.nextInt(200) + 1) + "号"
    }
}