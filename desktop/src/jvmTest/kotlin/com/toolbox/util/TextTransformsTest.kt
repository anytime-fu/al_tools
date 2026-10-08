package com.toolbox.util

import kotlin.test.Test
import kotlin.test.assertEquals

class TextTransformsTest {
    
    @Test
    fun upperAndLower() {
        assertEquals("ABC", TextTransforms.convert("abc", ConvertMode.UPPER))
        assertEquals("abc", TextTransforms.convert("ABC", ConvertMode.LOWER))
    }
    
    @Test
    fun sentence() {
        assertEquals("Hello world", TextTransforms.convert("hello world", ConvertMode.SENTENCE))
    }
    
    @Test
    fun camelCase() {
        assertEquals("helloWorld", TextTransforms.convert("hello world", ConvertMode.CAMEL))
    }
    
    @Test
    fun pascalCase() {
        assertEquals("HelloWorld", TextTransforms.convert("hello world", ConvertMode.PASCAL))
    }
    
    @Test
    fun snakeAndKebab() {
        assertEquals("hello_world", TextTransforms.convert("hello world", ConvertMode.SNAKE))
        assertEquals("hello-world", TextTransforms.convert("helloWorld", ConvertMode.KEBAB))
    }
    
    @Test
    fun constantCase() {
        assertEquals("HELLO_WORLD", TextTransforms.convert("hello world", ConvertMode.CONSTANT))
    }
    
    @Test
    fun fullToHalf() {
        assertEquals("AB1", TextTransforms.convert("ＡＢ１", ConvertMode.FULL_TO_HALF))
    }
    
    @Test
    fun halfToFull() {
        assertEquals("ＡＢ", TextTransforms.convert("AB", ConvertMode.HALF_TO_FULL))
    }
    
    @Test
    fun simpToTrad() {
        assertEquals("愛", TextTransforms.convert("爱", ConvertMode.SIMP_TO_TRAD))
    }
    
    @Test
    fun reverse() {
        assertEquals("cba", TextTransforms.convert("abc", ConvertMode.REVERSE))
    }
    
    @Test
    fun emptyInput() {
        assertEquals("", TextTransforms.convert("", ConvertMode.CAMEL))
    }
}