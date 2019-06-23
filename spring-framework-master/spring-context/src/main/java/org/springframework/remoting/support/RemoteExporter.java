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

package org.springframework.remoting.support;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class for classes that export a remote service.
 * Provides "service" and "serviceInterface" bean properties.
 *
 * <p>Note that the service interface being used will show some signs of
 * remotability, like the granularity of method calls that it offers.
 * Furthermore, it has to have serializable arguments etc.
 *
 * @author Juergen Hoeller
 * @since 26.12.2003
 */
public abstract class RemoteExporter extends RemotingSupport {

	private Object service;

	private Class<?> serviceInterface;

	private Boolean registerTraceInterceptor;

	private Object[] interceptors;


	/**
	 * Set the service to export.
	 * Typically populated via a bean reference.
	 */
	public void setService(Object service) {
		this.service = service;
	}

	/**
	 * Return the service to export.
	 */
	public Object getService() {
		return this.service;
	}

	/**
	 * Set the interface of the service to export.
	 * The interface must be suitable for the particular service and remoting strategy.
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		Assert.notNull(serviceInterface, "'serviceInterface' must not be null");
		Assert.isTrue(serviceInterface.isInterface(), "'serviceInterface' must be an interface");
		this.serviceInterface = serviceInterface;
	}

	/**
	 * Return the interface of the service to export.
	 */
	public Class<?> getServiceInterface() {
		return this.serviceInterface;
	}

	/**
	 * Set whether to register a RemoteInvocationTraceInterceptor for exported
	 * services. Only applied when a subclass uses {@code getProxyForService}
	 * for creating the proxy to expose.
	 * <p>Default is "true". RemoteInvocationTraceInterceptor's most important value
	 * is that it logs exception stacktraces on the server, before propagating an
	 * exception to the client. Note that RemoteInvocationTraceInterceptor will <i>not</i>
	 * be registered by default if the "interceptors" property has been specified.
	 * @see #setInterceptors
	 * @see #getProxyForService
	 * @see RemoteInvocationTraceInterceptor
	 */
	public void setRegisterTraceInterceptor(boolean registerTraceInterceptor) {
		this.registerTraceInterceptor = registerTraceInterceptor;
	}

	/**
	 * Set additional interceptors (or advisors) to be applied before the
	 * remote endpoint, e.g. a PerformanceMonitorInterceptor.
	 * <p>You may specify any AOP Alliance MethodInterceptors or other
	 * Spring AOP Advices, as well as Spring AOP Advisors.
	 * @see #getProxyForService
	 * @see org.springframework.aop.interceptor.PerformanceMonitorInterceptor
	 */
	public void setInterceptors(Object[] interceptors) {
		this.interceptors = interceptors;
	}


	/**
	 * Check whether the service reference has been set.
	 * @see #setService
	 */
	protected void checkService() throws IllegalArgumentException {
		Assert.notNull(getService(), "Property 'service' is required");
	}

	/**
	 * Check whether a service reference has been set,
	 * and whether it matches the specified service.
	 * @see #setServiceInterface
	 * @see #setService
	 */
	protected void checkServiceInterface() throws IllegalArgumentException {
		Class<?> serviceInterface = getServiceInterface();
		Assert.notNull(serviceInterface, "Property 'serviceInterface' is required");

		Object service = getService();
		if (service instanceof String) {
			throw new IllegalArgumentException("Service [" + service + "] is a String " +
					"rather than an actual service reference: Have you accidentally specified " +
					"the service bean name as value instead of as reference?");
		}
		if (!serviceInterface.isInstance(service)) {
			throw new IllegalArgumentException("Service interface [" + serviceInterface.getName() +
					"] needs to be implemented by service [" + service + "] of class [" +
					service.getClass().getName() + "]");
		}
	}

