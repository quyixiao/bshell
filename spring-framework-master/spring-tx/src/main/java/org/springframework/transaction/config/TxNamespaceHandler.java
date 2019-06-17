/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.transaction.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@code NamespaceHandler} allowing for the configuration of
 * declarative transaction management using either XML or using annotations.
 *
 * <p>This namespace handler is the central piece of functionality in the
 * Spring transaction management facilities and offers two approaches
 * to declaratively manage transactions.
 *
 * <p>One approach uses transaction semantics defined in XML using the
 * {@code <tx:advice>} elements, the other uses annotations
 * in combination with the {@code <tx:annotation-driven>} element.
 * Both approached are detailed to great extent in the Spring reference manual.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 *
 *
 * Spring声明式事务让我们从复杂的事务处理中得到解脱，使我们再也不需要去处理获得连接，关闭接连，事务提交和回滚等操作，现也不需要在与
 * 事务相关的方法中处理大量的try...catch...finally 代码，Spring中事务的使用虽然已经相对简单得多，但是，还是有么多的使用及配置规则
 * ，有兴趣的读者可以自己查阅相关的资料，进行深入研究，这里只列举出最常用的使用方法
 *
 * 同样，我们还是以最简单的方法进行直观的介绍
 *
 *
 *
 */
public class TxNamespaceHandler extends NamespaceHandlerSupport {

	static final String TRANSACTION_MANAGER_ATTRIBUTE = "transaction-manager";

	static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";


	static String getTransactionManagerName(Element element) {
		return (element.hasAttribute(TRANSACTION_MANAGER_ATTRIBUTE) ?
				element.getAttribute(TRANSACTION_MANAGER_ATTRIBUTE) : DEFAULT_TRANSACTION_MANAGER_BEAN_NAME);
	}


	/****
	 * 根据自定义标签的使用规则以及上面的代码，可以知道在遇到诸如<tx:annotation-driven 为开关配置，spring者会使用AnnotationDrivenBeanDefinitionParser
	 * 类的parse方法进行解析
	 *
	 */
	@Override
	public void init() {
		registerBeanDefinitionParser("advice", new TxAdviceBeanDefinitionParser());
		registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenBeanDefinitionParser());
		registerBeanDefinitionParser("jta-transaction-manager", new JtaTransactionManagerBeanDefinitionParser());
	}

}
