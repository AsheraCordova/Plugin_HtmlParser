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

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.ashera.attributedtext.AttributedString;
import com.ashera.attributedtext.AugmentedIntervalTree;
import com.ashera.attributedtext.BulletInterval;
import com.ashera.attributedtext.ImageInterval;
import com.ashera.attributedtext.StyleInterval;
import com.ashera.attributedtext.UrlInterval;
import com.ashera.core.IFragment;
import com.ashera.css.CssTree.Attribute;
import com.ashera.parser.css.CssDataHolder;
import com.ashera.parser.css.CssResult;
import com.ashera.widget.PluginInvoker;
import com.ashera.widget.WidgetAttribute;
import com.ashera.widget.WidgetAttributeMap;
import com.ashera.widget.WidgetAttributeValue;

import repackaged.org.ccil.cowan.tagsoup.TagSoupParser;

public class HtmlToSpannedConverter implements ContentHandler {
    private String mSource;
    private XMLReader mReader;
    private Stack<Integer> lineStart = new Stack<>();
    private Stack<Attributes> attributesStack = new Stack<>();
    private StringBuilder text = new StringBuilder();
    private AugmentedIntervalTree intervalTree;
    private AttributedString spannableString;
	private IFragment fragment;
	private int bulletIndent;
	private int bulletIndentInc;
	private int bulletSpacing;
	private CssDataHolder pageData;
	private Stack<WidgetAttributeMap> widgetAttributeMaps = new Stack<>();
	private Map<String, Object> htmlConfig;
    public HtmlToSpannedConverter(String source, TagSoupParser parser, Map<String, Object> htmlConfig, IFragment fragment) {
        mSource = source;
        mReader = parser;
        this.htmlConfig = htmlConfig;
        
        if (this.htmlConfig == null) {
        	//to avoid npe
        	this.htmlConfig = new java.util.HashMap<>(0);
        }
        
        this.fragment = fragment;
        pageData = (CssDataHolder) fragment.getStyleSheet();
        intervalTree = new AugmentedIntervalTree(htmlConfig, fragment);
        bulletIndentInc = (int) PluginInvoker.convertDpToPixel("20dp");
        bulletIndent = bulletIndentInc * -1;
        bulletSpacing = (int) PluginInvoker.convertDpToPixel("10dp");
    }

