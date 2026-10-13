package com.n2.oemlauncher.mobile.iconpack

import com.n2.oemlauncher.mobile.config.ImportLimits
import com.n2.oemlauncher.shared.model.AppKey
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

object AppFilterParser {
    fun parse(bytes: ByteArray): Map<String, String> {
        val text = bytes.decodeToString()
        if (text.contains("<!DOCTYPE", ignoreCase = true) ||
            text.contains("<!ENTITY", ignoreCase = true)
        ) throw IconImportException(ImportFailure.XML)
        val result = LinkedHashMap<String, String>()
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(StringReader(text))
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component").orEmpty()
                    val drawable = parser.getAttributeValue(null, "drawable").orEmpty()
                    val key = AppKey.canonical(
                        component.removePrefix(ImportLimits.XML_COMPONENT_PREFIX).removeSuffix("}"),
                    )
                    if (key != null && drawable.isNotBlank() && '/' !in drawable && '\\' !in drawable) {
                        result[key] = drawable
                    }
                }
                parser.next()
            }
        } catch (_: org.xmlpull.v1.XmlPullParserException) {
            throw IconImportException(ImportFailure.XML)
        }
        return result
    }
}
