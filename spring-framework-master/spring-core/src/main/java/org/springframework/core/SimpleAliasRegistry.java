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

package org.springframework.core;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of the {@link AliasRegistry} interface.
 * Serves as base class for
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
 * implementations.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 *
 * 主要使用map作为alias的缓存 ，并对接口AliasRegistry进行了实现
 *
 *
 *
 */
public class SimpleAliasRegistry implements AliasRegistry {

	/**
	 * Map from alias to canonical name
	 */
	private final Map<String, String> aliasMap = new ConcurrentHashMap<>(16);

	/**
	 * 从下面的代码中可以得知注册alias 的步骤如下
	 * 1.alias 与 beanName 相同的情况处理，若 alias 与 beanName 并名称的时候相同则不需要处理并删除掉所有的 alias
	 * 2.alias 覆盖处理，若 alasName 已经使用并已经指向了另一个 beanName 则需要用户的设置进行处理
	 * 3.alias 循环检查，当 A->B 存在时，若再次出现 A->C->B 时候则抛出异常
	 * 4.注册 alias
	 *
	 *
	 * 通过代码 getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder))
	 * 完成此工作，这里的实现只是扩展，当程序开发人员需要对注册 BeanDefinition 事件进行监听时，这里的实现只为
	 * 扩展，当程序开发人员需要注册 BeanDefinition 事件进行监听时可以能完注册监听器的方式并将处理逻辑监听器中，目前在 Spring 中并没有对此事件
	 * 做任何逻辑处理
	 *
	 * 要给 javaBean 增加别名，以方便不同的对象来调用，我们就可以直接使用 bean 标签中的 name 属性
	 * <bean id = "testBean" name = "testBean,testBean2" class="com.test">
	 *
	 * </bean>
	 *
	 * 同样，Spring还有另一种声明的别名的方式
	 * <bean id="testBean" class="com.test">
	 *
	 * </bean>
	 * <alias name="testBean" alias="testBean,testBean2"></alias>
	 *
	 * 考虑一个更为具体的例子，组件 A 在 XML 配置文件中定义一个名了 componentA 的 DataSource 类型的 Bean ，但组件B 却想组件 B 在其中 xml 文件中以 componentB 命名来
	 * 引用此命名来引用此 bean 而且在主程序 MYApp 的 Xml 配置文件中，希望 myApp 的名字来引用此 bean ，最后容器加载 3个 XML 文件来生成最终的 ApplicationContext，在此
	 * 情形下，可通过配置文件中添加下列 alias 元素来实现
	 *
	 * 	<alias name="componentA" alias="componentB" />
	 * 	<alias name="componentB" alias="myApp"/>
	 *
	 * 	这样一来，每个组件及组件主程序可以通过唯一名字来引用同一个数据源而互不干扰
	 * 	在之前的章节中已经讲过对于 bean 中的 name 元素解析，那么我们现在再深入分析对于 alias 标签的解析过程
	 *
	 *
	 *
	 *
	 * */
	@Override
	public void registerAlias(String name, String alias) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.hasText(alias, "'alias' must not be empty");
		//如果 beanName 与 alias 相同的话，不记录 alias 并删除对应的 alias
		if (alias.equals(name)) {
			this.aliasMap.remove(alias);
		} else {
			//如果 alias 不允许被覆盖则抛出异常
			String registeredName = this.aliasMap.get(alias);
			if (registeredName != null) {
				if (registeredName.equals(name)) {
					// An existing alias - no need to re-register
					return;
				}
				if (!allowAliasOverriding()) {
					throw new IllegalStateException("Cannot register alias '" + alias + "' for name '" +
							name + "': It is already registered for name '" + registeredName + "'.");
				}
			}
			// 当 A->B 存在时，若再次出现A->C-B 时候，则抛出异常
			checkForAliasCircle(name, alias);
			this.aliasMap.put(alias, name);
		}
	}

	/**
	 * Return whether alias overriding is allowed.
	 * Default is {@code true}.
	 */
	protected boolean allowAliasOverriding() {
		return true;
	}

	/**
	 * Determine whether the given name has the given alias registered.
	 *
	 * @param name  the name to check
	 * @param alias the alias to look for
	 * @since 4.2.1
	 */
	public boolean hasAlias(String name, String alias) {
		for (Map.Entry<String, String> entry : this.aliasMap.entrySet()) {
			String registeredName = entry.getValue();
			if (registeredName.equals(name)) {
				String registeredAlias = entry.getKey();
				return (registeredAlias.equals(alias) || hasAlias(registeredAlias, alias));
			}
		}
		return false;
	}

	@Override
	public void removeAlias(String alias) {
		String name = this.aliasMap.remove(alias);
		if (name == null) {
			throw new IllegalStateException("No alias '" + alias + "' registered");
		}
	}

	@Override
	public boolean isAlias(String name) {
		return this.aliasMap.containsKey(name);
	}

	@Override
	public String[] getAliases(String name) {
		List<String> result = new ArrayList<>();
		synchronized (this.aliasMap) {
			retrieveAliases(name, result);
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * Transitively retrieve all aliases for the given name.
	 *
	 * @param name   the target name to find aliases for
	 * @param result the resulting aliases list
	 */
	private void retrieveAliases(String name, List<String> result) {
		this.aliasMap.forEach((alias, registeredName) -> {
			if (registeredName.equals(name)) {
				result.add(alias);
				retrieveAliases(alias, result);
			}
		});
	}

	/**
	 * Resolve all alias target names and aliases registered in this
	 * factory, applying the given StringValueResolver to them.
	 * <p>The value resolver may for example resolve placeholders
	 * in target bean names and even in alias names.
	 *
	 * @param valueResolver the StringValueResolver to apply
	 */
	public void resolveAliases(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		synchronized (this.aliasMap) {
			Map<String, String> aliasCopy = new HashMap<>(this.aliasMap);
			for (String alias : aliasCopy.keySet()) {
				String registeredName = aliasCopy.get(alias);
				String resolvedAlias = valueResolver.resolveStringValue(alias);
				String resolvedName = valueResolver.resolveStringValue(registeredName);
				if (resolvedAlias == null || resolvedName == null || resolvedAlias.equals(resolvedName)) {
					this.aliasMap.remove(alias);
				} else if (!resolvedAlias.equals(alias)) {
					String existingName = this.aliasMap.get(resolvedAlias);
					if (existingName != null) {
						if (existingName.equals(resolvedName)) {
							// Pointing to existing alias - just remove placeholder
							this.aliasMap.remove(alias);
							break;
						}
						throw new IllegalStateException(
								"Cannot register resolved alias '" + resolvedAlias + "' (original: '" + alias +
										"') for name '" + resolvedName + "': It is already registered for name '" +
										registeredName + "'.");
					}
					checkForAliasCircle(resolvedName, resolvedAlias);
					this.aliasMap.remove(alias);
					this.aliasMap.put(resolvedAlias, resolvedName);
				} else if (!registeredName.equals(resolvedName)) {
					this.aliasMap.put(alias, resolvedName);
				}
			}
		}
	}

	/**
	 * Check whether the given name points back to the given alias as an alias
	 * in the other direction already, catching a circular reference upfront
	 * and throwing a corresponding IllegalStateException.
	 *
	 * @param name  the candidate name
	 * @param alias the candidate alias
	 * @see #registerAlias
	 * @see #hasAlias
	 */
	protected void checkForAliasCircle(String name, String alias) {
		if (hasAlias(alias, name)) {
			throw new IllegalStateException("Cannot register alias '" + alias +
					"' for name '" + name + "': Circular reference - '" +
					name + "' is a direct or indirect alias for '" + alias + "' already");
		}
	}

	/**
	 * Determine the raw name, resolving aliases to canonical names.
	 *
	 * @param name the user-specified name
	 * @return the transformed name
	 */
	public String canonicalName(String name) {
		String canonicalName = name;
		// Handle aliasing...
		String resolvedName;
		do {
			resolvedName = this.aliasMap.get(canonicalName);
			if (resolvedName != null) {
				canonicalName = resolvedName;
			}
		}
		while (resolvedName != null);
		return canonicalName;
	}

}
