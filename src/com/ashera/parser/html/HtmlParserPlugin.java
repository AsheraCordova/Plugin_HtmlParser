package com.ashera.parser.html;

import com.ashera.core.IFragment;
import com.ashera.plugin.IPlugin;
import com.ashera.widget.HasWidgets;
import com.ashera.widget.IWidget;
import com.ashera.widget.PluginInvoker;

public class HtmlParserPlugin implements IPlugin, IHtmlParser {

	@Override
	public Object invoke(String name, Object... args) {
		//start - htmlparser
		switch (name) {
		case "parse":
			return parse((String) args[0],(boolean) args[1],(IFragment) args[2]);
		case "parseWithParent":
			return parseWithParent((String) args[0],(boolean) args[1],(HasWidgets) args[2],(IFragment) args[3]);
		case "parseFile":
			return parseFile((String) args[0],(boolean) args[1],(IFragment) args[2]);
		case "parseFragment":
			return parseFragment((String) args[0],(boolean) args[1],(IFragment) args[2]);
		case "parseInclude":
			parseInclude((HasWidgets) args[0],(String) args[1],(String) args[2],(boolean) args[3],(IFragment) args[4]);
			return null;
		case "getHandler":
			return getHandler((HasWidgets) args[0],(int) args[1],(IFragment) args[2]);
		case "handlerStart":
			return handlerStart((Object) args[0],(IWidget) args[1],(int) args[2]);
		case "handlerEnd":
			handlerEnd((Object) args[0],(IWidget) args[1]);
			return null;
		case "addToCurrentParent":
			addToCurrentParent((Object) args[0],(IWidget) args[1]);
			return null;
		case "xml2json":
			return xml2json((String) args[0],(IFragment) args[1]);
		default:
			break;
		}
		throw new RuntimeException("Unknown method " + name);
		//end - htmlparser
	}

	@Override
	public String getName() {
		return "htmlparser";
	}

	@Override
	public IWidget parse(String html, boolean template, IFragment fragment) {
		return parseWithParent(html, template, null, fragment);
	}

	@Override
	public IWidget parseWithParent(String html, boolean template, HasWidgets parent, IFragment fragment) {
		HtmlSaxHandler handler = new HtmlSaxHandler(fragment, template);
		if(parent != null) {
			handler.initRoot(parent);
		}
		HtmlParser.parse(handler, html);
		return handler.getRoot();
	}


	@Override
	public IWidget parseFile(String fileName, boolean template, IFragment fragment) {
		String html; 
		if (fragment.getRootDirectory() != null) {
			html = PluginInvoker.readCdvDataAsString(fragment.getRootDirectory(), "www/" + fileName, fragment);
		} else {
			html = PluginInvoker.getFileAsset("www/" + fileName, fragment);
		}
		return parse(html, template, fragment);
	}
	

	@Override
	public IWidget parseFragment(String fileNameOrHtml, boolean template, IFragment fragment) {
		String html = fileNameOrHtml; 
		
		if (fileNameOrHtml.startsWith("layout")) {
			html = PluginInvoker.getFileAsset("www/" + fileNameOrHtml, fragment);
		}
		if (!html.trim().endsWith("</layout>")) {
			html = "<layout>" + html + "</layout>";
		}
		return parse(html, template, fragment);
	}

	@Override
	public Object getHandler(HasWidgets parent, int index, IFragment fragment) {
		HtmlSaxHandler handler = new HtmlSaxHandler(fragment, false);
		if (parent != null) {
			handler.initRoot(parent);
		}
		return handler;
	}

	@Override
	public IWidget handlerStart(Object handler, IWidget widget, int index) {
		return ((HtmlSaxHandler) handler).startCreateWidget(widget.getLocalName(), null, null, widget.getId(), index, null, widget.getAttributes(), widget.getParams(), widget.getUnResolvedAttributes());

	}

	@Override
	public void handlerEnd(Object handler, IWidget widget) {
		((HtmlSaxHandler) handler).endCreateWidget(widget.getLocalName());		
	}

	@Override
	public void addToCurrentParent(Object handler, IWidget widget) {
		((HtmlSaxHandler) handler).addToCurrentParent(widget);
		
	}

	@Override
	public void parseInclude(HasWidgets parent, String fileName, String componentId, boolean template, IFragment fragment) {
		HtmlSaxHandler handler = new HtmlSaxHandler(fragment, componentId, template);
		handler.initRoot(parent);
		String html = null;
		
		String inlineResource = fragment.getInlineResource(fileName);
		if (inlineResource != null) {
			html = inlineResource;
		} else {
			fileName = fileName.replace("@layout/", "") + ".xml";
			html = PluginInvoker.getFileAsset("www/layout/" + fileName, fragment);
		}
		
		HtmlParser.parse(handler, html);
	}

	@Override
	public String xml2json(String xml, IFragment fragment) {
		Html2JsonSaxHandler handler = new Html2JsonSaxHandler();
		HtmlParser.parse(handler, xml);
		return handler.toJson();	
	}
	
}
