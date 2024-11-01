package com.ashera.parser.html;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.ashera.core.IFragment;
import com.ashera.css.CssTree.Attribute;
import com.ashera.parser.css.CssDataHolder;
import com.ashera.parser.css.CssResult;
import com.ashera.widget.BaseHasWidgets;
import com.ashera.widget.HasWidgets;
import com.ashera.widget.ICompositeDecorator;
import com.ashera.widget.IDecorator;
import com.ashera.widget.IWidget;
import com.ashera.widget.PluginInvoker;
import com.ashera.widget.WidgetAttribute;
import com.ashera.widget.WidgetAttributeMap;
import com.ashera.widget.WidgetAttributeValue;
import com.ashera.widget.WidgetFactory;

public class HtmlSaxHandler implements ContentHandler{
	private CssDataHolder pageData = null;
	private IWidget root;
	private IWidget widget;
	private Stack<HasWidgets> hasWidgets = new Stack<HasWidgets>();
	private Stack<Boolean> pushParent = new Stack<Boolean>();
	private IFragment fragment;
	private Stack<IWidget> widgetsStack = new Stack<IWidget>();
	private boolean isTemplate = false;
	private int depth = 0;
	private static Properties languageProperties;
	private boolean isAndroid;
	private String inlineResourceName;
	private boolean inlineAppend;
	private String text = "";
	private String componentId;
	
