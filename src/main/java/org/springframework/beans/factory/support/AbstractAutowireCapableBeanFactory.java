package org.springframework.beans.factory.support;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.spi.ObjectFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * AbstractAutowireCapableBeanFactory实现 AutowireCapableBeanFactory 接口的 Bean 装配功能<br>
 * 其实现的 createBean/autowireBean/configureBean 方法,包含了 bean 创建装配的逻辑<br>
 * 整个流程：概括的讲先根据 Class 类型包装成 RootBeanDefinition 并设置 SCOPE ，然后调用重载方法
 * createBean(beanClass.getName(), bd, null) 就完事了，核心方法在createBean和doCreateBean。
 * 
 * @author yanbin
 * 
 */
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements
		AutowireCapableBeanFactory {

	/** 用于创建bean实例的策略 */
	private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();
	/** 为解析方法参数名称的策略 */
	private ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	/** 是否要自动解决beans之间的循环引用 */
	private boolean allowCircularReferences = true;

	/** 是否要求助注入一个未处理循环引用的bean实例，即使这个注入的bean 最终已经获得了装配 */
	private boolean allowRawInjectionDespiteWrapping = false;

	/** 依赖类型忽略对依赖的检查和自动装配 */
	private final Set<Class> ignoredDependencyTypes = new HashSet<Class>();

	/** 依赖接口忽略对依赖的检查和自动装配 */
	private final Set<Class> ignoredDependencyInterfaces = new HashSet<Class>();

	/** 缓存没有完成的FactoryBean实例 ： FactoryBean name --> BeanWrapper */
	private final Map<String, BeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<String, BeanWrapper>();

	/** 缓存被过滤的PropertyDescriptors ：bean Class -> PropertyDescriptor */
	private final Map<Class, PropertyDescriptor[]> filteredPropertyDescriptorsCache = new ConcurrentHashMap<Class, PropertyDescriptor[]>();

	public AbstractAutowireCapableBeanFactory() {
		super();
		ignoreDependencyInterface(BeanNameAware.class);
		ignoreDependencyInterface(BeanFactoryAware.class);
		ignoreDependencyInterface(BeanClassLoaderAware.class);
	}

	public AbstractAutowireCapableBeanFactory(BeanFactory parentBeanFactory) {
		this();
		setParentBeanFactory(parentBeanFactory);
	}

	/**
	 * 设置实例化策略
	 * 
	 * @param instantiationStrategy
	 */
	public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
		this.instantiationStrategy = instantiationStrategy;
	}

	protected InstantiationStrategy getInstantiationStrategy() {
		return this.instantiationStrategy;
	}

	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	protected ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}

	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.allowCircularReferences = allowCircularReferences;
	}

	public void setAllowRawInjectionDespiteWrapping(boolean allowRawInjectionDespiteWrapping) {
		this.allowRawInjectionDespiteWrapping = allowRawInjectionDespiteWrapping;
	}

	/**
	 * 增加忽略依赖的类型
	 * 
	 * @param type
	 */
	public void ignoreDependencyType(Class type) {
		this.ignoredDependencyTypes.add(type);
	}

	/**
	 * 增加忽略依赖的接口
	 * 
	 * @param ifc
	 */
	public void ignoreDependencyInterface(Class ifc) {
		this.ignoredDependencyInterfaces.add(ifc);
	}

	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		// 调用AbstractBeanFactory复制方法
		super.copyConfigurationFrom(otherFactory);
		// 属于AbstractAutowireCapableBeanFactory类型，则复制AbstractAutowireCapableBeanFactory的属性
		if (otherFactory instanceof AbstractAutowireCapableBeanFactory) {
			AbstractAutowireCapableBeanFactory otherAutowireFactory = (AbstractAutowireCapableBeanFactory) otherFactory;
			this.instantiationStrategy = otherAutowireFactory.instantiationStrategy;
			this.allowCircularReferences = otherAutowireFactory.allowCircularReferences;
			this.ignoredDependencyTypes.addAll(otherAutowireFactory.ignoredDependencyTypes);
			this.ignoredDependencyInterfaces.addAll(otherAutowireFactory.ignoredDependencyInterfaces);
		}
	}

	// -------------------------------------------------------------------------
	// Typical methods for creating and populating external bean instances
	// 为创建和populating bean实例的典型方法
	// -------------------------------------------------------------------------

	@Override
	public <T> T createBean(Class<T> beanClass) throws BeansException {
		RootBeanDefinition bd = new RootBeanDefinition(beanClass);
		bd.setScope(SCOPE_PROTOTYPE);
		return (T) createBean(beanClass.getName(), bd, null);
	}

	@Override
	public void autowireBean(Object existingBean) {
		RootBeanDefinition bd = new RootBeanDefinition(ClassUtils.getUserClass(existingBean));
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		// 初始化
		initBeanWrapper(bw);
		populateBean(bd.getBeanClass().getName(), bd, bw);
	}

	@Override
	public Object configureBean(Object existingBean, String beanName) throws BeansException {
		// 标记为bean为已经创建的
		markBeanAsCreated(beanName);
		BeanDefinition mbd = getMergedBeanDefinition(beanName);
		RootBeanDefinition bd = null;
		// 如果mbd属于RootBeanDefinition类型
		if (mbd instanceof RootBeanDefinition) {
			RootBeanDefinition rbd = (RootBeanDefinition) mbd;
			// 强转之后判断是否是Prototype类型
			if (rbd.isPrototype()) {
				bd = rbd;
			}
		}
		// 如果以上没有赋值，则new一个RootBeanDefinition
		if (bd == null) {
			bd = new RootBeanDefinition(mbd);
			bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		}
		// 进行装配
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		initBeanWrapper(bw);
		populateBean(beanName, bd, bw);
		return initializeBean(beanName, existingBean, bd);
	}

	@Override
	public Object resolveDependency(DependencyDescriptor descriptor, String beanName) throws BeansException {
		return resolveDependency(descriptor, beanName, null, null);
	}

	// -------------------------------------------------------------------------
	// Specialized methods for fine-grained control over the bean lifecycle
	// 为细粒度控制bean的生命周期的专门方法
	// -------------------------------------------------------------------------

	@Override
	public Object createBean(Class beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
		// 用一个 非singleton的bean definition， 为了避免注册一个依赖的bean
		RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		return createBean(beanClass.getName(), bd, null);
	}

	@Override
	public Object autowire(Class beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
		// 用一个 非singleton的bean definition， 为了避免注册一个依赖的bean
		final RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		// 装配模式是构造方法装配
		if (bd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR) {
			// 自动方法装配获取装配实例
			return autowireConstructor(beanClass.getName(), bd, null, null).getWrappedInstance();
		} else {
			Object bean;
			final BeanFactory parent = this;

			if (System.getSecurityManager() != null) {
				bean = AccessController.doPrivileged(new PrivilegedAction<Object>() {

					public Object run() {
						// 获取策略，实例化
						return getInstantiationStrategy().instantiate(bd, null, parent);
					}
				}, getAccessControlContext());
			} else {
				bean = getInstantiationStrategy().instantiate(bd, null, parent);
			}

			populateBean(beanClass.getName(), bd, new BeanWrapperImpl(bean));
			return bean;
		}
	}

	@Override
	public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
			throws BeansException {
		// 如果装配类型是构造方法自动装配，抛出异常
		if (autowireMode == AUTOWIRE_CONSTRUCTOR) {
			throw new IllegalArgumentException("AUTOWIRE_CONSTRUCTOR not supported for existing bean instance");
		}
		// 用一个 非singleton的bean definition， 为了避免注册一个依赖的bean
		RootBeanDefinition bd = new RootBeanDefinition(ClassUtils.getUserClass(existingBean), autowireMode,
				dependencyCheck);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		initBeanWrapper(bw);
		populateBean(bd.getBeanClass().getName(), bd, bw);
	}

	@Override
	public void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException {
		markBeanAsCreated(beanName);
		BeanDefinition bd = getMergedBeanDefinition(beanName);
		// new bean装配器
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		initBeanWrapper(bw);
		applyPropertyValues(beanName, bd, bw, bd.getPropertyValues());
	}

	@Override
	public Object initializeBean(Object existingBean, String beanName) {
		return initializeBean(beanName, existingBean, null);
	}

	@Override
	public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
			throws BeansException {
		Object result = existingBean;
		// 循环所有的后置处理器
		for (BeanPostProcessor beanProcessor : getBeanPostProcessors()) {
			result = beanProcessor.postProcessBeforeInitialization(result, beanName);
			if (result == null) {
				return result;
			}
		}
		return result;
	}

	@Override
	public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
			throws BeansException {
		Object result = existingBean;
		// 循环所有的后置处理器
		for (BeanPostProcessor beanProcessor : getBeanPostProcessors()) {
			result = beanProcessor.postProcessAfterInitialization(result, beanName);
			if (result == null) {
				return result;
			}
		}
		return result;
	}

	// ---------------------------------------------------------------------
	// Implementation of relevant AbstractBeanFactory template methods
	// 实现AbstractBeanFactory 有重大作用的模板方法
	// ---------------------------------------------------------------------

	@Override
	protected Object createBean(final String beanName, final RootBeanDefinition mbd, final Object[] args)
			throws BeanCreationException {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating instance of bean '" + beanName + "'");
		}
		// 确保bean class是在这个点上解析的
		resolveBeanClass(mbd, beanName);
		// 准备方法的override
		try {
			mbd.prepareMethodOverrides();
		} catch (BeanDefinitionValidationException ex) {
			throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
					"Validation of method overrides failed", ex);
		}

		// BeanPostProcessors 一个机会 返回一个代理实例 代替目标对象实例
		try {
			Object bean = resolveBeforeInstantiation(beanName, mbd);
			if (bean != null) {
				return bean;
			}
		} catch (Throwable ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"BeanPostProcessor before instantiation of bean failed", ex);
		}

		// 创建实例
		Object beanInstance = doCreateBean(beanName, mbd, args);
		if (logger.isDebugEnabled()) {
			logger.debug("Finished creating instance of bean '" + beanName + "'");
		}
		return beanInstance;
	}

	/**
	 * 实际创建指定的bean 实例，在postProcessBeforeInstantiation这个点上预创建的准备已经完成了。<br>
	 * 和默认创建bean 实例的区别：使用一个factory方法 和 自动装配构造方法
	 * 
	 * @param beanName
	 * @param mbd
	 * @param args
	 * @return
	 */
	protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args) {
		BeanWrapper instanceWrapper = null;
		// 是单例
		if (mbd.isSingleton()) {
			// 移除这个beanName的装配器
			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
		}
		// 如果装配器为空，则创建一个实例装配器
		if (instanceWrapper == null) {
			instanceWrapper = createBeanInstance(beanName, mbd, args);
		}

		// 获取装配器的实例和类型
		final Object bean = (instanceWrapper != null ? instanceWrapper.getWrappedInstance() : null);
		Class beanType = (instanceWrapper != null ? instanceWrapper.getWrappedClass() : null);

		// 允许post-processors 修改 merged的bean definition
		synchronized (mbd.postProcessingLock) {
			if (!mbd.postProcessed) {
				// 后置处理
				applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
				mbd.postProcessed = true;
			}
		}

		// 是否单例 且 是否允许循环引用 且是否是在当前创建中
		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences && isSingletonCurrentlyInCreation(beanName));
		if (earlySingletonExposure) {
			if (logger.isDebugEnabled()) {
				logger.debug("Eagerly caching bean '" + beanName
						+ "' to allow for resolving potential circular references");
			}
			// 允许解析缓存的bean 循环引用关系
			addSingletonFactory(beanName, new ObjectFactory() {
				public Object getObject() throws BeansException {
					return getEarlyBeanReference(beanName, mbd, bean);
				}
			});
		}

		// 实例化 bean的实例
		Object exposedObject = bean;
		try {
			populateBean(beanName, mbd, instanceWrapper);
			if (exposedObject != null) {
				exposedObject = initializeBean(beanName, exposedObject, mbd);
			}
		} catch (Throwable ex) {
			if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
				throw (BeanCreationException) ex;
			} else {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Initialization of bean failed", ex);
			}
		}

		// 处理关联bean
		if (earlySingletonExposure) {
			// 获取bean 不允许关联早期的bean
			Object earlySingletonReference = getSingleton(beanName, false);
			if (earlySingletonReference != null) {
				if (exposedObject == bean) {
					exposedObject = earlySingletonReference;
				}
				// 处理循环引用
				else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
					String[] dependentBeans = getDependentBeans(beanName);
					Set<String> actualDependentBeans = new LinkedHashSet<String>(dependentBeans.length);
					for (String dependentBean : dependentBeans) {
						// 移除 关联的这些已经创建的bean
						if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
							actualDependentBeans.add(dependentBean);
						}
					}
					// 如果有存在没有移除的，抛出异常
					if (!actualDependentBeans.isEmpty()) {
						throw new BeanCurrentlyInCreationException(beanName, "Bean with name '" + beanName
								+ "' has been injected into other beans ["
								+ StringUtils.collectionToCommaDelimitedString(actualDependentBeans)
								+ "] in its raw version as part of a circular reference, but has eventually been "
								+ "wrapped. This means that said other beans do not use the final version of the "
								+ "bean. This is often the result of over-eager type matching - consider using "
								+ "'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
					}
				}
			}
		}

		// 注册bean 为自由的bean 没有依赖的
		try {
			registerDisposableBeanIfNecessary(beanName, bean, mbd);
		} catch (BeanDefinitionValidationException ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
		}

		return exposedObject;
	}

	@Override
	protected Class predictBeanType(String beanName, RootBeanDefinition mbd, Class... typesToMatch) {
		Class beanClass;
		if (mbd.getFactoryMethodName() != null) {
			beanClass = getTypeForFactoryMethod(beanName, mbd, typesToMatch);
		} else {
			beanClass = resolveBeanClass(mbd, beanName, typesToMatch);
		}
		// 应用SmartInstantiationAwareBeanPostProcessors
		// 在before-instantiation切点之后预设定最后的bean类型
		if (beanClass != null && !mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
					SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
					Class processedType = ibp.predictBeanType(beanClass, beanName);
					if (processedType != null) {
						return processedType;
					}
				}
			}
		}
	}

	/**
	 * 根据Factory 方法获取 类型。只适用已经注册了singleton的目标对象
	 * 
	 * @param beanName
	 * @param mbd
	 * @param typesToMatch
	 * @return
	 */
	protected Class getTypeForFactoryMethod(String beanName, RootBeanDefinition mbd, Class[] typesToMatch) {
		Class factoryClass;
		boolean isStatic = true;
		String factoryBeanName = mbd.getFactoryBeanName();
		if (factoryBeanName != null) {
			if (factoryBeanName.equals(beanName)) {
				throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
						"factory-bean reference points back to the same bean definition");
			}
			// 根据factoryBean 名称获取类型
			factoryClass = getType(factoryBeanName);
			isStatic = false;
		} else {
			factoryClass = resolveBeanClass(mbd, beanName, typesToMatch);
		}

		if (factoryClass == null) {
			return null;
		}

		int minNrOfArgs = mbd.getConstructorArgumentValues().getArgumentCount();
		Method[] candidates = ReflectionUtils.getUniqueDeclaredMethods(factoryClass);
		Set<Class> returnTypes = new HashSet<Class>(1);
		for (Method factoryMethod : candidates) {
			if (Modifier.isStatic(factoryMethod.getModifiers()) == isStatic
					&& factoryMethod.getName().equals(mbd.getFactoryMethodName())
					&& factoryMethod.getParameterTypes().length >= minNrOfArgs) {
				returnTypes.add(factoryMethod.getReturnType());
			}
		}

		if (returnTypes.size() == 1) {
			// 所有方法，返回的是同一个type
			return returnTypes.iterator().next();
		} else {
			return null;
		}
	}

	/**
	 * 这个重新父类的方法，为了是试图查询到FactoryBean的泛型参数的元数据。如果可以确定对象的类型的话<br>
	 */
	@Override
	protected Class<?> getTypeForFactoryBean(String beanName, RootBeanDefinition mbd) {
		class Holder {
			Class<?> value = null;
		}
		final Holder objectType = new Holder();
		String factoryBeanName = mbd.getFactoryBeanName();
		final String factoryMethodName = mbd.getFactoryMethodName();
		// 如果工厂beanname 不为空 且 工厂方法名字不为空
		if (factoryBeanName != null && factoryMethodName != null) {
			// 尝试获取所有没有被实例化的FactoryBean's的对象类型
			BeanDefinition fbDef = getBeanDefinition(factoryBeanName);
			if (fbDef instanceof AbstractBeanDefinition) {
				Class<?> fbClass = ((AbstractBeanDefinition) fbDef).getBeanClass();
				// 判断这个class是否是cglib的代理类
				if (ClassUtils.isCglibProxyClass(fbClass)) {
					// 如果是的的话 CGLIB的子类方法是隐藏 泛型变量的。要找他的父类
					fbClass = fbClass.getSuperclass();
				}
				ReflectionUtils.doWithMethods(fbClass, new ReflectionUtils.MethodCallback() {
					public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
						if (method.getName().equals(factoryMethodName)
								&& FactoryBean.class.isAssignableFrom(method.getReturnType())) {
							objectType.value = GenericTypeResolver.resolveReturnTypeArgument(method, FactoryBean.class);
						}
					}
				});
				if (objectType.value != null) {
					return objectType.value;
				}
			}
		}
		FactoryBean<?> fb = (mbd.isSingleton() ? getSingletonFactoryBeanForTypeCheck(beanName, mbd)
				: getNonSingletonFactoryBeanForTypeCheck(beanName, mbd));
		if (fb != null) {
			// 试图到实例的较早阶段去获取FactoryBean的object类型
			objectType.value = getTypeForFactoryBean(fb);
			if (objectType.value != null) {
				return objectType.value;
			}
		}

		// 没有类型被找到，回到父类的方法，FactoryBean实例创建的整个过程
		return super.getTypeForFactoryBean(beanName, mbd);
	}

	/**
	 * 获取一个较早存取的指定bean的关系
	 * 
	 * @param beanName
	 * @param mbd
	 * @param bean
	 * @return
	 */
	protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
		Object exposedObject = bean;
		if (bean != null && !mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
					SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
					exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
					if (exposedObject == null) {
						return exposedObject;
					}
				}
			}
		}
		return exposedObject;
	}

	// ---------------------------------------------------------------------
	// Implementation methods
	// ---------------------------------------------------------------------

	/**
	 * 不用FactoryBean的整个初始化过程，通过这个方法捷径获取 一个 singleton的FactoryBean的实例
	 * 
	 * @param beanName
	 * @param mbd
	 * @return
	 */
	private FactoryBean getSingletonFactoryBeanForTypeCheck(String beanName, RootBeanDefinition mbd) {
		synchronized (getSingletonMutex()) {
			BeanWrapper bw = this.factoryBeanInstanceCache.get(beanName);
			if (bw != null) {
				return (FactoryBean) bw.getWrappedInstance();
			}
			// 如果实例是当前正在创建的，则返回空
			if (isSingletonCurrentlyInCreation(beanName)) {
				return null;
			}
			Object instance = null;
			try {
				// 在创建之前，标记bean为当前创建的。即使只是部分
				beforeSingletonCreation(beanName);
				// BeanPostProcessors 一个机会 返回一个代理实例 代替目标对象实例
				instance = resolveBeforeInstantiation(beanName, mbd);
				if (instance == null) {
					bw = createBeanInstance(beanName, mbd, null);
					instance = bw.getWrappedInstance();
				}
			} finally {
				// 完成这个bean的部分创建，做后续处理
				afterSingletonCreation(beanName);
			}
			FactoryBean fb = getFactoryBean(beanName, instance);
			if (bw != null) {
				this.factoryBeanInstanceCache.put(beanName, bw);
			}
			return fb;
		}
	}

	/**
	 * 不用FactoryBean的整个初始化过程，通过这个方法捷径获取 一个 singleton的FactoryBean的实例<br>
	 * 这个是非Singleton中没有的
	 * 
	 * @param beanName
	 * @param mbd
	 * @return
	 */
	private FactoryBean getNonSingletonFactoryBeanForTypeCheck(String beanName, RootBeanDefinition mbd) {
		if (isPrototypeCurrentlyInCreation(beanName)) {
			return null;
		}
		Object instance = null;
		try {
			// 在创建之前，标记bean为当前创建的。即使只是部分
			beforePrototypeCreation(beanName);
			// BeanPostProcessors 一个机会 返回一个代理实例 代替目标对象实例
			instance = resolveBeforeInstantiation(beanName, mbd);
			if (instance == null) {
				BeanWrapper bw = createBeanInstance(beanName, mbd, null);
				instance = bw.getWrappedInstance();
			}
		} finally {
			// 完成这个bean的部分创建，做后续处理
			afterPrototypeCreation(beanName);
		}
		return getFactoryBean(beanName, instance);
	}

	/**
	 * 位置定的BeanDefinition 应用MergedBeanDefinitionPostProcessors处理。
	 * 调用他们的postProcessMergedBeanDefinition方法
	 * 
	 * @param mbd
	 * @param beanType
	 * @param beanName
	 * @throws BeansException
	 */
	protected void applyMergedBeanDefinitionPostProcessors(RootBeanDefinition mbd, Class beanType, String beanName)
			throws BeansException {
		try {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof MergedBeanDefinitionPostProcessor) {
					MergedBeanDefinitionPostProcessor bdp = (MergedBeanDefinitionPostProcessor) bp;
					bdp.postProcessMergedBeanDefinition(mbd, beanType, beanName);
				}
			}
		} catch (Exception ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Post-processing failed of bean type [" + beanType + "] failed", ex);
		}
	}

	/**
	 * 应用before-instantiation post-processors
	 * 
	 * @param beanName
	 * @param mbd
	 * @return
	 */
	protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
		Object bean = null;
		if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
			// Make sure bean class is actually resolved at this point.
			if (mbd.hasBeanClass() && !mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
				bean = applyBeanPostProcessorsBeforeInstantiation(mbd.getBeanClass(), beanName);
				if (bean != null) {
					bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
				}
			}
			mbd.beforeInstantiationResolved = (bean != null);
		}
		return bean;
	}

	/**
	 * 在指定的bean definition 上应用 InstantiationAwareBeanPostProcessors后处理器。
	 * 调用他们的postProcessBeforeInstantiation方法，返回的object会是目标对象实例后的结果
	 * 
	 * @param beanClass
	 * @param beanName
	 * @return
	 * @throws BeansException
	 */
	public Object applyBeanPostProcessorsBeforeInstantiation(Class beanClass, String beanName) throws BeansException {
		for (BeanPostProcessor bp : getBeanPostProcessors()) {
			if (bp instanceof InstantiationAwareBeanPostProcessor) {
				InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
				Object result = ibp.postProcessBeforeInstantiation(beanClass, beanName);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	/**
	 * 位置定的bean创建一个新的实例，采用一个适当的实例化策略：工厂方法，构造装配 或者 简单的实例化
	 * 
	 * @param beanName
	 * @param mbd
	 * @param args
	 * @return
	 */
	protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, Object[] args) {
		// 确保这个bean class 在这个点上已经真实的被解析了
		Class beanClass = resolveBeanClass(mbd, beanName);
		if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
		}

		if (mbd.getFactoryMethodName() != null) {
			return instantiateUsingFactoryMethod(beanName, mbd, args);
		}

		boolean resolved = false;
		boolean autowireNecessary = false;
		if (args == null) {
			synchronized (mbd.constructorArgumentLock) {
				if (mbd.resolvedConstructorOrFactoryMethod != null) {
					resolved = true;
					autowireNecessary = mbd.constructorArgumentsResolved;
				}
			}
		}
		// 重新创建一个相同的bean的快捷方式
		if (resolved) {
			if (autowireNecessary) {
				return autowireConstructor(beanName, mbd, null, null);
			} else {
				return instantiateBean(beanName, mbd);
			}
		}

		// 需要确定构造器
		Constructor[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
		if (ctors != null || mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_CONSTRUCTOR
				|| mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
			return autowireConstructor(beanName, mbd, ctors, args);
		}

		// 没有特殊指定处理: 利用简单的无参构造
		return instantiateBean(beanName, mbd);
	}

	/**
	 * 检查所有的注册，为指定的bean class确定候选的构造器。
	 * 
	 * @param beanClass
	 * @param beanName
	 * @return
	 */
	protected Constructor[] determineConstructorsFromBeanPostProcessors(Class beanClass, String beanName) {
		if (beanClass != null && hasInstantiationAwareBeanPostProcessors()) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
					SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
					Constructor[] ctors = ibp.determineCandidateConstructors(beanClass, beanName);
					if (ctors != null) {
						return ctors;
					}
				}
			}
		}
		return null;
	}

	/**
	 * 利用默认的构造方法实例化指定的bean
	 * 
	 * @param beanName
	 * @param mbd
	 * @return
	 */
	protected BeanWrapper instantiateBean(final String beanName, final RootBeanDefinition mbd) {
		try {
			Object beanInstance;
			final BeanFactory parent = this;
			if (System.getSecurityManager() != null) {
				beanInstance = AccessController.doPrivileged(new PrivilegedAction<Object>() {
					public Object run() {
						return getInstantiationStrategy().instantiate(mbd, beanName, parent);
					}
				}, getAccessControlContext());
			} else {
				beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
			}
			BeanWrapper bw = new BeanWrapperImpl(beanInstance);
			initBeanWrapper(bw);
			return bw;
		} catch (Throwable ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
		}
	}

	/**
	 * 利用一个命名的工厂方法实例化bean，这个方法也许是静态的，如果mbd这个参数是指定一个class，而不是一个factoryBean，
	 * 或者他配置在依赖注入的工厂类上的一个实例变量
	 * 
	 * @param beanName
	 * @param mbd
	 * @param explicitArgs
	 * @return
	 */
	protected BeanWrapper instantiateUsingFactoryMethod(String beanName, RootBeanDefinition mbd, Object[] explicitArgs) {
		return new ConstructorResolver(this).instantiateUsingFactoryMethod(beanName, mbd, explicitArgs);
	}

	/**
	 * “自动装配构造函数”（与构造器参数的类型）的行为。如果显式构造函数参数值被指定了也同样适用。为所有bean
	 * factory匹配参数。这相当于构造函数的注入：在这种模式下，spring bean factory 可以主宰基于构造函数的依赖解析的结果
	 * 
	 * @param beanName
	 * @param mbd
	 * @param ctors
	 * @param explicitArgs
	 * @return
	 */
	protected BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd, Constructor[] ctors,
			Object[] explicitArgs) {
		return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
	}

	/**
	 * 填充bean。在给定的BeanWrapper 包含 从bean definition中来的属性值 来填充bean实例
	 * 
	 * @param beanName
	 * @param mbd
	 * @param bw
	 */
	protected void populateBean(String beanName, AbstractBeanDefinition mbd, BeanWrapper bw) {
		PropertyValues pvs = mbd.getPropertyValues();

		if (bw == null) {
			if (!pvs.isEmpty()) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Cannot apply property values to null instance");
			} else {
				// 为null实例跳过属性填充阶段
				return;
			}
		}

		// 给任何instantiationawarebeanpostprocessors机会之前修改bean的状态属性设置。这可以被用来，例如，可支持字段注入的样式。
		boolean continueWithPropertyPopulation = true;

		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof InstantiationAwareBeanPostProcessor) {
					InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
					if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
						continueWithPropertyPopulation = false;
						break;
					}
				}
			}
		}

		if (!continueWithPropertyPopulation) {
			return;
		}

		if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME
				|| mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
			MutablePropertyValues newPvs = new MutablePropertyValues(pvs);

			// 增加属性值，基于自动装备的名字。
			if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
				autowireByName(beanName, mbd, bw, newPvs);
			}

			// 增加属性值，基于自动装备的类型。
			if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
				autowireByType(beanName, mbd, bw, newPvs);
			}

			pvs = newPvs;
		}

		boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
		boolean needsDepCheck = (mbd.getDependencyCheck() != RootBeanDefinition.DEPENDENCY_CHECK_NONE);

		if (hasInstAwareBpps || needsDepCheck) {
			PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw);
			if (hasInstAwareBpps) {
				for (BeanPostProcessor bp : getBeanPostProcessors()) {
					if (bp instanceof InstantiationAwareBeanPostProcessor) {
						InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
						pvs = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
						if (pvs == null) {
							return;
						}
					}
				}
			}
			if (needsDepCheck) {
				checkDependencies(beanName, mbd, filteredPds, pvs);
			}
		}

		applyPropertyValues(beanName, mbd, bw, pvs);
	}

	/**
	 * 根据name 自动装配。填充任何丢失的属性值，关联到这个工厂中的另外的beans。如果autowire设置了"byName".
	 * 
	 * @param beanName
	 * @param mbd
	 * @param bw
	 * @param pvs
	 */
	protected void autowireByName(String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
		String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
		for (String propertyName : propertyNames) {
			if (containsBean(propertyName)) {
				Object bean = getBean(propertyName);
				pvs.add(propertyName, bean);
				registerDependentBean(propertyName, beanName);
				if (logger.isDebugEnabled()) {
					logger.debug("Added autowiring by name from bean name '" + beanName + "' via property '"
							+ propertyName + "' to bean named '" + propertyName + "'");
				}
			} else {
				if (logger.isTraceEnabled()) {
					logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName
							+ "' by name: no matching bean found");
				}
			}
		}
	}

	/**
	 * 抽象方法定义"autowire by type" （bean的属性是by type）的行为，
	 * 
	 * @param beanName
	 * @param mbd
	 * @param bw
	 * @param pvs
	 */
	protected void autowireByType(String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
		TypeConverter converter = getCustomTypeConverter();
		if (converter == null) {
			converter = bw;
		}

		Set<String> autowiredBeanNames = new LinkedHashSet<String>(4);
		String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
		for (String propertyName : propertyNames) {
			try {
				PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
				// 不要尝试为type object的装配by type ：没有道理，即使它在技术上是不满意的，非简单的属性。
				if (!Object.class.equals(pd.getPropertyType())) {
					MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
					boolean eager = !PriorityOrdered.class.isAssignableFrom(bw.getWrappedClass());
					DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
					Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
					if (autowiredArgument != null) {
						pvs.add(propertyName, autowiredArgument);
					}
					for (String autowiredBeanName : autowiredBeanNames) {
						registerDependentBean(autowiredBeanName, beanName);
						if (logger.isDebugEnabled()) {
							logger.debug("Autowiring by type from bean name '" + beanName + "' via property '"
									+ propertyName + "' to bean named '" + autowiredBeanName + "'");
						}
					}
					autowiredBeanNames.clear();
				}
			} catch (BeansException ex) {
				throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
			}
		}
	}

	/**
	 * 返回一个数组 内容是non-simple bean properties 为不满足的。
	 * 
	 * @param mbd
	 * @param bw
	 * @return
	 */
	protected String[] unsatisfiedNonSimpleProperties(AbstractBeanDefinition mbd, BeanWrapper bw) {
		Set<String> result = new TreeSet<String>();
		PropertyValues pvs = mbd.getPropertyValues();
		PropertyDescriptor[] pds = bw.getPropertyDescriptors();
		for (PropertyDescriptor pd : pds) {
			if (pd.getWriteMethod() != null && !isExcludedFromDependencyCheck(pd) && !pvs.contains(pd.getName())
					&& !BeanUtils.isSimpleProperty(pd.getPropertyType())) {
				result.add(pd.getName());
			}
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * Extract a filtered set of PropertyDescriptors from the given BeanWrapper,
	 * excluding ignored dependency types or properties defined on ignored
	 * dependency interfaces.
	 * 
	 * @param bw
	 *            the BeanWrapper the bean was created with
	 * @return the filtered PropertyDescriptors
	 * @see #isExcludedFromDependencyCheck
	 */

	/**
	 * 从给定的BeanWrapper提取一个过滤过的PropertyDescriptors集合。不包括忽略的依赖类型或者是定义在忽略的依赖接口上的属性
	 * 
	 * @param bw
	 * @return
	 */
	protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw) {
		PropertyDescriptor[] filtered = this.filteredPropertyDescriptorsCache.get(bw.getWrappedClass());
		if (filtered == null) {
			synchronized (this.filteredPropertyDescriptorsCache) {
				filtered = this.filteredPropertyDescriptorsCache.get(bw.getWrappedClass());
				if (filtered == null) {
					List<PropertyDescriptor> pds = new LinkedList<PropertyDescriptor>(Arrays.asList(bw
							.getPropertyDescriptors()));
					for (Iterator<PropertyDescriptor> it = pds.iterator(); it.hasNext();) {
						PropertyDescriptor pd = it.next();
						if (isExcludedFromDependencyCheck(pd)) {
							it.remove();
						}
					}
					filtered = pds.toArray(new PropertyDescriptor[pds.size()]);
					this.filteredPropertyDescriptorsCache.put(bw.getWrappedClass(), filtered);
				}
			}
		}
		return filtered;
	}

	/**
	 * 确定给定的bean property 是不从 依赖检查中来的
	 * 
	 * @param pd
	 * @return
	 */
	protected boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
		return (AutowireUtils.isExcludedFromDependencyCheck(pd)
				|| this.ignoredDependencyTypes.contains(pd.getPropertyType()) || AutowireUtils
					.isSetterDefinedInInterface(pd, this.ignoredDependencyInterfaces));
	}

	/**
	 * 执行一个依赖性检查，检查所有暴露的属性已经被设置。依赖性检查能简单或者全部
	 * 
	 * @param beanName
	 * @param mbd
	 * @param pds
	 * @param pvs
	 * @throws UnsatisfiedDependencyException
	 */
	protected void checkDependencies(String beanName, AbstractBeanDefinition mbd, PropertyDescriptor[] pds,
			PropertyValues pvs) throws UnsatisfiedDependencyException {

		int dependencyCheck = mbd.getDependencyCheck();
		for (PropertyDescriptor pd : pds) {
			if (pd.getWriteMethod() != null && !pvs.contains(pd.getName())) {
				boolean isSimple = BeanUtils.isSimpleProperty(pd.getPropertyType());
				boolean unsatisfied = (dependencyCheck == RootBeanDefinition.DEPENDENCY_CHECK_ALL)
						|| (isSimple && dependencyCheck == RootBeanDefinition.DEPENDENCY_CHECK_SIMPLE)
						|| (!isSimple && dependencyCheck == RootBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
				if (unsatisfied) {
					throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, pd.getName(),
							"Set this property value or disable dependency checking for this bean.");
				}
			}
		}
	}

	/**
	 * 在这个工厂中，应用解析任何运行时关联另外bean的属性值。必须使用深copy ，所以我们不永久的改变这个属性
	 * 
	 * @param beanName
	 * @param mbd
	 * @param bw
	 * @param pvs
	 */
	protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
		if (pvs == null || pvs.isEmpty()) {
			return;
		}

		MutablePropertyValues mpvs = null;
		List<PropertyValue> original;

		if (System.getSecurityManager() != null) {
			if (bw instanceof BeanWrapperImpl) {
				((BeanWrapperImpl) bw).setSecurityContext(getAccessControlContext());
			}
		}

		if (pvs instanceof MutablePropertyValues) {
			mpvs = (MutablePropertyValues) pvs;
			if (mpvs.isConverted()) {
				// 快捷方式：使用预转化值 as-is
				try {
					bw.setPropertyValues(mpvs);
					return;
				} catch (BeansException ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Error setting property values", ex);
				}
			}
			original = mpvs.getPropertyValueList();
		} else {
			original = Arrays.asList(pvs.getPropertyValues());
		}

		TypeConverter converter = getCustomTypeConverter();
		if (converter == null) {
			converter = bw;
		}
		BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);

		// 创建一个深拷贝，解析这些值的关系
		List<PropertyValue> deepCopy = new ArrayList<PropertyValue>(original.size());
		boolean resolveNecessary = false;
		for (PropertyValue pv : original) {
			if (pv.isConverted()) {
				deepCopy.add(pv);
			} else {
				String propertyName = pv.getName();
				Object originalValue = pv.getValue();
				Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
				Object convertedValue = resolvedValue;
				boolean convertible = bw.isWritableProperty(propertyName)
						&& !PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
				if (convertible) {
					convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
				}

				// 尽可能的存储转换后的值在合并bean definition的时候， 为了避免重新转换所有已经创建的bean实例
				if (resolvedValue == originalValue) {
					if (convertible) {
						pv.setConvertedValue(convertedValue);
					}
					deepCopy.add(pv);
				} else if (convertible && originalValue instanceof TypedStringValue
						&& !((TypedStringValue) originalValue).isDynamic()
						&& !(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
					pv.setConvertedValue(convertedValue);
					deepCopy.add(pv);
				} else {
					resolveNecessary = true;
					deepCopy.add(new PropertyValue(pv, convertedValue));
				}
			}
		}
		if (mpvs != null && !resolveNecessary) {
			mpvs.setConverted();
		}

		// 设置深拷贝
		try {
			bw.setPropertyValues(new MutablePropertyValues(deepCopy));
		} catch (BeansException ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Error setting property values", ex);
		}
	}

	/**
	 * 为指定的目标属性转换指定的值
	 * 
	 * @param value
	 * @param propertyName
	 * @param bw
	 * @param converter
	 * @return
	 */
	private Object convertForProperty(Object value, String propertyName, BeanWrapper bw, TypeConverter converter) {
		if (converter instanceof BeanWrapperImpl) {
			return ((BeanWrapperImpl) converter).convertForProperty(value, propertyName);
		} else {
			PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
			MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
			return converter.convertIfNecessary(value, pd.getPropertyType(), methodParam);
		}
	}

	/**
	 * 实例化给定的bean实例，应用 factory的callbacks，除了实例化方法和bean的后处理器
	 * 
	 * @param beanName
	 * @param bean
	 * @param mbd
	 * @return
	 */
	protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {
		if (System.getSecurityManager() != null) {
			AccessController.doPrivileged(new PrivilegedAction<Object>() {
				public Object run() {
					invokeAwareMethods(beanName, bean);
					return null;
				}
			}, getAccessControlContext());
		} else {
			invokeAwareMethods(beanName, bean);
		}

		Object wrappedBean = bean;
		if (mbd == null || !mbd.isSynthetic()) {
			wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
		}

		try {
			invokeInitMethods(beanName, wrappedBean, mbd);
		} catch (Throwable ex) {
			throw new BeanCreationException((mbd != null ? mbd.getResourceDescription() : null), beanName,
					"Invocation of init method failed", ex);
		}

		if (mbd == null || !mbd.isSynthetic()) {
			wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
		}
		return wrappedBean;
	}

	private void invokeAwareMethods(final String beanName, final Object bean) {
		if (bean instanceof Aware) {
			if (bean instanceof BeanNameAware) {
				((BeanNameAware) bean).setBeanName(beanName);
			}
			if (bean instanceof BeanClassLoaderAware) {
				((BeanClassLoaderAware) bean).setBeanClassLoader(getBeanClassLoader());
			}
			if (bean instanceof BeanFactoryAware) {
				((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
			}
		}
	}

	/**
	 * 给一个bean 一个可能性去反应 他所有的 properties 是set， 一个可能去知道他自己的 bean
	 * factory。这个意思是检查这个bean是否是实现了InitializingBean或者 定义了一个自定义的init method，
	 * 调用必须要的callback(s)
	 * 
	 * @param beanName
	 * @param bean
	 * @param mbd
	 * @throws Throwable
	 */
	protected void invokeInitMethods(String beanName, final Object bean, RootBeanDefinition mbd) throws Throwable {

		boolean isInitializingBean = (bean instanceof InitializingBean);
		if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
			if (logger.isDebugEnabled()) {
				logger.debug("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
			}
			if (System.getSecurityManager() != null) {
				try {
					AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
						public Object run() throws Exception {
							((InitializingBean) bean).afterPropertiesSet();
							return null;
						}
					}, getAccessControlContext());
				} catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			} else {
				((InitializingBean) bean).afterPropertiesSet();
			}
		}

		if (mbd != null) {
			String initMethodName = mbd.getInitMethodName();
			if (initMethodName != null && !(isInitializingBean && "afterPropertiesSet".equals(initMethodName))
					&& !mbd.isExternallyManagedInitMethod(initMethodName)) {
				invokeCustomInitMethod(beanName, bean, mbd);
			}
		}
	}

	/**
	 * 调用指定的custom 实例方法 在给定的bean上。 被invokeInitMethods 调用
	 * 
	 * @param beanName
	 * @param bean
	 * @param mbd
	 * @throws Throwable
	 */
	protected void invokeCustomInitMethod(String beanName, final Object bean, RootBeanDefinition mbd) throws Throwable {
		String initMethodName = mbd.getInitMethodName();
		final Method initMethod = (mbd.isNonPublicAccessAllowed() ? BeanUtils.findMethod(bean.getClass(),
				initMethodName) : ClassUtils.getMethodIfAvailable(bean.getClass(), initMethodName));
		if (initMethod == null) {
			if (mbd.isEnforceInitMethod()) {
				throw new BeanDefinitionValidationException("Couldn't find an init method named '" + initMethodName
						+ "' on bean with name '" + beanName + "'");
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("No default init method named '" + initMethodName + "' found on bean with name '"
							+ beanName + "'");
				}
				// 忽略不存在默认的生命周期方法
				return;
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Invoking init method  '" + initMethodName + "' on bean with name '" + beanName + "'");
		}

		if (System.getSecurityManager() != null) {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
				public Object run() throws Exception {
					ReflectionUtils.makeAccessible(initMethod);
					return null;
				}
			});
			try {
				AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					public Object run() throws Exception {
						initMethod.invoke(bean);
						return null;
					}
				}, getAccessControlContext());
			} catch (PrivilegedActionException pae) {
				InvocationTargetException ex = (InvocationTargetException) pae.getException();
				throw ex.getTargetException();
			}
		} else {
			try {
				ReflectionUtils.makeAccessible(initMethod);
				initMethod.invoke(bean);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

	@Override
	protected Object postProcessObjectFromFactoryBean(Object object, String beanName) {
		return applyBeanPostProcessorsAfterInitialization(object, beanName);
	}

	@Override
	protected void removeSingleton(String beanName) {
		super.removeSingleton(beanName);
		this.factoryBeanInstanceCache.remove(beanName);
	}

	/**
	 * 指定DependencyDescriptor 变种 autowire="byType"。
	 * 
	 * @author yanbin
	 * 
	 */
	private static class AutowireByTypeDependencyDescriptor extends DependencyDescriptor {

		public AutowireByTypeDependencyDescriptor(MethodParameter methodParameter, boolean eager) {
			super(methodParameter, false, eager);
		}

		@Override
		public String getDependencyName() {
			return null;
		}
	}

}
