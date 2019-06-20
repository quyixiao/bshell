/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

/**
 * Strategy interface for translating an incoming
 * {@link javax.servlet.http.HttpServletRequest} into a
 * logical view name when no view name is explicitly supplied.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 *
 * 初始化 RequestToViewNameTranslator
 * 当 controller 处理方法没有一个view 对象或逻辑视图名称，并且在该方法中没有直接 response 的输出流程里面写数据的时候，Spring 就会采用
 * 约定好的方式提供一个逻辑视图的名称，这个逻辑视图的名称是通过 Spring 定义的 org.Springframework.web.servlet.RequestToViewNameTranslator
 * 接口的 getViewName 方法来实现的，我们可以实现自己的 RequestToViewNameTranslator 接口来约定好没有返回名称的时候如何确定视图的名称
 * Spring 已经给我们提供了一个它自己的实现，那就是 org.Springframework.web.servlet.DefaultRequestToViewNameTranslator
 * 在介绍 DefaultRequestToViewNameTranslator 如何约定视图名称之前，先来看一下它支持用户定义的属性
 * 		prefix:前缀，表示约定好的视图名称需要加上前缀，默认的是空串
 * 		suffix: 后缀，表示约定好的视图名称需要加上后缀，默认是空串
 * 		separator: 分隔符，默认是反斜杠
 * 		stripLeadingSlash : 如果是首字符分隔符，是否要去除，默认的 true
 * 		stripTrailingSlash: 如果是最后一个字符的分隔符，是否要去除，默认是 true
 * 		stripExtension: 如果请求的路径包含扩展名是否要去除，默认是 true
 * 		urlDecode :是否需要对 URL解码，默认是 true,它会采用 request指定的编码或者 ISO-8859-1编码 URL 进行解析
 * 	当我们没有在 SpringMVC 的配置文件中的定义一个名为 viewNameTranslator 的 bean的时候，Spring 会为我们提供一个默认的 viewNameTranslatokr
 * 		，即 DefaultRequestToViewNameTranslator
 *
 *
 */
public interface RequestToViewNameTranslator {

	/**
	 * Translate the given {@link HttpServletRequest} into a view name.
	 * @param request the incoming {@link HttpServletRequest} providing
	 * the context from which a view name is to be resolved
	 * @return the view name, or {@code null} if no default found
	 * @throws Exception if view name translation fails
	 */
	@Nullable
	String getViewName(HttpServletRequest request) throws Exception;

}
