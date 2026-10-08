package com.toolbox.ui.data.xml

import com.toolbox.ui.components.AppFilterChip

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource

private enum class XmlMode(val label: String) {
    FORMAT("格式化"),
    MINIFY("压缩"),
    XPATH("XPath 查询")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XmlFormatScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(XmlMode.FORMAT) }
    var xpathExpr by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("XML 格式化") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                XmlMode.values().forEach { m ->
                    AppFilterChip(
                        selected = mode == m,
                        onClick = { mode = m },
                        label = { Text(m.label) }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        message = ""
                        try {
                            output = when (mode) {
                                XmlMode.FORMAT -> transformXml(input, indent = true)
                                XmlMode.MINIFY -> transformXml(input, indent = false)
                                XmlMode.XPATH -> queryXPath(input, xpathExpr)
                            }
                            message = "处理成功"
                        } catch (e: Exception) {
                            output = ""
                            message = "处理失败：${e.message}"
                        }
                    }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("执行")
                }

                AppOutlinedButton(
                    onClick = {
                        if (output.isNotEmpty()) {
                            Toolkit.getDefaultToolkit().systemClipboard
                                .setContents(StringSelection(output), null)
                        }
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制")
                }

                AppOutlinedButton(
                    onClick = {
                        input = ""
                        output = ""
                        message = ""
                    }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空")
                }
            }

            if (mode == XmlMode.XPATH) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = xpathExpr,
                    onValueChange = { xpathExpr = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("XPath 表达式") },
                    placeholder = { Text("//book[@id='1']/title") },
                    singleLine = true
                )
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.startsWith("处理失败")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("XML 输入", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("<root>...</root>") }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("结果", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = output,
                        onValueChange = {},
                        modifier = Modifier.fillMaxSize(),
                        readOnly = true,
                        placeholder = { Text("结果...") }
                    )
                }
            }
        }
    }
}

private fun parseXml(text: String) =
    DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(InputSource(StringReader(text)))

private fun transformXml(text: String, indent: Boolean): String {
    val doc = parseXml(text)
    doc.documentElement.normalize()
    if (!indent) {
        stripWhitespace(doc.documentElement)
    }
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, if (indent) "yes" else "no")
    if (indent) {
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
    }
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    val writer = StringWriter()
    transformer.transform(DOMSource(doc), StreamResult(writer))
    return writer.toString()
}

private fun stripWhitespace(node: Node) {
    val children = node.childNodes
    val toRemove = mutableListOf<Node>()
    for (i in 0 until children.length) {
        val child = children.item(i)
        if (child.nodeType == Node.TEXT_NODE && child.textContent.isBlank()) {
            toRemove.add(child)
        } else if (child.nodeType == Node.ELEMENT_NODE) {
            stripWhitespace(child)
        }
    }
    toRemove.forEach { node.removeChild(it) }
}

private fun queryXPath(text: String, expression: String): String {
    if (expression.isBlank()) throw IllegalArgumentException("请输入 XPath 表达式")
    val doc = parseXml(text)
    val xpath = XPathFactory.newInstance().newXPath()
    val result = xpath.evaluate(expression, doc, XPathConstants.NODESET) as NodeList
    if (result.length == 0) return "（无匹配节点）"
    return buildString {
        for (i in 0 until result.length) {
            val node = result.item(i)
            append("[${i + 1}] ")
            append(nodeToString(node))
            appendLine()
        }
    }
}

private fun nodeToString(node: Node): String {
    return when (node.nodeType) {
        Node.TEXT_NODE -> node.textContent.trim()
        Node.ATTRIBUTE_NODE -> "${node.nodeName}=\"${node.textContent}\""
        else -> {
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            transformer.setOutputProperty(OutputKeys.INDENT, "no")
            val writer = StringWriter()
            transformer.transform(DOMSource(node), StreamResult(writer))
            writer.toString().trim()
        }
    }
}
