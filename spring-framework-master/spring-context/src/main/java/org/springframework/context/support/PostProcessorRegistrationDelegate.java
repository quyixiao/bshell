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

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
class PostProcessorRegistrationDelegate {


	/****
	 *  对 beanDefinitionRegister 类型处理
	 *
	 *
	 *
	 *
	 *
	 *  从上面的方法中我们可以看出，对于 BeanFactoryPostProcessor 的处理主要分两种情况，一个是对于 BeanDefinitionRegistry类的
	 *  特殊处理，另一种是对普通的 BeanFactoryPostProcessor 进行处理，而对于每种情况都需要考虑硬编码注册的后处理器以及通过配置
	 *  注入的后处理器
	 *  对于 BeanDefinitionRegistry 类型的处理类的处理主要包括以下内容
	 *  对于硬编码注册的后处理器的处理，主要地通过 AbstractApplicationContext 中添加处理方法 addBeanFactoryPostProcessor 进行
	 *  添加
	 *
	 *  public void addBeanFactoryPostProcessor（BeanFactoryPostProcessor beanFactoryPostProcessor）{
	 *      this.beanFatoryPostProcessors.add(BeanFactoryPostProcessor);
	 *  }
	 *
	 *  添加后的处理器会存在 beanFactoryPostProcessors 中，而在处理 BeanFactoryPostProcessor 时候会首先检测 beanFactoryPostProcessors
	 *  是否有数据，当然，BeanDefinitionRegistryPostProcessor 继承自BeanFactoryPostProcessor，不但有 BeanFactoryPostProcessor
	 *  的特性，同时还有自定义的个性化方法，也需要在此调用所以，这里需要从 beanFactoryPostProcessor 中挑出 BeanDefinitionRegistrProcessor
	 *  的后处理器，并进行 postProcessBeanDefinitionRegistr方法的激活
	 *  记录后处理器主要使用三个 List 来完成
	 *  registryPostProcessors :记录通过硬编码的方式注册的，BeanDefinitionRegistryPostProcessor 类型的处理器
	 *  regularPostProcessors：记录通过硬编码方式注册的 BeanFactoryPostProcessor 类型的处理器
	 *  registryPostProcessorBeans: 记录通过配置方式注册的 BeanDefinitionRegistryPostProcessor 类型的处理器
	 *  3.对以上的所记录的 List 中的后处理器进行统一的调用，BeanFactoryPostProcessor 的 postProcessBeanFactory 方法
	 *  4.对 beanFactoryPostProcessors 中非 BeanDefinitionRegistryPostProcessor 类型后处理器进行统一 BeanFactoryPostProcessors
	 *  的 postProcessBeanFactory 方法调用
	 *  5.普通 beanFactory处理
	 *  BeanDefinitionRegistryPostProcessor 只对 BeanDefinitionRegistry 类型的 ConfigurableListableBeanFactory 有效，所以
	 *  如果判断所示的 beanFactory 并不是 BeanDefinitionRegistry，那么便可以忽略 BeanDefinitionRegistryPostProcessor,而直接处理
	 *  BeanFactoryPostProcessor，当然获取的方式与上面的获取的类似。
	 *  这里需要提到的是，对于硬编码的方式手动添加的后处理器是不需要做任何排序 的，但是在配置文件中读取的处理器，Spring 并不保存读取
	 *  顺序的要求，Spring 对于后处理器的调用支持按照 PriorityOrdered 或者 Ordered 的顺序调用
	 *
	 *
	 *
	 */
	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		Set<String> processedBeans = new HashSet<>();

		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			List<BeanFactoryPostProcessor> regularPostProcessors = new LinkedList<>();
			/***
			 * BeanDefinitionRegistryPostProcessor
			 */
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new LinkedList<>();

