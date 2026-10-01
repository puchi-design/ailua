package com.example.ui.themeengine.external.mtz

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

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
        require(bytes.size <= 1024 * 1024) { "description.xml 过大" }
        val xml = bytes.toString(Charsets.UTF_8)
        require(!Regex("<!\\s*(DOCTYPE|ENTITY)", RegexOption.IGNORE_CASE).containsMatchIn(xml)) {
            "不支持包含 DTD 的主题元数据"
        }
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        }
        val root = factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes)).documentElement
        val fields = linkedMapOf<String, String>()
        fun visit(node: Node) {
            if (node is Element) {
                fieldName(node.localName ?: node.tagName)?.let { field ->
                    val directText = (0 until node.childNodes.length).mapNotNull { index ->
                        node.childNodes.item(index).takeIf {
                            it.nodeType == Node.TEXT_NODE || it.nodeType == Node.CDATA_SECTION_NODE
                        }?.nodeValue
                    }.joinToString("").trim()
                    directText.takeIf(String::isNotEmpty)?.let { fields.putIfAbsent(field, it) }
                }
                val attributes = node.attributes
                for (index in 0 until attributes.length) {
                    val attribute = attributes.item(index)
                    fieldName(attribute.localName ?: attribute.nodeName)?.let { field ->
                        attribute.nodeValue?.trim()?.takeIf(String::isNotEmpty)?.let {
                            fields.putIfAbsent(field, it)
                        }
                    }
                }
            }
            val children = node.childNodes
            for (index in 0 until children.length) visit(children.item(index))
        }
        visit(root)
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