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

package org.springframework.remoting.httpinvoker;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} for HTTP invoker proxies. Exposes the proxied service
 * for use as a bean reference, using the specified service interface.
 *
 * <p>The service URL must be an HTTP URL exposing an HTTP invoker service.
 * Optionally, a codebase URL can be specified for on-demand dynamic code download
 * from a remote location. For details, see HttpInvokerClientInterceptor docs.
 *
 * <p>Serializes remote invocation objects and deserializes remote invocation
 * result objects. Uses Java serialization just like RMI, but provides the
 * same ease of setup as Caucho's HTTP-based Hessian protocol.
 *
 * <p><b>HTTP invoker is the recommended protocol for Java-to-Java remoting.</b>
 * It is more powerful and more extensible than Hessian, at the expense of
 * being tied to Java. Nevertheless, it is as easy to set up as Hessian,
 * which is its main advantage compared to RMI.
 *
 * <p><b>WARNING: Be aware of vulnerabilities due to unsafe Java deserialization:
 * Manipulated input streams could lead to unwanted code execution on the server
 * during the deserialization step. As a consequence, do not expose HTTP invoker
 * endpoints to untrusted clients but rather just between your own services.</b>
 * In general, we strongly recommend any other message format (e.g. JSON) instead.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setServiceInterface
 * @see #setServiceUrl
 * @see #setCodebaseUrl
 * @see HttpInvokerClientInterceptor
 * @see HttpInvokerServiceExporter
 * @see org.springframework.remoting.rmi.RmiProxyFactoryBean
 * @see org.springframework.remoting.caucho.HessianProxyFactoryBean
 *
 *
 * 分析了对服务端的解析以太处理过程后，我们接下来分析客户端的调用过程，在服务端调用的分析中我们反复提到需要从HttpServletRequest中提取
 * 从客户端传来的RemoteInvocation实例，然后进行相应的解析，所以，在客户端，一个比较重要的任务就构建RemoteInvocation实例，并传送
 * 到服务端，根据配置文件中的信息，我们还是首先锁定HttpInvokerProxyFactoryBean类，并查看其层次结构，如图12-4所示
 * 	从层次结构中我们看到，HttpInvokerProxyFactoryBean类同样实现了InitializingBean接口，同时，又实现了FactoryBean以及MethodInterceptor
 * 	这已经是进行老生常谈的问题了，实现这几个接口以及这第几个接口在Spring中是用来做什么的，我这里也不现赘述了，我们还是根据实现的InitializginbBean
 * 	接口分析初始化过程的逻辑
 *
 *
 *
 *
 */
public class HttpInvokerProxyFactoryBean extends HttpInvokerClientInterceptor implements FactoryBean<Object> {

	@Nullable
	private Object serviceProxy;


	// 在afterPropertiesSet中主要创建了一个代理，该代理封装了配置的服务接口，并使用当前的类也就是HttpInvokerProxyFactoryBean作为
	//增强，因为HttpInvokerProxyFactoryBean实现了MethodInterceptor方法，所以可以作为增强拦截器
	// 同样，又由于HttpInvokerProxyFactoryBean实现了FactoryBean接口，所以通过Spring中普通方式调用该bean时调用的并不是该本身，而
	// 是getObect()方法返回的实例，也就是说，实例化过程中所创建的代理
	// 那么综合之前的使用示例，我们再次回顾一下，HttpInvokerProxyFactoryBean类型bean在初始化过程中创建的封装服务接口的代理，并使用
	//自身作为增强拦截器，然后又因为实现了FactoryBean接口，所在获取Bean的时候，返回的其实是创建的代理，那么，汇总上面的逻辑
	// 当调用如下代码时，其实是调用代理类中的服务方法，而在在调用代理类中的服务方法时又会使用代理类中的增强器进行增强
	// 	ApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext-client.xml");
	//	HttpInvokeTestI httpInvokeTestI = (HttpInvokeTestI)context.getBean("remoteService");
	//  System.out.println(httpInvokeTestI.getTestPo("dddddddd"));
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		Class<?> ifc = getServiceInterface();
		Assert.notNull(ifc, "Property 'serviceInterface' is required");
		//创建代理并使用当前方法为拦截器增强
		this.serviceProxy = new ProxyFactory(ifc, this).getProxy(getBeanClassLoader());
	}


	@Override
	@Nullable
	public Object getObject() {
		return this.serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return getServiceInterface();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
