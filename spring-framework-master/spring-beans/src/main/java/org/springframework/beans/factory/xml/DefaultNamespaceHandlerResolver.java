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

package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Default implementation of the {@link NamespaceHandlerResolver} interface.
 * Resolves namespace URIs to implementation classes based on the mappings
 * contained in mapping file.
 *
 * <p>By default, this implementation looks for the mapping file at
 * {@code META-INF/spring.handlers}, but this can be changed using the
 * {@link #DefaultNamespaceHandlerResolver(ClassLoader, String)} constructor.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see NamespaceHandler
 * @see DefaultBeanDefinitionDocumentReader
 */
public class DefaultNamespaceHandlerResolver implements NamespaceHandlerResolver {

	/**
	 * The location to look for the mapping files. Can be present in multiple JAR files.
	 */
	public static final String DEFAULT_HANDLER_MAPPINGS_LOCATION = "META-INF/spring.handlers";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** ClassLoader to use for NamespaceHandler classes */
	@Nullable
	private final ClassLoader classLoader;

	/** Resource location to search for */
	private final String handlerMappingsLocation;

	/** Stores the mappings from namespace URI to NamespaceHandler class name / instance */
	@Nullable
	private volatile Map<String, Object> handlerMappings;


	/**
	 * Create a new {@code DefaultNamespaceHandlerResolver} using the
	 * default mapping file location.
	 * <p>This constructor will result in the thread context ClassLoader being used
	 * to load resources.
	 * @see #DEFAULT_HANDLER_MAPPINGS_LOCATION
	 */
	public DefaultNamespaceHandlerResolver() {
		this(null, DEFAULT_HANDLER_MAPPINGS_LOCATION);
	}

	/**
	 * Create a new {@code DefaultNamespaceHandlerResolver} using the
	 * default mapping file location.
	 * @param classLoader the {@link ClassLoader} instance used to load mapping resources
	 * (may be {@code null}, in which case the thread context ClassLoader will be used)
	 * @see #DEFAULT_HANDLER_MAPPINGS_LOCATION
	 */
	public DefaultNamespaceHandlerResolver(@Nullable ClassLoader classLoader) {
		this(classLoader, DEFAULT_HANDLER_MAPPINGS_LOCATION);
	}

	/**
	 * Create a new {@code DefaultNamespaceHandlerResolver} using the
	 * supplied mapping file location.
	 * @param classLoader the {@link ClassLoader} instance used to load mapping resources
	 * may be {@code null}, in which case the thread context ClassLoader will be used)
	 * @param handlerMappingsLocation the mapping file location
	 */
	public DefaultNamespaceHandlerResolver(@Nullable ClassLoader classLoader, String handlerMappingsLocation) {
		Assert.notNull(handlerMappingsLocation, "Handler mappings location must not be null");
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
		this.handlerMappingsLocation = handlerMappingsLocation;
	}


	/**
	 * Locate the {@link NamespaceHandler} for the supplied namespace URI
	 * from the configured mappings.
	 * @param namespaceUri the relevant namespace URI
	 * @return the located {@link NamespaceHandler}, or {@code null} if none found
	 *
	 *
	 * 有了命名空间，就可以进行 NamespaceHandler 的提取了，继续之前的 parseCustomElement 函数的跟踪，
	 * 分析 NameSpaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(nameUri),
	 * 在 readerContext 初始化的时候其属性，namespaceHandlerResolver 已经被初始化为 DefaultNamespaceHandlerResolver 的实例
	 * 所以，这里调用 resolver 方法其实是调用的是 DefaultNamespaceHandlerResolver类中的方法，我们进入 DefaultNamespaceHandlerResolver 的 resolver 方法进行查看
	 *
	 */
	@Override
	@Nullable
	public NamespaceHandler resolve(String namespaceUri) {
		//获取所有的已经配置的 handler 映射
		Map<String, Object> handlerMappings = getHandlerMappings();
		//根据命名空间找到了对应的信息
		Object handlerOrClassName = handlerMappings.get(namespaceUri);
		if (handlerOrClassName == null) {
			return null;
		}

		else if (handlerOrClassName instanceof NamespaceHandler) {
			//已经做过解析的情况下，直接从缓存中读取
			return (NamespaceHandler) handlerOrClassName;
		}
		else {
			//没有做过解析，则返回类路径
			String className = (String) handlerOrClassName;
			try {
				//使用反射将类路径转化为类
				Class<?> handlerClass = ClassUtils.forName(className, this.classLoader);
				if (!NamespaceHandler.class.isAssignableFrom(handlerClass)) {
					throw new FatalBeanException("Class [" + className + "] for namespace [" + namespaceUri +
							"] does not implement the [" + NamespaceHandler.class.getName() + "] interface");
				}
				//初始化类
				NamespaceHandler namespaceHandler = (NamespaceHandler) BeanUtils.instantiateClass(handlerClass);
				//调用自定义的 NamespaceHandler 的初始化方法
				namespaceHandler.init();
				//记录在缓存
				// 这个方法清晰的阐述了解析自定义的 NamespaceHandler 的过程，通过之前的示例程序，我们了解到了如果使用自定义的标签，那
				//其中的一项必不可少的操作就是就是 Spring.handlers文件中配置的命名空间与命名空间处理器之间的关系，只有这样，Spring 才能根据映射
				//关系找到匹配的处理器，而寻找匹配的处理器就是在上面的函数中的实现，当获取到自定义的 NamespaceHandler之后，就可以进行处理器初始化并
				//解析了，我们不妨再次回忆一下示例中的命名空间的处理器的内部吗？
				/***
				 *
				 public class MyNamespaceHandler extends NamespaceHandlerSupport {

				 	public void init() {
				 		registerBeanDefinitionParser("user2", new UserBeanDefinitionParser());
				 	}

				 }
				 当得到自定义的命名空间处理后，马上会执行 namespaceHandler.init(); 来进行自定义 BeanDefinitionParser 的注册，在这里，你可以
				 注册多个标签解析器，当前示例中只有支持<myname:user 的写法，你也可以在这里注册多个解析器，如<mynameA,<mynameB 等，使得 myname 的
				 命名空间可以支持多个标签的解析
				 注册后，命名空间处理器就可以根据标签的不同来调用不同的解析器进行解析，那么，根据上面的函数与之前的介绍过的例子，我们基本
				 可以推断 getHandlerMapings 的主要功能就是读取 Spring.handlers 配置文件中的缓存在 map 中

				 */

				handlerMappings.put(namespaceUri, namespaceHandler);
				return namespaceHandler;
			}
			catch (ClassNotFoundException ex) {
				throw new FatalBeanException("NamespaceHandler class [" + className + "] for namespace [" +
						namespaceUri + "] not found", ex);
			}
			catch (LinkageError err) {
				throw new FatalBeanException("Invalid NamespaceHandler class [" + className + "] for namespace [" +
						namespaceUri + "]: problem with handler class file or dependent class", err);
			}
		}
	}

	/**
	 * Load the specified NamespaceHandler mappings lazily.
	 */
	private Map<String, Object> getHandlerMappings() {
		//
		Map<String, Object> handlerMappings = this.handlerMappings;
		//如果没有缓存则开始进行缓存
		if (handlerMappings == null) {
			synchronized (this) {
				handlerMappings = this.handlerMappings;
				if (handlerMappings == null) {
					try {
						// this.handlerMappingsLocation 在构造函数中已经被初始化：META-INF/Spring.handlers
						Properties mappings =
								PropertiesLoaderUtils.loadAllProperties(this.handlerMappingsLocation, this.classLoader);
						if (logger.isDebugEnabled()) {
							logger.debug("Loaded NamespaceHandler mappings: " + mappings);
						}
						Map<String, Object> mappingsToUse = new ConcurrentHashMap<>(mappings.size());
						// 将 Properties 格式文件合并到 Map 格式 handlerMappings中
						// 同我们想象的一样，借助工具类 PropertiesLoaderUtils 对属性 handlerMappingsLocation 进行配置文件的读取，
						// handlerMappingsLocation 被默认的初始化为 "META-INF/Spring.handlers"
						CollectionUtils.mergePropertiesIntoMap(mappings, mappingsToUse);
						handlerMappings = mappingsToUse;
						this.handlerMappings = handlerMappings;
					}
					catch (IOException ex) {
						throw new IllegalStateException(
								"Unable to load NamespaceHandler mappings from location [" + this.handlerMappingsLocation + "]", ex);
					}
				}
			}
		}
		return handlerMappings;
	}


	@Override
	public String toString() {
		return "NamespaceHandlerResolver using mappings " + getHandlerMappings();
	}

}
