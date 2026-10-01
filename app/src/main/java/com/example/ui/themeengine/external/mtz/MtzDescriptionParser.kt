package com.example.ui.themeengine.external.mtz

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream

data class MtzDescription(
    val title: String? = null,
    val designer: String? = null,
    val author: String? = null,
    val version: String? = null,
    val description: String? = null,
    val uiVersion: String? = null
) {
    val metadata: Map<String, String>
        get() = buildMap {
            title?.let { put("title", it) }
            designer?.let { put("designer", it) }
            author?.let { put("author", it) }
            version?.let { put("version", it) }
            description?.let { put("description", it) }
            uiVersion?.let { put("uiVersion", it) }
        }
}

/** Tolerates missing and differently cased fields in MIUI description.xml. */
object MtzDescriptionParser {
    fun parse(bytes: ByteArray): MtzDescription {
        val fields = linkedMapOf<String, String>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        ByteArrayInputStream(bytes).use { input ->
            parser.setInput(input, null)
            var activeField: String? = null
            var activeDepth = -1
            var value = StringBuilder()
            while (true) {
                when (parser.next()) {
                    XmlPullParser.START_TAG -> {
                        val field = fieldName(parser.name)
                        if (field != null && activeField == null) {
                            activeField = field
                            activeDepth = parser.depth
                            value = StringBuilder()
                        }
                        for (index in 0 until parser.attributeCount) {
                            val attribute = fieldName(parser.getAttributeName(index)) ?: continue
                            parser.getAttributeValue(index)?.trim()
                                ?.takeIf(String::isNotEmpty)?.let { fields.putIfAbsent(attribute, it) }
                        }
                    }
                    XmlPullParser.TEXT, XmlPullParser.CDSECT -> {
                        if (activeField != null) value.append(parser.text)
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.depth == activeDepth && fieldName(parser.name) == activeField) {
                            value.toString().trim().takeIf(String::isNotEmpty)?.let {
                                fields.putIfAbsent(activeField, it)
                            }
                            activeField = null
                            activeDepth = -1
                        }
                    }
                    XmlPullParser.END_DOCUMENT -> break
                }
            }
        }
        return MtzDescription(
            title = fields["title"], designer = fields["designer"],
            author = fields["author"], version = fields["version"],
            description = fields["description"], uiVersion = fields["uiVersion"]
        )
    }

    private fun fieldName(name: String?): String? = when (
        name?.substringAfterLast(':')?.lowercase()?.filter(Char::isLetterOrDigit)
    ) {
        "title" -> "title"
        "designer" -> "designer"
        "author" -> "author"
        "version" -> "version"
        "description" -> "description"
        "uiversion" -> "uiVersion"
        else -> null
    }
}


