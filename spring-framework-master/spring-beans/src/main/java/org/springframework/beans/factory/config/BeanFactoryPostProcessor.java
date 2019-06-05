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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * Allows for custom modification of an application context's bean definitions,
 * adapting the bean property values of the context's underlying bean factory.
 *
 * <p>Application contexts can auto-detect BeanFactoryPostProcessor beans in
 * their bean definitions and apply them before any other beans get created.
 *
 * <p>Useful for custom config files targeted at system administrators that
 * override bean properties configured in the application context.
 *
 * <p>See PropertyResourceConfigurer and its concrete implementations
 * for out-of-the-box solutions that address such configuration needs.
 *
 * <p>A BeanFactoryPostProcessor may interact with and modify bean
 * definitions, but never bean instances. Doing so may cause premature bean
 * instantiation, violating the container and causing unintended side-effects.
 * If bean instance interaction is required, consider implementing
 * {@link BeanPostProcessor} instead.
 *
 * @author Juergen Hoeller
 * @since 06.07.2003
 * @see BeanPostProcessor
 * @see PropertyResourceConfigurer
 *
 *
 *
 * 正式的开始介绍之前，我们先了解下 BeanFactoryPostProcessor 的用法
 * BeanFactoryPostProcessor 接口跟 BeanPostProcessor 类似，可以对 bean 的定义配置元数据进行处理，也就是说，Spring 的 Ioc 容器
 * 允许 BeanFactoryPostProcessor 在容器实际实例化任何的其他的 bean 之前读取配置数据，并有可能修改它，如果你愿意，你可以配置多个
 * BeanFactoryPostProcessor，你还能通过设置 order 属性来控制 BeanFatoryPostProcessor 的执行次序，仅当 BeanFactoryPostProcessor
 * 实现了 Ordered 接口对你才可以设置 此属性，因此在实现 BeanFacotryPostProcessor 时，就就当考虑实现 Ordered接口，请参考 BeanFactoryPostProcessor
 * 和 Ordered 接口的 JavaDoc 以获得更加详细的信息
 * 如果你想改变实际的 bean 的实例，例如从配置元数据创建对象，那么你最好使用 BeanPostProessor，同样的的作用范围是容器级的，它只和你所
 * 使用的容器相关，如果你在容器中定义一个 BeanFactoryPostProcessor，它仅仅对此容器中的 bean 进行处理，BeanFactoryPostProcessor
 * ，它仅仅对容器中的 bean 进行处理，beanFactoryPostProcessor 不会对定义的另一个容器中的 bean 进行后置处理，即使用这两个容器都是同
 * 一个层次的上的，Spring 中对于 BeanFactoryPostProcessor 的典型应用，比如 PropertyPlaceholderConfigurer
 *
 *
 *
 * 1.BeanFactoryPostProcessor 的典型应用 ： propertyPlaceholderConfigurer
 * 有时候，阅读 Spring 的 Bean 的描述的文件时，你也许会遇到类似的如下的一些配置
 * <bean id="messeage" class ="disconfig.HelloMessage">
 * 		<property name = "mes">
 * 		 	<value>${bean.message}</value>
 * 	    </property>
 * </bean>
 *
 * 其中竟然出现了变量的引用：${bean.message} ，这就是 Spring 分期配置，可以在另外的配置文件中为 bean.message 指定值，如下
 * bean.property配置如下的定义
 * 当访问名为 message的 bean 时，mes 属性就会配置为字符串"Hi,can you find me ?" 但是 Spring 框架是怎么知道的存在这样的配置文件呢
 * 这就是靠 PropertyPlaceholderConfigurer 这个类bean 来实现的
 *
 *
 * <bean id="meshandler" class="org.Springframework.bean.factory.config.PropertyPlaceholderConfigurer">
 * 		<property name="locations">
        </property>
 * </bean>
 *
 * 在这个 bean 中指定的配置文件为 config/bean.properties，到这里似乎找到了我们的答案了
 *
 * 但是其实还有一个问题，这个"mesHandler",只不过是 Spring 框架管理的一个 bean，并没有被别的 bean 或者对象引用，Spring 的 beanFactory
 * 是怎样的的从这个配置文件中获取配置信息的呢，
 * 查看层级结构可以看出，PropertyPlaceholderConfigurer 这个类间接的继承了 BeanFactoryPostProcessor接口，这是一个特别的接口。
 * 当 Spring 加载任何实现这个接口的 bean 的配置的时候，都会在 bean 工厂载入所有的 bean 的配置之后执行，postProcessBeanFactory 方法
 * 在 PropertyResourceConfigurer 类中实现了 postProcessBeanFactory 方法，在方法中先后调用了 mergeProperties，convertProperties，
 * processProperties 这3个 方法，分别得到了配置，将得到配置转换成合适的类型，最后将配置内容告知 beanFactory
 *
 * 		正是通过实现 BeanFactoryPostProcessor接口，BeanFactory 会得到实例化的你任何 bean 前获得配置信息，从而能够正确的解析
 * 	bean 的描述文件中的变量引用
 * 	git comm
 *
 *
 */
@FunctionalInterface
public interface BeanFactoryPostProcessor {
	/**
	 * Modify the application context's internal bean factory after its standard
	 * initialization. All bean definitions will have been loaded, but no beans
	 * will have been instantiated yet. This allows for overriding or adding
	 * properties even to eager-initializing beans.
	 * @param beanFactory the bean factory used by the application context
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
