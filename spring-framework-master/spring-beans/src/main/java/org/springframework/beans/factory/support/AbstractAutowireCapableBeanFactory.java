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

package org.springframework.beans.factory.support;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract bean factory superclass that implements default bean creation,
 * with the full capabilities specified by the {@link RootBeanDefinition} class.
 * Implements the {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory}
 * interface in addition to AbstractBeanFactory's {@link #createBean} method.
 * <p>
 * <p>Provides bean creation (with constructor resolution), property population,
 * wiring (including autowiring), and initialization. Handles runtime bean
 * references, resolves managed collections, calls initialization methods, etc.
 * Supports autowiring constructors, properties by name, and properties by type.
 * <p>
 * <p>The main template method to be implemented by subclasses is
 * {@link #resolveDependency(DependencyDescriptor, String, Set, TypeConverter)},
 * used for autowiring by type. In case of a factory which is capable of searching
 * its bean definitions, matching beans will typically be implemented through such
 * a search. For other factory styles, simplified matching algorithms can be implemented.
 * <p>
 * <p>Note that this class does <i>not</i> assume or implement bean definition
 * registry capabilities. See {@link DefaultListableBeanFactory} for an implementation
 * of the {@link org.springframework.beans.factory.ListableBeanFactory} and
 * {@link BeanDefinitionRegistry} interfaces, which represent the API and SPI
 * view of such a factory, respectively.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @author Costin Leau
 * @author Chris Beams
 * @author Sam Brannen
 * @see RootBeanDefinition
 * @see DefaultListableBeanFactory
 * @see BeanDefinitionRegistry
 * @since 13.02.2004
 *
 *
 *
 * 综合AbstractBeanFactory并对接口Autowire CapableBeanFactory进行实现
 *
 *
 *
 *
 *
 *
 */
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
		implements AutowireCapableBeanFactory {

	/**
	 * Strategy for creating bean instances
	 */
	private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();

	/**
	 * Resolver strategy for method parameter names
	 */
	@Nullable
	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	/**
	 * Whether to automatically try to resolve circular references between beans
	 */
	private boolean allowCircularReferences = true;

	/**
	 * Whether to resort to injecting a raw bean instance in case of circular reference,
	 * even if the injected bean eventually got wrapped.
	 */
	private boolean allowRawInjectionDespiteWrapping = false;

	/**
	 * Dependency types to ignore on dependency check and autowire, as Set of
	 * Class objects: for example, String. Default is none.
	 */
	private final Set<Class<?>> ignoredDependencyTypes = new HashSet<>();

	/**
	 * Dependency interfaces to ignore on dependency check and autowire, as Set of
	 * Class objects. By default, only the BeanFactory interface is ignored.
	 */
	private final Set<Class<?>> ignoredDependencyInterfaces = new HashSet<>();

	/**
	 * The name of the currently created bean, for implicit dependency registration
	 * on getBean etc invocations triggered from a user-specified Supplier callback.
	 */
	private final NamedThreadLocal<String> currentlyCreatedBean = new NamedThreadLocal<>("Currently created bean");

	/**
	 * Cache of unfinished FactoryBean instances: FactoryBean name --> BeanWrapper
	 */
	private final Map<String, BeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>(16);

	/**
	 * Cache of filtered PropertyDescriptors: bean Class -> PropertyDescriptor array
	 */
	private final ConcurrentMap<Class<?>, PropertyDescriptor[]> filteredPropertyDescriptorsCache =
			new ConcurrentHashMap<>(256);


	/**
	 * Create a new AbstractAutowireCapableBeanFactory.
	 */
	public AbstractAutowireCapableBeanFactory() {
		super();
		ignoreDependencyInterface(BeanNameAware.class);
		ignoreDependencyInterface(BeanFactoryAware.class);
		ignoreDependencyInterface(BeanClassLoaderAware.class);
	}

	/**
	 * Create a new AbstractAutowireCapableBeanFactory with the given parent.
	 *
	 * @param parentBeanFactory parent bean factory, or {@code null} if none
	 */
	public AbstractAutowireCapableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		this();
		setParentBeanFactory(parentBeanFactory);
	}


	/**
	 * Set the instantiation strategy to use for creating bean instances.
	 * Default is CglibSubclassingInstantiationStrategy.
	 *
	 * @see CglibSubclassingInstantiationStrategy
	 */
	public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
		this.instantiationStrategy = instantiationStrategy;
	}

	/**
	 * Return the instantiation strategy to use for creating bean instances.
	 */
	protected InstantiationStrategy getInstantiationStrategy() {
		return this.instantiationStrategy;
	}

	/**
	 * Set the ParameterNameDiscoverer to use for resolving method parameter
	 * names if needed (e.g. for constructor names).
	 * <p>Default is a {@link DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * Return the ParameterNameDiscoverer to use for resolving method parameter
	 * names if needed.
	 */
	@Nullable
	protected ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}

	/**
	 * Set whether to allow circular references between beans - and automatically
	 * try to resolve them.
	 * <p>Note that circular reference resolution means that one of the involved beans
	 * will receive a reference to another bean that is not fully initialized yet.
	 * This can lead to subtle and not-so-subtle side effects on initialization;
	 * it does work fine for many scenarios, though.
	 * <p>Default is "true". Turn this off to throw an exception when encountering
	 * a circular reference, disallowing them completely.
	 * <p><b>NOTE:</b> It is generally recommended to not rely on circular references
	 * between your beans. Refactor your application logic to have the two beans
	 * involved delegate to a third bean that encapsulates their common logic.
	 */
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.allowCircularReferences = allowCircularReferences;
	}

	/**
	 * Set whether to allow the raw injection of a bean instance into some other
	 * bean's property, despite the injected bean eventually getting wrapped
	 * (for example, through AOP auto-proxying).
	 * <p>This will only be used as a last resort in case of a circular reference
	 * that cannot be resolved otherwise: essentially, preferring a raw instance
	 * getting injected over a failure of the entire bean wiring process.
	 * <p>Default is "false", as of Spring 2.0. Turn this on to allow for non-wrapped
	 * raw beans injected into some of your references, which was Spring 1.2's
	 * (arguably unclean) default behavior.
	 * <p><b>NOTE:</b> It is generally recommended to not rely on circular references
	 * between your beans, in particular with auto-proxying involved.
	 *
	 * @see #setAllowCircularReferences
	 */
	public void setAllowRawInjectionDespiteWrapping(boolean allowRawInjectionDespiteWrapping) {
		this.allowRawInjectionDespiteWrapping = allowRawInjectionDespiteWrapping;
	}

	/**
	 * Ignore the given dependency type for autowiring:
	 * for example, String. Default is none.
	 */
	public void ignoreDependencyType(Class<?> type) {
		this.ignoredDependencyTypes.add(type);
	}

	/**
	 * Ignore the given dependency interface for autowiring.
	 * <p>This will typically be used by application contexts to register
	 * dependencies that are resolved in other ways, like BeanFactory through
	 * BeanFactoryAware or ApplicationContext through ApplicationContextAware.
	 * <p>By default, only the BeanFactoryAware interface is ignored.
	 * For further types to ignore, invoke this method for each type.
	 *
	 * @see org.springframework.beans.factory.BeanFactoryAware
	 * @see org.springframework.context.ApplicationContextAware
	 */
	public void ignoreDependencyInterface(Class<?> ifc) {
		this.ignoredDependencyInterfaces.add(ifc);
	}

	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		super.copyConfigurationFrom(otherFactory);
		if (otherFactory instanceof AbstractAutowireCapableBeanFactory) {
			AbstractAutowireCapableBeanFactory otherAutowireFactory =
					(AbstractAutowireCapableBeanFactory) otherFactory;
			this.instantiationStrategy = otherAutowireFactory.instantiationStrategy;
			this.allowCircularReferences = otherAutowireFactory.allowCircularReferences;
			this.ignoredDependencyTypes.addAll(otherAutowireFactory.ignoredDependencyTypes);
			this.ignoredDependencyInterfaces.addAll(otherAutowireFactory.ignoredDependencyInterfaces);
		}
	}


	//-------------------------------------------------------------------------
	// Typical methods for creating and populating external bean instances
	//-------------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public <T> T createBean(Class<T> beanClass) throws BeansException {
		// Use prototype bean definition, to avoid registering bean as dependent bean.
		RootBeanDefinition bd = new RootBeanDefinition(beanClass);
		bd.setScope(SCOPE_PROTOTYPE);
		bd.allowCaching = ClassUtils.isCacheSafe(beanClass, getBeanClassLoader());
		// For the nullability warning, see the elaboration in AbstractBeanFactory.doGetBean;
		// in short: This is never going to be null unless user-declared code enforces null.
		return (T) createBean(beanClass.getName(), bd, null);
	}

	@Override
	public void autowireBean(Object existingBean) {
		// Use non-singleton bean definition, to avoid registering bean as dependent bean.
		RootBeanDefinition bd = new RootBeanDefinition(ClassUtils.getUserClass(existingBean));
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		bd.allowCaching = ClassUtils.isCacheSafe(bd.getBeanClass(), getBeanClassLoader());
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		initBeanWrapper(bw);
		populateBean(bd.getBeanClass().getName(), bd, bw);
	}

	@Override
	public Object configureBean(Object existingBean, String beanName) throws BeansException {
		markBeanAsCreated(beanName);
		BeanDefinition mbd = getMergedBeanDefinition(beanName);
		RootBeanDefinition bd = null;
		if (mbd instanceof RootBeanDefinition) {
			RootBeanDefinition rbd = (RootBeanDefinition) mbd;
			bd = (rbd.isPrototype() ? rbd : rbd.cloneBeanDefinition());
		}
		if (bd == null) {
			bd = new RootBeanDefinition(mbd);
		}
		if (!bd.isPrototype()) {
			bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
			bd.allowCaching = ClassUtils.isCacheSafe(ClassUtils.getUserClass(existingBean), getBeanClassLoader());
		}
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		initBeanWrapper(bw);
		populateBean(beanName, bd, bw);
		// For the nullability warning, see the elaboration in AbstractBeanFactory.doGetBean;
		// in short: This is never going to be null unless user-declared code enforces null.
		return initializeBean(beanName, existingBean, bd);
	}

	@Override
	@Nullable
	public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName) throws BeansException {
		return resolveDependency(descriptor, requestingBeanName, null, null);
	}


	//-------------------------------------------------------------------------
	// Specialized methods for fine-grained control over the bean lifecycle
	//-------------------------------------------------------------------------

	@Override
	public Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
		// Use non-singleton bean definition, to avoid registering bean as dependent bean.
		RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		// For the nullability warning, see the elaboration in AbstractBeanFactory.doGetBean;
		// in short: This is never going to be null unless user-declared code enforces null.
		return createBean(beanClass.getName(), bd, null);
	}

	@Override
	public Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
		// Use non-singleton bean definition, to avoid registering bean as dependent bean.
		final RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		if (bd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR) {
			return autowireConstructor(beanClass.getName(), bd, null, null).getWrappedInstance();
		} else {
			Object bean;
			final BeanFactory parent = this;
			if (System.getSecurityManager() != null) {
				bean = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
								getInstantiationStrategy().instantiate(bd, null, parent),
						getAccessControlContext());
			} else {
				bean = getInstantiationStrategy().instantiate(bd, null, parent);
			}
			populateBean(beanClass.getName(), bd, new BeanWrapperImpl(bean));
			return bean;
		}
	}

	@Override
	public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
			throws BeansException {

		if (autowireMode == AUTOWIRE_CONSTRUCTOR) {
			throw new IllegalArgumentException("AUTOWIRE_CONSTRUCTOR not supported for existing bean instance");
		}
		// Use non-singleton bean definition, to avoid registering bean as dependent bean.
		RootBeanDefinition bd =
				new RootBeanDefinition(ClassUtils.getUserClass(existingBean), autowireMode, dependencyCheck);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		initBeanWrapper(bw);
		populateBean(bd.getBeanClass().getName(), bd, bw);
	}

	@Override
	public void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException {
		markBeanAsCreated(beanName);
		BeanDefinition bd = getMergedBeanDefinition(beanName);
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		initBeanWrapper(bw);
		applyPropertyValues(beanName, bd, bw, bd.getPropertyValues());
	}

	@Override
	public Object initializeBean(Object existingBean, String beanName) {
		return initializeBean(beanName, existingBean, null);
	}


	/****
	 * 处理器的应用
	 * beanPostProcessor 相信大家都不陌生，这是 Spring 中开放式架构中一个必不可少的亮点
	 * 给用户充足的权限去更改或者去 扩展 Spring，而除了 BeanPostProcessor 外还有很多其他的 PostProcessor，当然
	 * 大部分的都是以此为基础的，继承自BeanPostProcessor，BeanPostProcessor 的使用位置就是这里，在调用客户自定义
	 * 初始化方法前以及调用自定义初始化方法后分别会调用 BeanPostProcessor 的 PostProcessBeforeInitialization 和
	 * postProcessAfterInitialization 方法，使用户可以根据自己的业务需求进行响应的处理
	 *
	 */
	@Override
	public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
			throws BeansException {

		Object result = existingBean;
		for (BeanPostProcessor beanProcessor : getBeanPostProcessors()) {
			Object current = beanProcessor.postProcessBeforeInitialization(result, beanName);
			if (current == null) {
				return result;
			}
			result = current;
		}
		return result;
	}

	/****
	 * 对于后处理器的使用我们还未过多的接触，后续章节会使用大量的篇幅介绍，这里，我们只需要了解在 Spring 获取 bean 的规则中的一条
	 * ，尽可能的保证所有的 bean 初始化后都会调用注册的 BeanPostProcessor 的 postProcessAfterInitialization 方法进行处理，
	 * 在实际开发过程中大可以针对此特性设计业务逻辑
	 * @param existingBean the new bean instance
	 * @param beanName     the name of the bean
	 * @return
	 * @throws BeansException
	 *

	 *
	 *
	 */
	@Override
	public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
			throws BeansException {

		Object result = existingBean;
		for (BeanPostProcessor beanProcessor : getBeanPostProcessors()) {
			Object current = beanProcessor.postProcessAfterInitialization(result, beanName);
			if (current == null) {
				return result;
			}
			result = current;
		}
		return result;
	}

	@Override
	public void destroyBean(Object existingBean) {
		new DisposableBeanAdapter(existingBean, getBeanPostProcessors(), getAccessControlContext()).destroy();
	}


	//---------------------------------------------------------------------
	// Implementation of relevant AbstractBeanFactory template methods
	//---------------------------------------------------------------------

	/**
	 * Central method of this class: creates a bean instance,
	 * populates the bean instance, applies post-processors, etc.
	 *
	 * @see #doCreateBean
	 *
	 * 我们不可能指望在一个函数中的完成一个复杂的逻辑，而且我们跟踪了这么多Spring代码，经历了这么多的函数，或多或少的发现我们
	 * 一些规律，一个真正干活的函数都是以 do开头的，比如 doGetObjectFromBean，而给我们错觉就是函数，比如 getObjectFromFactoryBean，
	 * 其实只是从 全局角度上去做一些统筹工作，这个规则对于 createBean 也不例外，那么让我们看看在 createBean 函数中做的哪些你准备工作
	 *
	 *
	 */
	@Override
	protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException {

		if (logger.isDebugEnabled()) {
			logger.debug("Creating instance of bean '" + beanName + "'");
		}
		RootBeanDefinition mbdToUse = mbd;

		// Make sure bean class is actually resolved at this point, and
		// clone the bean definition in case of a dynamically resolved Class
		// which cannot be stored in the shared merged bean definition.
		// 锁定 class,根据设置的 class 属性或者根据 className 来解析 Class
		Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
		if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
			mbdToUse = new RootBeanDefinition(mbd);
			mbdToUse.setBeanClass(resolvedClass);
		}

		// Prepare method overrides.
		try {
			//验证及准备覆盖的方法
			mbdToUse.prepareMethodOverrides();
		} catch (BeanDefinitionValidationException ex) {
			throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
					beanName, "Validation of method overrides failed", ex);
		}

		try {
			// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
			// 给 BeanPostProcessors 一个机会返回代理来替代真正的实例
			Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
			if (bean != null) {
				return bean;
			}
		} catch (Throwable ex) {
			throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
					"BeanPostProcessor before instantiation of bean failed", ex);
		}

		try {
			//从代码中的我们可以总结出函数的完成的具体步骤及功能
			//根据设置的 class 属性或者根据 ClassName 来解析 Class
			// 对 override 属性进行标记及验证
			//很多的读者可能会不知道这个方法的作用，因为在这个 Spring 配置里根本没有诸如 override-method 之类的配置
			//那么这个方法到底是干什么的呢
			// 其实在Spring 中确实没有 override-method 这样的配置，但是如果读过前面的部分，可能会发现，在 Spring 配置中存在
			//lookup-method 和 replace-method 的，而这两个配置的加载其实就是将配置统一放在 BeanDefinition中的 methodOverrides 属性里
			// 而这个函数操作其实也就是针对于这两相配置的
			//应用初始化的后处理器，解析指定的 bean是否存在初始化的短路操作
			//创建 bean
			//我们首先查看下对 override 属性标记及验证的逻辑实现

			Object beanInstance = doCreateBean(beanName, mbdToUse, args);
			if (logger.isDebugEnabled()) {
				logger.debug("Finished creating instance of bean '" + beanName + "'");
			}
			return beanInstance;
		} catch (BeanCreationException ex) {
			// A previously detected exception with proper bean creation context already...
			throw ex;
		} catch (ImplicitlyAppearedSingletonException ex) {
			// An IllegalStateException to be communicated up to DefaultSingletonBeanRegistry...
			throw ex;
		} catch (Throwable ex) {
			throw new BeanCreationException(
					mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
		}
	}

	/**
	 * Actually create the specified bean. Pre-creation processing has already happened
	 * at this point, e.g. checking {@code postProcessBeforeInstantiation} callbacks.
	 * <p>Differentiates between default bean instantiation, use of a
	 * factory method, and autowiring a constructor.
	 *
	 * @param beanName the name of the bean
	 * @param mbd      the merged bean definition for the bean
	 * @param args     explicit arguments to use for constructor or factory method invocation
	 * @return a new instance of the bean
	 * @throws BeanCreationException if the bean could not be created
	 * @see #instantiateBean
	 * @see #instantiateUsingFactoryMethod
	 * @see #autowireConstructor
	 * 循环依赖
	 * 实例化 bean 是一个非常复杂的过程，而其中最比较难以理解的就是对循环依赖的解决,不管是之前的读者有没有对循环依赖方面的研究
	 * ，这里有必要先对此知识稍作回顾
	 * 什么是我一依赖
	 * 	循环依赖，
	 * 		实例化bean 是一个非常得要的过程，而其中最比较难以理解的对循环依赖的解决不管之前读者有没有依赖方面的研究，这里必要先对此知识
	 * 稍做回顾的
	 * 		什么是循环依赖
	 * 	循环依赖是循环引用的，就是两个或多个 bean 相互之间的持有对方，比如 CircleA引用 CirCleB ，CirCleB 引用 CircleC，CirCleC 引用
	 * 	CirCleA，则它们最终反映为一个环，此处不是循环调用，循环调用的方法之间是口环调用，如果5-2所示
	 * 	循环调用是无法解决的，除非有终结条件，否则就是死循环，最终导致内存溢出的错误
	 * 		Spring 是如何解决循环依赖的呢
	 * 	Spring 容器循环依赖包括构造器循环依赖和 setter 循环依赖，那 Spring 容器是如何解决循环依赖的呢，首先我们来定义循环引用的类
	 * 		public class TestA{
	 * 		 	private TestB testb;
	 * 		 	public void a(){
	 * 		 	  testb.b();
	 * 		 	}
	 * 		 	public TestB gettestB(){
	 * 		 	  return testb;
	 * 		 	}
	 * 		 public setTestB(testB b ){
	 *				this.testb = b;
	 * 		 }
	 * 		}
	 *
	 * 	public class TestB(){
	 * 	    private TestC testC;
	 *
	 * 	    public TestC getTestC(){
	 * 	    	return testC;
	 * 	    }
	 * 	    public void setTestC(Test c ){
	 * 	        this.testC = c;
	 * 	    }
	 *
	 * 	}
	 *
	 * 	public Class TestC {
	 * 	    public TestA testA;
	 * 	    public void c(){
	 * 	        testA.a();
	 * 	    }
	 * 	    public TestA getTestA(){
	 * 	        return testA;
	 * 	    }
	 * 	    public void setTestA(TestA a ){
	 * 	        this.testA = a;
	 * 	    }
	 * 	}
	 * 	在 Spring 中将循环依赖分为三种情况
	 * 	表示通过构造器注入构成的循环依赖，此依赖是无法解决的，只能抛出 BeanCurrentlyInCreationException 异常表示的循环依赖
	 * 	如果在创建 TestA 类时，构造器需要 TestB 类，在创建 TestB类时，又发现需要 TestC 的类，则又要去创建 TestC,最终在创建 TestC时
	 * 	发现又需要TestA,从而形成一个环，没有法办创建
	 * 	Spring 容器将每一个正在创建 bean 的标识符放在一个 "当前创建 bean 池" 中，bean 标识在创建过程中一直保持在这个池中，因此，如果
	 * 	在创建 bean 的过程中，发现自己已经在当前创建 bean 池里时，将抛出 BeanCurrentlyInCreationException，异常表示循环依赖，而对于创建
	 * 	完毕的 bean 将从当前创建 bean 池中清除掉
	 *
	 * 	我们可以通过直观的测试一个用例来进行分析
	 * 	<bean id = "testA" class="com.bean.TestA">
	 * 	    <construct-arg index="0" ref="testB"/>
	 * 	</bean>
	 * 	<bean id="TestB" class="com.bean.TestB">
	 * 		<constructor-arg index="0" ref="testC"/>
	 *  </bean>
	 * <bean id="testC" class="com.bean.TestC">
	 * 		<constructor-arg index="0" ref="testA"/>
	 * </bean>
	 *
	 * 创建测试用例
	 * @Test(expected = BeanCurrentlyInCreationException.class)
	 * public void testCircleByConstructor() throws Throwable{
	 *     try{
	 *         new ClassPathXmlApplicationContext("text.xml");
	 *     }catch(Exception e){
	 *         //因为要在创建 testC 时抛出
	 *         throwable e1 = e.getCause().getCause().getCause();
	 *         throw e1;
	 *     }
	 * }
	 *
	 *
	 *
	 * 针对以上代码分析如下
	 * 	Spring 容器创建 testA bean 首先去当前创建 bean 的池，查找是否当前bean正在创建，如果没有发现，则继续准备
	 * 	其需要的构造器参数 "testB" ,并将"testA" 标识符放到 "当前创建的 bean 的池中"
	 * 	 Spring 容器创建"testB" bean 首先去 当前创建 bean 池，查找是否当前 bean 正在创建，如果没有发现，则继续准备
	 * 	 其需要的构造器参数 "testC" ，并将"testB"标识符放到当前 创建的 bean 池中
	 * 	 Spring 容器创建 "testC" bean，首先去"当前创建的 bean 池中"查找是否当前 bean正在创建，如果没有发现，则继续准备其需要的构造器参数
	 * 	 "testA" ,并将"testC" 标识符放到"当前创建的 bean 池中"
	 * 到此为止 Spring 容器要创建的"testA" bean 发现该 bean 标识符在当前创建 bean 池中，因为表示循环依赖，
	 * 抛出 BeanCurrentlyInCreationException
	 *
	 *
	 *
	 *
	 *	介绍了循环依赖以及 Spring 中的循环依赖的处理方式后，我们继续4.5小结中的内容，当经过 resolveBeforeInstantiation 方法后
	 *程序有两个选择，如果创建了代理或者说重写了 InstantiationAwareBeanPostProcessor 的 postProcessBeforeInstantiation 方法并
	 *并在方法 postProcessBeforeInstantiation 中改变了 bean，则直接返回就可以了，否则需要进行常规需要bean 的创建而常规的 bean
	 * 的创建就是 beandoCreateBean 中完成
	 *
	 *
	 *
	 * 尽管日志和异常的内容非常的重要，但是阅读源码的时候似乎大部分人都会被直接掉，在此不深入探讨日志及异常的设计，我们看看整个函数的概要
	 * 思路
	 * 1.如果是单例则需要首先清除缓存
	 * 2.实例化 bean，将 beanDefinition 转换为 BeanWrapper
	 * 转换是一个复杂的过程，但是我们可以尝试要概括大致的功能，如下所示
	 * 		如果存在工厂方法则使用工厂方法进行初始化
	 * 		一个类有多个构造函数，每个构造函数都是不同的参数，所以需要根据参数锁定构造函数并进行初始化。
	 *		如果既不存在工厂方法也不存在带有参数的构造函数，则使用默认的构造函数进行 bean 的初始化。
	 * 3. MergedBeanDefinitionPostProcessor 的应用
	 * 	bean 合并后的处理，autowired 注解正是通过此方法实现诸如类型的预解析
	 * 4.依赖处理
	 * 	在 Spring 中会有循环依赖的情况，例如当，A中含有B中的属性，而 B中又含有 A的属性，时就会构成一个循环依赖，此时如果 A B 都是单例
	 * 	,那么在 Spring 中的处理方就是当创建 B 的时候，涉及自动注入A 的步骤，并不是直接去再次创建 A,而是通过放入缓存中的 ObjectFactory
	 * 	来创建实例，这样就解决了循环依赖的问题
	 * 5.属性的填充，将所有的属性填充到 bean 的实例中
	 * 6.循环依赖检查
	 * 	之前有提到过，在 Spring 中解决循环依赖只对单例有效，而对于 prototype 的 bean，Spring 没有好的解决办法，唯一要做的就是抛异常
	 * 	在这个步骤里面会检测已经加载的 bean 是否已经出现了循环依赖，并判断是否需要抛出异常
	 * 7.注册 DisposableBean
	 * 	如果配置了 destroy-method，这里需要注册以便于在销毁的时候调用
	 * 8.完成创建并返回
	 * 	可以看到，下面的步骤非常的繁琐，每步都使用大量的代码来完成其功能，最复杂的也就是最难以理解的当属循环依赖的处理，在真正进入 doCreateBean
	 * 	前面我们有必要先了解下循环依赖
	 *
	 *
	 *
	 * 	这段代码不是很复杂，但是很多人是不太理解这段代码的作用，而且，这段代码仅仅从函数中去理解很难弄懂其中的含义，我们需要从全局的角度上
	 * 	去考虑 Spring 依赖解决方法
	 *
	 *
	 * earlySingletonExposure ：从字面上的意思是理解就是提早暴光的单例，我们暂时不定义他的学名叫什么，我们感兴趣的有哪些条件影响这个值
	 * mbd.isSingleton()：没有太多可以解释的，此 rootBeanDefinition 代表的是否是单例
	 * this.allowCircularReferences:是否允许循环依赖，很抱歉，并没有找到配置在文件中如何配置，但是在 AbstractRefreshableApplicationContext 中
	 * 提供了设置函数，可以通过硬编码的方式进行设置或者可以通过自定义的命名空间来进行配置，其中硬编码的方式代码如下
	 * ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext("aspectText.xml");
	 * bf.setAllowBeanDefinitionOverriding(false);
	 * isSingletonCurrentlyInCreation(beanName):该 bean 是否在创建中，在 Spring 中，会有个专门属性默认为 DefaultSingletonBeanRegistry
	 * 的 singletonsCurrentlyInCreation 来记录 bean 的加载状态，在 bean 开始创建前会将 beanName 记录在属性中，在 bean 的创建结束
	 * 后从属性中移除，那么我们跟随代码一路起来可是对这个属性的记录并没有多少印象，这个状态是哪里记录的呢，不同的 scope 的记录位置并不是一
	 * 样的，我们以 singleton 为例，在 singleton 下记录属性的函数在 DefaultSingletonBeanRegistry 类的 public Object getSingleton
	 * (String beanName,ObjectFactory singletonFactory) 函数的 beforeSingletonCreation(beanName)和 afterSingletonCreation(BeanName)
	 * 中，在这两段函数中分别 this.singletonsCurrentlyInCreation.add(beanName)与 this.singletonsCurrentInCreation.remove(beanName)
	 * 来进行状态的记录与移除的
	 *  经过以上的分析，我们了解变量 earlySingletonExposure 是否是单例，是否允许循环依赖，是否对应 bean 正在创建 bean 条件综合
	 *  ，当3个条件都满足时会执行，addSingletonFactory操作，那么加入 SingletonFactory 的作用是什么呢，又是什么时候调用呢
	 *  我们还是以最简单的 AB 循环依赖为例，类 A 中含有属性类 B，而类 B中又含有属性类 A,那么初始化 BeanA的过程如图 5-3所示
	 *
	 *  图5-3展示了创建 beanA 的流程，图中我们看到，在创建 A 的时候首先会记录类 A 所对应的 beanName，并将 beanA创建的工厂加入到缓存中
	 *  ，而在对 A 属性进行填充的也就是调用 populate 方法的时候又会再一次对 B 进行递归创建，同样的，因为在 B 中同样存在 A 属性，因此在实例化
	 *  B 的 populate 方法中又会再次的初始化 B，也就是图形的最后，调用 getBean(A),关键是在这里，有心的同学可以去找找相关的代码实现方式，
	 *  我们之前已经讲过，在这个函数中，并不是直接去实例化 A，而是先去检测缓存中是否存在已经创建好的对应的 bean,或者是否已经创建好的 ObjectFactory
	 *  ,而此时对于 A 的 ObjectFactory 我们早已创建，所以便不会再去向后执行，而是直接调用ObjectFactory去创建，A，这里最关键的是 ObjectFactory 的实现
	 *
	 *
	 *  在 getEarlyBeanReference 函数中并没有太多的逻辑处理，或者说除了后处理的调用外没有别的处理工作，根据以上的分析，基本可以理清
	 *  Spring 处理循环依赖的解决办法，在 B 创建依赖 A 时通过 ObjectkFactory提供的实例化方法来中断 A 中的属性填充，使得 B 中的持有的 A
	 *  仅仅是风初始化并没有填充任何属性 A，而这正初始化 A 的步骤还是在最开始的创建 A 的时候进行的,但是因为 A 与 B 中的 A 所表示的属性地址是
	 *  一样的，所以在 A 中创建的 好的属性填充自然可以通过 A 获取，这样就解决了循环依赖的问题了
	 *
	 *
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
	protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
			throws BeanCreationException {

		// Instantiate the bean.
		BeanWrapper instanceWrapper = null;
		if (mbd.isSingleton()) {
			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
		}
		if (instanceWrapper == null) {
			//根据指定的 bean 使用对应的使用对应的策略创建新的实例，如工厂方法，构造函数自动注入，简单的初始化
			instanceWrapper = createBeanInstance(beanName, mbd, args);
		}
		final Object bean = instanceWrapper.getWrappedInstance();
		Class<?> beanType = instanceWrapper.getWrappedClass();
		if (beanType != NullBean.class) {
			mbd.resolvedTargetType = beanType;
		}

		// Allow post-processors to modify the merged bean definition.
		synchronized (mbd.postProcessingLock) {
			if (!mbd.postProcessed) {
				try {
					//应用的 MergedBeanDefitionPostProcessor
					applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
				} catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Post-processing of merged bean definition failed", ex);
				}
				mbd.postProcessed = true;
			}
		}

		// Eagerly cache singletons to be able to resolve circular references
		// even when triggered by lifecycle interfaces like BeanFactoryAware.
		// 是否需要提早暴光，单例&允许循环依赖&当前的 bean正在创建中，检测循环依赖
		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
				isSingletonCurrentlyInCreation(beanName));
		if (earlySingletonExposure) {
			if (logger.isDebugEnabled()) {
				logger.debug("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			//为了避免后期的循环依赖，可以在 bean 初始化完成前将创建的实例的 ObjectFactory 加入工厂
			addSingletonFactory(beanName, new ObjectFactory<Object>() {
				@Override
				public Object getObject() throws BeansException {
					// 对 bean 的再一次依赖引用，主要是应用 SmartInstantiationAware BeanPostProcessor
					//其中我们熟知的 Aop 就是在这里将 Advice 动态的织入到 bean 中，若没有则直接返回 bean，不做任何处理
					return AbstractAutowireCapableBeanFactory.this.getEarlyBeanReference(beanName, mbd, bean);
				}
			});
		}

		// Initialize the bean instance.
		// 对 bean 再一次依赖引用，主要应用 SmartInstantiationAware BeanPostProcessor
		// 其中我们熟知的 AOP 就是这里将 advice 动态织入到 bean 中，若没有则直接返回 bean,不做任何处理
		Object exposedObject = bean;
		try {
			// 对 bean 进行填充，将各个属性的值注入，其中，可以存在依赖于其他的 bean 的属性，则会递归初始化依赖 bean
			populateBean(beanName, mbd, instanceWrapper);
			//调用初始化方法如，init-method
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		} catch (Throwable ex) {
			if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
				throw (BeanCreationException) ex;
			} else {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
			}
		}

		if (earlySingletonExposure) {
			//
			Object earlySingletonReference = getSingleton(beanName, false);
			// earlySingletonReference 只有在检测到有循环依赖的情况下才会不为空
			if (earlySingletonReference != null) {
				//如果 exposeObject 没有初始化方法中被改变，也就是没有被增强
				if (exposedObject == bean) {
					exposedObject = earlySingletonReference;
				} else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
					String[] dependentBeans = getDependentBeans(beanName);
					Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
					for (String dependentBean : dependentBeans) {
						//检测依赖，
						if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
							actualDependentBeans.add(dependentBean);
						}
					}
					/****
					 * 因为 bean 创建后其所有的依赖的 bean 一定是已经创建的，actualDependentBeans 不为空则表示当前 bean 创建后其
					 * 依赖的 bean 却没有全部创建完毕，也就是说存在循环依赖
					 */
					if (!actualDependentBeans.isEmpty()) {
						throw new BeanCurrentlyInCreationException(beanName,
								"Bean with name '" + beanName + "' has been injected into other beans [" +
										StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
										"] in its raw version as part of a circular reference, but has eventually been " +
										"wrapped. This means that said other beans do not use the final version of the " +
										"bean. This is often the result of over-eager type matching - consider using " +
										"'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
					}
				}
			}
		}

		// Register bean as disposable.
		try {
			//根据 scopse 注册 bean
			registerDisposableBeanIfNecessary(beanName, bean, mbd);
		} catch (BeanDefinitionValidationException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
		}

		return exposedObject;
	}

	@Override
	@Nullable
	protected Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		Class<?> targetType = determineTargetType(beanName, mbd, typesToMatch);

		// Apply SmartInstantiationAwareBeanPostProcessors to predict the
		// eventual type after a before-instantiation shortcut.
		if (targetType != null && !mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
					SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
					Class<?> predicted = ibp.predictBeanType(targetType, beanName);
					if (predicted != null && (typesToMatch.length != 1 || FactoryBean.class != typesToMatch[0] ||
							FactoryBean.class.isAssignableFrom(predicted))) {
						return predicted;
					}
				}
			}
		}
		return targetType;
	}

	/**
	 * Determine the target type for the given bean definition.
	 *
	 * @param beanName     the name of the bean (for error handling purposes)
	 * @param mbd          the merged bean definition for the bean
	 * @param typesToMatch the types to match in case of internal type matching purposes
	 *                     (also signals that the returned {@code Class} will never be exposed to application code)
	 * @return the type for the bean if determinable, or {@code null} otherwise
	 */
	@Nullable
	protected Class<?> determineTargetType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		Class<?> targetType = mbd.getTargetType();
		if (targetType == null) {
			targetType = (mbd.getFactoryMethodName() != null ?
					getTypeForFactoryMethod(beanName, mbd, typesToMatch) :
					resolveBeanClass(mbd, beanName, typesToMatch));
			if (ObjectUtils.isEmpty(typesToMatch) || getTempClassLoader() == null) {
				mbd.resolvedTargetType = targetType;
			}
		}
		return targetType;
	}

	/**
	 * Determine the target type for the given bean definition which is based on
	 * a factory method. Only called if there is no singleton instance registered
	 * for the target bean already.
	 * <p>This implementation determines the type matching {@link #createBean}'s
	 * different creation strategies. As far as possible, we'll perform static
	 * type checking to avoid creation of the target bean.
	 *
	 * @param beanName     the name of the bean (for error handling purposes)
	 * @param mbd          the merged bean definition for the bean
	 * @param typesToMatch the types to match in case of internal type matching purposes
	 *                     (also signals that the returned {@code Class} will never be exposed to application code)
	 * @return the type for the bean if determinable, or {@code null} otherwise
	 * @see #createBean
	 */
	@Nullable
	protected Class<?> getTypeForFactoryMethod(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		ResolvableType cachedReturnType = mbd.factoryMethodReturnType;
		if (cachedReturnType != null) {
			return cachedReturnType.resolve();
		}

		Class<?> factoryClass;
		boolean isStatic = true;

		String factoryBeanName = mbd.getFactoryBeanName();
		if (factoryBeanName != null) {
			if (factoryBeanName.equals(beanName)) {
				throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
						"factory-bean reference points back to the same bean definition");
			}
			// Check declared factory method return type on factory class.
			factoryClass = getType(factoryBeanName);
			isStatic = false;
		} else {
			// Check declared factory method return type on bean class.
			factoryClass = resolveBeanClass(mbd, beanName, typesToMatch);
		}

		if (factoryClass == null) {
			return null;
		}
		factoryClass = ClassUtils.getUserClass(factoryClass);

		// If all factory methods have the same return type, return that type.
		// Can't clearly figure out exact method due to type converting / autowiring!
		Class<?> commonType = null;
		Method uniqueCandidate = null;
		int minNrOfArgs = mbd.getConstructorArgumentValues().getArgumentCount();
		Method[] candidates = ReflectionUtils.getUniqueDeclaredMethods(factoryClass);
		for (Method factoryMethod : candidates) {
			if (Modifier.isStatic(factoryMethod.getModifiers()) == isStatic &&
					factoryMethod.getName().equals(mbd.getFactoryMethodName()) &&
					factoryMethod.getParameterCount() >= minNrOfArgs) {
				// Declared type variables to inspect?
				if (factoryMethod.getTypeParameters().length > 0) {
					try {
						// Fully resolve parameter names and argument values.
						Class<?>[] paramTypes = factoryMethod.getParameterTypes();
						String[] paramNames = null;
						ParameterNameDiscoverer pnd = getParameterNameDiscoverer();
						if (pnd != null) {
							paramNames = pnd.getParameterNames(factoryMethod);
						}
						ConstructorArgumentValues cav = mbd.getConstructorArgumentValues();
						Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = new HashSet<>(paramTypes.length);
						Object[] args = new Object[paramTypes.length];
						for (int i = 0; i < args.length; i++) {
							ConstructorArgumentValues.ValueHolder valueHolder = cav.getArgumentValue(
									i, paramTypes[i], (paramNames != null ? paramNames[i] : null), usedValueHolders);
							if (valueHolder == null) {
								valueHolder = cav.getGenericArgumentValue(null, null, usedValueHolders);
							}
							if (valueHolder != null) {
								args[i] = valueHolder.getValue();
								usedValueHolders.add(valueHolder);
							}
						}
						Class<?> returnType = AutowireUtils.resolveReturnTypeForFactoryMethod(
								factoryMethod, args, getBeanClassLoader());
						uniqueCandidate = (commonType == null ? factoryMethod : null);
						commonType = ClassUtils.determineCommonAncestor(returnType, commonType);
						if (commonType == null) {
							// Ambiguous return types found: return null to indicate "not determinable".
							return null;
						}
					} catch (Throwable ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("Failed to resolve generic return type for factory method: " + ex);
						}
					}
				} else {
					uniqueCandidate = (commonType == null ? factoryMethod : null);
					commonType = ClassUtils.determineCommonAncestor(factoryMethod.getReturnType(), commonType);
					if (commonType == null) {
						// Ambiguous return types found: return null to indicate "not determinable".
						return null;
					}
				}
			}
		}

		if (commonType != null) {
			// Clear return type found: all factory methods return same type.
			mbd.factoryMethodReturnType = (uniqueCandidate != null ?
					ResolvableType.forMethodReturnType(uniqueCandidate) : ResolvableType.forClass(commonType));
		}
		return commonType;
	}

	/**
	 * This implementation attempts to query the FactoryBean's generic parameter metadata
	 * if present to determine the object type. If not present, i.e. the FactoryBean is
	 * declared as a raw type, checks the FactoryBean's {@code getObjectType} method
	 * on a plain instance of the FactoryBean, without bean properties applied yet.
	 * If this doesn't return a type yet, a full creation of the FactoryBean is
	 * used as fallback (through delegation to the superclass's implementation).
	 * <p>The shortcut check for a FactoryBean is only applied in case of a singleton
	 * FactoryBean. If the FactoryBean instance itself is not kept as singleton,
	 * it will be fully created to check the type of its exposed object.
	 */
	@Override
	@Nullable
	protected Class<?> getTypeForFactoryBean(String beanName, RootBeanDefinition mbd) {
		String factoryBeanName = mbd.getFactoryBeanName();
		String factoryMethodName = mbd.getFactoryMethodName();

		if (factoryBeanName != null) {
			if (factoryMethodName != null) {
				// Try to obtain the FactoryBean's object type from its factory method declaration
				// without instantiating the containing bean at all.
				BeanDefinition fbDef = getBeanDefinition(factoryBeanName);
				if (fbDef instanceof AbstractBeanDefinition) {
					AbstractBeanDefinition afbDef = (AbstractBeanDefinition) fbDef;
					if (afbDef.hasBeanClass()) {
						Class<?> result = getTypeForFactoryBeanFromMethod(afbDef.getBeanClass(), factoryMethodName);
						if (result != null) {
							return result;
						}
					}
				}
			}
			// If not resolvable above and the referenced factory bean doesn't exist yet,
			// exit here - we don't want to force the creation of another bean just to
			// obtain a FactoryBean's object type...
			if (!isBeanEligibleForMetadataCaching(factoryBeanName)) {
				return null;
			}
		}

		// Let's obtain a shortcut instance for an early getObjectType() call...
		FactoryBean<?> fb = (mbd.isSingleton() ?
				getSingletonFactoryBeanForTypeCheck(beanName, mbd) :
				getNonSingletonFactoryBeanForTypeCheck(beanName, mbd));

		if (fb != null) {
			// Try to obtain the FactoryBean's object type from this early stage of the instance.
			Class<?> result = getTypeForFactoryBean(fb);
			if (result != null) {
				return result;
			} else {
				// No type found for shortcut FactoryBean instance:
				// fall back to full creation of the FactoryBean instance.
				return super.getTypeForFactoryBean(beanName, mbd);
			}
		}

		if (factoryBeanName == null && mbd.hasBeanClass()) {
			// No early bean instantiation possible: determine FactoryBean's type from
			// static factory method signature or from class inheritance hierarchy...
			if (factoryMethodName != null) {
				return getTypeForFactoryBeanFromMethod(mbd.getBeanClass(), factoryMethodName);
			} else {
				return GenericTypeResolver.resolveTypeArgument(mbd.getBeanClass(), FactoryBean.class);
			}
		}

		return null;
	}

	/**
	 * Introspect the factory method signatures on the given bean class,
	 * trying to find a common {@code FactoryBean} object type declared there.
	 *
	 * @param beanClass         the bean class to find the factory method on
	 * @param factoryMethodName the name of the factory method
	 * @return the common {@code FactoryBean} object type, or {@code null} if none
	 */
	@Nullable
	private Class<?> getTypeForFactoryBeanFromMethod(Class<?> beanClass, final String factoryMethodName) {
		class Holder {
			@Nullable
			Class<?> value = null;
		}
		final Holder objectType = new Holder();

		// CGLIB subclass methods hide generic parameters; look at the original user class.
		Class<?> fbClass = ClassUtils.getUserClass(beanClass);

		// Find the given factory method, taking into account that in the case of
		// @Bean methods, there may be parameters present.
		ReflectionUtils.doWithMethods(fbClass, method -> {
			if (method.getName().equals(factoryMethodName) &&
					FactoryBean.class.isAssignableFrom(method.getReturnType())) {
				Class<?> currentType = GenericTypeResolver.resolveReturnTypeArgument(method, FactoryBean.class);
				if (currentType != null) {
					objectType.value = ClassUtils.determineCommonAncestor(currentType, objectType.value);
				}
			}
		});

		return (objectType.value != null && Object.class != objectType.value ? objectType.value : null);
	}

	/**
	 * Obtain a reference for early access to the specified bean,
	 * typically for the purpose of resolving a circular reference.
	 *
	 * @param beanName the name of the bean (for error handling purposes)
	 * @param mbd      the merged bean definition for the bean
	 * @param bean     the raw bean instance
	 * @return the object to expose as bean reference
	 */
	protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
		Object exposedObject = bean;
		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
					SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
					exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
				}
			}
		}
		return exposedObject;
	}


	//---------------------------------------------------------------------
	// Implementation methods
	//---------------------------------------------------------------------

	/**
	 * Obtain a "shortcut" singleton FactoryBean instance to use for a
	 * {@code getObjectType()} call, without full initialization of the FactoryBean.
	 *
	 * @param beanName the name of the bean
	 * @param mbd      the bean definition for the bean
	 * @return the FactoryBean instance, or {@code null} to indicate
	 * that we couldn't obtain a shortcut FactoryBean instance
	 */
	@Nullable
	private FactoryBean<?> getSingletonFactoryBeanForTypeCheck(String beanName, RootBeanDefinition mbd) {
		synchronized (getSingletonMutex()) {
			BeanWrapper bw = this.factoryBeanInstanceCache.get(beanName);
			if (bw != null) {
				return (FactoryBean<?>) bw.getWrappedInstance();
			}
			if (isSingletonCurrentlyInCreation(beanName) ||
					(mbd.getFactoryBeanName() != null && isSingletonCurrentlyInCreation(mbd.getFactoryBeanName()))) {
				return null;
			}

			Object instance = null;
			try {
				// Mark this bean as currently in creation, even if just partially.
				beforeSingletonCreation(beanName);
				// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
				instance = resolveBeforeInstantiation(beanName, mbd);
				if (instance == null) {
					bw = createBeanInstance(beanName, mbd, null);
					instance = bw.getWrappedInstance();
				}
			} finally {
				// Finished partial creation of this bean.
				afterSingletonCreation(beanName);
			}

			FactoryBean<?> fb = getFactoryBean(beanName, instance);
			if (bw != null) {
				this.factoryBeanInstanceCache.put(beanName, bw);
			}
			return fb;
		}
	}

	/**
	 * Obtain a "shortcut" non-singleton FactoryBean instance to use for a
	 * {@code getObjectType()} call, without full initialization of the FactoryBean.
	 *
	 * @param beanName the name of the bean
	 * @param mbd      the bean definition for the bean
	 * @return the FactoryBean instance, or {@code null} to indicate
	 * that we couldn't obtain a shortcut FactoryBean instance
	 */
	@Nullable
	private FactoryBean<?> getNonSingletonFactoryBeanForTypeCheck(String beanName, RootBeanDefinition mbd) {
		if (isPrototypeCurrentlyInCreation(beanName)) {
			return null;
		}

		Object instance = null;
		try {
			// Mark this bean as currently in creation, even if just partially.
			beforePrototypeCreation(beanName);
			// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
			instance = resolveBeforeInstantiation(beanName, mbd);
			if (instance == null) {
				BeanWrapper bw = createBeanInstance(beanName, mbd, null);
				instance = bw.getWrappedInstance();
			}
		} catch (BeanCreationException ex) {
			// Can only happen when getting a FactoryBean.
			if (logger.isDebugEnabled()) {
				logger.debug("Bean creation exception on non-singleton FactoryBean type check: " + ex);
			}
			onSuppressedException(ex);
			return null;
		} finally {
			// Finished partial creation of this bean.
			afterPrototypeCreation(beanName);
		}

		return getFactoryBean(beanName, instance);
	}

	/**
	 * Apply MergedBeanDefinitionPostProcessors to the specified bean definition,
	 * invoking their {@code postProcessMergedBeanDefinition} methods.
	 *
	 * @param mbd      the merged bean definition for the bean
	 * @param beanType the actual type of the managed bean instance
	 * @param beanName the name of the bean
	 * @see MergedBeanDefinitionPostProcessor#postProcessMergedBeanDefinition
	 */
	protected void applyMergedBeanDefinitionPostProcessors(RootBeanDefinition mbd, Class<?> beanType, String beanName) {
		for (BeanPostProcessor bp : getBeanPostProcessors()) {
			if (bp instanceof MergedBeanDefinitionPostProcessor) {
				MergedBeanDefinitionPostProcessor bdp = (MergedBeanDefinitionPostProcessor) bp;
				bdp.postProcessMergedBeanDefinition(mbd, beanType, beanName);
			}
		}
	}

	/**
	 * Apply before-instantiation post-processors, resolving whether there is a
	 * before-instantiation shortcut for the specified bean.
	 *
	 * @param beanName the name of the bean
	 * @param mbd      the bean definition for the bean
	 * @return the shortcut-determined bean instance, or {@code null} if none
	 *
	 * 在真正的调用 doCreate 方法创建 bean 的实例前使用了这样的一个方法 resolveBeforeInstantiation(beanName,mbd) 对 BeanDefinition
	 * 中的属性做些前置处理，无论其中量否有相应的逻辑实现我们都可以理解，因为真正在逻辑实现前后留有处理函数也是可以扩展的一种体现，但是，这并
	 * 不是最重要的，在函数中还提供了一个短路的判断，这才是最为关键的部分
	 * if(bean != null){
	 *     return bean;
	 * }
	 * 当经过前置处理后返回的结果如果不为空的话，那么会直接的略过后续的 bean 的创建而直接返回结果，这一特性虽然很多容易被忽略，但是却是
	 * 起着至关重要的作用，我们熟知的 aop 功能就是基于这里的判断的
	 *
	 *
	 *
	 *
	 * 此方法中最吸引我们的无疑是两个方法，applyBeanPostProcessorsBeforeInstantiation 和applyBeanPostProcessorsAfterInitialization
	 * ，两个方法实现非常的简单，无非是对后处理器的所有的InstantiationAwareBeanPostProcessor类型的后处理器进行
	 * postProcessBfforeInstantiation 方法和 BeanPostProcessor 的 postProcessAfterInitialization 方法的调用
	 *
	 *
	 */
	@Nullable
	protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
		Object bean = null;
		//如果尚未被解析
		if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
			// Make sure bean class is actually resolved at this point.
			if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
				Class<?> targetType = determineTargetType(beanName, mbd);
				if (targetType != null) {

					bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
					if (bean != null) {
						// 在讲解从缓存中获取单例 bean 的时候就提到过,Spring 中的规则是在 bean 的初初始化后尽可能的保证将注册后的处理器
						// postProcessAfterInitialization 方法应用到该 bean 中，因为如果返回的 bean 不为空，那么便不会再次经历
						// 普通的 bean 的创建过程，所有的只能在这里应用后的处理器 postProcessAfterInitialization 方法


						bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
					}
				}
			}
			mbd.beforeInstantiationResolved = (bean != null);
		}
		return bean;
	}

	/**
	 * Apply InstantiationAwareBeanPostProcessors to the specified bean definition
	 * (by class and name), invoking their {@code postProcessBeforeInstantiation} methods.
	 * <p>Any returned object will be used as the bean instead of actually instantiating
	 * the target bean. A {@code null} return value from the post-processor will
	 * result in the target bean being instantiated.
	 *
	 * @param beanClass the class of the bean to be instantiated
	 * @param beanName  the name of the bean
	 * @return the bean object to use instead of a default instance of the target bean, or {@code null}
	 * @see InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation
	 *
	 * 1.实例化前的后处理器应用
	 * bean 的实例化前的调用，也就是将 AbstractBeanDefinition 转化为 BeanWrapper 前处理，给子类一个修改 BeanDefinition
	 * 的机会，也就是说当程序经过这个方法后，bean 可能已经不是我们认为的 bean 了，而是或许成为了一个经过处理的代理 bean 了，可能是
	 * 通过 cglib 生成的，也可能是通过其他的技术生成的，这在第7章会详细的介绍，我们只需要知道，在 bean的实例化前会调用后处理器
	 * 进行处理
	 */
	@Nullable
	protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
		for (BeanPostProcessor bp : getBeanPostProcessors()) {
			if (bp instanceof InstantiationAwareBeanPostProcessor) {
				InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
				Object result = ibp.postProcessBeforeInstantiation(beanClass, beanName);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	/**
	 * Create a new instance for the specified bean, using an appropriate instantiation strategy:
	 * factory method, constructor autowiring, or simple instantiation.
	 *
	 * @param beanName the name of the bean
	 * @param mbd      the bean definition for the bean
	 * @param args     explicit arguments to use for constructor or factory method invocation
	 * @return a BeanWrapper for the new instance
	 * @see #obtainFromSupplier
	 * @see #instantiateUsingFactoryMethod
	 * @see #autowireConstructor
	 * @see #instantiateBean
	 * 当我们了解了循环依赖以后就可以深入分析创建 bean 的每个步骤了，首先我们从 createBeanInstance 方法开始
	 *
	 *
	 * 虽然对于代码中的实例化细节非常的得要，但是在 createBeanInstance 方法中我们还是可以清晰的看到实例化的逻辑
	 * 1.如果 RootBeanDefinition 中存在 factoryMethodName 属性，或者说在配置文件中配置了 factory-method,
	 * 那么 Spring 会尝试使用 instantiateUsingFactoryMethod（beanName,mbd,args） 方法根据 RootBeanDefinition 中配置生成的
	 * bean 实例
	 * 2.解析构造函数并进行构造函数的实例化，因为一个 bean 对应的类中可能会有多个构造函数，而每个构造函数的参数不同，Spring 在根据
	 * 参数及类型去判断最终会使用哪个构造函数进行实例化，但是判断的过程是比较消耗性能的步骤，所以彩缓存机制，如果已经解析过了，则不需要
	 * 重复解析而是直接从 RootBeanDefinition 中的属性 resolvedConstructorOrFactoryMethod 缓存中的值去取，否则需要再次解析，
	 * 并将解析的结果添加至 RootBeanDefinition 中的属性 resolvedConstructorOrFactoryMethod 中
	 *
	 *
	 */
	protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
		// Make sure bean class is actually resolved at this point.
		//解析 class
		Class<?> beanClass = resolveBeanClass(mbd, beanName);

		if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
		}

		//如果工厂方法不为空则使用工厂方法初始化策略
		Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
		if (instanceSupplier != null) {
			return obtainFromSupplier(instanceSupplier, beanName);
		}

		if (mbd.getFactoryMethodName() != null) {
			return instantiateUsingFactoryMethod(beanName, mbd, args);
		}

		// Shortcut when re-creating the same bean...
		boolean resolved = false;
		boolean autowireNecessary = false;
		if (args == null) {
			//一个类有多个构造函数，每个构造函数都有不同的参数，所以调用前需要先根据参数锁定的构造函数对应的工厂方法
			synchronized (mbd.constructorArgumentLock) {
				if (mbd.resolvedConstructorOrFactoryMethod != null) {
					resolved = true;
					autowireNecessary = mbd.constructorArgumentsResolved;
				}
			}
		}
		//如果已经解析过则使用解析好的构造函数方法不需要再次锁定
		if (resolved) {
			if (autowireNecessary) {
				// 构造函数自动注入
				return autowireConstructor(beanName, mbd, null, null);
			} else {
				//使用默念的构造函数构造
				return instantiateBean(beanName, mbd);
			}
		}

		// Need to determine the constructor...
		// 需要根据参数解析构造函数
		Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
		if (ctors != null ||
				mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_CONSTRUCTOR ||
				mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
			//构造函数自动的注入
			return autowireConstructor(beanName, mbd, ctors, args);
		}

		// No special handling: simply use no-arg constructor.
		//使用默认的构造函数构造
		return instantiateBean(beanName, mbd);
	}

	/**
	 * Obtain a bean instance from the given supplier.
	 *
	 * @param instanceSupplier the configured supplier
	 * @param beanName         the corresponding bean name
	 * @return a BeanWrapper for the new instance
	 * @see #getObjectForBeanInstance
	 * @since 5.0
	 */
	protected BeanWrapper obtainFromSupplier(Supplier<?> instanceSupplier, String beanName) {
		String outerBean = this.currentlyCreatedBean.get();
		this.currentlyCreatedBean.set(beanName);
		Object instance;
		try {
			instance = instanceSupplier.get();
		} finally {
			if (outerBean != null) {
				this.currentlyCreatedBean.set(outerBean);
			} else {
				this.currentlyCreatedBean.remove();
			}
		}
		BeanWrapper bw = new BeanWrapperImpl(instance);
		initBeanWrapper(bw);
		return bw;
	}

	/**
	 * Overridden in order to implicitly register the currently created bean as
	 * dependent on further beans getting programmatically retrieved during a
	 * {@link Supplier} callback.
	 *
	 * @see #obtainFromSupplier
	 * @since 5.0
	 */
	@Override
	protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

		String currentlyCreatedBean = this.currentlyCreatedBean.get();
		if (currentlyCreatedBean != null) {
			registerDependentBean(beanName, currentlyCreatedBean);
		}

		return super.getObjectForBeanInstance(beanInstance, name, beanName, mbd);
	}

	/**
	 * Determine candidate constructors to use for the given bean, checking all registered
	 * {@link SmartInstantiationAwareBeanPostProcessor SmartInstantiationAwareBeanPostProcessors}.
	 *
	 * @param beanClass the raw class of the bean
	 * @param beanName  the name of the bean
	 * @return the candidate constructors, or {@code null} if none specified
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor#determineCandidateConstructors
	 */
	@Nullable
	protected Constructor<?>[] determineConstructorsFromBeanPostProcessors(@Nullable Class<?> beanClass, String beanName)
			throws BeansException {

		if (beanClass != null && hasInstantiationAwareBeanPostProcessors()) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
					SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
					Constructor<?>[] ctors = ibp.determineCandidateConstructors(beanClass, beanName);
					if (ctors != null) {
						return ctors;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Instantiate the given bean using its default constructor.
	 *
	 * @param beanName the name of the bean
	 * @param mbd      the bean definition for the bean
	 * @return a BeanWrapper for the new instance
	 *  经历了带有参数的构造函数实例构造，相信你会非常轻松的愉快的地理解不带参数的构造函数的实例化过程
	 *  你会发现，些方法并没有什么实质性的逻辑，带有参数的实例构造中，Spring 把精力都放在了构造函数以及参数的匹配上，所以
	 *  如果没有参数的话，那将是一个非常简单的事情，直接调用实例化进行实例化就可以了。
	 *
	 *
	 */
	protected BeanWrapper instantiateBean(final String beanName, final RootBeanDefinition mbd) {
		try {
			Object beanInstance;
			final BeanFactory parent = this;
			if (System.getSecurityManager() != null) {
				beanInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
								getInstantiationStrategy().instantiate(mbd, beanName, parent),
						getAccessControlContext());
			} else {
				beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
			}
			BeanWrapper bw = new BeanWrapperImpl(beanInstance);
			initBeanWrapper(bw);
			return bw;
		} catch (Throwable ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
		}
	}

	/**
	 * Instantiate the bean using a named factory method. The method may be static, if the
	 * mbd parameter specifies a class, rather than a factoryBean, or an instance variable
	 * on a factory object itself configured using Dependency Injection.
	 *
	 * @param beanName     the name of the bean
	 * @param mbd          the bean definition for the bean
	 * @param explicitArgs argument values passed in programmatically via the getBean method,
	 *                     or {@code null} if none (-> use constructor argument values from bean definition)
	 * @return a BeanWrapper for the new instance
	 * @see #getBean(String, Object[])
	 */
	protected BeanWrapper instantiateUsingFactoryMethod(
			String beanName, RootBeanDefinition mbd, @Nullable Object[] explicitArgs) {

		return new ConstructorResolver(this).instantiateUsingFactoryMethod(beanName, mbd, explicitArgs);
	}

	/**
	 * "autowire constructor" (with constructor arguments by type) behavior.
	 * Also applied if explicit constructor argument values are specified,
	 * matching all remaining arguments with beans from the bean factory.
	 * <p>This corresponds to constructor injection: In this mode, a Spring
	 * bean factory is able to host components that expect constructor-based
	 * dependency resolution.
	 *
	 * @param beanName     the name of the bean
	 * @param mbd          the bean definition for the bean
	 * @param ctors        the chosen candidate constructors
	 * @param explicitArgs argument values passed in programmatically via the getBean method,
	 *                     or {@code null} if none (-> use constructor argument values from bean definition)
	 * @return a BeanWrapper for the new instance
	 */
	protected BeanWrapper autowireConstructor(
			String beanName, RootBeanDefinition mbd, @Nullable Constructor<?>[] ctors, @Nullable Object[] explicitArgs) {

		return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
	}

	/**
	 * Populate the bean instance in the given BeanWrapper with the property values
	 * from the bean definition.
	 *
	 * @param beanName the name of the bean
	 * @param mbd      the bean definition for the bean
	 * @param bw       BeanWrapper with bean instance
	 */
	//属性注入
	//在了解循环依赖的时候，我们曾经反复的提到 populateBean 这个函数，也多少了解了这个函数的主要功能就是属性的填充，那么究竟是
	//如何实现填充的呢？
	// 在 populateBean 函数中提供了这样的处理流程
	// InstantiationAwareBeanPostProcessor 处理器 postProcessAfterInstantiation 函数的应用，此函数可以控制程序是否继续进行
	//填充，根据注入类型（ByName/byType），提取依赖的 bean，并统一的存入 PropertyValues 中
	// 应用 InstantiationAwareBeanPostProcesser 处理器 postProcessPropertyValues 方法，对属性获取完毕填充前对属性再次进行
	//处理，典型的应用是 RequiredAnnotationBeanProstProcessor 类型中的属性验证
	//将所有的 PropertyValues 中的属性填充至 BeanWrapper 中
	//在上面的步骤中有几个地方是我们比较感兴趣的，它们分别是依赖注入，autowireByName/autowireByType 以及属性的填充，那么，接下来
	//进一步分析这几个功能的实现细节


	protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
		PropertyValues pvs = mbd.getPropertyValues();

		if (bw == null) {
			if (!pvs.isEmpty()) {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
			} else {
				// Skip property population phase for null instance
				//没有可以填充的属性
				return;
			}
		}

		// Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
		// state of the bean before properties are set. This can be used, for example,
		// to support styles of field injection.
		// 给 InstantiationAwareBeanPostProcessors 最后的一次机会在属性设置前改变 bean
		// 如：可以用来支持属性的注入的类型
		boolean continueWithPropertyPopulation = true;

		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof InstantiationAwareBeanPostProcessor) {
					InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
					//返回的值为是否继续填充 bean
					if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
						continueWithPropertyPopulation = false;
						break;
					}
				}
			}
		}
		//如果后处理器发出停止填充的命令则终止后续的执行
		if (!continueWithPropertyPopulation) {
			return;
		}

		if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME ||
				mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
			MutablePropertyValues newPvs = new MutablePropertyValues(pvs);

			// Add property values based on autowire by name if applicable.
			// 根据名称自动注入
			if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
				autowireByName(beanName, mbd, bw, newPvs);
			}

			// Add property values based on autowire by type if applicable.
			//根据类型自动注入
			if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
				autowireByType(beanName, mbd, bw, newPvs);
			}

			pvs = newPvs;
		}
		// 后处理器已经初始化
		boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
		boolean needsDepCheck = (mbd.getDependencyCheck() != RootBeanDefinition.DEPENDENCY_CHECK_NONE);

		//需要依赖检查
		if (hasInstAwareBpps || needsDepCheck) {
			PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
			if (hasInstAwareBpps) {
				for (BeanPostProcessor bp : getBeanPostProcessors()) {
					if (bp instanceof InstantiationAwareBeanPostProcessor) {
						InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
						//对所有的需要依赖检查的属性进行后处理
						pvs = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
						if (pvs == null) {
							return;
						}
					}
				}
			}
			if (needsDepCheck) {
				//依赖检查，对应的 depends-on 属性，3.0已经弃用此属性了
				checkDependencies(beanName, mbd, filteredPds, pvs);
			}
		}
		//将属性应用到 bean中
		applyPropertyValues(beanName, mbd, bw, pvs);
	}

	/**
	 * Fill in any missing property values with references to
	 * other beans in this factory if autowire is set to "byName".
	 *
	 * @param beanName the name of the bean we're wiring up.
	 *                 Useful for debugging messages; not used functionally.
	 * @param mbd      bean definition to update through autowiring
	 * @param bw       BeanWrapper from which we can obtain information about the bean
	 * @param pvs      the PropertyValues to register wired objects with
	 */
	// 上文提到的根据注入的类型，byName/byType 提取依赖的 bean,并统一的存入 PropertyValues 中，那么我们首先了解下 byName 功能是
	//如何实现的
	//如果读者之前了解了 autowire 的使用方法，相信理解这个函数的功能不会太困难，无非是在传入参数pvs 中找出已经加载的 bean
	//并递归实例化，进而加入到 psv中
	protected void autowireByName(
			String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
		//寻找 bw中需要依赖注入的属性
		String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
		for (String propertyName : propertyNames) {

			if (containsBean(propertyName)) {
				//递归初始化相关的 bean
				Object bean = getBean(propertyName);
				pvs.add(propertyName, bean);
				// 注册依赖
				registerDependentBean(propertyName, beanName);
				if (logger.isDebugEnabled()) {
					logger.debug("Added autowiring by name from bean name '" + beanName +
							"' via property '" + propertyName + "' to bean named '" + propertyName + "'");
				}
			} else {
				if (logger.isTraceEnabled()) {
					logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName +
							"' by name: no matching bean found");
				}
			}
		}
	}

	/**
	 * Abstract method defining "autowire by type" (bean properties by type) behavior.
	 * <p>This is like PicoContainer default, in which there must be exactly one bean
	 * of the property type in the bean factory. This makes bean factories simple to
	 * configure for small namespaces, but doesn't work as well as standard Spring
	 * behavior for bigger applications.
	 *
	 * @param beanName the name of the bean to autowire by type
	 * @param mbd      the merged bean definition to update through autowiring
	 * @param bw       BeanWrapper from which we can obtain information about the bean
	 * @param pvs      the PropertyValues to register wired objects with
	 */
	// autowireByType 与 autowireByName 对于我们理解与使用来说复杂程度都很相似，但是其实现功能的复杂度却完全
	//不一样。
	//实现根据名称自动匹配的第一步就是寻找 bw 中需要依赖注入的属性，然后遍历这些属性并寻找类型匹配的 bean,其中最
	//复杂的就是寻找类型匹配的 bean，同时，Spring 提供了对集合的类型注入的支持，如果使用注解的方式：
	//@Autowired
	//private List<Test> tests;
	//Spring 将会把所有与 Test 匹配的类型找出来并注入到 tests 属性中，正是由于这一因素，所以在 autowireBype 函数中
	//新建了局部遍历 autowiredBeanNames,用于存储所有依赖的 bean,如果是对非集合类属性注入来说，此属性并无用处
	// 对于寻找类型匹配的逻辑实现封装在了 resolveDependency 函数中

	protected void autowireByType(
			String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

		TypeConverter converter = getCustomTypeConverter();
		if (converter == null) {
			converter = bw;
		}

		Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
		//寻找 bw 中需要依赖注入的属性
		String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
		for (String propertyName : propertyNames) {
			try {
				PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
				// Don't try autowiring by type for type Object: never makes sense,
				// even if it technically is a unsatisfied, non-simple property.
				if (Object.class != pd.getPropertyType()) {
					//探测指定属性的 set 方法
					MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
					// Do not allow eager init for type matching in case of a prioritized post-processor.
					boolean eager = !PriorityOrdered.class.isInstance(bw.getWrappedInstance());
					DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
					//解析指定 beanName 的属性所匹配的值，并把解析到的属性名称储存丰 autowiredBeanNames 中，当属性存在
					//多个封装的 bean 时如：
					// @autoWired private List<A> alist ; 将会找到所匹配的 A 类型的 bean 并将其注入
					Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
					if (autowiredArgument != null) {
						pvs.add(propertyName, autowiredArgument);
					}
					for (String autowiredBeanName : autowiredBeanNames) {
						// 注册依赖
						registerDependentBean(autowiredBeanName, beanName);
						if (logger.isDebugEnabled()) {
							logger.debug("Autowiring by type from bean name '" + beanName + "' via property '" +
									propertyName + "' to bean named '" + autowiredBeanName + "'");
						}
					}
					autowiredBeanNames.clear();
				}
			} catch (BeansException ex) {
				throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
			}
		}
	}


	/**
	 * Return an array of non-simple bean properties that are unsatisfied.
	 * These are probably unsatisfied references to other beans in the
	 * factory. Does not include simple properties like primitives or Strings.
	 *
	 * @param mbd the merged bean definition the bean was created with
	 * @param bw  the BeanWrapper the bean was created with
	 * @return an array of bean property names
	 * @see org.springframework.beans.BeanUtils#isSimpleProperty
	 */
	protected String[] unsatisfiedNonSimpleProperties(AbstractBeanDefinition mbd, BeanWrapper bw) {
		Set<String> result = new TreeSet<>();
		PropertyValues pvs = mbd.getPropertyValues();
		PropertyDescriptor[] pds = bw.getPropertyDescriptors();
		for (PropertyDescriptor pd : pds) {
			if (pd.getWriteMethod() != null && !isExcludedFromDependencyCheck(pd) && !pvs.contains(pd.getName()) &&
					!BeanUtils.isSimpleProperty(pd.getPropertyType())) {
				result.add(pd.getName());
			}
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * Extract a filtered set of PropertyDescriptors from the given BeanWrapper,
	 * excluding ignored dependency types or properties defined on ignored dependency interfaces.
	 *
	 * @param bw    the BeanWrapper the bean was created with
	 * @param cache whether to cache filtered PropertyDescriptors for the given bean Class
	 * @return the filtered PropertyDescriptors
	 * @see #isExcludedFromDependencyCheck
	 * @see #filterPropertyDescriptorsForDependencyCheck(org.springframework.beans.BeanWrapper)
	 */
	protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw, boolean cache) {
		PropertyDescriptor[] filtered = this.filteredPropertyDescriptorsCache.get(bw.getWrappedClass());
		if (filtered == null) {
			filtered = filterPropertyDescriptorsForDependencyCheck(bw);
			if (cache) {
				PropertyDescriptor[] existing =
						this.filteredPropertyDescriptorsCache.putIfAbsent(bw.getWrappedClass(), filtered);
				if (existing != null) {
					filtered = existing;
				}
			}
		}
		return filtered;
	}

	/**
	 * Extract a filtered set of PropertyDescriptors from the given BeanWrapper,
	 * excluding ignored dependency types or properties defined on ignored dependency interfaces.
	 *
	 * @param bw the BeanWrapper the bean was created with
	 * @return the filtered PropertyDescriptors
	 * @see #isExcludedFromDependencyCheck
	 */
	protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw) {
		List<PropertyDescriptor> pds =
				new LinkedList<>(Arrays.asList(bw.getPropertyDescriptors()));
		for (Iterator<PropertyDescriptor> it = pds.iterator(); it.hasNext(); ) {
			PropertyDescriptor pd = it.next();
			if (isExcludedFromDependencyCheck(pd)) {
				it.remove();
			}
		}
		return pds.toArray(new PropertyDescriptor[pds.size()]);
	}

	/**
	 * Determine whether the given bean property is excluded from dependency checks.
	 * <p>This implementation excludes properties defined by CGLIB and
	 * properties whose type matches an ignored dependency type or which
	 * are defined by an ignored dependency interface.
	 *
	 * @param pd the PropertyDescriptor of the bean property
	 * @return whether the bean property is excluded
	 * @see #ignoreDependencyType(Class)
	 * @see #ignoreDependencyInterface(Class)
	 */
	protected boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
		return (AutowireUtils.isExcludedFromDependencyCheck(pd) ||
				this.ignoredDependencyTypes.contains(pd.getPropertyType()) ||
				AutowireUtils.isSetterDefinedInInterface(pd, this.ignoredDependencyInterfaces));
	}

	/**
	 * Perform a dependency check that all properties exposed have been set,
	 * if desired. Dependency checks can be objects (collaborating beans),
	 * simple (primitives and String), or all (both).
	 *
	 * @param beanName the name of the bean
	 * @param mbd      the merged bean definition the bean was created with
	 * @param pds      the relevant property descriptors for the target bean
	 * @param pvs      the property values to be applied to the bean
	 * @see #isExcludedFromDependencyCheck(java.beans.PropertyDescriptor)
	 */
	protected void checkDependencies(
			String beanName, AbstractBeanDefinition mbd, PropertyDescriptor[] pds, PropertyValues pvs)
			throws UnsatisfiedDependencyException {

		int dependencyCheck = mbd.getDependencyCheck();
		for (PropertyDescriptor pd : pds) {
			if (pd.getWriteMethod() != null && !pvs.contains(pd.getName())) {
				boolean isSimple = BeanUtils.isSimpleProperty(pd.getPropertyType());
				boolean unsatisfied = (dependencyCheck == RootBeanDefinition.DEPENDENCY_CHECK_ALL) ||
						(isSimple && dependencyCheck == RootBeanDefinition.DEPENDENCY_CHECK_SIMPLE) ||
						(!isSimple && dependencyCheck == RootBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
				if (unsatisfied) {
					throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, pd.getName(),
							"Set this property value or disable dependency checking for this bean.");
				}
			}
		}
	}

	/**
	 * Apply the given property values, resolving any runtime references
	 * to other beans in this bean factory. Must use deep copy, so we
	 * don't permanently modify this property.
	 *
	 * @param beanName the bean name passed for better exception information
	 * @param mbd      the merged bean definition
	 * @param bw       the BeanWrapper wrapping the target object
	 * @param pvs      the new property values
	 */
	protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
		if (pvs.isEmpty()) {
			return;
		}

		MutablePropertyValues mpvs = null;
		List<PropertyValue> original;

		if (System.getSecurityManager() != null) {
			if (bw instanceof BeanWrapperImpl) {
				((BeanWrapperImpl) bw).setSecurityContext(getAccessControlContext());
			}
		}

		if (pvs instanceof MutablePropertyValues) {
			mpvs = (MutablePropertyValues) pvs;
			if (mpvs.isConverted()) {
				// Shortcut: use the pre-converted values as-is.
				try {
					bw.setPropertyValues(mpvs);
					return;
				} catch (BeansException ex) {
					throw new BeanCreationException(
							mbd.getResourceDescription(), beanName, "Error setting property values", ex);
				}
			}
			original = mpvs.getPropertyValueList();
		} else {
			original = Arrays.asList(pvs.getPropertyValues());
		}

		TypeConverter converter = getCustomTypeConverter();
		if (converter == null) {
			converter = bw;
		}
		BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);

		// Create a deep copy, resolving any references for values.
		List<PropertyValue> deepCopy = new ArrayList<>(original.size());
		boolean resolveNecessary = false;
		for (PropertyValue pv : original) {
			if (pv.isConverted()) {
				deepCopy.add(pv);
			} else {
				String propertyName = pv.getName();
				Object originalValue = pv.getValue();
				Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
				Object convertedValue = resolvedValue;
				boolean convertible = bw.isWritableProperty(propertyName) &&
						!PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
				if (convertible) {
					convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
				}
				// Possibly store converted value in merged bean definition,
				// in order to avoid re-conversion for every created bean instance.
				if (resolvedValue == originalValue) {
					if (convertible) {
						pv.setConvertedValue(convertedValue);
					}
					deepCopy.add(pv);
				} else if (convertible && originalValue instanceof TypedStringValue &&
						!((TypedStringValue) originalValue).isDynamic() &&
						!(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
					pv.setConvertedValue(convertedValue);
					deepCopy.add(pv);
				} else {
					resolveNecessary = true;
					deepCopy.add(new PropertyValue(pv, convertedValue));
				}
			}
		}
		if (mpvs != null && !resolveNecessary) {
			mpvs.setConverted();
		}

		// Set our (possibly massaged) deep copy.
		try {
			bw.setPropertyValues(new MutablePropertyValues(deepCopy));
		} catch (BeansException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Error setting property values", ex);
		}
	}

	/**
	 * Convert the given value for the specified target property.
	 */
	@Nullable
	private Object convertForProperty(
			@Nullable Object value, String propertyName, BeanWrapper bw, TypeConverter converter) {

		if (converter instanceof BeanWrapperImpl) {
			return ((BeanWrapperImpl) converter).convertForProperty(value, propertyName);
		} else {
			PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
			MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
			return converter.convertIfNecessary(value, pd.getPropertyType(), methodParam);
		}
	}


	/**
	 * Initialize the given bean instance, applying factory callbacks
	 * as well as init methods and bean post processors.
	 * <p>Called from {@link #createBean} for traditionally defined beans,
	 * and from {@link #initializeBean} for existing bean instances.
	 *
	 * @param beanName the bean name in the factory (for debugging purposes)
	 * @param bean     the new bean instance we may need to initialize
	 * @param mbd      the bean definition that the bean was created with
	 *                 (can also be {@code null}, if given an existing bean instance)
	 * @return the initialized bean instance (potentially wrapped)
	 * @see BeanNameAware
	 * @see BeanClassLoaderAware
	 * @see BeanFactoryAware
	 * @see #applyBeanPostProcessorsBeforeInitialization
	 * @see #invokeInitMethods
	 * @see #applyBeanPostProcessorsAfterInitialization
	 * 初始化 bean
	 * 大家应该记得 bean 配置时 bean 中有一个 init-method 属性，这个属性的作用是 bean 实例化前调用 init-method
	 * 指定的方法来根据用户业务进行相应的实例化，我们现在就已经进入了这个方法了，首先看一下这个方法的执行位置，Spring
	 * 中程序已经执行了过 bean 的实例化了，并且进行了属性的填充，而就在这时将公调用用户设定的初始化方法。
	 *
	 * 虽然说此函数的主要目的是进行客户设定的初始化方法的调用，但是除此之外还有其他的心要工作
	 *
	 *
	 */
	protected Object initializeBean(final String beanName, final Object bean, @Nullable RootBeanDefinition mbd) {
		if (System.getSecurityManager() != null) {
			AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
				invokeAwareMethods(beanName, bean);
				return null;
			}, getAccessControlContext());
		} else {
			//对特殊的 bean 处理，Aware,BeanClassLoaderAware，BeanFactoryAware
			invokeAwareMethods(beanName, bean);
		}

		Object wrappedBean = bean;
		if (mbd == null || !mbd.isSynthetic()) {
			//应用后处理器
			wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
		}

		try {
			//激活用户自定义的 init 方法
			invokeInitMethods(beanName, wrappedBean, mbd);
		} catch (Throwable ex) {
			throw new BeanCreationException(
					(mbd != null ? mbd.getResourceDescription() : null),
					beanName, "Invocation of init method failed", ex);
		}
		if (mbd == null || !mbd.isSynthetic()) {
			//后处理器应用
			wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
		}

		return wrappedBean;
	}


	/***
	 * 按照上面的方法我们可以获得到 Spring 中的 BeanFactory，并且可以根据 BeanFactory 获取所有的 bean
	 * ，以及相关的配置，当然还有其他的 Aware 的使用方法都大同小异，看一下 Spring 的实现方式，相信读者便会使用了
	 * 代码简单得已经不能再简单了，读者可以自己尝试使用别的Aware，都比较简单
	 * @param beanName
	 * @param bean
	 */
	private void invokeAwareMethods(final String beanName, final Object bean) {
		if (bean instanceof Aware) {
			if (bean instanceof BeanNameAware) {
				((BeanNameAware) bean).setBeanName(beanName);
			}
			if (bean instanceof BeanClassLoaderAware) {
				ClassLoader bcl = getBeanClassLoader();
				if (bcl != null) {
					((BeanClassLoaderAware) bean).setBeanClassLoader(bcl);
				}
			}
			if (bean instanceof BeanFactoryAware) {
				((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
			}
		}
	}

	/**
	 * Give a bean a chance to react now all its properties are set,
	 * and a chance to know about its owning bean factory (this object).
	 * This means checking whether the bean implements InitializingBean or defines
	 * a custom init method, and invoking the necessary callback(s) if it does.
	 *
	 * @param beanName the bean name in the factory (for debugging purposes)
	 * @param bean     the new bean instance we may need to initialize
	 * @param mbd      the merged bean definition that the bean was created with
	 *                 (can also be {@code null}, if given an existing bean instance)
	 * @throws Throwable if thrown by init methods or by the invocation process
	 * @see #invokeCustomInitMethod
	 */
	//激活自定义的 init 方法
	//客户定制的初始化方法除了我们熟知的使用配置 init-method 之外 ，还有使用自定义的 bean实现，initializingBean 接口
	//,并在 afterPropertiesSet 中实现自己的初始化业务逻辑
	// init-method 与 afterPropertiesSet 都是初始化 bean 的时候执行，执行顺序是after，ProertiesSEt 先执行，而 init-method
	//后执行
	// 在 invokeInitMethods 方法中就实现了这两个步骤的初始化方法调用

	protected void invokeInitMethods(String beanName, final Object bean, @Nullable RootBeanDefinition mbd)
			throws Throwable {
		//首先是会检查是否是 InitializingBean，如果是的话，需要调用 after
		boolean isInitializingBean = (bean instanceof InitializingBean);
		if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
			if (logger.isDebugEnabled()) {
				logger.debug("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
			}
			if (System.getSecurityManager() != null) {
				try {
					AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
						((InitializingBean) bean).afterPropertiesSet();
						return null;
					}, getAccessControlContext());
				} catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			} else {
				//属性的初始化处理
				((InitializingBean) bean).afterPropertiesSet();
			}
		}

		if (mbd != null && bean.getClass() != NullBean.class) {
			String initMethodName = mbd.getInitMethodName();
			if (StringUtils.hasLength(initMethodName) &&
					!(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
					!mbd.isExternallyManagedInitMethod(initMethodName)) {
				//调用自定义的初始化方法
				invokeCustomInitMethod(beanName, bean, mbd);
			}
		}
	}

	/**
	 * Invoke the specified custom init method on the given bean.
	 * Called by invokeInitMethods.
	 * <p>Can be overridden in subclasses for custom resolution of init
	 * methods with arguments.
	 *
	 * @see #invokeInitMethods
	 */
	protected void invokeCustomInitMethod(String beanName, final Object bean, RootBeanDefinition mbd)
			throws Throwable {

		String initMethodName = mbd.getInitMethodName();
		Assert.state(initMethodName != null, "No init method set");
		final Method initMethod = (mbd.isNonPublicAccessAllowed() ?
				BeanUtils.findMethod(bean.getClass(), initMethodName) :
				ClassUtils.getMethodIfAvailable(bean.getClass(), initMethodName));

		if (initMethod == null) {
			if (mbd.isEnforceInitMethod()) {
				throw new BeanDefinitionValidationException("Couldn't find an init method named '" +
						initMethodName + "' on bean with name '" + beanName + "'");
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("No default init method named '" + initMethodName +
							"' found on bean with name '" + beanName + "'");
				}
				// Ignore non-existent default lifecycle methods.
				return;
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Invoking init method  '" + initMethodName + "' on bean with name '" + beanName + "'");
		}

		if (System.getSecurityManager() != null) {
			AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
				ReflectionUtils.makeAccessible(initMethod);
				return null;
			});
			try {
				AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () ->
						initMethod.invoke(bean), getAccessControlContext());
			} catch (PrivilegedActionException pae) {
				InvocationTargetException ex = (InvocationTargetException) pae.getException();
				throw ex.getTargetException();
			}
		} else {
			try {
				ReflectionUtils.makeAccessible(initMethod);
				initMethod.invoke(bean);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}


	/**
	 * Applies the {@code postProcessAfterInitialization} callback of all
	 * registered BeanPostProcessors, giving them a chance to post-process the
	 * object obtained from FactoryBeans (for example, to auto-proxy them).
	 *
	 * @see #applyBeanPostProcessorsAfterInitialization
	 *
	 *
	 *
	 *
	 */
	@Override
	protected Object postProcessObjectFromFactoryBean(Object object, String beanName) {
		return applyBeanPostProcessorsAfterInitialization(object, beanName);
	}

	/**
	 * Overridden to clear FactoryBean instance cache as well.
	 */
	@Override
	protected void removeSingleton(String beanName) {
		super.removeSingleton(beanName);
		this.factoryBeanInstanceCache.remove(beanName);
	}


	/**
	 * Special DependencyDescriptor variant for Spring's good old autowire="byType" mode.
	 * Always optional; never considering the parameter name for choosing a primary candidate.
	 */
	@SuppressWarnings("serial")
	private static class AutowireByTypeDependencyDescriptor extends DependencyDescriptor {

		public AutowireByTypeDependencyDescriptor(MethodParameter methodParameter, boolean eager) {
			super(methodParameter, false, eager);
		}

		@Override
		public String getDependencyName() {
			return null;
		}
	}

}