	/**
	 * Get a proxy for the given service object, implementing the specified
	 * service interface.
	 * <p>Used to export a proxy that does not expose any internals but just
	 * a specific interface intended for remote access. Furthermore, a
	 * {@link RemoteInvocationTraceInterceptor} will be registered (by default).
	 * @return the proxy
	 * @see #setServiceInterface
	 * @see #setRegisterTraceInterceptor
	 * @see RemoteInvocationTraceInterceptor
	 *
	 *
	 * 语法处理类的初始化主要规则为，如果配置的service属性对应的类实现了Remote接口且没有配置serviceInterface属性，那么直接使用service
	 * 作为处理类，否则，使用RMIInvocationWrapper对service的代理类和当前类也就是RMIServiceExporter进行封装
	 *
	 * 结过这样的封装，客户端与服务端便可以达成一致的协义，当客户端与服务端很好的连接在一起了，而RMIInvocationWapper封装了处理请求的代理类
	 * 在invoke中便会用不用代理类进一步的处理
	 *
	 * 之前的逻辑已经非常的清楚了，当请求RMI服务时会由注册表Registry实例将请求转向之前注册的处理类去处理，也就是说之前封装的RMIInvocationWrapper
	 * ,然后由RMIInvocationWrapper中的invoke方法进行处理，那么为什么不是在invoke方法中直接使用这些service，而是通过代理再次使用service封装呢
	 *
	 * 这其中的一个关键点是，在创建代理时添加一个增强拦截器RemoteInvocationTraceInterceptor ,目的是为了对方法的调用进行打印跟踪，
	 * 但是如果直接在invoke方法中的硬编码这此日志，会使代码看起来很不优雅，而且耦合度很高，使用代理的方式就会解决这样的问题，而且会有很高的可
	 * 扩展性
	 *
	 *
	 * 对于Spring中HttpInvoker服务的实现，我们还是首先从服务端进行分析，
	 * 根据remote-servlet.xml中配置，我们分析入口应该为org.Springframework.remoting.httpinvoker.HttpInvokerServiceExporter,
	 * 如图12-3 所示
	 *
	 * 通过层次关系我们看到HttpInvokerServiceExporter 类实现了，InitializingBean接口以及HttpRequestHandler接口，分析RMI服务时
	 * 我们已经解析了，当某个bean 继承自InitiazingBean 接口的时候，Spring会确保这个bean在初始化时调用其afterPropertiesSet方法，而对于
	 * HttpRequestHandler接口，因为我们在配置中已经将此接口配置成Web服务，那么当有相应的请求的时候，Spring的Web服务就会将程序导致
	 * HttpRequestHandler的HandleRequest方法中首先，我们从afterPropertiesSet方法开始分析，看看bean的初始化的过程做了哪些逻辑
	 *
	 * 通过将上面的3个方法中联，可以看到，初始化过程中实现的逻辑主要创建一个代理，代理中封装了对特定请求的处理方法以及接口等信息，而这个代理 的最
	 * 关键的目的是加入了RemoteInvocationTranceInterceptor增强器，当然创建代理还有些其他的好处，比如代码优雅，方便扩展，
	 * RemoteInvocationTraceInterceptor中的增强主要对增强的目标方法进行了一些相关的信息日志打印，并没有在此基础上进行任何功能的增强，
	 * 那么这个任何功能性的增强，那么这个代理究竟是在什么时候使用的呢，暂留下悬念，我们接下来分析当有Web请求时，HttpRequestHandler
	 * 的HandleRequest方法的处理
	 *
	 *
	 *
	 *
	 *
	 */
	protected Object getProxyForService() {
		// 验证 service
		checkService();
		// 验证serviceInterface
		checkServiceInterface();
		// 使用JDK的方式创建代理
		ProxyFactory proxyFactory = new ProxyFactory();
		// 添加代理接口
		proxyFactory.addInterface(getServiceInterface());

		if (this.registerTraceInterceptor != null ? this.registerTraceInterceptor : this.interceptors == null) {
			// 加入代理的横切面RemoteInvocationTranceInterceptor 并记录exporter名称
			proxyFactory.addAdvice(new RemoteInvocationTraceInterceptor(getExporterName()));
		}
		if (this.interceptors != null) {
			AdvisorAdapterRegistry adapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();
			for (Object interceptor : this.interceptors) {
				proxyFactory.addAdvisor(adapterRegistry.wrap(interceptor));
			}
		}
		// 设置要代理的目标类
		proxyFactory.setTarget(getService());
		proxyFactory.setOpaque(true);
		// 创建代理
		return proxyFactory.getProxy(getBeanClassLoader());
	}

	/**
	 * Return a short name for this exporter.
	 * Used for tracing of remote invocations.
	 * <p>Default is the unqualified class name (without package).
	 * Can be overridden in subclasses.
	 * @see #getProxyForService
	 * @see RemoteInvocationTraceInterceptor
	 * @see org.springframework.util.ClassUtils#getShortName
	 */
	protected String getExporterName() {
		return ClassUtils.getShortName(getClass());
	}

}
