package com.ashera.parser.html;

import com.ashera.core.IFragment;
import com.ashera.widget.HasWidgets;
import com.ashera.widget.IWidget;

public interface IHtmlParser {
	IWidget parse(String html, boolean template, IFragment fragment);
	IWidget parseWithParent(String html, boolean template, HasWidgets parent, IFragment fragment);
	IWidget parseFile(String fileName, boolean template, IFragment fragment);
	IWidget parseFragment(String fileName, boolean template, IFragment fragment);
	void parseInclude(HasWidgets parent, String fileName, String componentId, boolean template, IFragment fragment);
	Object getHandler(HasWidgets parent, int index,  IFragment fragment);
	IWidget handlerStart(Object handler, IWidget widget, int index);
	void handlerEnd(Object handler, IWidget widget);
	void addToCurrentParent(Object handler, IWidget widget);
	
}
