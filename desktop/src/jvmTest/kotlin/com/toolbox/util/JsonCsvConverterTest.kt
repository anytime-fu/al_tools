package com.toolbox.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonCsvConverterTest {
    
    @Test
    fun jsonToCsvBasic() {
        val csv = JsonCsvConverter.jsonToCsv("""[{"a":"1","b":"2"},{"a":"3","b":"4"}]""")
        assertEquals("a,b\n1,2\n3,4\n", csv)
    }
    
    @Test
    fun jsonToCsvSingleObject() {
        val csv = JsonCsvConverter.jsonToCsv("""{"x":"y"}""")
        assertEquals("x\ny\n", csv)
    }
    
    @Test
    fun jsonToCsvEscapesComma() {
        val csv = JsonCsvConverter.jsonToCsv("""[{"a":"x,y"}]""")
        assertEquals("a\n\"x,y\"\n", csv)
    }
    
    @Test
    fun csvToJsonBasic() {
        val json = JsonCsvConverter.csvToJson("a,b\n1,2\n")
        assertEquals(
            """[{"a":"1","b":"2"}]""",
            json.replace(Regex("\\s"), "")
        )
    }
    
    @Test
    fun csvHandlesQuotedComma() {
        val json = JsonCsvConverter.csvToJson("a\n\"x,y\"\n")
        assertEquals("""[{"a":"x,y"}]""", json.replace(Regex("\\s"), ""))
    }
    
    @Test
    fun roundTrip() {
        val json = """[{"name":"甲","age":"1"},{"name":"乙","age":"2"}]"""
        val csv = JsonCsvConverter.jsonToCsv(json)
        val back = JsonCsvConverter.csvToJson(csv)
        assertEquals(json, back.replace(Regex("\\s"), ""))
    }
    
    @Test
    fun jsonToCsvRejectsScalar() {
        assertFailsWith<IllegalArgumentException> {
            JsonCsvConverter.jsonToCsv("42")
        }
    }
}