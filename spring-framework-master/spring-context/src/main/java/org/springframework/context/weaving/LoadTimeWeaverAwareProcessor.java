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

package org.springframework.context.weaving;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * implementation that passes the context's default {@link LoadTimeWeaver}
 * to beans that implement the {@link LoadTimeWeaverAware} interface.
 *
 * <p>{@link org.springframework.context.ApplicationContext Application contexts}
 * will automatically register this with their underlying {@link BeanFactory bean factory},
 * provided that a default {@code LoadTimeWeaver} is actually available.
 *
 * <p>Applications should not use this class directly.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see LoadTimeWeaverAware
 * @see org.springframework.context.ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 */
public class LoadTimeWeaverAwareProcessor implements BeanPostProcessor, BeanFactoryAware {

	@Nullable
	private LoadTimeWeaver loadTimeWeaver;

	@Nullable
	private BeanFactory beanFactory;


	/**
	 * Create a new {@code LoadTimeWeaverAwareProcessor} that will
	 * auto-retrieve the {@link LoadTimeWeaver} from the containing
	 * {@link BeanFactory}, expecting a bean named
	 * {@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}.
	 */
	public LoadTimeWeaverAwareProcessor() {
	}

	/**
	 * Create a new {@code LoadTimeWeaverAwareProcessor} for the given
	 * {@link LoadTimeWeaver}.
	 * <p>If the given {@code loadTimeWeaver} is {@code null}, then a
	 * {@code LoadTimeWeaver} will be auto-retrieved from the containing
	 * {@link BeanFactory}, expecting a bean named
	 * {@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}.
	 * @param loadTimeWeaver the specific {@code LoadTimeWeaver} that is to be used
	 */
	public LoadTimeWeaverAwareProcessor(@Nullable LoadTimeWeaver loadTimeWeaver) {
		this.loadTimeWeaver = loadTimeWeaver;
	}

	/**
	 * Create a new {@code LoadTimeWeaverAwareProcessor}.
	 * <p>The {@code LoadTimeWeaver} will be auto-retrieved from
	 * the given {@link BeanFactory}, expecting a bean named
	 * {@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}.
	 * @param beanFactory the BeanFactory to retrieve the LoadTimeWeaver from
	 */
	public LoadTimeWeaverAwareProcessor(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	/****
	 *
	 * 织入
	 * 当我们完成了所有的 AspectJ 的准备工作后便可以进行织入
	 * 当我们完成了所有的 AspectJ 的准备工作后便可以进行织入分析了，首先还是从 LoadTimeWeaverAvareProcessor 开始.
	 * LoadTimeWeaverAwareProcessor 实现 beanPostProcessor方法，对于 beanPostProcessor 接口来讲，postProcessBeforeInittialization
	 * 与 PostProcessAfterInitialization 有着其特殊意义，也就是说在所有的了 bean 初始化之前与之后都会分别调用对应的庐江，那么在
	 * LoadTimeWeaverAwareProcessor 中的 postProcessBeforeInitialization 函数中完成了什么样的逻辑呢，
	 *
	 * 我们综合之前讲解所有的信息，将所有的相关信息串联在一起来分析这个函数
	 * 在 LoadTimeWeaverAvareProcessor 中的 postProcessBeforeInitialization 函数中，因为开始判断注定这个后处理器只对 LoadTimeWeaverAware
	 * 类型的 bean 起作用，而纵观所有的 bean 的实现，LoadTimeWeaver 接口类只有 AspectJWeavingEnabler
	 *
	 * 当在 Spring 中调用 AspectJWeavingEnabler时，this.loadTimeWeaver，尚未被初始化，那么会直接调用 beanFactory.getBean 方法
	 * 获得对应的 beanLoadTimeWeaver 属性中，当然 AspectJWeavingEnabler 同样的实现了 BeanClassLoaderWare 以及 Ordered接口，实现
	 * BeanClassLoaderAware 接口保证了在 bean 初始化的时候调用，AbstractAutoWireCapableBeanFactory的 invokeAwareMethods 的时候
	 * 将 beanClassLoader 赋值给当前类，而实现 Ordered 接口，实现 BeanClassLoaderAware 接口保证在 bean 初始化的时候调用 AbstractAutowireCapableBeanFactory
	 * 的 invokeAwareMethods 的时候，将 beanClassLoader 赋值给当前类，而实现 Ordered 接口则保证在实例化bean 的时候当前 bean 会被
	 * beanClassLoader 赋值给当前类，而实现了 Ordered 接口则保证了实例化 bean 时，当前 bean 会被最先初始化。
	 * 		而 DefaultContextLoadTimeWeaver 类又同时实现了，LoadTimeWeaver，BeanClassLoaderAware 以及 DisposableBean ，其中
	 * DisposableBean 接口保证在 bean 销毁时会调用 destory 进行 bean 的清理，而 BeanClassLoaderAware 接口则保证在 bean 的初始化
	 * 的调用，AbstractAutowireCapableBeanFactory 的 invokeAwareMethods 时调用 setBeanClassLoader 方法
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 */
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof LoadTimeWeaverAware) {
			LoadTimeWeaver ltw = this.loadTimeWeaver;
			if (ltw == null) {
				Assert.state(this.beanFactory != null,
						"BeanFactory required if no LoadTimeWeaver explicitly specified");
				ltw = this.beanFactory.getBean(
						ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME, LoadTimeWeaver.class);
			}
			((LoadTimeWeaverAware) bean).setLoadTimeWeaver(ltw);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String name) {
		return bean;
	}

}
