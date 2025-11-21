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
	String xml2json(String xml, IFragment fragment);
}