	public HtmlSaxHandler(IFragment fragment, boolean template) {
		this(fragment, null, template);
	}
	public HtmlSaxHandler(IFragment fragment, String componentId, boolean template) {
		isAndroid = PluginInvoker.getOS().equalsIgnoreCase("android");
		this.isTemplate = template;
		this.fragment = fragment;
		this.componentId = componentId;
		if (fragment.getStyleSheet() == null) {
		    pageData = new CssDataHolder();
		    fragment.setStyleSheet(pageData);		
			String href =  "www/css/styles.css";
			pageData.addCss(PluginInvoker.getFileAsset(href, fragment));
		} else {
		    pageData = (CssDataHolder) fragment.getStyleSheet();
		}

	}
	
	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
	}

	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
	}

	@Override
	public void setDocumentLocator(Locator locator) {
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
	}

	@Override
	public void startDocument() throws SAXException {
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
	}

	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (inlineResourceName != null) {
			text += new String(ch, start,length);
		}
		if (depth > 0) {
			return;
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		String operatingSystem = atts.getValue("os");		
		if((operatingSystem != null && operatingSystem.toLowerCase().indexOf(PluginInvoker.getOS().toLowerCase()) == -1) || depth > 0) {
			depth++;
			return;
		}		

		if (localName.equalsIgnoreCase("include")) {
			String componentId = this.componentId;
			String lcomponentId = getValue("componentId", atts);
			if (componentId != null || lcomponentId != null) {
				if (componentId == null) {
					componentId = lcomponentId;
				} else {
					componentId = componentId + "#" + lcomponentId;
				}
			}
			
			if (componentId != null) {
				for (int i = 0; i < atts.getLength(); i++) {			
					String key = atts.getLocalName(i);
					if (!key.equals("layout") && !key.equals("componentId") && !key.endsWith("_type") ) {
						Object value = atts.getValue(i);
						String type = getValue(key + "_type", atts);
						if (type != null) {
							value = com.ashera.model.ModelStore.changeModelDataType(com.ashera.model.ModelDataType.valueOf(type), value);	
						}
						
						com.ashera.model.ModelStore.storeModelToScope(componentId + "#" + key, com.ashera.model.ModelScope.component, value, fragment, widget, null);
					}
				}
			}
			
			PluginInvoker.parseInclude((BaseHasWidgets) hasWidgets.peek(), getValue("layout", atts), componentId, isTemplate, fragment);
			return;
		}

		String tagName = localName;
		String widgetOverride = getValue("widget-override", atts);

		if (widgetOverride == null) {
			// try type
			String value = getValue("type", atts);
			if (value != null) {
				widgetOverride = localName + "-" + value;
			}
		}

		
		if (languageProperties == null) {
			try {
				languageProperties = new Properties();
				String alternateNames = PluginInvoker.getFileAsset("www/language.properties", fragment);					
				languageProperties.load(new StringReader(alternateNames));
			} catch (Exception e) {
			}
		}

		if (languageProperties.containsKey(localName)) {
			String widgetOverrideName = languageProperties.getProperty(localName);
			if (widgetOverrideName != null && !widgetOverrideName.isEmpty()) {
				localName = widgetOverrideName;
			}
		}
		
		if (widgetOverride != null) {
			localName = widgetOverride;
		}
		this.widget = WidgetFactory.get(localName, isTemplate);
		
		Map<String, Object> params = getParams(atts, localName);
		startCreateWidget(localName, widget, tagName, getValue("android:id", atts), -1, atts, null, params, null);
	}

	private Map<String, Object> getParams(Attributes atts, String localName) {
		String decorators = getValue("decorator", atts);
		Map<String, Object> params = new java.util.HashMap<String, Object>();
		if (decorators != null) {
			params.put("decorator", decorators);
		}
		
		// first step apply theme attributes and allow widget to override this style
		if (!isAndroid && widget != null) {
			widget.applyThemeConstructorArgsStyle(widget.getGroupName(), params);
		}

		String createAttrCount = atts.getValue("create-attr-count");
		if (createAttrCount != null) {
			params = new java.util.HashMap<String, Object>();
			Integer count = Integer.parseInt(createAttrCount);
			for (int i = 0; i < count; i++) {
				String attr = atts.getValue("create-attr-" + i);
				params.put(getAttrKeyWithoutNameSpace(attr), getValue(attr, atts));
			}
		}
		
		Set<String> constructorAttributes = WidgetFactory.getConstructorAttributes(localName);
		if (constructorAttributes != null) {
			for (String constructorAttribute : constructorAttributes) {
				String value = getValue(constructorAttribute, atts);
				if (value != null) {
					params.put(getAttrKeyWithoutNameSpace(constructorAttribute), value);
				}
			}
		}
		
		return params;
	}

	private String getAttrKeyWithoutNameSpace(String attr) {
	    if (attr.indexOf(":") == -1) {
	        return attr;
	    }
        return attr.substring(attr.indexOf(":") + 1);
    }

    public IWidget startCreateWidget(String localName, IWidget widget, String tagName, String id, int index,
			Attributes atts, WidgetAttributeMap widgetAttributeMap, Map<String, Object> params, Map<String, String> unresolvedAttributes) {
		if (localName.equalsIgnoreCase("Inline")) {
			String type = getValue("type", atts);
			
			if (type.equals("resource")) {
				inlineResourceName = getValue("name", atts);
				inlineAppend = false;
			} else if (type.equals("style")) {
				inlineResourceName = "style";	
				inlineAppend = true;
			} else if (type.equals("javascript")) {
				inlineResourceName = "javascript";	
				inlineAppend = true;
			} else {
				throw new RuntimeException("unknown Inline type");
			}
		}

    	
    	this.widget = widget;
    	if (widget == null) {
    		widget = WidgetFactory.get(localName, isTemplate);
    		this.widget = widget;
    	}
		
		String decorators = (String) params.get("decorator");
		if (decorators != null && !isTemplate) {
			ICompositeDecorator compositeDecorator = WidgetFactory.getCompositeDecorator();
			if (compositeDecorator != null) {
				ICompositeDecorator compositeDecoratorProto = compositeDecorator.newInstance(localName, this.widget);

				String[] decoratorArr = decorators.split("\\|");
				for (String decorator : decoratorArr) {
					String[] decoratorItems = decorator.split("\\:"); 
					IDecorator iDecorator = WidgetFactory.getDecorator(compositeDecoratorProto, this.widget, decoratorItems[0]);
					
					if (iDecorator != null) {
						if (decoratorItems.length > 1) {
							iDecorator.setSupportedAttributes(Arrays.asList(decoratorItems[1].split(",")));
						}
						this.widget = compositeDecoratorProto;
						compositeDecoratorProto.addDecorator(iDecorator);
						localName = compositeDecoratorProto.getLocalName();
					}
				}
			}			
		}

		//set root
		if (/*localName.equals("body") && */root == null && widget != null) {
			this.root = this.widget;
			
			/*if (!"root".equals(atts.getValue("id"))) {
				throw new RuntimeException("Id of the root has to be root");
			}*/
		}
		boolean parentPushed = false;
		if (widget != null) {
			HasWidgets parent = null;
			if (!hasWidgets.isEmpty()) {
				parent = hasWidgets.peek().getCompositeLeaf(widget);
			}
			if (parent != null) {
				widget.setParent(parent);

				
				if (widget instanceof BaseHasWidgets) {
					// if widget has event bubblers then set it to the child to propogate the event
					Set<Integer> eventBubblers = parent.getEventBubblers();
					if (eventBubblers != null) {
						widget.setEventBubblers(eventBubblers);
					}
				}
			}
			this.widget.create(fragment, params);
			widget.setId(id);
			widget.setComponentId(componentId);

			if (widgetAttributeMap == null) {
				populateAttributes(widget, parent, tagName, localName, atts, isTemplate);
			} else {
				this.widget.updateWidgetMap(widgetAttributeMap);
				
				if (unresolvedAttributes != null) {
					for (String key : unresolvedAttributes.keySet()) {
						final WidgetAttribute attribute = widget.getAttribute(parent, localName, key);
						if (attribute != null) {
							WidgetAttributeValue attributeValue = new WidgetAttributeValue(unresolvedAttributes.get(key));
							widget.updateWidgetMap(attribute, attributeValue);
						}
					}
				}
			}
			
			if (parent != null) {
				if (widget.asWidget() != null) {
					parent.add(widget, index);
				}
			}

			if (widget instanceof HasWidgets) {
				parentPushed = true;
				HasWidgets hasWidget = (HasWidgets) widget;
				hasWidgets.push(hasWidget);
			}
		}
		
		
		pushParent.add(parentPushed);
		widgetsStack.add(widget);
		
		return widget;
	}
	
	private String getValue(String key, Attributes attributes) {
		String os = PluginInvoker.getOS().toLowerCase();
		if (attributes.getValue(key + "-" + os) != null) {
			return attributes.getValue(key + "-" + os);
		}
		
		return attributes.getValue(key);
	}
	
	private void populateAttributes(final IWidget widget, final HasWidgets parent, final String tagName, final String localName, Attributes atts, boolean isTemplate) {
		// first theme
		if (!isAndroid ) {
			widget.applyThemeStyle(widget.getGroupName());
		}
		
		//second style/textAppearance etc
		Set<WidgetAttribute> attributes = WidgetFactory.getStyleAttributes(localName);
		if (attributes != null) {
			for (WidgetAttribute widgetAttribute : attributes) {
				String style = atts.getValue(widgetAttribute.getAttributeName());
				if (style != null) {
					pageData.getCss(tagName, style.replaceFirst("@style/", ""), atts.getValue("id"), new CssResult() {
						@Override
						public void put(String key, Attribute value) {
							WidgetAttribute attribute = widget.getAttribute(parent, localName, key);
	
							if (attribute != null) {
								WidgetAttributeValue attributeValue = new WidgetAttributeValue(value.value,
										value.orientation, value.minWidth, value.minHeight, value.maxWidth,
										value.maxHeight);
	
								widget.updateWidgetMap(attribute, attributeValue);
							}
						}
					});
				}
			}
		}
		
		//third local attribute
		for (int i = 0; i < atts.getLength(); i++) {			
			String key = atts.getLocalName(i);
			
			if (key.startsWith("layout_") && isTemplate) {
				widget.addUnResolvedAttribute(key, atts.getValue(i));
			} else {
				final WidgetAttribute attribute = widget.getAttribute(parent, localName, key);
				
				if (attribute != null) {
					WidgetAttributeValue attributeValue = new WidgetAttributeValue(atts.getValue(i));
					widget.updateWidgetMap(attribute, attributeValue);
				} else {
					widget.addUnResolvedAttribute(key, atts.getValue(i));
				}
			}
		}
	}

	
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (inlineResourceName != null) {
			fragment.setInlineResource(inlineResourceName, text, inlineAppend);
		}
		inlineAppend = false;
		inlineResourceName = null;
		text = "";
		if (depth > 0) {
			depth--;
			return;
		}

		if (localName.equalsIgnoreCase("include")) {
			return;
		}
		
		endCreateWidget(localName);
	}

	public void endCreateWidget(String localName) {
		if (pushParent.pop()) {
			hasWidgets.pop();
		}

		
		this.widget = widgetsStack.pop();		
		if (widget != null) {
			widget.initialized();
		}
	}

	public IWidget getRoot() {
		return root;
	}
	
	public void initRoot(HasWidgets parent) {
		root = parent;
		hasWidgets.push(parent);
	}
	
	public void addToCurrentParent(IWidget widget) {
		hasWidgets.peek().add(widget, -1);
	}
}