			/***
			 * 硬编码注册后的处理器
			 */
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					// 对于 BeanDefinitionRegistryPostProcessor 类型，在 BeanFactoryPostProcessor 的基础上还有自己定义的方法，需要
					// 先调用
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					registryProcessors.add(registryProcessor);
				}
				else {
					//记录常规 BeanFactoryPostProcessor
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			// 配置注册的后处理器
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					//BeanDefinitionRegistryPostProcessor 的特殊处理
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			//激活 postProcessBeanFactory方法，之前激活的是 postProcessBeanDefinitionRegistry
			// 硬编码设置的 BeanDefinitionRegistryPostProcessor
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			//配置的 BeanDefinitionRegistryPostProcessor
			registryProcessors.addAll(currentRegistryProcessors);
			//常规 BeanFactoryPostProcessor
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		// 对于配置中的读取 BeanFactoryPostProcessor 的处理
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		//对后处理器进行分类
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
				//已经处理过
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		//按照 order 排序
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		// 无序，直接调用
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	// 注册 BeanPostProcessor
	// 上文中提到过的 BeanFactoryPostProcessors 的调用，现在我们来探索下 BeanPostProcessor，但是这里并不是调用，而是
	// 注册，真正的调用其实是在 bean 的实例化阶段进行的，这是一个很重要的步骤，也是很多的功能 BeanFactory 不支持的重要原因
	// Spring 中的大部分的功能都是通过后处理方式进行扩展的，这是 Spring 框架的一个特性，但是在 BeanFactory 中其实并没有
	// 实现后处理注册的，所以在调用的时候，如果没有进行手动注册其实是不能使用的，但是 ApplicationContext 中却添加了自动注册的
	// 功能，如自定义这样一个后处理器
	// public class MyInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor{
	// 		public Object postProcessBeforeInitialization(Object bean,String   beanName) throws BeanException{
	//			System.out.printlen("===========");
	//          return null;
	//		}
	// }
	// 在配置文件中添加配置
	// <bean class="processors.MyInstantiationAwareBeanPostcessor"/>
	// 在配置文件中添加配置
	// 那么使用 BeanFactory 方式进行 Spring 的 Bean 的加载时不会有任何改变，但是使用 ApplicationContext 方式获取 bean 的时候
	// 会在获取每个 bean 打印出"========" ,而这个特性就是在 registerBeanPostProcessors 方法中完成
	// 我们继续探索 registerBeanPostProcessors 的方法实现
	//
	//
	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		// BeanPostProcessorChecker 是这个普通的信息打印，可能会有些情况，当 Spring 的配置中的后处理器还是没有被注册
		// 就可以开始了 bean 初始化时，便会打印出 BeanPostProcessorChecker 中设定的信息
		//
		//
		//
		// 配合源码以及注释，在 registerBeanPostProcessors 方法中所做的逻辑相信大家已经很清楚了，我们再做一下总结
		// 首先我们会发现，对于 BeanPostProcessor 的处理 BeanFactoryPostProcessor 的处理极为相信，但是似乎又有些不一样的
		// 地方，经过反复的对比发现，对于 BeanFactoryPostProcessor 的处理要区分两种情况，一种方式是通过硬编码的方式的处理
		// ,另一种是通过配置文件的方式的处理，那么为什么在 BeanPostProcessor 的处理只考虑了配置文件的方式而不考虑硬编码的方式呢
		// 提出了这个问题，还是因为读者没有完全理解两者实现的功能，对于 BeanPostProcessor 并不需要马上调用，再说，当然不需要
		// 硬编码的方式了，这里的功能只需要将配置文件 BeanPostProcessor 提取出来并注册进行 beanFactory 就可以了。
		// 对于 beanFactory 的注册，也不是直接注册就可以了，在 Spring 中支持对于 BeanPostProcessor 的排序，比如根据 PriorityOrdered
		// 进行排序，根据 Ordered 进行排序或者无序，而 Spring 在 BeanPostProcessor 的激活顺序的时候也会考虑对于顺序的问题而先进行
		// 排序
		// 这里可能有个地方读者不是很理解，对于 internalPostProcessors 中存储的后处理器也就是 mergedBeanDefinitionPostProcessor 类型
		// 处理器，在代码中似乎是被重复调用了，如
		// 可以看到，在 registerBeanPostProcessors 方法的实现中其实已经确保了，beanPostProcessor 的唯一性，个人猜想，
		// 之所以选择在 registerBeanPostProcessors 中没有进行重复移除操作或许是为了的品质分类的效果，更逻辑理为清晰吧

		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;

		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		//  使用 ProorityOrdered 保证顺序
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		// MergedBeanDifinitionPostProcessor
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		//使用 Ordered 保证顺序
		List<String> orderedPostProcessorNames = new ArrayList<>();
		// 无序 BeanPostProcessor
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		// 第一步，注册所有实现 ProorityOrdered的 BeanPostProcessor
		//
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		// 第二步 ，注册所有的实现 Ordered 的 BeanPostProcessor
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		// 第三步，注册所有的无序的 BeanPostProcessor
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		// 第四步，注册所有的MergedBeanDifinitionPostProcessor 类型的 BeanPostProcessor，并非重复注册
		// 在 beanFactory.addBeanPostProcessor 中会先移除已经存在的 BeanPostProcessor
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		// 添加 ApplicationListener 探测器
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		Collections.sort(postProcessors, comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
