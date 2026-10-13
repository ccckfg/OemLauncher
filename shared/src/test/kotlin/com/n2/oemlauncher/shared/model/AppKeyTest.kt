package com.n2.oemlauncher.shared.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppKeyTest {

    @Test
    fun parsesFullComponentName() {
        val key = AppKey.parse("com.example.app/com.example.app.MainActivity")
        assertEquals(AppKey("com.example.app", "com.example.app.MainActivity"), key)
    }

    @Test
    fun expandsShortClassName() {
        assertEquals(
            "com.example.app/com.example.app.Main",
            AppKey.canonical("com.example.app/.Main"),
        )
    }

    @Test
    fun acceptsInnerClasses() {
        assertEquals("com.a/com.a.Outer\$Inner", AppKey.canonical("com.a/com.a.Outer\$Inner"))
    }

    @Test
    fun rejectsMalformedKeys() {
        listOf("", "noslash", "/com.a.Main", "com.a/", "com..a/com.a.Main", "com.a/1Main", "c d/x.Y")
            .forEach { assertNull("expected null for '$it'", AppKey.parse(it)) }
    }

    @Test
    fun rejectsOverlongKeys() {
        val longPackage = "a." + "b".repeat(600)
        assertNull(AppKey.parse("$longPackage/x.Y"))
    }
}
