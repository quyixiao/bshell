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

package org.springframework.beans.factory;

import org.springframework.lang.Nullable;

/**
 * Interface to be implemented by objects used within a {@link BeanFactory} which
 * are themselves factories for individual objects. If a bean implements this
 * interface, it is used as a factory for an object to expose, not directly as a
 * bean instance that will be exposed itself.
 *
 * <p><b>NB: A bean that implements this interface cannot be used as a normal bean.</b>
 * A FactoryBean is defined in a bean style, but the object exposed for bean
 * references ({@link #getObject()}) is always the object that it creates.
 *
 * <p>FactoryBeans can support singletons and prototypes, and can either create
 * objects lazily on demand or eagerly on startup. The {@link SmartFactoryBean}
 * interface allows for exposing more fine-grained behavioral metadata.
 *
 * <p>This interface is heavily used within the framework itself, for example for
 * the AOP {@link org.springframework.aop.framework.ProxyFactoryBean} or the
 * {@link org.springframework.jndi.JndiObjectFactoryBean}. It can be used for
 * custom components as well; however, this is only common for infrastructure code.
 *
 * <p><b>{@code FactoryBean} is a programmatic contract. Implementations are not
 * supposed to rely on annotation-driven injection or other reflective facilities.</b>
 * {@link #getObjectType()} {@link #getObject()} invocations may arrive early in
 * the bootstrap process, even ahead of any post-processor setup. If you need access
 * other beans, implement {@link BeanFactoryAware} and obtain them programmatically.
 *
 * <p>Finally, FactoryBean objects participate in the containing BeanFactory's
 * synchronization of bean creation. There is usually no need for internal
 * synchronization other than for purposes of lazy initialization within the
 * FactoryBean itself (or the like).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 08.03.2003
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see org.springframework.jndi.JndiObjectFactoryBean
 * 一般情况下，Spring 通过反射机制利用 bean 的 Class 属性指定实现类的实例化 bean，在某些情况下
 * 实例化 bean 的过程比较复杂，如果按照传统的方式，则需要在 bean 中提供大量的配置信息，西里方式的粘液性是受限的，
 * 这时采用编码的方式可能会得到一个简单的方案， Spring 为此提供了一个 org.Springframework.bean.factory.FactoryBean 工厂类接口，
 * 用户可以实现该接口实例化 bean 的逻辑
 * FactoryBean 接口对于 Spring 框架来说 占有重要的地位，Spring 自身就提供了70多个 FactoryBean 的实现，它们隐藏了实例化一些复杂的 bean
 * 的细节，给上层应用带来了便利，从 Spring3.0开始，FactoryBean 开始支持泛型，即接口声明改成 FactoryBean<T>的形式
 *
 *
 * 当配置文件中<bean>的 class 属性配置的实现类是 FactoryBean 时，通过 getBean()方法返回不是 FactoryBean本身，而是 FactoryBean#getObject()
 * 方法返回的对象，相当于 FactoryBean#getObject()代理了 getBean()方法，例如：如果使用传统的方式配置下面的 Car的<bean>时，Car 的的每个属性分别对应的
 * 一个<property>元素标签
 * public class Car{
 *     private int maxSpeed;
 *     private String brand;
 *     private double price;
 *     // get set 方法
 * }
 *
 * 如果用 FactoryBean 的方式实现就会以灵活一些，下例通过逗号分割符方式一次性的为 Car 的所有属性指定配置
 * public class CarFactoryBean implements FactoryBean<Car>{
 *     private String carInfo;
 *     public Car getObject(){
 *         Car car = new Car();
 *         String infos[] = carInfo.split(",");
 *         car.setBrand(infos[0]);
 *         car.setMaxSpeed(Interger.valueOf(infos[1]));
 *         car.setPrice(Double.valueof(infos[2]));
 *         return car;
 *     }
 *
 *     public class<Car> getObjectType(){
 *     		return Car.class;
 *     }
 *
 *     public boolean isSingleton(){
 *     		return false;
 *     }
 *
 *     public String getCarInfo(){
 *     		return this.carInfo;
 *     	}
 *
 *     //接受逗号分割符设置的属性信息
 *     public void setCarInfo(){
 *     		this.carInfo = carInfo;
 *     }
 *
 * }
 *
 * 有了这个 CarFactoryBean后，我们就可以在配置文件中使用下面的这种自定义配置方式配置 CarBean 了
 * <bean id="car" class="com.test.factorybean.CarFactoryBean" carInfo="超级跑车,400,200000">
 * 当调用 getBean("car")时，Spring 通过反射机制发现了 CarFactoryBean 实现了 FactoryBean 的接口，这时 Spring 容器就调用接口方法 CarFactoryBean#getObject()方法
 *返回了，如果希望获取CarFactoryBean 的实例，则需要使用 getBean(beanName)方法时 beanName 前面显示的加上&前缀，例如 getBean("&car")
 *
 *
 *
 *
 *
 */
