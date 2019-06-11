/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.Proxy;

import org.springframework.aop.SpringProxy;

/**
 * Default {@link AopProxyFactory} implementation, creating either a CGLIB proxy
 * or a JDK dynamic proxy.
 *
 * <p>Creates a CGLIB proxy if one the following is true for a given
 * {@link AdvisedSupport} instance:
 * <ul>
 * <li>the {@code optimize} flag is set
 * <li>the {@code proxyTargetClass} flag is set
 * <li>no proxy interfaces have been specified
 * </ul>
 *
 * <p>In general, specify {@code proxyTargetClass} to enforce a CGLIB proxy,
 * or specify one or more interfaces to use a JDK dynamic proxy.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 12.03.2004
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 */
@SuppressWarnings("serial")
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {


	/***
	 * 至此已经完成了代理的创建，不管我们之前是否有过阅读 Spring 源代码的，但是都是或多或少的听过 Spring 代理中的 JDKproxy 的实现和
	 * CglibProxy 的实现，Spring 是如何选取的呢，网上介绍到处都是，现在我们就是从源代码的角度分析，看看到底 Spring 是如何选择代理方式的
	 * 从 if 中判断条件可以看到3个方面的影响着 Spring 的判断
	 * optimize:用来控制通过 CGLiB 的创建代理是否使用激进的优化策略，除非完全了解AOP 代理是如何处理优化，否则不推荐用户使用这个设置
	 * ，目前这个属性仅用于 CGLIB 代理，对于 JDK动态代理，缺省的代理  无效
	 * proxyTargetClass：这个属性为 true时，目标类本身被代理而不是目标类的接口，如果这个属性值被设为 true,CGLIB 代理将被创建，设置
	 * 方式，<aop:aspectj-autoproxy-proxy-target-class="true"></aop>
	 * hasNoUserSuppliedProxyInterfaces:是否存在代理接口
	 *
	 * 下面是 JDK 代理和 Cglib 代理方式的总结
	 * 如果目标对象实现了接口，默认的情况下会采用 JDK 动态代理去实现
	 * 如果目标对象实现了接口，可以强制使用 CGLIB 实现 AOP
	 * 如果目标对象没有实现了接口，必须采用 CGLIB库，Spring 会自动的在 JDK 动态代理和 CGLIB 之间转换
	 * 如何强制使用 CGLIB 实现 AOP?
	 * 1.添加 CBLIB库，Spring_HOME/cglib/*.jar
	 * 2.在 Spring 配置文件中加入<aop:aspectj-autoproxy proxy-target-class="true"></aop>
	 * JDK 动态代理和 CGLIB 字节码生成的区别
	 * JDK 动态代理只能对接口实现了接口的类生成代理，而不能针对类
	 * CGLIB是针对类实现代理，主要是对指定生成的一个子类，覆盖其中的方法，因为这是继承，所以该类或方法最好不要声明成 final
	 *
	 * 2.获取代理
	 *  确定了使用哪种代理方式后，便可以进行代理的创建了，但是创建之前有必要回顾一下两种方式的使用方法
	 *  JDK 代理使用示例
	 *
	 *
	 *
	 *
	 *
	 */
	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			return new ObjenesisCglibAopProxy(config);
		}
		else {
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
