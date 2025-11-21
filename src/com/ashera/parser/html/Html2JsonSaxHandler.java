//start - license
/*
 * Copyright (c) 2025 Ashera Cordova
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
//end - license
package com.ashera.parser.html;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * SAX-based XML -> JSON converter - Attributes => "@attr" - Text content =>
 * "#text" - Repeated children => arrays - Pure Java, no dependencies
 */
public class Html2JsonSaxHandler implements ContentHandler {
	private Deque<Map<String, Object>> stack = new ArrayDeque<>();
	private Deque<String> nameStack = new ArrayDeque<>();
	private Map<String, Object> root;
	private StringBuilder textBuffer = new StringBuilder();

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if (localName.equals("html") || localName.equals("body")) {
			return;
		}
		textBuffer.setLength(0);

		Map<String, Object> node = new LinkedHashMap<>();

		// add attributes
		for (int i = 0; i < attributes.getLength(); i++) {
			node.put("@" + attributes.getQName(i), attributes.getValue(i));
		}

		if (!stack.isEmpty()) {
			Map<String, Object> parent = stack.peek();
			Object existing = parent.get(qName);
			if (existing == null) {
				parent.put(qName, node);
			} else if (existing instanceof List) {
				((List<Object>) existing).add(node);
			} else {
				List<Object> arr = new ArrayList<>();
				arr.add(existing);
				arr.add(node);
				parent.put(qName, arr);
			}
		}

		stack.push(node);
		nameStack.push(qName);
		if (root == null) {
			root = new java.util.HashMap<>();
			root.put(qName, node);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		textBuffer.append(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if (localName.equals("html") || localName.equals("body")) {
			return;
		}
		String text = textBuffer.toString().trim();
		if (!text.isEmpty()) {
			Map<String, Object> current = stack.peek();
			Object existing = current.get("#text");
			if (existing == null) {
				current.put("#text", text);
			} else {
				current.put("#text", existing.toString() + text);
			}
		}

		stack.pop();
		nameStack.pop();
		textBuffer.setLength(0);
	}

	public Map<String, Object> getRoot() {
		return root;
	}


	// ---------------- JSON serializer ----------------
	public String toJson() {
		StringBuilder sb = new StringBuilder();
		writeJson(root, sb);
		return sb.toString();
	}

	private void writeJson(Object v, StringBuilder sb) {
		if (v == null) {
			sb.append("null");
		} else if (v instanceof String) {
			sb.append('"').append(escape((String) v)).append('"');
		} else if (v instanceof Number || v instanceof Boolean) {
			sb.append(v.toString());
		} else if (v instanceof Map) {
			sb.append('{');
			boolean first = true;
			for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
				if (!first)
					sb.append(',');
				first = false;
				sb.append('"').append(escape(String.valueOf(e.getKey()))).append('"').append(':');
				writeJson(e.getValue(), sb);
			}
			sb.append('}');
		} else if (v instanceof List) {
			sb.append('[');
			boolean first = true;
			for (Object item : (List<?>) v) {
				if (!first)
					sb.append(',');
				first = false;
				writeJson(item, sb);
			}
			sb.append(']');
		} else {
			sb.append('"').append(escape(String.valueOf(v))).append('"');
		}
	}

	private String escape(String s) {
		StringBuilder out = new StringBuilder(s.length() + 16);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '"':
				out.append("\\\"");
				break;
			case '\\':
				out.append("\\\\");
				break;
			case '\b':
				out.append("\\b");
				break;
			case '\f':
				out.append("\\f");
				break;
			case '\n':
				out.append("\\n");
				break;
			case '\r':
				out.append("\\r");
				break;
			case '\t':
				out.append("\\t");
				break;
			default:
				if (c < 0x20)
					out.append(String.format("\\u%04x", (int) c));
				else
					out.append(c);
			}
		}
		return out.toString();
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		
	}

	@Override
	public void startDocument() throws SAXException {
		
	}

	@Override
	public void endDocument() throws SAXException {
		
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		
	}
}