public interface FactoryBean<T> {

	/**
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * <p>As with a {@link BeanFactory}, this allows support for both the
	 * Singleton and Prototype design pattern.
	 * <p>If this FactoryBean is not fully initialized yet at the time of
	 * the call (for example because it is involved in a circular reference),
	 * throw a corresponding {@link FactoryBeanNotInitializedException}.
	 * <p>As of Spring 2.0, FactoryBeans are allowed to return {@code null}
	 * objects. The factory will consider this as normal value to be used; it
	 * will not throw a FactoryBeanNotInitializedException in this case anymore.
	 * FactoryBean implementations are encouraged to throw
	 * FactoryBeanNotInitializedException themselves now, as appropriate.
	 * @return an instance of the bean (can be {@code null})
	 * @throws Exception in case of creation errors
	 * @see FactoryBeanNotInitializedException
	 * T getObject(); 返回由 FactoryBean 创建的 bean实例，如果是 isSingleton()返回 true,则该实例会放到 Spring 容器中单个实例缓存池中
	 */
	@Nullable
	T getObject() throws Exception;

	/**
	 * Return the type of object that this FactoryBean creates,
	 * or {@code null} if not known in advance.
	 * <p>This allows one to check for specific types of beans without
	 * instantiating objects, for example on autowiring.
	 * <p>In the case of implementations that are creating a singleton object,
	 * this method should try to avoid singleton creation as far as possible;
	 * it should rather estimate the type in advance.
	 * For prototypes, returning a meaningful type here is advisable too.
	 * <p>This method can be called <i>before</i> this FactoryBean has
	 * been fully initialized. It must not rely on state created during
	 * initialization; of course, it can still use such state if available.
	 * <p><b>NOTE:</b> Autowiring will simply ignore FactoryBeans that return
	 * {@code null} here. Therefore it is highly recommended to implement
	 * this method properly, using the current state of the FactoryBean.
	 * @return the type of object that this FactoryBean creates,
	 * or {@code null} if not known at the time of the call
	 * @see ListableBeanFactory#getBeansOfType
	 * Class<T> getObjectType(); 返回 factoryBean 创建的 bean 的类型
	 */
	@Nullable
	Class<?> getObjectType();

	/**
	 * Is the object managed by this factory a singleton? That is,
	 * will {@link #getObject()} always return the same object
	 * (a reference that can be cached)?
	 * <p><b>NOTE:</b> If a FactoryBean indicates to hold a singleton object,
	 * the object returned from {@code getObject()} might get cached
	 * by the owning BeanFactory. Hence, do not return {@code true}
	 * unless the FactoryBean always exposes the same reference.
	 * <p>The singleton status of the FactoryBean itself will generally
	 * be provided by the owning BeanFactory; usually, it has to be
	 * defined as singleton there.
	 * <p><b>NOTE:</b> This method returning {@code false} does not
	 * necessarily indicate that returned objects are independent instances.
	 * An implementation of the extended {@link SmartFactoryBean} interface
	 * may explicitly indicate independent instances through its
	 * {@link SmartFactoryBean#isPrototype()} method. Plain {@link FactoryBean}
	 * implementations which do not implement this extended interface are
	 * simply assumed to always return independent instances if the
	 * {@code isSingleton()} implementation returns {@code false}.
	 * <p>The default implementation returns {@code true}, since a
	 * {@code FactoryBean} typically manages a singleton instance.
	 * @return whether the exposed object is a singleton
	 * @see #getObject()
	 * @see SmartFactoryBean#isPrototype()
	 * boolean isSingleton(); 返回由 FactoryBean 创建的 bean 实例的作用域是 singleton 还是 prototype
	 */
	default boolean isSingleton() {
		return true;
	}

}
