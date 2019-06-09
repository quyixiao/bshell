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

package org.springframework.aop.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utility class for handling registration of AOP auto-proxy creators.
 *
 * <p>Only a single auto-proxy creator can be registered yet multiple concrete
 * implementations are available. Therefore this class wraps a simple escalation
 * protocol, allowing classes to request a particular auto-proxy creator and know
 * that class, {@code or a subclass thereof}, will eventually be resident
 * in the application context.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 * @see AopNamespaceUtils
 */
public abstract class AopConfigUtils {

	/**
	 * The bean name of the internally managed auto-proxy creator.
	 */
	public static final String AUTO_PROXY_CREATOR_BEAN_NAME =
			"org.springframework.aop.config.internalAutoProxyCreator";

	/**
	 * Stores the auto proxy creator classes in escalation order.
	 */
	private static final List<Class<?>> APC_PRIORITY_LIST = new ArrayList<>();

	/**
	 * Setup the escalation list.
	 */
	static {
		APC_PRIORITY_LIST.add(InfrastructureAdvisorAutoProxyCreator.class);
		APC_PRIORITY_LIST.add(AspectJAwareAdvisorAutoProxyCreator.class);
		APC_PRIORITY_LIST.add(AnnotationAwareAspectJAutoProxyCreator.class);
	}


	@Nullable
	public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAutoProxyCreatorIfNecessary(registry, null);
	}

	@Nullable
	public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry,
			@Nullable Object source) {

		return registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);
	}

	@Nullable
	public static BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAspectJAutoProxyCreatorIfNecessary(registry, null);
	}

	@Nullable
	public static BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry,
			@Nullable Object source) {

		return registerOrEscalateApcAsRequired(AspectJAwareAdvisorAutoProxyCreator.class, registry, source);
	}

	@Nullable
	public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry, null);
	}


	/***
	 *
	 * 注册或者升级 AnnotationAwareAspectJAutoProxyCreator
	 *
	 * 对于 AOP 的实现，基本上都是靠 AnnotationAwareAspectJAutoProxyCreator 去完成，它可以根据@Point 注解定义的切点来自动的代理
	 * 相匹配的 bean ，但是为了配置简便，Spring 自定义的配置来帮助我们自动注册 AnnotionAwareAspectAutoProxyCreator ，其注册过程
	 * 就是这里实现的
	 *
	 */
	@Nullable
	public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry,
			@Nullable Object source) {

		return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, source);
	}


	// 强制使用的过程其实是一个属性设置的过程
	// proxy-target-class ：Spring AOP 的一部分使用 JDK 动态代理或者 CGLIB 来目标对象创建代理 ，建义尽量使用 JDK 动态代理，如果
	// 被代理的实现了至少一个接口，则会使用JDK 动态代理 ，所有的该目标类型实现了接口都将被代理，若该目标对象没有实现任何接口，则创建一个
	// CGLIB 代理 ，如果你希望强制使用 CGLIB 代理 ，例如希望代理目标对象所有的方法，而不只是实现自动接口的方法，那也可以，但是需要考虑
	// 以下的两个问题
	// 无法通知 advise Final 方法 ，因为他们不能被覆写
	// 你需要将 CGLIB 二进制发行包放在 classpath 下面
	// 与之相比较 ，JDK 本身就提供了动态代理，强制使用 CGLIB 代理需要将<aop:config> 的 proxy-target-class 属性设置为 true
	// <aop:config proxy-target-class="true"> .... </aop:config>
	// 当需要使用 CGLIB 代理和@AspectJ 自动支持，可以按照以下的方式设置<aop:aspectj-autoproxy>的 proxy-target-class 属性
	// <aop:aspect-autoproxy proxy-target-class="true">
	// 而实际使用的过程中才会发现 细节的差别，this devil is in the detail
	// JDK 动态代理： 其代理对象必须是某个接口的实现，它是通过在运行期间创建一个接口的实现来完成对目标对象的代理
	// CBLIB 代理：实现原理类似于 JDK 动态代理，只是它在运行期间生成代理对象是针对目标类的扩展子类，CGLIB 是高效的代码生成包，底层是依靠
	// ASM （开源 Java 字节码编辑类库，）操作字节码的实现，性能 JDK 强
	// expose-proxy :有时候目标对象内部的自我调用将无法实施切面中的增强，如下示例
	// 此处是 this 指向目标对象，因此调用 this.b()将不会执行 b 事务切面，即不会执行事务增加，因此 b 方法的事务定义
	// "@Transaction(progagation=Propagation.REQUIRES_NEW)" 将不会实施，为了解决这一个点问题，我们可以这样做
	// <aop:aspectj-autoproxy expose-proxy="true"/>
	// 然后将以上代码中的"this.b()",修改为((AService))AopContext.currentProxy()).b();即可，通过以上的修改便可以完成对 a和 b 方法的
	// 同时增强
	// 最后注册组件并通知，便于监听器进一步处理，这里就不再赘述了


	public static void forceAutoProxyCreatorToUseClassProxying(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			definition.getPropertyValues().add("proxyTargetClass", Boolean.TRUE);
		}
	}

	public static void forceAutoProxyCreatorToExposeProxy(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			definition.getPropertyValues().add("exposeProxy", Boolean.TRUE);
		}
	}

	@Nullable
	private static BeanDefinition registerOrEscalateApcAsRequired(Class<?> cls, BeanDefinitionRegistry registry,
			@Nullable Object source) {

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		// 如果已经存在了自动代理创建且存在的自动代理创建器与现在的的不一致那么需要根据优先级来判断到底需要使用哪些
		// auto_proxy_creator_bean_name
		// "org.Springframework.aop.config.internalautoProxyCreator"
		//
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
				int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
				int requiredPriority = findPriorityForClass(cls);
				if (currentPriority < requiredPriority) {
					// 改变 bean 最重要的就是改变 bean 所对应的 className 属性
					apcDefinition.setBeanClassName(cls.getName());
				}
			}
			//如果已经存在自动代理创建器并且与将要创建的一致，那么无需再此创建
			return null;
		}

		RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
		beanDefinition.setSource(source);
		beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// AUTO_PROXY_CREATOR_BEAN_NAME = "org.Springframework.aop.config.internalAutoProxyCreator"
		// 以上的代码中实现了自动注册 AnnotationAwareAspectJAutoProxyCreator 类的功能，同时这里还涉及了一些优先级
		// 的问题，如果已经存在了自动代理 创建器，而且存在的自动代理创建器现在的不一致，那么需要根据优先级来判断到底使用哪个

		registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
		return beanDefinition;
	}

	private static int findPriorityForClass(Class<?> clazz) {
		return APC_PRIORITY_LIST.indexOf(clazz);
	}

	private static int findPriorityForClass(@Nullable String className) {
		for (int i = 0; i < APC_PRIORITY_LIST.size(); i++) {
			Class<?> clazz = APC_PRIORITY_LIST.get(i);
			if (clazz.getName().equals(className)) {
				return i;
			}
		}
		throw new IllegalArgumentException(
				"Class name [" + className + "] is not a known auto-proxy creator class");
	}

}
