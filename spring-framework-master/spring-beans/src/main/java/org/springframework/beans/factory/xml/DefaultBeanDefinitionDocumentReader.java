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

package org.springframework.beans.factory.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Default implementation of the {@link BeanDefinitionDocumentReader} interface that
 * reads bean definitions according to the "spring-beans" DTD and XSD format
 * (Spring's default XML bean definition format).
 * <p>
 * <p>The structure, elements, and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). {@code <beans>} does not need to be the root
 * element of the XML document: this class will parse all bean definition elements
 * in the XML file, regardless of the actual root element.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 18.12.2003
 * <p>
 * <p>
 * 通过实现接口BeanDefinitionDocumentReader的DefaultBeanDefinitionDocumentReader类对
 * Document进行解析，并使用BeanDefinitionParserDelegate对Element解析
 *
 *
 *
 *
 *
 *
 */
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

	public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;

	public static final String NESTED_BEANS_ELEMENT = "beans";

	public static final String ALIAS_ELEMENT = "alias";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String ALIAS_ATTRIBUTE = "alias";

	public static final String IMPORT_ELEMENT = "import";

	public static final String RESOURCE_ATTRIBUTE = "resource";

	public static final String PROFILE_ATTRIBUTE = "profile";


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private XmlReaderContext readerContext;

	@Nullable
	private BeanDefinitionParserDelegate delegate;


	/**
	 * This implementation parses bean definitions according to the "spring-beans" XSD
	 * (or DTD, historically).
	 * <p>Opens a DOM Document; then initializes the default settings
	 * specified at the {@code <beans/>} level; then parses the contained bean definitions.
	 */
	@Override
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		this.readerContext = readerContext;
		logger.debug("Loading bean definitions");
		Element root = doc.getDocumentElement();
		doRegisterBeanDefinitions(root);
	}

	/**
	 * Return the descriptor for the XML resource that this parser works on.
	 */
	protected final XmlReaderContext getReaderContext() {
		Assert.state(this.readerContext != null, "No XmlReaderContext available");
		return this.readerContext;
	}

	/**
	 * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor}
	 * to pull the source metadata from the supplied {@link Element}.
	 */
	@Nullable
	protected Object extractSource(Element ele) {
		return getReaderContext().extractSource(ele);
	}


	/**
	 * Register each bean definition within the given root {@code <beans/>} element.
	 */
	protected void doRegisterBeanDefinitions(Element root) {
		// Any nested <beans> elements will cause recursion in this method. In
		// order to propagate and preserve <beans> default-* attributes correctly,
		// keep track of the current (parent) delegate, which may be null. Create
		// the new (child) delegate with a reference to the parent for fallback purposes,
		// then ultimately reset this.delegate back to its original (parent) reference.
		// this behavior emulates a stack of delegates without actually necessitating one.
		BeanDefinitionParserDelegate parent = this.delegate;
		this.delegate = createDelegate(getReaderContext(), root, parent);

		if (this.delegate.isDefaultNamespace(root)) {
			//处理 profile 属性，
			//有了这个特性我们就可以同时在配置文件中部署两套配置来适用于生产环境和开发环境，
			//这样可以方便的进行切换，部署环境，最常用的就是更换不同数据库，
			//了解了 profile 的使用，再来看代码会清晰多了，首先程序会获取beans 节点是否定义了 profile属性
			//如果定义了 profile 则会到环境变量中去寻找，所以这里首先断言，environment 不可能为空，因为 profile
			// 是可以同时指定多个的，需要程序对其拆分，并解析每个 profile 都是符合环境定义的，不定义则不会费性能去解析
			String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
			if (StringUtils.hasText(profileSpec)) {
				String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
						profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
				if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
					if (logger.isInfoEnabled()) {
						logger.info("Skipped XML bean definition file due to specified profiles [" + profileSpec +
								"] not matching: " + getReaderContext().getResource());
					}
					return;
				}
			}
		}

		//解析前处理，留给子类实现
		preProcessXml(root);
		parseBeanDefinitions(root, this.delegate);
		//解析后处理，留给子类实现，就像面向对象设计方法常用的一句话，一个类要么是面向继承设计的，要么就是
		//final修饰的，在 DefaultBeanDefinitionDocumentReader 中并没有用 final 修饰的，所以它就是面向继承设计的
		//这两个类方法正是为子类设计的，如果读者有了解过设计模式，可以很快的反映这是模版方法模式，如果继承自己 DefaultBeanDefinitionDocumentReader 的子类需要在
		//解析前后做一些处理的话，那么需要重写这两个方法就可以了
		postProcessXml(root);

		this.delegate = parent;
	}

	protected BeanDefinitionParserDelegate createDelegate(
			XmlReaderContext readerContext, Element root, @Nullable BeanDefinitionParserDelegate parentDelegate) {

		BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
		delegate.initDefaults(root, parentDelegate);
		return delegate;
	}

	/**
	 * Parse the elements at the root level in the document:
	 * "import", "alias", "bean".
	 *
	 * @param root the DOM root element of the document
	 *             使用Spring的Bean规则从Document的根元素开始进行Bean定义的Document对象
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		//Bean定义的Document对象使用了Spring默认的XML命名空间，对 Beans 进行处理
		//这个代码看起来逻辑还是蛮清晰的，因为在 Spring 的 XML 配置里有两大类 Bean 声明，一个是默认的如：
		// <bean id="test" class="test.TestBean">
		//另一类是自定义的，如
		//<tx:annotation-driven/>
		// 而两个方式的读取及解析差别还是非常大的，如果采用 Spring 默认的配置，Spring 那当然知道该怎样做
		// 但是如果是自己定义的，那么就需要用户实现一些接口及配置了，对于根节点或者子节点如果是默认的命名空间
		// 的话则采用 parseDefaultElement 方法进行解析，否则使用 delegate.parsecustomElement 方法对自定义命名空间进行
		//解析，而判断是否默认命名空间还是自定义命名空间的墨汁其实是使用 node.getNamespaceUri()获取命名空间，并与 Spring
		//中的固定的命名空间 http://www.Springframework.org/schemea/beans进行对比，如果一致则认为是默认的，否则就认为是
		//自定义的，而对于默认的标签解析与自定义标签解析我们将会在下一章进行讨论
		// 在前面的章节中，我们提到了 Spring 中存在默认的标签与自定义的标签两种，而上一章节中，我们分析了 Spring 中对默认的
		// 标签的解析过程，相信大家一定已经有所感悟，那么现在将开始新的里程，分析 Spring 中自定义的标签的加载过程，同样，我们
		//还是先回顾一下，当完成从配置文件到 Document 的转换并提取对应的 root后，将开始所有的元素解析，而这一过程中便开始了
		// 默认的标签与自定义标签两中格式的区分，函数如下
		if (delegate.isDefaultNamespace(root)) {
			//获取Bean定义的Document对象根元素的所有子节点
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				//获得Document节点是XML元素节点
				if (node instanceof Element) {
					Element ele = (Element) node;
					//Bean定义的Document的元素节点使用的是Spring默认的XML命名空间
					if (delegate.isDefaultNamespace(ele)) {
						//使用Spring的Bean规则解析元素节点，对 Bean 进行处理
						parseDefaultElement(ele, delegate);
					} else {
						//没有使用Spring默认的XML命名空间，则使用用户自定义的解//析规则解析元素节点
						//对 Bean 的处理
						delegate.parseCustomElement(ele);
					}
				}
			}
		} else {
			//Document的根节点没有使用Spring默认的命名空间，则使用用户自定义的
			//解析规则解析Document根节点
			delegate.parseCustomElement(root);
		}
	}

	//使用Spring的Bean规则解析Document元素节点
	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
		//如果元素节点是<Import>导入元素，进行导入解析
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			importBeanDefinitionResource(ele);
		}
		//如果元素节点是<Alias>别名元素，进行别名解析
		else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			processAliasRegistration(ele);
		}
		//元素节点既不是导入元素，也不是别名元素，即普通的<Bean>元素，
		//按照Spring的Bean规则解析元素
		else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			processBeanDefinition(ele, delegate);
		} else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			// recurse
			doRegisterBeanDefinitions(ele);
		}
	}

	/**
	 * Parse an "import" element and load the bean definitions
	 * from the given resource into the bean factory.
	 * 解析<Import>导入元素，从给定的导入路径加载Bean定义资源到Spring IoC容器中
	 *
	 * 对于 Spring 配置文件的编写，我想，经历过庞大的项目的人，都会有一种恐惧的心理，太多的配置文件了
	 * ，不过，分模块是大多数的人能想到的，但是，怎么分模块，那仁者见仁，智者见智了，使用 import 是个好办法
	 * ,例如我们可以 这样的使用 Spring 配置文件
	 *
	 * <beans>
	 *     <import resource="customerContext.xml"></import>
	 *     <import resource="systemContext.xml"></import>
	 * </beans>
	 * ApplicationContext文件中使用 import 的方式来
	 *
	 * 下面的代码不难理解，相信配合注释会很好的理解，我们总结一下大致的流程便于读者更好的梳理，在解析<import 标签时，Spring 进行解析的步骤大致如下
	 *
	 * 获取 resource 属性所表示的路径
	 * 解析路径中的系统属性，格式如 "${user.dir}"
	 * 判定 location 是绝对路径还是相对路径。
	 * 如果是绝对路径则递归调用 bean 的解析过程，进另一次的解析
	 * 如果是相对路径，则计算出绝对路径，并进行解析。
	 * 通知监听器，解析完成
	 */
	protected void importBeanDefinitionResource(Element ele) {
		//获取 resource 属性
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		if (!StringUtils.hasText(location)) {
			//如果不存在 resource 属性则不做任何处理
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}
		//解析系统属性，格式如 ： ${user.dir}
		// Resolve system properties: e.g. "${user.dir}"
		location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<>(4);

		// Discover whether the location is an absolute or relative URI
		// 判定 location 是决定 URI 还是相对 URI
		boolean absoluteLocation = false;
		try {
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		} catch (URISyntaxException ex) {
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}

		// Absolute or relative?
		//如果是绝对 URI,则直接根据地址加载对应的配置文件
		if (absoluteLocation) {
			try {
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		} else {
			// No URL -> considering resource location as relative to the current file.
			//如果是绝对地址，则根据相对地址计算出绝对地址
			try {
				int importCount;
				// Resource 存在多个实现类，如果 VFsResource，FileSystemResource等
				// 而每个 resource 的 createRelative 方式实现都不一样，所以，这里先使用子类的方法尝试解析
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				if (relativeResource.exists()) {
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					actualResources.add(relativeResource);
				} else {
					//如果解析不成功，则使用默认的解析器 ResourcePatternResolver 进行解析
					String baseLocation = getReaderContext().getResource().getURL().toString();
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			} catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to import bean definitions from relative location [" + location + "]",
						ele, ex);
			}
		}
		//解析后进行监听器的激活处理
		Resource[] actResArray = actualResources.toArray(new Resource[actualResources.size()]);
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}

	/**
	 * Process the given alias element, registering the alias with the registry.
	 * 通过上面较长的篇幅我们终于分析完成了默认标签中对 bean 标签的处理，那么我们之前提到过，对配置文件的解析包含了 import 标签
	 * ，alias 标签，bean 标签，beans 标签处理，现在我们已经完成了最重要的也是最核心的功能，其他的解析步骤也都是围绕着第三个解析而进行
	 * 的，在分析第3个解析步骤后，再回过头来看 aliasr的解析
	 *
	 * 在对 bean 进行定义的时，除了使用 id属性业指定名称之外 ，为了提供多个名称，可以使用 alias标签来指定，而所有的这些名称都指向
	 * 同一个 bean ,在某些情况下提供了别名非常的有用，比如为了让应用每一个组件能更加容易地对公共组件进行引用
	 *
	 *
	 */
	protected void processAliasRegistration(Element ele) {
		// 获取 beanName
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		// 获取 alias
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		boolean valid = true;
		if (!StringUtils.hasText(name)) {
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		if (!StringUtils.hasText(alias)) {
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}
		if (valid) {
			try {
				//注册 alias
				getReaderContext().getRegistry().registerAlias(name, alias);
			} catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			// 别名注册后通知监听器作相应处理，可以发现，跟之前讲过的 bean 中的 alias解析大同小异，都是将别名与
			// beanName 组成对应注册至 registry 中，这里不再赘述。

			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * Process the given bean element, parsing the bean definition
	 * and registering it with the registry.
	 * 在4种标签的解析中，对 bean 标签的解析最为复杂也最为重要，所以我们从此标签开始
	 * 深入分析，如果能理解此标签的解析过程，其他的标签的解析自然会迎刃而解，首先我们进入函数 processBeanDefinition(ele,delegate)
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		//首先委托BeanDefinitionParserDelegate类的parseBeanDefinitionElement方法进行解析元素，
		//返回BeanDefinitionHolder类型的实例，bdHolder，经过这个方法后，bdHolder 实例已经包含我
		//们的配置文件中配置各种属性，例如 class,name,id,alias 之类的属性
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		if (bdHolder != null) {
			// 当返回的 bdHolder 不为空的情况下，若存在默认标签的子节点下再有自定义属性，还需要再次对自定义标签进行解析。
			// 解析完成后，需要对解析后的 bdHolder 进行注册，同样，注册操作委托给了 BeanDefinitionReaderUtils 的 registerBeanDefinition 方法
			// 最后发出响应事件，通知想关的监听器，这个bean 已经加载完成了
			// 我们重语义上分析，如果需要和话就对 beanDefinition 进行修饰，那这句代码到底是什么功能呢，其实这句代码适用于这样的场景
			// <bean id="test" class="test.MyClass">
			// 			<mybean:user username="aa">
			// </bean>
			// 当 Spring中的 bean 使用的是默认的标签的配置，但是其中的子元素却使用了自定义的配置时，当这句代码便会起作用了，可能会有人会疑问
			// 可能有人会有疑问，之前讲过，对 Bean 的解析分为两种类型，一种是默认的解析类型，另一种上是自定义的解析类型，这不正是自定义类型的解析
			// 吗？为什么会在默认的解析中单独的中添加一个点方法处理呢，确实，这个问题让人迷惑，但是，不知道聪明的读者是否会发现，这个自定义类型并不是
			// 以 Bean 的形式出现的呢，我们之前讲过的两种类型的不同处理只是针对Bean 的，这个自定义类型其实是属性的，好了，我们继续分析这段代码的逻辑
			//
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				// Register the final decorated instance.
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			// Send registration event.
			// 通知监听器解析及注册完成
			//通过getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));完成此工作，这里
			//的实现只为扩展，当程序开发人员需要对注册 BeanDefinition 事件监听时可以通过注册监听器的方式并将处理逻辑写入监听器中
			// 目前在 Spring 中并没有对此事做出任何逻辑处理
			//
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}


	/**
	 * Allow the XML to be extensible by processing any custom element types first,
	 * before we start to process the bean definitions. This method is a natural
	 * extension point for any other custom pre-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 *
	 * @see #getReaderContext()
	 */
	protected void preProcessXml(Element root) {
	}

	/**
	 * Allow the XML to be extensible by processing any custom element types last,
	 * after we finished processing the bean definitions. This method is a natural
	 * extension point for any other custom post-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 *
	 * @see #getReaderContext()
	 */
	protected void postProcessXml(Element root) {
	}

}