    public AttributedString convert() {
    	if (mSource == null) {
    		mSource = "";
    	}
        mReader.setContentHandler(this);
        try {
            mReader.parse(new InputSource(new StringReader(mSource)));
        } catch (IOException e) {
            // We are reading from a string. There should not be IO problems.
            throw new RuntimeException(e);
        } catch (SAXException e) {
            // TagSoup doesn't throw parse exceptions.
            throw new RuntimeException(e);
        }

        // Fix flags and range for paragraph-type markup.
        intervalTree.setText(text.toString());
//        intervalTree.printTree();
        spannableString = PluginInvoker.createAttributedString(fragment, text.toString());
        intervalTree.apply(spannableString);
        return spannableString;
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void startDocument() throws SAXException {
    }

    public void endDocument() throws SAXException {
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }
    
    
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
    	localName = localName.toLowerCase();
    	if (localName.equals("body") || localName.equals("html")) {
    		return;
    	}
    	WidgetAttributeMap parent = null;
    	if (!widgetAttributeMaps.isEmpty()) {
    		parent = widgetAttributeMaps.peek();
    	}
    	WidgetAttributeMap widgetAttributeMap = getAttributes(localName, attributes);
    	widgetAttributeMaps.push(widgetAttributeMap);
    	if (parent != null) {
    		widgetAttributeMap.setParent(parent);
    	}

    	if (localName.equals(Html.UNORDERED_LIST)) {
    		bulletIndent += bulletIndentInc;
    	} else if (localName.equals("div") && !text.toString().equals("") && !text.toString().endsWith("\n")) {
    		text.append("\n");
    	}
    	lineStart.push(text.length());
    	if (localName.equals(Html.LIST_ITEM)) {
    		String bulletString = PluginInvoker.getAttributedBulletHtml();
    		if (bulletString != null) {
    			text.append(bulletString);
    		}
    	}
    	attributesStack.push(attributes);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
    	localName = localName.toLowerCase();
    	if (localName.equals("body") || localName.equals("html")) {
    		return;
    	}
    	if (localName.equals("div") && !text.toString().equals("") && !text.toString().endsWith("\n")) {
    		text.append("\n");
    	}
    	int start = lineStart.pop();
    	WidgetAttributeMap style = widgetAttributeMaps.pop();
    	Attributes attributes = attributesStack.pop();
    	if (localName.equals("br")) {  
    		text.append("\n");
    	} else if (localName.equals("a")) {    		
	    	intervalTree.insertNode(new UrlInterval(start, text.length(), style, 
	    			attributes.getValue("", "href")));
    	} else if (localName.equals("img")) {    
    		text = text.append("\uFFFC");
	    	intervalTree.insertNode(new ImageInterval(start, text.length(), 
	    			attributes.getValue("", "src")));
    	} else if (localName.equals(Html.UNORDERED_LIST)) {    
    		bulletIndent = bulletIndent - bulletIndentInc;
    	} else if (localName.equals(Html.LIST_ITEM)) {
    		text = text.append("\n");
    		intervalTree.insertNode(new BulletInterval(start, text.length() , 
	    			style, bulletIndent, bulletSpacing));    		    		
    	} else {
    		intervalTree.insertNode(new StyleInterval(start, text.length(), style));
    	}
    }

   
    public void characters(char ch[], int start, int length) throws SAXException {
    	String str = new String(ch, start,length);
    	Object textAllCaps = htmlConfig.get("textAllCaps");
		if (textAllCaps != null &&  (boolean) textAllCaps) {
			str = str.toUpperCase();
    	}
		

    	String[] arr = str.split("\n");
		Object password = htmlConfig.get("password");

    	for (String splitStr : arr) {
    		if (password != null &&  (boolean) password) {
    			String result = "";
    			for (int i = 0; i < splitStr.length(); i++) {
    				result += "*";
    			}
    			splitStr = result;
        	}
    		text = text.append(splitStr/*.trim()*/);
		}		
    }
    

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
    }

    public void processingInstruction(String target, String data) throws SAXException {
    }

    public void skippedEntity(String name) throws SAXException {
    }
    
    private WidgetAttributeMap getAttributes(final String localName, Attributes atts) {
    	final WidgetAttributeMap styles = new WidgetAttributeMap();
		pageData.getCss(localName, atts.getValue("class"), atts.getValue("id"), new CssResult() {
			@Override
			public void put(String key, Attribute value) {
				WidgetAttribute attribute = WidgetAttribute.builder().withName(key).withType("string").build();
				
				if (checkIfNotSupported(localName, key)) {
					return;
				}
				
				if (attribute != null) {
					WidgetAttributeValue attributeValue = new WidgetAttributeValue(value.value, value.orientation,
							value.minWidth, value.minHeight, value.maxWidth, value.maxHeight);

					
					styles.put(attribute, attributeValue);
				}
			}
		});
			
		
		for (int i = 0; i < atts.getLength(); i++) {			
			String key = atts.getLocalName(i);
			final WidgetAttribute attribute = WidgetAttribute.builder().withName(key).withType("string").build();
			if (checkIfNotSupported(localName, key)) {
				continue;
			}
			if (attribute != null) {
				WidgetAttributeValue attributeValue = new WidgetAttributeValue(atts.getValue(key));
				styles.put(attribute, attributeValue);
				
			}
		}
		
		return styles;
	}
    

	private boolean checkIfNotSupported(final String localName, String key) {
		// text align on non div elements will not work
		boolean textAlign = (key.equals("text-align") || key.equals("textAlign")) && !localName.equals("div");
		// background on div elements will not work
		boolean background = (key.equals("background-color") || key.equals("background")) && localName.equals("div");
		return textAlign || background;
	}
}
