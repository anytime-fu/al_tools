package com.toolbox.util

enum class ConvertMode(val label: String) {
    UPPER("全大写"),
    LOWER("全小写"),
    TITLE("标题格式"),
    SENTENCE("句首大写"),
    CAMEL("camelCase"),
    PASCAL("PascalCase"),
    SNAKE("snake_case"),
    KEBAB("kebab-case"),
    CONSTANT("CONSTANT_CASE"),
    FULL_TO_HALF("全角转半角"),
    HALF_TO_FULL("半角转全角"),
    SIMP_TO_TRAD("简体转繁体"),
    TRAD_TO_SIMP("繁体转简体"),
    REVERSE("反转文本")
}

object TextTransforms {
fun convert(text: String, mode: ConvertMode): String {
    if (text.isEmpty()) return ""
    return when (mode) {
        ConvertMode.UPPER -> text.uppercase()
        ConvertMode.LOWER -> text.lowercase()
        ConvertMode.TITLE -> text.split(Regex("(\\s+)")).joinToString("") { word ->
            if (word.isBlank()) word
            else word.lowercase().replaceFirstChar { it.uppercase() }
        }
        ConvertMode.SENTENCE -> text.lowercase().replaceFirstChar { it.uppercase() }
        ConvertMode.CAMEL -> {
            val words = splitWords(text)
            if (words.isEmpty()) ""
            else words.mapIndexed { i, w ->
                if (i == 0) w.lowercase() else w.lowercase().replaceFirstChar { it.uppercase() }
            }.joinToString("")
        }
        ConvertMode.PASCAL -> splitWords(text)
            .joinToString("") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
        ConvertMode.SNAKE -> splitWords(text).joinToString("_") { it.lowercase() }
        ConvertMode.KEBAB -> splitWords(text).joinToString("-") { it.lowercase() }
        ConvertMode.CONSTANT -> splitWords(text).joinToString("_") { it.uppercase() }
        ConvertMode.FULL_TO_HALF -> text.map { ch ->
            when {
                ch.code == 0x3000 -> ' '
                ch.code in 0xFF01..0xFF5E -> (ch.code - 0xFEE0).toChar()
                else -> ch
            }
        }.joinToString("")
        ConvertMode.HALF_TO_FULL -> text.map { ch ->
            when {
                ch == ' ' -> '\u3000'
                ch.code in 0x21..0x7E -> (ch.code + 0xFEE0).toChar()
                else -> ch
            }
        }.joinToString("")
        ConvertMode.SIMP_TO_TRAD -> text.map { SIMP_TO_TRAD[it] ?: it }.joinToString("")
        ConvertMode.TRAD_TO_SIMP -> text.map { TRAD_TO_SIMP[it] ?: it }.joinToString("")
        ConvertMode.REVERSE -> text.reversed()
    }
}

fun splitWords(text: String): List<String> =
    text.split(Regex("[^A-Za-z0-9]+"))
        .flatMap { token -> token.split(Regex("(?<=[a-z0-9])(?=[A-Z])")) }
        .filter { it.isNotEmpty() }

private val SIMP_TO_TRAD: Map<Char, Char> = "爱=愛,碍=礙,袄=襖,坝=壩,罢=罷,摆=擺,败=敗,办=辦,帮=幫,绑=綁,饱=飽,宝=寶,报=報,贝=貝,备=備,笔=筆,币=幣,毕=畢,闭=閉,边=邊,编=編,变=變,辩=辯,辫=辮,标=標,别=別,宾=賓,饼=餅,补=補,参=參,惨=慘,灿=燦,苍=蒼,舱=艙,仓=倉,厕=廁,侧=側,册=冊,测=測,层=層,搀=攙,缠=纏,产=產,阐=闡,颤=顫,场=場,尝=嘗,长=長,偿=償,厂=廠,畅=暢,钞=鈔,车=車,彻=徹,尘=塵,陈=陳,衬=襯,称=稱,惩=懲,诚=誠,迟=遲,驰=馳,耻=恥,齿=齒,宠=寵,冲=沖,虫=蟲,筹=籌,丑=醜,厨=廚,础=礎,储=儲,触=觸,处=處,传=傳,疮=瘡,闯=闖,创=創,纯=純,词=詞,赐=賜,聪=聰,丛=叢,凑=湊,窜=竄,错=錯,达=達,带=帶,贷=貸,单=單,掸=撣,惮=憚,弹=彈,当=當,挡=擋,党=黨,荡=蕩,档=檔,导=導,岛=島,祷=禱,灯=燈,邓=鄧,敌=敵,涤=滌,递=遞,缔=締,点=點,电=電,淀=澱,钓=釣,调=調,叠=疊,谍=諜,钉=釘,顶=頂,锭=錠,动=動,栋=棟,冻=凍,斗=鬥,独=獨,读=讀,赌=賭,镀=鍍,锻=鍛,断=斷,队=隊,对=對,吨=噸,顿=頓,夺=奪,堕=墮,鹅=鵝,额=額,饿=餓,儿=兒,尔=爾,饵=餌,贰=貳,发=發,罚=罰,阀=閥,贩=販,饭=飯,访=訪,纺=紡,飞=飛,废=廢,费=費,纷=紛,坟=墳,愤=憤,粪=糞,丰=豐,讽=諷,凤=鳳,肤=膚,辐=輻,抚=撫,辅=輔,赋=賦,复=復,负=負,妇=婦,缚=縛,该=該,盖=蓋,干=幹,赶=趕,秆=稈,冈=岡,刚=剛,钢=鋼,岗=崗,纲=綱,搁=擱,鸽=鴿,阁=閣,铬=鉻,个=個,给=給,龚=龔,巩=鞏,贡=貢,沟=溝,构=構,购=購,够=夠,蛊=蠱,顾=顧,关=關,观=觀,馆=館,惯=慣,贯=貫,广=廣,规=規,归=歸,龟=龜,轨=軌,诡=詭,贵=貴,刽=劊,辊=輥,锅=鍋,国=國,过=過,骇=駭,韩=韓,汉=漢,号=號,阂=閡,鹤=鶴,贺=賀,轰=轟,鸿=鴻,红=紅,后=後,护=護,沪=滬,户=戶,哗=嘩,华=華,画=畫,划=劃,怀=懷,坏=壞,欢=歡,环=環,还=還,缓=緩,换=換,唤=喚,痪=瘓,焕=煥,涣=渙,黄=黃,谎=謊,挥=揮,辉=輝,毁=毀,贿=賄,秽=穢,会=會,烩=燴,汇=匯,讳=諱,诲=誨,绘=繪,荤=葷,浑=渾,货=貨,获=獲,祸=禍"
    .split(",")
    .mapNotNull { pair ->
        val parts = pair.split("=")
        if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
            parts[0][0] to parts[1][0]
        } else null
    }
    .toMap()

private val TRAD_TO_SIMP: Map<Char, Char> = SIMP_TO_TRAD.entries.associate { (s, t) -> t to s }
}
