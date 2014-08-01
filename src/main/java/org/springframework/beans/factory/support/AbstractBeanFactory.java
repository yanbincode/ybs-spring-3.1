package org.springframework.beans.factory.support;

import java.beans.PropertyEditor;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.spi.ObjectFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 抽象的beanFactory。是beanFactory的抽象实现类
 * 
 * @author yanbin
 * 
 */
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {

	/** 父类bean factory ： 供继承使用 */
	private BeanFactory parentBeanFactory;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private ClassLoader tempClassLoader;

	/** 是否元数据的缓存 */
	private boolean cacheBeanMetadata = true;

	/** bean 表达式解析器 */
	private BeanExpressionResolver beanExpressionResolver;

	/** 转换服务 */
	private ConversionService conversionService;

	/** 客户 PropertyEditorRegistrars 提供给this factory 的 bean */
	private final Set<PropertyEditorRegistrar> propertyEditorRegistrars = new LinkedHashSet<PropertyEditorRegistrar>(4);

	/** 一个客户 类型转换器，重写默认的 PropertyEditor 进程 */
	private TypeConverter typeConverter;

	/** 客户PropertyEditors 提供给this factory 的 bean */
	private final Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<Class<?>, Class<? extends PropertyEditor>>(
			4);

	/** StringValueResolver */
	private final List<StringValueResolver> embeddedValueResolvers = new LinkedList<StringValueResolver>();

	/** BeanPostProcessors 提供在创建bean */
	private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<BeanPostProcessor>();

	/** 标记 InstantiationAwareBeanPostProcessors 是否已经被注册 */
	private boolean hasInstantiationAwareBeanPostProcessors;

	/** 标记 DestructionAwareBeanPostProcessors 是否已经被注册 */
	private boolean hasDestructionAwareBeanPostProcessors;

	/** 定义scopes */
	private final Map<String, Scope> scopes = new HashMap<String, Scope>();

	/** 提供一个 安全的上下文使用， 当运行在一个SecurityManager */
	private SecurityContextProvider securityContextProvider;

	/** RootBeanDefinition */
	private final Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<String, RootBeanDefinition>();

	/** 存储已经创建的bean 的bean name */
	private final Set<String> alreadyCreated = Collections.synchronizedSet(new HashSet<String>());

	/** 存储正在创建的bean 的bean name */
	private final ThreadLocal<Object> prototypesCurrentlyInCreation = new NamedThreadLocal<Object>(
			"Prototype beans currently in creation");

	public AbstractBeanFactory() {
	}

	public AbstractBeanFactory(BeanFactory parentBeanFactory) {
		this.parentBeanFactory = parentBeanFactory;
	}

	// ---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	// ---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		return doGetBean(name, null, null, false);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return doGetBean(name, requiredType, null, false);
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		return doGetBean(name, null, args, false);
	}

	/**
	 * 返回一个bean ，是共享的 ，独立的或指定的
	 * 
	 * @param name
	 * @param requiredType
	 * @param args
	 * @return
	 * @throws BeansException
	 */
	public <T> T getBean(String name, Class<T> requiredType, Object... args) throws BeansException {
		return doGetBean(name, requiredType, args, false);
	}

	/**
	 * 执行do getbean的操作
	 * 
	 * @param name
	 *            bean name
	 * @param requiredType
	 *            必须的类型
	 * @param args
	 *            TODO：
	 * @param typeCheckOnly
	 *            标记这个实例获得是为了类型检查的，并不是实际被使用的
	 * @return
	 * @throws BeansException
	 */
	protected <T> T doGetBean(final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly)
			throws BeansException {
		final String beanName = transformedBeanName(name);
		Object bean;

		Object sharedInstance = getSingleton(beanName);
		// 获取的实例不为空
		if (sharedInstance != null && args == null) {
			// 打日志
			if (logger.isDebugEnabled()) {
				// 判断是否是单例当前正在创建的bean
				if (isSingletonCurrentlyInCreation(beanName)) {
					logger.debug("Returning eagerly cached instance of singleton bean '" + beanName
							+ "' that is not fully initialized yet - a consequence of a circular reference");
				} else {
					logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
				}
			}
			bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
		}
		// 非单例的实例，则需要做相关的验证处理，新创建一个实例
		else {
			if (isPrototypeCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(beanName);
			}

			// 判断 bean definition 是否在factory中
			BeanFactory parentBeanFactory = getParentBeanFactory();
			// factory 不为空，且 factory中没有beanName
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// 检查parent
				String nameToLookup = originalBeanName(name);
				if (args != null) {
					return (T) parentBeanFactory.getBean(nameToLookup, args);
				} else {
					// 没有参数，调用标准的getBean()方法
					return parentBeanFactory.getBean(nameToLookup, requiredType);
				}
			}

			// 判断这个bean 是实际在使用的
			if (!typeCheckOnly) {
				// 标记bean已经被创建了
				markBeanAsCreated(beanName);
			}

			// merged BeanDefinition
			final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
			// 检查 merged 的 BeanDefinition
			checkMergedBeanDefinition(mbd, beanName, args);

			// 保证初始化 依赖于当前bean的 bean
			String[] dependsOn = mbd.getDependsOn();
			if (dependsOn != null) {
				// 循环处理 依赖的bean
				for (String dependsOnBean : dependsOn) {
					// 递归调用
					getBean(dependsOnBean);
					registerDependentBean(dependsOnBean, beanName);
				}
			}

			// 创建实例
			// 如果是单例的
			if (mbd.isSingleton()) {
				sharedInstance = getSingleton(beanName, new ObjectFactory<Object>() {
					public Object getObject() throws BeansException {
						try {
							return createBean(beanName, mbd, args);
						} catch (BeansException ex) {
							// 报错了则要销毁这个单例的bean name
							destroySingleton(beanName);
							throw ex;
						}
					}
				});
				// 创建完之后 获取 bean的实例
				bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
			}
			// 如果不是单例的，则创建一个新实例
			else if (mbd.isPrototype()) {
				Object prototypeInstance = null;
				try {
					// 创建之前做的事情
					beforePrototypeCreation(beanName);
					// 创建实例
					prototypeInstance = createBean(beanName, mbd, args);
				} finally {
					// 创建实例完成之后做的事情
					afterPrototypeCreation(beanName);
				}
				// 获取bean
				bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
			}
			// 不是单例也不是模板
			else {
				// TODO: 从scope里面做事情
				String scopeName = mbd.getScope();
				final Scope scope = this.scopes.get(scopeName);
				if (scope == null) {
					throw new IllegalStateException("No Scope registered for scope '" + scopeName + "'");
				}
				try {
					Object scopedInstance = scope.get(beanName, new ObjectFactory<Object>() {
						public Object getObject() throws BeansException {
							// 创建之前
							beforePrototypeCreation(beanName);
							try {
								// 创建bean
								return createBean(beanName, mbd, args);
							} finally {
								// 创建之后
								afterPrototypeCreation(beanName);
							}
						}
					});
					// 创建完成之后，获取bean
					bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
				} catch (IllegalStateException ex) {
					throw new BeanCreationException(
							beanName,
							"Scope '"
									+ scopeName
									+ "' is not active for the current thread; "
									+ "consider defining a scoped proxy for this bean if you intend to refer to it from a singleton",
							ex);
				}
			}
		}
		// 检查所需类型的实际的bean实例的类型是否相匹配。
		// isAssignableFrom(): 判定此 Class 对象所表示的类或接口与指定的 Class
		// 参数所表示的类或接口是否相同，或是否是其超类或超接口。
		if (requiredType != null && bean != null && !requiredType.isAssignableFrom(bean.getClass())) {
			// 类型不匹配
			try {
				// 转换类型
				return getTypeConverter().convertIfNecessary(bean, requiredType);
			} catch (TypeMismatchException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug(
							"Failed to convert bean '" + name + "' to required type ["
									+ ClassUtils.getQualifiedName(requiredType) + "]", ex);
				}
				throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
			}
		}
		// 最后返回bean
		return (T) bean;
	}

	@Override
	public boolean containsBean(String name) {
		String beanName = transformedBeanName(name);
		// 判断单例中是否包含，或BeanDefinition中包含
		if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
			// 不能废弃的，且是factoryBean中
			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name));
		}
		// 没找到则检查parent factory中是否存在
		BeanFactory parentBeanFactory = getParentBeanFactory();
		return (parentBeanFactory != null && parentBeanFactory.containsBean(originalBeanName(name)));
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			if (beanInstance instanceof FactoryBean) {
				return (BeanFactoryUtils.isFactoryDereference(name) || ((FactoryBean<?>) beanInstance).isSingleton());
			} else {
				return !BeanFactoryUtils.isFactoryDereference(name);
			}
		} else if (containsSingleton(beanName)) {
			return true;
		} else {
			// singleton 没有找到到，则检查 bean Definition
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// 在beanfactory中没有定义bean ，委派到parent中去判断
				return parentBeanFactory.isSingleton(originalBeanName(name));
			}

			// 如果没有parentBeanFactory 或者 在当前factory中包含 bean 。
			RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

			// mbd 在beanFactory中 如果不是废弃的，则返回已经创建的bean的状态
			if (mbd.isSingleton()) {
				if (isFactoryBean(beanName, mbd)) {
					// 如果是废弃的直接返回true
					if (BeanFactoryUtils.isFactoryDereference(name)) {
						return true;
					}
					FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
					return factoryBean.isSingleton();
				} else {
					return !BeanFactoryUtils.isFactoryDereference(name);
				}
			} else {
				return false;
			}
		}
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// 在beanfactory中没有定义bean ，委派到parent中去判断
			return parentBeanFactory.isPrototype(originalBeanName(name));
		}
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		if (mbd.isPrototype()) {
			// mbd 在beanFactory中 如果不是废弃的，则返回已经创建的bean的状态
			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName, mbd));
		} else {
			// 是Singleton的或者 作用域的，不是 prototype的 。
			// 即便这样FactoryBean也许会仍然产生一个prototype object
			if (BeanFactoryUtils.isFactoryDereference(name)) {
				// 废弃的则返回false
				return false;
			}
			if (isFactoryBean(beanName, mbd)) {
				final FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
				//
				if (System.getSecurityManager() != null) {
					return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
						public Boolean run() {
							return ((factoryBean instanceof SmartFactoryBean && ((SmartFactoryBean<?>) factoryBean)
									.isPrototype()) || !factoryBean.isSingleton());
						}
					}, getAccessControlContext());
				} else {
					return ((factoryBean instanceof SmartFactoryBean && ((SmartFactoryBean<?>) factoryBean)
							.isPrototype()) || !factoryBean.isSingleton());
				}
			} else {
				return false;
			}
		}
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> targetType) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);
		Class<?> typeToMatch = (targetType != null ? targetType : Object.class);

		// 检查手动已注册的单例
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			if (beanInstance instanceof FactoryBean) {
				if (!BeanFactoryUtils.isFactoryDereference(name)) {
					// 获取FactoryBean的类型
					Class<?> type = getTypeForFactoryBean((FactoryBean<?>) beanInstance);
					// 判断类型是否符合
					return (type != null && ClassUtils.isAssignable(typeToMatch, type));
				} else {
					// 废弃的实例
					return ClassUtils.isAssignableValue(typeToMatch, beanInstance);
				}
			} else {
				// 实例不属于FactoryBean的子类
				return !BeanFactoryUtils.isFactoryDereference(name)
						&& ClassUtils.isAssignableValue(typeToMatch, beanInstance);
			}
		} else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			// 如果不存在单例实例，检查是否有beanname 的key在缓存中。如果有，单没有实例，则返回fales
			return false;
		} else {
			// 不存在单例中--> 检查bean definition
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// 在beanfactory中没有定义bean ，委派到parent中去判断
				return parentBeanFactory.isTypeMatch(originalBeanName(name), targetType);
			}

			// 检索对应的 bean definition
			RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

			// 如果检查任何装饰的bean definition，：我们假定它会比较容易确定装饰bean的类型与代理的类型。
			BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
			if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
				// 获取merged bean efinition
				RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
				// TODO:
				Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd, FactoryBean.class, typeToMatch);
				if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
					return typeToMatch.isAssignableFrom(targetClass);
				}
			}

			Class<?> beanClass = predictBeanType(beanName, mbd, FactoryBean.class, typeToMatch);
			if (beanClass == null) {
				return false;
			}

			// 检查这个 bean class 是否是我们正在处理的FactoryBean
			if (FactoryBean.class.isAssignableFrom(beanClass)) {
				if (!BeanFactoryUtils.isFactoryDereference(name)) {
					// 如果它是一个FactoryBean，我们想看看它的创造，而不是工厂类。
					Class<?> type = getTypeForFactoryBean(beanName, mbd);
					return (type != null && typeToMatch.isAssignableFrom(type));
				} else {
					return typeToMatch.isAssignableFrom(beanClass);
				}
			} else {
				return !BeanFactoryUtils.isFactoryDereference(name) && typeToMatch.isAssignableFrom(beanClass);
			}
		}
	}

	@Override
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		// 检查手动已注册的单例
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			if (beanInstance instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
				// 获取FactoryBean的类型
				return getTypeForFactoryBean((FactoryBean<?>) beanInstance);
			} else {
				// 不属于FactoryBean 直接返回类型
				return beanInstance.getClass();
			}
		} else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			// 实例为空
			return null;

		} else {
			// 不存在单例中--> 检查bean definition
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// 在beanfactory中没有定义bean ，委派到parent中去判断
				return parentBeanFactory.getType(originalBeanName(name));
			}

			// 没有父bean parent
			RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

			// 如果检查任何装饰的bean definition，：我们假定它会比较容易确定装饰bean的类型与代理的类型。
			BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
			if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
				RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
				Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd);
				if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
					return targetClass;
				}
			}

			Class<?> beanClass = predictBeanType(beanName, mbd);

			// 检查这个 bean class 是否是我们正在处理的FactoryBean
			if (beanClass != null && FactoryBean.class.isAssignableFrom(beanClass)) {
				if (!BeanFactoryUtils.isFactoryDereference(name)) {
					// 返回bean类型
					return getTypeForFactoryBean(beanName, mbd);
				} else {
					return beanClass;
				}
			} else {
				return (!BeanFactoryUtils.isFactoryDereference(name) ? beanClass : null);
			}
		}
	}

	@Override
	public String[] getAliases(String name) {
		String beanName = transformedBeanName(name);
		List<String> aliases = new ArrayList<String>();
		boolean factoryPrefix = name.startsWith(FACTORY_BEAN_PREFIX);
		String fullBeanName = beanName;
		if (factoryPrefix) {
			// 补齐beanName
			fullBeanName = FACTORY_BEAN_PREFIX + beanName;
		}
		if (!fullBeanName.equals(name)) {
			aliases.add(fullBeanName);
		}

		// 关键：调用父类的getAliases()的方法
		String[] retrievedAliases = super.getAliases(beanName);
		// 遍历所有的别名数组
		for (String retrievedAlias : retrievedAliases) {
			// 加上&符号
			String alias = (factoryPrefix ? FACTORY_BEAN_PREFIX : "") + retrievedAlias;
			// 判断alias和name不同
			if (!alias.equals(name)) {
				aliases.add(alias);
			}
		}
		// 单例缓存中不存在beanName的key 且 BeanDefinition也不存在
		if (!containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			// 如果parent存在，从parentFactory中取
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null) {
				aliases.addAll(Arrays.asList(parentBeanFactory.getAliases(fullBeanName)));
			}
		}
		// 将array 转换成 数组
		return StringUtils.toStringArray(aliases);
	}

	// ---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	// ---------------------------------------------------------------------

	@Override
	public BeanFactory getParentBeanFactory() {
		return this.parentBeanFactory;
	}

	@Override
	public boolean containsLocalBean(String name) {
		String beanName = transformedBeanName(name);
		// 是否包含bean ：判断singleton中是否包含 或者 BeanDefinition中包含。
		// 且不是废弃的bean 或者是FactoryBean
		return ((containsSingleton(beanName) || containsBeanDefinition(beanName)) && (!BeanFactoryUtils
				.isFactoryDereference(name) || isFactoryBean(beanName)));
	}

	// ---------------------------------------------------------------------
	// Implementation of ConfigurableBeanFactory interface
	// ---------------------------------------------------------------------

	@Override
	public void setParentBeanFactory(BeanFactory parentBeanFactory) {
		if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
			throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
		}
		this.parentBeanFactory = parentBeanFactory;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = (beanClassLoader != null ? beanClassLoader : ClassUtils.getDefaultClassLoader());
	}

	@Override
	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	@Override
	public void setTempClassLoader(ClassLoader tempClassLoader) {
		this.tempClassLoader = tempClassLoader;
	}

	@Override
	public ClassLoader getTempClassLoader() {
		return this.tempClassLoader;
	}

	@Override
	public void setCacheBeanMetadata(boolean cacheBeanMetadata) {
		this.cacheBeanMetadata = cacheBeanMetadata;
	}

	@Override
	public boolean isCacheBeanMetadata() {
		return this.cacheBeanMetadata;
	}

	@Override
	public void setBeanExpressionResolver(BeanExpressionResolver resolver) {
		this.beanExpressionResolver = resolver;
	}

	@Override
	public BeanExpressionResolver getBeanExpressionResolver() {
		return this.beanExpressionResolver;
	}

	@Override
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	@Override
	public void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar) {
		Assert.notNull(registrar, "PropertyEditorRegistrar must not be null");
		this.propertyEditorRegistrars.add(registrar);
	}

	public Set<PropertyEditorRegistrar> getPropertyEditorRegistrars() {
		return this.propertyEditorRegistrars;
	}

	@Override
	public void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass) {
		Assert.notNull(requiredType, "Required type must not be null");
		Assert.isAssignable(PropertyEditor.class, propertyEditorClass);
		this.customEditors.put(requiredType, propertyEditorClass);
	}

	@Override
	public void copyRegisteredEditorsTo(PropertyEditorRegistry registry) {
		registerCustomEditors(registry);
	}

	public Map<Class<?>, Class<? extends PropertyEditor>> getCustomEditors() {
		return this.customEditors;
	}

	@Override
	public void setTypeConverter(TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
	}

	protected TypeConverter getCustomTypeConverter() {
		return this.typeConverter;
	}

	@Override
	public TypeConverter getTypeConverter() {
		TypeConverter customConverter = getCustomTypeConverter();
		if (customConverter != null) {
			return customConverter;
		} else {
			// 构建一个默认的TypeConverter，注册custom editors
			SimpleTypeConverter typeConverter = new SimpleTypeConverter();
			typeConverter.setConversionService(getConversionService());
			registerCustomEditors(typeConverter);
			return typeConverter;
		}
	}

	@Override
	public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		this.embeddedValueResolvers.add(valueResolver);
	}

	@Override
	public String resolveEmbeddedValue(String value) {
		// 参数赋值，以免改变原参数的值
		String result = value;
		for (StringValueResolver resolver : this.embeddedValueResolvers) {
			result = resolver.resolveStringValue(result);
		}
		return result;
	}

	@Override
	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
		// 先移除再增加
		this.beanPostProcessors.remove(beanPostProcessor);
		this.beanPostProcessors.add(beanPostProcessor);
		// 是实例化形式的BeanPostProcesso
		if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
			this.hasInstantiationAwareBeanPostProcessors = true;
		}
		// 是销毁形式的BeanPostProcesso
		if (beanPostProcessor instanceof DestructionAwareBeanPostProcessor) {
			this.hasDestructionAwareBeanPostProcessors = true;
		}
	}

	@Override
	public int getBeanPostProcessorCount() {
		return this.beanPostProcessors.size();
	}

	/**
	 * 获取所有的BeanPostProcessors
	 * 
	 * @return
	 */
	public List<BeanPostProcessor> getBeanPostProcessors() {
		return this.beanPostProcessors;
	}

	/**
	 * 返回是否是实例化形式下的BeanPostProcessors
	 * 
	 * @return
	 */
	protected boolean hasInstantiationAwareBeanPostProcessors() {
		return this.hasInstantiationAwareBeanPostProcessors;
	}

	/**
	 * 返回判断是否是销毁形式下的BeanPostProcessors
	 * 
	 * @return
	 */
	protected boolean hasDestructionAwareBeanPostProcessors() {
		return this.hasDestructionAwareBeanPostProcessors;
	}

	@Override
	public void registerScope(String scopeName, Scope scope) {
		Assert.notNull(scopeName, "Scope identifier must not be null");
		Assert.notNull(scope, "Scope must not be null");
		// 注册的scope如果是spring默认的两种，则无法再注册
		if (SCOPE_SINGLETON.equals(scopeName) || SCOPE_PROTOTYPE.equals(scopeName)) {
			throw new IllegalArgumentException("Cannot replace existing scopes 'singleton' and 'prototype'");
		}
		this.scopes.put(scopeName, scope);
	}

	@Override
	public String[] getRegisteredScopeNames() {
		return StringUtils.toStringArray(this.scopes.keySet());
	}

	@Override
	public Scope getRegisteredScope(String scopeName) {
		Assert.notNull(scopeName, "Scope identifier must not be null");
		return this.scopes.get(scopeName);
	}

	public void setSecurityContextProvider(SecurityContextProvider securityProvider) {
		this.securityContextProvider = securityProvider;
	}

	@Override
	public AccessControlContext getAccessControlContext() {
		// 重写父类方法，增加从securityContextProvider中取的可能
		return (this.securityContextProvider != null ? this.securityContextProvider.getAccessControlContext()
				: AccessController.getContext());
	}

	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		Assert.notNull(otherFactory, "BeanFactory must not be null");
		// 复制属性
		setBeanClassLoader(otherFactory.getBeanClassLoader());
		setCacheBeanMetadata(otherFactory.isCacheBeanMetadata());
		setBeanExpressionResolver(otherFactory.getBeanExpressionResolver());
		// 如果otherFactory 属于AbstractBeanFactory类型，则复制AbstractBeanFactory下的属性值
		if (otherFactory instanceof AbstractBeanFactory) {
			AbstractBeanFactory otherAbstractFactory = (AbstractBeanFactory) otherFactory;
			this.customEditors.putAll(otherAbstractFactory.customEditors);
			this.propertyEditorRegistrars.addAll(otherAbstractFactory.propertyEditorRegistrars);
			this.beanPostProcessors.addAll(otherAbstractFactory.beanPostProcessors);
			this.hasInstantiationAwareBeanPostProcessors = this.hasInstantiationAwareBeanPostProcessors
					|| otherAbstractFactory.hasInstantiationAwareBeanPostProcessors;
			this.hasDestructionAwareBeanPostProcessors = this.hasDestructionAwareBeanPostProcessors
					|| otherAbstractFactory.hasDestructionAwareBeanPostProcessors;
			this.scopes.putAll(otherAbstractFactory.scopes);
			this.securityContextProvider = otherAbstractFactory.securityContextProvider;
		} else {
			setTypeConverter(otherFactory.getTypeConverter());
		}
	}

	@Override
	public BeanDefinition getMergedBeanDefinition(String name) throws BeansException {
		String beanName = transformedBeanName(name);
		// 根据给的bean name 返回一个'merged' BeanDefinition 。如果必须，用它的父merge子的bean定义
		// 判断 不包含BeanDefinition且存在parentBeanFactory，且类型属于ConfigurableBeanFactory
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) getParentBeanFactory()).getMergedBeanDefinition(beanName);
		}
		// 返回本地的merge BeanDefinition
		return getMergedLocalBeanDefinition(beanName);
	}

	@Override
	public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		Object beanInstance = getSingleton(beanName, false);
		// 判断获取的实例不为空，则判断是否属于FactoryBean
		if (beanInstance != null) {
			return (beanInstance instanceof FactoryBean);
		}
		// 判断如果注册了beanname的，却实例为空则返回false
		else if (containsSingleton(beanName)) {
			return false;
		}

		// 如果缓存单例对象中连beanName都没有，则检查beanDefinition
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			// 不在当前的，则检查parent
			return ((ConfigurableBeanFactory) getParentBeanFactory()).isFactoryBean(name);
		}
		// 如果beanDefinition中，有则检查本地的BeanDefinition
		return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));
	}

	/**
	 * 在指定的beanName 的Prototype类型bean创建之前，回调这个方法
	 * 
	 * @param beanName
	 */
	@SuppressWarnings("unchecked")
	protected void beforePrototypeCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		if (curVal == null) {
			this.prototypesCurrentlyInCreation.set(beanName);
		} else if (curVal instanceof String) {
			// 如果值是string的，则放到一个set中
			Set<String> beanNameSet = new HashSet<String>(2);
			beanNameSet.add((String) curVal);
			beanNameSet.add(beanName);
			this.prototypesCurrentlyInCreation.set(beanNameSet);
		} else {
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.add(beanName);
		}
	}

	/**
	 * 在指定的beanName 的Prototype类型bean创建完之后，回调这个方法
	 * 
	 * @param beanName
	 */
	@SuppressWarnings("unchecked")
	protected void afterPrototypeCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		if (curVal instanceof String) {
			this.prototypesCurrentlyInCreation.remove();
		} else if (curVal instanceof Set) {
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.remove(beanName);
			if (beanNameSet.isEmpty()) {
				this.prototypesCurrentlyInCreation.remove();
			}
		}
	}

	/**
	 * 返回当前的beanName是否是正在创建
	 * 
	 * @param beanName
	 * @return
	 */
	protected final boolean isPrototypeCurrentlyInCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		// 不为空且beanName相同，或者属于set且包含beanName
		return (curVal != null && (curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal)
				.contains(beanName))));
	}

	@Override
	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return isSingletonCurrentlyInCreation(beanName) || isPrototypeCurrentlyInCreation(beanName);
	}

	@Override
	public void destroyBean(String beanName, Object beanInstance) {
		destroyBean(beanName, beanInstance, getMergedLocalBeanDefinition(beanName));
	}

	/**
	 * 销毁bean
	 * 
	 * @param beanName
	 * @param beanInstance
	 * @param mbd
	 */
	protected void destroyBean(String beanName, Object beanInstance, RootBeanDefinition mbd) {
		// 调用注入的bean适配器的销毁方法
		new DisposableBeanAdapter(beanInstance, beanName, mbd, getBeanPostProcessors(), getAccessControlContext())
				.destroy();
	}

	@Override
	public void destroyScopedBean(String beanName) {
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		// 如果返回的bean定义是Singleton或者Prototype string自带的。则抛出异常
		if (mbd.isSingleton() || mbd.isPrototype()) {
			throw new IllegalArgumentException("Bean name '" + beanName
					+ "' does not correspond to an object in a mutable scope");
		}
		// 获取scope
		String scopeName = mbd.getScope();
		Scope scope = this.scopes.get(scopeName);
		if (scope == null) {
			throw new IllegalStateException("No Scope SPI registered for scope '" + scopeName + "'");
		}
		// 移除bean
		Object bean = scope.remove(beanName);
		if (bean != null) {
			destroyBean(beanName, bean, mbd);
		}
	}

	// 在子类中实现
	// @Override
	// public void registerAlias(String beanName, String alias) throws
	// BeanDefinitionStoreException {
	//
	// }
	//
	// @Override
	// public void resolveAliases(StringValueResolver valueResolver) {
	//
	// }

	// ---------------------------------------------------------------------
	// Implementation methods
	// ---------------------------------------------------------------------

	/**
	 * 剥离factory废弃的前缀，返回bean的name 。一般是去除开头的 “&”
	 * 
	 * @param name
	 * @return
	 */
	protected String transformedBeanName(String name) {
		return canonicalName(BeanFactoryUtils.transformedBeanName(name));
	}

	/**
	 * 确定原来的名字，解决局部定义的别名的规范名称。
	 * 
	 * @param name
	 * @return
	 */
	protected String originalBeanName(String name) {
		String beanName = transformedBeanName(name);
		if (name.startsWith(FACTORY_BEAN_PREFIX)) {
			beanName = FACTORY_BEAN_PREFIX + beanName;
		}
		return beanName;
	}

	/**
	 * 用注册到工厂的customer editor初始化BeanWrapper
	 * 
	 * @param bw
	 */
	protected void initBeanWrapper(BeanWrapper bw) {
		bw.setConversionService(getConversionService());
		registerCustomEditors(bw);
	}

	/**
	 * 用已经用BeanFactory注册的customer editor。来初始化给定的PropertyEditorRegistry
	 * 
	 * @param registry
	 */
	protected void registerCustomEditors(PropertyEditorRegistry registry) {
		PropertyEditorRegistrySupport registrySupport = (registry instanceof PropertyEditorRegistrySupport ? (PropertyEditorRegistrySupport) registry
				: null);
		// 不为空
		if (registrySupport != null) {
			registrySupport.useConfigValueEditors();
		}
		if (!this.propertyEditorRegistrars.isEmpty()) {
			// 循环 propertyEditorRegistrars
			for (PropertyEditorRegistrar registrar : this.propertyEditorRegistrars) {
				try {
					// TODO:无限递归循环嘛~~~递归循环注册到所有的PropertyEditorRegistrar中
					registrar.registerCustomEditors(registry);
				} catch (BeanCreationException ex) {
					// 异常处理
					Throwable rootCause = ex.getMostSpecificCause();
					if (rootCause instanceof BeanCurrentlyInCreationException) {
						// 打印日志，继续循环
						BeanCreationException bce = (BeanCreationException) rootCause;
						if (isCurrentlyInCreation(bce.getBeanName())) {
							if (logger.isDebugEnabled()) {
								logger.debug("PropertyEditorRegistrar [" + registrar.getClass().getName()
										+ "] failed because it tried to obtain currently created bean '"
										+ ex.getBeanName() + "': " + ex.getMessage());
							}
							onSuppressedException(ex);
							continue;
						}
					}
					throw ex;
				}
			}
		}
		if (!this.customEditors.isEmpty()) {
			for (Map.Entry<Class<?>, Class<? extends PropertyEditor>> entry : this.customEditors.entrySet()) {
				Class<?> requiredType = entry.getKey();
				Class<? extends PropertyEditor> editorClass = entry.getValue();
				// TODO：无限递归嘛~ 递归循环注册
				registry.registerCustomEditor(requiredType, BeanUtils.instantiateClass(editorClass));
			}
		}
	}

	/**
	 * 返回一个merged RootBeanDefinition
	 * 
	 * @param beanName
	 * @return
	 * @throws BeansException
	 */
	protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
		// 用最小的锁，对并发的map第一时间进行快速检查
		RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
		if (mbd != null) {
			return mbd;
		}
		return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
	}

	/**
	 * 返回最顶层的RootBeanDefinition
	 * 
	 * @param beanName
	 * @param bd
	 * @return
	 * @throws BeanDefinitionStoreException
	 */
	protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd)
			throws BeanDefinitionStoreException {
		return getMergedBeanDefinition(beanName, bd, null);
	}

	/**
	 * 返回一个RootBeanDefinition
	 * 
	 * @param beanName
	 * @param bd
	 * @param containingBd
	 * @return
	 * @throws BeanDefinitionStoreException
	 */
	protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd, BeanDefinition containingBd)
			throws BeanDefinitionStoreException {

		synchronized (this.mergedBeanDefinitions) {
			RootBeanDefinition mbd = null;

			// 检查全部的锁，为了执行相同的merge bean。如果没有指定containingBd，则获取RootBeanDefinition
			if (containingBd == null) {
				mbd = this.mergedBeanDefinitions.get(beanName);
			}

			// 获取的RootBeanDefinition 定义为空
			if (mbd == null) {
				// 判断parent
				if (bd.getParentName() == null) {
					// 父也为空，则根据指定的bd 克隆一份
					if (bd instanceof RootBeanDefinition) {
						mbd = ((RootBeanDefinition) bd).cloneBeanDefinition();
					} else {
						// 不属于RootBeanDefinition类型，则new一个
						mbd = new RootBeanDefinition(bd);
					}
				} else {
					// 父存在，则利用父的merge
					BeanDefinition pbd;
					try {
						String parentBeanName = transformedBeanName(bd.getParentName());
						if (!beanName.equals(parentBeanName)) {
							// 父名和指定的名称不一样
							pbd = getMergedBeanDefinition(parentBeanName);
						} else {
							// 父BeanFactory属于ConfigurableBeanFactory，进行强转获取，不属于则抛出异常
							if (getParentBeanFactory() instanceof ConfigurableBeanFactory) {
								pbd = ((ConfigurableBeanFactory) getParentBeanFactory())
										.getMergedBeanDefinition(parentBeanName);
							} else {
								throw new NoSuchBeanDefinitionException(bd.getParentName(), "Parent name '"
										+ bd.getParentName() + "' is equal to bean name '" + beanName
										+ "': cannot be resolved without an AbstractBeanFactory parent");
							}
						}
					} catch (NoSuchBeanDefinitionException ex) {
						throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,
								"Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
					}
					// 深层copy 用 有限的值
					mbd = new RootBeanDefinition(pbd);
					mbd.overrideFrom(bd);
				}

				// 如果在配置之前（判断scope的值是否为空），则设置默认为singleton的scope
				if (!StringUtils.hasLength(mbd.getScope())) {
					mbd.setScope(RootBeanDefinition.SCOPE_SINGLETON);
				}

				// 如果指定的containingBd不空，不能Singleton，则需要将mbd设置成containingBd scope
				if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
					mbd.setScope(containingBd.getScope());
				}

				// 只缓存合并的bean的定义，如果我们对已经创建的bean的实例，或者至少已经创建了一个实例。
				if (containingBd == null && isCacheBeanMetadata() && isBeanEligibleForMetadataCaching(beanName)) {
					this.mergedBeanDefinitions.put(beanName, mbd);
				}
			}
			return mbd;
		}
	}

	/**
	 * 检查要merge的RootBeanDefinition
	 * 
	 * @param mbd
	 * @param beanName
	 * @param args
	 * @throws BeanDefinitionStoreException
	 */
	protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, Object[] args)
			throws BeanDefinitionStoreException {
		// 检查RootBeanDefinition 如果不是abstract
		if (mbd.isAbstract()) {
			throw new BeanIsAbstractException(beanName);
		}
		// 参数不能为空，且RootBeanDefinition 必须是Prototype的scope
		if (args != null && !mbd.isPrototype()) {
			throw new BeanDefinitionStoreException(
					"Can only specify arguments for the getBean method when referring to a prototype bean definition");
		}
	}

	/**
	 * 移除指定beanName的RootBeanDefinition
	 * 
	 * @param beanName
	 */
	protected void clearMergedBeanDefinition(String beanName) {
		this.mergedBeanDefinitions.remove(beanName);
	}

	/**
	 * 针对指定的bean definition 解析 bean class
	 * 
	 * @param mbd
	 * @param beanName
	 * @param typesToMatch
	 * @return
	 * @throws CannotLoadBeanClassException
	 */
	protected Class<?> resolveBeanClass(final RootBeanDefinition mbd, String beanName, final Class<?>... typesToMatch)
			throws CannotLoadBeanClassException {
		try {
			// 如果存在class 则直接获取
			if (mbd.hasBeanClass()) {
				return mbd.getBeanClass();
			}
			// 根据RootBeanDefinition 类型 获取class
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
					public Class<?> run() throws Exception {
						return doResolveBeanClass(mbd, typesToMatch);
					}
				}, getAccessControlContext());
			} else {
				return doResolveBeanClass(mbd, typesToMatch);
			}
		} catch (PrivilegedActionException pae) {
			ClassNotFoundException ex = (ClassNotFoundException) pae.getException();
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
		} catch (ClassNotFoundException ex) {
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
		} catch (LinkageError err) {
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), err);
		}
	}

	/**
	 * 指定的bean definition 解析 bean class
	 * 
	 * @param mbd
	 * @param typesToMatch
	 * @return
	 * @throws ClassNotFoundException
	 */
	private Class<?> doResolveBeanClass(RootBeanDefinition mbd, Class<?>... typesToMatch) throws ClassNotFoundException {
		// 如果typesToMatch不为空
		if (!ObjectUtils.isEmpty(typesToMatch)) {
			ClassLoader tempClassLoader = getTempClassLoader();
			if (tempClassLoader != null) {
				if (tempClassLoader instanceof DecoratingClassLoader) {
					DecoratingClassLoader dcl = (DecoratingClassLoader) tempClassLoader;
					for (Class<?> typeToMatch : typesToMatch) {
						dcl.excludeClass(typeToMatch.getName());
					}
				}
				String className = mbd.getBeanClassName();
				// 解析到了className 反射获取 Class
				return (className != null ? ClassUtils.forName(className, tempClassLoader) : null);
			}
		}
		return mbd.resolveBeanClass(getBeanClassLoader());
	}

	/**
	 * 评估给定的一个字符串包含在一个bean definition下，潜在的作为一种表达式的解析
	 * 
	 * @param value
	 * @param beanDefinition
	 * @return
	 */
	protected Object evaluateBeanDefinitionString(String value, BeanDefinition beanDefinition) {
		if (this.beanExpressionResolver == null) {
			return value;
		}
		Scope scope = (beanDefinition != null ? getRegisteredScope(beanDefinition.getScope()) : null);
		return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
	}

	/**
	 * 为指定的bean 预估最终的bean 类型
	 * 
	 * @param beanName
	 * @param mbd
	 * @param typesToMatch
	 * @return
	 */
	protected Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		if (mbd.getFactoryMethodName() != null) {
			return null;
		}
		return resolveBeanClass(mbd, beanName, typesToMatch);
	}

	/**
	 * 判断指定的bean 是否是FactoryBean
	 * 
	 * @param beanName
	 * @param mbd
	 * @return
	 */
	protected boolean isFactoryBean(String beanName, RootBeanDefinition mbd) {
		Class<?> predictedType = predictBeanType(beanName, mbd, FactoryBean.class);
		return (predictedType != null && FactoryBean.class.isAssignableFrom(predictedType))
				|| (mbd.hasBeanClass() && FactoryBean.class.isAssignableFrom(mbd.getBeanClass()));
	}

	/**
	 * 尽可能的确定这个bean type是FactoryBean定义的。如果没有为目标类注册singleton instance才调用这个方法
	 * 
	 * @param beanName
	 * @param mbd
	 * @return
	 */
	protected Class<?> getTypeForFactoryBean(String beanName, RootBeanDefinition mbd) {
		if (!mbd.isSingleton()) {
			return null;
		}
		try {
			FactoryBean<?> factoryBean = doGetBean(FACTORY_BEAN_PREFIX + beanName, FactoryBean.class, null, true);
			return getTypeForFactoryBean(factoryBean);
		} catch (BeanCreationException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Ignoring bean creation exception on FactoryBean type check: " + ex);
			}
			onSuppressedException(ex);
			return null;
		}
	}

	/**
	 * 标记bean已经被创建了
	 * 
	 * @param beanName
	 */
	protected void markBeanAsCreated(String beanName) {
		this.alreadyCreated.add(beanName);
	}

	/**
	 * 判断确定指定的bean是符合其bean定义元数据缓存。alreadyCreated是否包含指定beanname
	 * 
	 * @param beanName
	 * @return
	 */
	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
		return this.alreadyCreated.contains(beanName);
	}

	/**
	 * 移除一个指定beanName的singleton 如果经过检查已经创建了
	 * 
	 * @param beanName
	 * @return
	 */
	protected boolean removeSingletonIfCreatedForTypeCheckOnly(String beanName) {
		if (!this.alreadyCreated.contains(beanName)) {
			removeSingleton(beanName);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 为给定的 bean 实例获取object，无论是实例的本身，还是一个FactoryBean案例被创建的object
	 * 
	 * @param beanInstance
	 * @param name
	 * @param beanName
	 * @param mbd
	 * @return
	 */
	protected Object getObjectForBeanInstance(Object beanInstance, String name, String beanName, RootBeanDefinition mbd) {
		// 如果指定的name 是废弃的，且beanInstance 不属于 FactoryBean 。会抛出异常
		if (BeanFactoryUtils.isFactoryDereference(name) && !(beanInstance instanceof FactoryBean)) {
			throw new BeanIsNotAFactoryException(transformedBeanName(name), beanInstance.getClass());
		}

		// 如果这个beanInstance不属于FactoryBean类型 或者 指定的name 是废弃的。直接返回 beanInstance
		if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
			return beanInstance;
		}

		Object object = null;
		if (mbd == null) {
			object = getCachedObjectForFactoryBean(beanName);
		}
		if (object == null) {
			// 从factory中返回 bean 实例
			FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
			// 从factorybean中获取的object，如果是singleton 则缓存这个对象
			if (mbd == null && containsBeanDefinition(beanName)) {
				mbd = getMergedLocalBeanDefinition(beanName);
			}
			boolean synthetic = (mbd != null && mbd.isSynthetic());
			object = getObjectFromFactoryBean(factory, beanName, !synthetic);
		}
		return object;
	}

	/**
	 * 指定的beanname 是否在使用
	 * 
	 * @param beanName
	 * @return
	 */
	public boolean isBeanNameInUse(String beanName) {
		return isAlias(beanName) || containsLocalBean(beanName) || hasDependentBean(beanName);
	}

	/**
	 * 确定给的bean 是否在 关闭的容器的时候必须被销毁
	 * 
	 * @param bean
	 * @param mbd
	 * @return
	 */
	protected boolean requiresDestruction(Object bean, RootBeanDefinition mbd) {
		return (bean != null && (bean instanceof DisposableBean || mbd.getDestroyMethodName() != null || hasDestructionAwareBeanPostProcessors()));
	}

	/**
	 * 注册为DisposableBean如果需要的话：在这个factory将指定的bean添加到自由bean的队列中。
	 * 注册它为DisposableBean实例，并指定在容器关闭时销毁的方法。 只适用于单例scope。
	 * 
	 * @param beanName
	 * @param bean
	 * @param mbd
	 */
	protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
		AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
		if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
			if (mbd.isSingleton()) {
				registerDisposableBean(beanName, new DisposableBeanAdapter(bean, beanName, mbd,
						getBeanPostProcessors(), acc));
			} else {
				Scope scope = this.scopes.get(mbd.getScope());
				if (scope == null) {
					throw new IllegalStateException("No Scope registered for scope '" + mbd.getScope() + "'");
				}
				scope.registerDestructionCallback(beanName, new DisposableBeanAdapter(bean, beanName, mbd,
						getBeanPostProcessors(), acc));
			}
		}
	}

	// ---------------------------------------------------------------------
	// Abstract methods to be implemented by subclasses
	// ---------------------------------------------------------------------

	/**
	 * 检查这个beanfactory 是否包含 给的beanName的 beanDefinition
	 * 
	 * @param beanName
	 * @return
	 */
	protected abstract boolean containsBeanDefinition(String beanName);

	/**
	 * 根据指定的beanName 获取beanDefinition
	 * 
	 * @param beanName
	 * @return
	 * @throws BeansException
	 */
	protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

	/**
	 * 为给定的 BeanDefinition 创建一个bean的实例
	 * 
	 * @param beanName
	 * @param mbd
	 * @param args
	 * @return
	 * @throws BeanCreationException
	 */
	protected abstract Object createBean(String beanName, RootBeanDefinition mbd, Object[] args)
			throws BeanCreationException;

}
