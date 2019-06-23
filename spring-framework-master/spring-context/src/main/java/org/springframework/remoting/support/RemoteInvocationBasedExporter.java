/*
 * Copyright 2002-2007 the original author or authors.
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

import java.lang.reflect.InvocationTargetException;

/**
 * Abstract base class for remote service exporters that are based
 * on deserialization of {@link RemoteInvocation} objects.
 *
 * <p>Provides a "remoteInvocationExecutor" property, with a
 * {@link DefaultRemoteInvocationExecutor} as default strategy.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see RemoteInvocationExecutor
 * @see DefaultRemoteInvocationExecutor
 */
public abstract class RemoteInvocationBasedExporter extends RemoteExporter {

	private RemoteInvocationExecutor remoteInvocationExecutor = new DefaultRemoteInvocationExecutor();


	/**
	 * Set the RemoteInvocationExecutor to use for this exporter.
	 * Default is a DefaultRemoteInvocationExecutor.
	 * <p>A custom invocation executor can extract further context information
	 * from the invocation, for example user credentials.
	 */
	public void setRemoteInvocationExecutor(RemoteInvocationExecutor remoteInvocationExecutor) {
		this.remoteInvocationExecutor = remoteInvocationExecutor;
	}

	/**
	 * Return the RemoteInvocationExecutor used by this exporter.
	 */
	public RemoteInvocationExecutor getRemoteInvocationExecutor() {
		return this.remoteInvocationExecutor;
	}


	/**
	 * Apply the given remote invocation to the given target object.
	 * The default implementation delegates to the RemoteInvocationExecutor.
	 * <p>Can be overridden in subclasses for custom invocation behavior,
	 * possibly for applying additional invocation parameters from a
	 * custom RemoteInvocation subclass. Note that it is preferable to use
	 * a custom RemoteInvocationExecutor which is a reusable strategy.
	 * @param invocation the remote invocation
	 * @param targetObject the target object to apply the invocation to
	 * @return the invocation result
	 * @throws NoSuchMethodException if the method name could not be resolved
	 * @throws IllegalAccessException if the method could not be accessed
	 * @throws InvocationTargetException if the method invocation resulted in an exception
	 * @see RemoteInvocationExecutor#invoke
	 * RMI 服务激活调用
	 * 	之前反复的提到过bean初始化的时候，做了服务名称绑定，this.registry.bind(this.ServiceName,this.exportedObject),其中的
	 * 	exportedObject其实是被RMIInvocationWrapper进行封装的，也就是说当其他服务调用serviceName的RMI服务时，Java会为我们封装其
	 * 	内部操作，而直接将代码转向RMIInvocationWrapper的invoke方法
	 *
	 * 	因此this.RMIExporter为之前初始化的RMIServiceExporter，
	 * 	invocation为包含着需要激活的方法参数
	 * 	而wrappedObject则是之前封装的代理类
	 *
	 *
	 *
	 */
	protected Object invoke(RemoteInvocation invocation, Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		if (logger.isTraceEnabled()) {
			logger.trace("Executing " + invocation);
		}
		try {
			return getRemoteInvocationExecutor().invoke(invocation, targetObject);
		}
		catch (NoSuchMethodException ex) {
			if (logger.isDebugEnabled()) {
				logger.warn("Could not find target method for " + invocation, ex);
			}
			throw ex;
		}
		catch (IllegalAccessException ex) {
			if (logger.isDebugEnabled()) {
				logger.warn("Could not access target method for " + invocation, ex);
			}
			throw ex;
		}
		catch (InvocationTargetException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Target method failed for " + invocation, ex.getTargetException());
			}
			throw ex;
		}
	}

	/**
	 * Apply the given remote invocation to the given target object, wrapping
	 * the invocation result in a serializable RemoteInvocationResult object.
	 * The default implementation creates a plain RemoteInvocationResult.
	 * <p>Can be overridden in subclasses for custom invocation behavior,
	 * for example to return additional context information. Note that this
	 * is not covered by the RemoteInvocationExecutor strategy!
	 * @param invocation the remote invocation
	 * @param targetObject the target object to apply the invocation to
	 * @return the invocation result
	 * @see #invoke
	 *
	 *
	 * 执行调用
	 * 根据反序列化方式得到RemoteInvocation对象中的信息，进行方法的调用，注意，此时调用的实体并不是服务接口或者服务类，而是之前的初始化
	 * 时候构造封装了服务接口以及服务类的代理
	 * 	完成了RemoteInvocation实例的提取，也就意味着可以通过RemoteInvocation实例中提供了信息进行方法的调用了
	 *
	 */
	protected RemoteInvocationResult invokeAndCreateResult(RemoteInvocation invocation, Object targetObject) {
		try {
			// 激活代理类中对应的invocation中的方法
			// 对应方法的激活也就是invoke方法的调用，虽然经过层层的环绕，但是最终还是实现了我们熟知的调用invocation.invoke(targetObject)，
			// 也就是执行RemoteInvocation类中的invoke方法，大致的逻辑还是通过RemoteInvocation中对应的方法信息在targetObject上去执行
			// 此方法在分析RMI的功能的时候，已经分析过了，不再赘述，但是对于当前方法的targetObject参数，此targetObject是代理类，调用
			// 代理类的时候，需要考虑增强方法的调用，这是读者需要注意的地方


			Object value = invoke(invocation, targetObject);
			// 封装结果以便于序列化
			// 对于返回的结果需要使用RemoteInvocationResult进行封装，之所以需要通过使用RemoteInvocationResult类进行封装，是因为无法
			// 保证所有的操作返回的结果都继承Serializable接口，也就是说无法保证所有返回结果都可以直接进行序列化，那么就必需使用RemoteInvocationResult进行统一的封装

			return new RemoteInvocationResult(value);
		}
		catch (Throwable ex) {
			return new RemoteInvocationResult(ex);
		}
	}

}
