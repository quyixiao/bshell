/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.support;

import java.beans.PropertyEditor;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import org.xml.sax.InputSource;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyEditorRegistrySupport;
import org.springframework.beans.propertyeditors.ClassArrayEditor;
import org.springframework.beans.propertyeditors.ClassEditor;
import org.springframework.beans.propertyeditors.FileEditor;
import org.springframework.beans.propertyeditors.InputSourceEditor;
import org.springframework.beans.propertyeditors.InputStreamEditor;
import org.springframework.beans.propertyeditors.PathEditor;
import org.springframework.beans.propertyeditors.ReaderEditor;
import org.springframework.beans.propertyeditors.URIEditor;
import org.springframework.beans.propertyeditors.URLEditor;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * PropertyEditorRegistrar implementation that populates a given
 * {@link org.springframework.beans.PropertyEditorRegistry}
 * (typically a {@link org.springframework.beans.BeanWrapper} used for bean
 * creation within an {@link org.springframework.context.ApplicationContext})
 * with resource editors. Used by
 * {@link org.springframework.context.support.AbstractApplicationContext}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.0
 */
public class ResourceEditorRegistrar implements PropertyEditorRegistrar {

	private final PropertyResolver propertyResolver;

	private final ResourceLoader resourceLoader;


	/**
	 * Create a new ResourceEditorRegistrar for the given {@link ResourceLoader}
	 * and {@link PropertyResolver}.
	 * @param resourceLoader the ResourceLoader (or ResourcePatternResolver)
	 * to create editors for (usually an ApplicationContext)
	 * @param propertyResolver the PropertyResolver (usually an Environment)
	 * @see org.springframework.core.env.Environment
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 * @see org.springframework.context.ApplicationContext
	 */
	public ResourceEditorRegistrar(ResourceLoader resourceLoader, PropertyResolver propertyResolver) {
		this.resourceLoader = resourceLoader;
		this.propertyResolver = propertyResolver;
	}


	/**
	 * Populate the given {@code registry} with the following resource editors:
	 * ResourceEditor, InputStreamEditor, InputSourceEditor, FileEditor, URLEditor,
	 * URIEditor, ClassEditor, ClassArrayEditor.
	 * <p>If this registrar has been configured with a {@link ResourcePatternResolver},
	 * a ResourceArrayPropertyEditor will be registered as well.
	 * @see org.springframework.core.io.ResourceEditor
	 * @see org.springframework.beans.propertyeditors.InputStreamEditor
	 * @see org.springframework.beans.propertyeditors.InputSourceEditor
	 * @see org.springframework.beans.propertyeditors.FileEditor
	 * @see org.springframework.beans.propertyeditors.URLEditor
	 * @see org.springframework.beans.propertyeditors.URIEditor
	 * @see org.springframework.beans.propertyeditors.ClassEditor
	 * @see org.springframework.beans.propertyeditors.ClassArrayEditor
	 * @see org.springframework.core.io.support.ResourceArrayPropertyEditor
	 */
	/***
	 *
	 *
	 1.定义属性编辑器
	 public class DatePropertyEditorRegistrar implements PropertyEditorRegistrar {
		@Override
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			registry.registerCustomEditor(Date.class,new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"),true));
		}
	}

	 2.注册到 Spring中
	 <bean class="org.springframework.beans.factory.config.CustomEditorConfigurer">
	 	<property name="propertyEditorRegistrars">
	 		<list>
	 			<bean class="com.bjsxt.spring.DatePropertyEditorRegistrar"></bean>
	 		</list>
	 	</property>
	 </bean>
	 通过在配置文件中将自定义的 DatePropertyEditorRegistrar 注册进入 org.Springframework.beans.factory.config.CustomEditorConfigurer
	 的 propertyEditorRegistrars 属性中，可以具有与方法同样的效果
	 我们了解了自定义属性的编辑器的使用，但是，似乎这与本节中围绕的核心代码 beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this,getEnvironment()))
	 并无联系，因为在注册自定义属性编辑器的时候使用的是 propertyEditorRegistry 的 registerCustomEditor 方法，而这里使用的是 ConfigurableListableBeanFactory 的
	 addPropertyEditorRegistrar 方法，我们不妨深入探索下 resourceEditoryRegistart 的内部实现，在 resourceEditorRegistar中，我们最
	 关心的方法是 registerCustomEditors


	 */
	@Override
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		ResourceEditor baseEditor = new ResourceEditor(this.resourceLoader, this.propertyResolver);
		// 在 doregisterEditor 函数中，可以看到在之前反映到的自定义属性中使用的关键代码，registry.registerCustomEditor(requiredType,editor)
		//回过头来看看，ResourceEditorRegistrar 类的 registerCustomEditors 方法的核心功能，其实无非是注册了一系列常用的类型
		//属性编辑器，例如doRegisterEditor(registry, Class.class, new ClassEditor(classLoader));实现的功能就是注册
		// Class 类对应的属性编辑器，那么，注册后，一旦某个实体 bean 中存在一些 Class 类型的属性，那么 Spring 会调用
		// ClassEditor 将配置中定义的 String 类型回换成 class 类型并进行赋值，分析到这里，我们不禁有个疑问，虽说
		// ResourceEditorRegistrar 类的 registerCustomEditors 方法实现了指注册的功能，但是 beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this,getEnvironment()))
		// 仅仅是注册了 ResourceEditoRegistrar 实例，却并没有调用 ResourceEditorRegistrar 的 registerCustomEditors
		// 方法进行注册，那么到底是在什么时候进行注册的呢，进一步查看 resourceEditoRegistrar 的 registerCustomEditors 方法的
		// 调用层次结构 ，如图6-1所示
		//发现 abstractbeanFactory 中的 registerCustomEditors 方法中被调用过，继续查看 AbstractBeanFactory 中的
		// registerCustomEditors 方法中调用的层次结构，如图 6-2 所示

		doRegisterEditor(registry, Resource.class, baseEditor);
		doRegisterEditor(registry, ContextResource.class, baseEditor);
		doRegisterEditor(registry, InputStream.class, new InputStreamEditor(baseEditor));
		doRegisterEditor(registry, InputSource.class, new InputSourceEditor(baseEditor));
		doRegisterEditor(registry, File.class, new FileEditor(baseEditor));
		doRegisterEditor(registry, Path.class, new PathEditor(baseEditor));
		doRegisterEditor(registry, Reader.class, new ReaderEditor(baseEditor));
		doRegisterEditor(registry, URL.class, new URLEditor(baseEditor));

		ClassLoader classLoader = this.resourceLoader.getClassLoader();
		doRegisterEditor(registry, URI.class, new URIEditor(classLoader));
		doRegisterEditor(registry, Class.class, new ClassEditor(classLoader));
		doRegisterEditor(registry, Class[].class, new ClassArrayEditor(classLoader));

		if (this.resourceLoader instanceof ResourcePatternResolver) {
			doRegisterEditor(registry, Resource[].class,
					new ResourceArrayPropertyEditor((ResourcePatternResolver) this.resourceLoader, this.propertyResolver));
		}
	}

	/**
	 * Override default editor, if possible (since that's what we really mean to do here);
	 * otherwise register as a custom editor.
	 */
	private void doRegisterEditor(PropertyEditorRegistry registry, Class<?> requiredType, PropertyEditor editor) {
		if (registry instanceof PropertyEditorRegistrySupport) {
			((PropertyEditorRegistrySupport) registry).overrideDefaultEditor(requiredType, editor);
		}
		else {
			registry.registerCustomEditor(requiredType, editor);
		}
	}

}
