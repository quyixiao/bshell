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

package org.springframework.context.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.weaving.AspectJWeavingEnabler;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Parser for the &lt;context:load-time-weaver/&gt; element.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
class LoadTimeWeaverBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	/**
	 * The bean name of the internally managed AspectJ weaving enabler.
	 * @since 4.3.1
	 */
	public static final String ASPECTJ_WEAVING_ENABLER_BEAN_NAME =
			"org.springframework.context.config.internalAspectJWeavingEnabler";

	private static final String ASPECTJ_WEAVING_ENABLER_CLASS_NAME =
			"org.springframework.context.weaving.AspectJWeavingEnabler";

	private static final String DEFAULT_LOAD_TIME_WEAVER_CLASS_NAME =
			"org.springframework.context.weaving.DefaultContextLoadTimeWeaver";

	private static final String WEAVER_CLASS_ATTRIBUTE = "weaver-class";

	private static final String ASPECTJ_WEAVING_ATTRIBUTE = "aspectj-weaving";


	/****
	 *
	 * @param element the {@code Element} that is being parsed
	 * @return
	 *
	 *
	 * 当通过 AspectJ 功能验证后，便可以进行 AspectJWeavingEnabler 注册了，注册方式很简单，无非是将类路径初始化 RootBeanDefinition
	 * 中，在 RootBeanDefinition 的获取时转换成对应的 class 类
	 * 尽管在 init 方法中注册了 AspectJWeavingEnabler，但是对于标签本身 Spring 也会以 bean 的形式保存，也就是当 Spring 解析到了
	 * <context:load-time-weaver></context:load-time-weaver>  标签的时候也会产生一个 bean
	 * 而这个 bean 中的信息是什么呢
	 *
	 * 将 org.Springframework.context.weaving.AspectWeavingEnabler 封装 BeanDefition 中注册。
	 * 当通过 AspectJ 功能难后便可以进行 AspectJWeavingEnabler的注册了，注册的方式很简单，无非是将类路径注册在新的初始化的
	 * RootBeanDefinition 中，在 RootBeanDefinition获取时会转换成对应的 class
	 * 尽管在 init 方法中注册，AspectJWeavingEnabler，但是对于标签本身 Spring 也会了 bean 的形式保存了，也就是当 Spring 解析到
	 * <context:load-time-weaver/> 标签的时候也会产生一个 bean ,而这个 bean 中的信息是什么呢？
	 *
	 * 其中可以看到
	 * 	WEAVER_CLASS_ATTRIBUTE = "weaver-class";
	 *	DEFAULT_LOAD_TIME_WEAVER_CLASS_NAME ="org.springframework.context.weaving.DefaultContextLoadTimeWeaver";
	 *	ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME;
	 * 单凭以上的信息我们至少可以推断，当 Spring 在读取自定义标签<context:load-time-weaver></context:load-time-weaver>
	 * 后会产生一个 bean,而这个 bean 的 id 为 loadTimeWeaver， class 为 org.Springframework.context.weaving
	 * DefaultContextLoadTimeWeaver ,也就是完成了 DefaultContextLoadTimeWeaver 类的注册
	 *
	 * 	完成了以上的注册功能后，并不意味着在 Spring 中就可以使用 AspectJ 了，因为我们还有一个重要的步骤，就是 LoadTimeWeaverAwareProcessor
	 * 	的注册，在 AbstractApplicationContext 中的 prepareBeanFactory 函数中有这样的一段代码
	 *
	 *
	 *
	 *
	 *
	 */
	@Override
	protected String getBeanClassName(Element element) {
		if (element.hasAttribute(WEAVER_CLASS_ATTRIBUTE)) {
			return element.getAttribute(WEAVER_CLASS_ATTRIBUTE);
		}
		return DEFAULT_LOAD_TIME_WEAVER_CLASS_NAME;
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME;
	}


	/****
	 * 继续跟进 LoadTimeWeaverBeanDinitionParser，作为 BeanDefinitionParser 接口实现类，他的核心逻辑是从 parse()函数开始的，
	 * 而经过父类的封装，LoadTimeWeaverBeanDefinitionParser 类的核心实现被转移到了 doParse 函数中，如下
	 *
	 * 其实之前的分析动态 AOP 也就是在分析配置<aop:aspect-autoproxy></aop>中已经反映到了自定义配置的解析流程，对于<aop:aspectj-autoproxy></aop>
	 * 解析无非是以标签作为标志，进而进行相关处理，类的注册，对了么对于自定义标签<context:load-time-weaver/> 其实是起到了同样的作用
	 *
	 * 上面的函数的核心作用其实是注册一个对于ApectJ 处理的类，org.Springframework.context.weaving.AspectJWeavingEnabler，它的注册
	 * 流程总结起来如下
	 *
	 *
	 *
	 */
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

		if (isAspectJWeavingEnabled(element.getAttribute(ASPECTJ_WEAVING_ATTRIBUTE), parserContext)) {
			if (!parserContext.getRegistry().containsBeanDefinition(ASPECTJ_WEAVING_ENABLER_BEAN_NAME)) {
				RootBeanDefinition def = new RootBeanDefinition(ASPECTJ_WEAVING_ENABLER_CLASS_NAME);
				parserContext.registerBeanComponent(
						new BeanComponentDefinition(def, ASPECTJ_WEAVING_ENABLER_BEAN_NAME));
			}

			if (isBeanConfigurerAspectEnabled(parserContext.getReaderContext().getBeanClassLoader())) {
				new SpringConfiguredBeanDefinitionParser().parse(element, parserContext);
			}
		}
	}


	/***
	 *
	 * 是否开启了 AspectJ
	 * 之前虽然反复反映到了在配置文件中加入了<context:load-time-weaver/> 便相当于加入了 AspectJ开关，但是并不是配置了这个标签就意味着
	 * 开启了 AspectJ功能，这个标签中还有一个属性 aspectj-weaving ，这个属性有3下备选的值，on,off,和 autodetect，默认的 autodetect，也就是说
	 * 如果我们只是使用了<context:load-time-weaver/> ,那么 Spring 会帮助我们检测是否可以使用 AspectJ 的功能，而检测的依据便是文件
	 * META-INF/aop.xml 是否存在，看看在 Spring 中的实现方式
	 *
	 *
	 */
	protected boolean isAspectJWeavingEnabled(String value, ParserContext parserContext) {
		if ("on".equals(value)) {
			return true;
		}
		else if ("off".equals(value)) {
			return false;
		}
		else {
			// Determine default...
			// 自动检测
			ClassLoader cl = parserContext.getReaderContext().getBeanClassLoader();
			return (cl != null && cl.getResource(AspectJWeavingEnabler.ASPECTJ_AOP_XML_RESOURCE) != null);
		}
	}

	protected boolean isBeanConfigurerAspectEnabled(@Nullable ClassLoader beanClassLoader) {
		return ClassUtils.isPresent(SpringConfiguredBeanDefinitionParser.BEAN_CONFIGURER_ASPECT_CLASS_NAME,
				beanClassLoader);
	}

}
