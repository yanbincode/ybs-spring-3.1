package org.springframework.beans.factory.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.spi.ObjectFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 默认实现类，它不仅支持 Singleton Bean 的注册，也支持 DisposableBean 的注册管理用来清理要丢弃的 bean
 * 以及他们依赖的资源。
 * 
 * @author yanbin
 * 
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	protected static final Object NULL_OBJECT = new Object();

	/** 对子类都可以使用的有效的日志 */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 缓存单例对象 bean name --> bean instance */
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>();

	/** 缓存单例工厂 bean name --> factories */
	private final Map<String, ObjectFactory> singletonFactories = new HashMap<String, ObjectFactory>();

	/** 缓存早期的单例对象，bean name --> bean instance */
	private final Map<String, Object> earlySingletonObjects = new HashMap<String, Object>();

	/** 存放 已经注册的单例， 存放的是bean names 按注册的顺序 */
	private final Set<String> registeredSingletons = new LinkedHashSet<String>(16);

	/** 缓存正在创建的bean的bean name */
	private final Set<String> singletonsCurrentlyInCreation = Collections.synchronizedSet(new HashSet<String>());

	/** 缓存当前在创建过程中通过检查，被排除的bean name */
	private final Set<String> inCreationCheckExclusions = new HashSet<String>();

	/** 抑制异常集合 */
	private Set<Exception> suppressedExceptions;

	/** 标志当前的单例bean 是否是在 当前销毁中 */
	private boolean singletonsCurrentlyInDestruction = false;

	/** 可自由使用的 bean beanname --> instance */
	private final Map<String, Object> disposableBeans = new LinkedHashMap<String, Object>();

	/** bean包含下的bean bean name --> Set 对应bean包含下的所有bean name */
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<String, Set<String>>();

	/** bean 缓存所有依赖的bean 及其依赖 已知bean --> Set 存的是被依赖的bean 存子 */
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<String, Set<String>>();

	/** bean 缓存所有被依赖的bean 已知被依赖bean --> Set 存的是依赖的bean 存父 */
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<String, Set<String>>();

	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Assert.notNull(beanName, "'beanName' must not be null");
		synchronized (this.singletonObjects) {
			Object oldObject = this.singletonObjects.get(beanName);
			if (oldObject != null) {
				throw new IllegalStateException("Could not register object [" + singletonObject + "] under bean name '"
						+ beanName + "': there is already object [" + oldObject + "] bound");
			}
			addSingleton(beanName, singletonObject);
		}
	}

	/**
	 * 添加单例
	 * 
	 * @param beanName
	 * @param singletonObject
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			// 单例缓存中增加
			this.singletonObjects.put(beanName, (singletonObject != null ? singletonObject : NULL_OBJECT));
			// 单例工厂移除
			this.singletonFactories.remove(beanName);
			// 早期的单例对象集合移除
			this.earlySingletonObjects.remove(beanName);
			// 注册的单例对象 增加
			this.registeredSingletons.add(beanName);
		}
	}

	/**
	 * 增加单例工厂
	 * 
	 * @param beanName
	 * @param singletonFactory
	 */
	protected void addSingletonFactory(String beanName, ObjectFactory singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		synchronized (this.singletonObjects) {
			if (!this.singletonObjects.containsKey(beanName)) {
				this.singletonFactories.put(beanName, singletonFactory);
				this.earlySingletonObjects.remove(beanName);
				this.registeredSingletons.add(beanName);
			}
		}
	}

	@Override
	public Object getSingleton(String beanName) {
		return getSingleton(beanName, true);
	}

	/**
	 * 根据bean name 获取实例， 并且判断是否允许 早期的bean进行关联
	 * 
	 * @param beanName
	 * @param allowEarlyReference
	 *            是否允许早期的bean 关联
	 * @return
	 */
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		// 缓存单例对象中获取bean
		Object singletonObject = this.singletonObjects.get(beanName);
		// 如果为空
		if (singletonObject == null) {
			synchronized (this.singletonObjects) {
				// 从早期的单例里面去取
				singletonObject = this.earlySingletonObjects.get(beanName);
				// 如果还是为空，且 允许关联早期的bean
				if (singletonObject == null && allowEarlyReference) {
					// 获取这个bean的 类创建工厂
					ObjectFactory singletonFactory = this.singletonFactories.get(beanName);
					// 如果工厂不为空
					if (singletonFactory != null) {
						// 创建实例
						singletonObject = singletonFactory.getObject();
						// 早期的放入缓存
						this.earlySingletonObjects.put(beanName, singletonObject);
						// 工厂移除
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}
		// 返回这个实例
		return (singletonObject != NULL_OBJECT ? singletonObject : null);
	}

	/**
	 * 根据bean name 获取一个注册的对象，如果没有注册，则马上创建和注册
	 * 
	 * @param beanName
	 * @param singletonFactory
	 * @return
	 */
	public Object getSingleton(String beanName, ObjectFactory singletonFactory) {
		Assert.notNull(beanName, "'beanName' must not be null");
		synchronized (this.singletonObjects) {
			Object singletonObject = this.singletonObjects.get(beanName);
			if (singletonObject == null) {
				// 在销毁中不能再次创建，先等
				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while the singletons of this factory are in destruction "
									+ "(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				// 创建之前做的事情
				beforeSingletonCreation(beanName);
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<Exception>();
				}
				try {
					singletonObject = singletonFactory.getObject();
				} catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				} finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					// 创建之后做的事情
					afterSingletonCreation(beanName);
				}
				// 将创建的实例添加进缓存
				addSingleton(beanName, singletonObject);
			}
			return (singletonObject != NULL_OBJECT ? singletonObject : null);
		}
	}

	/**
	 * 在创建一个实例过程中出现的异常，注册到suppressedExceptions缓存中
	 * 
	 * @param ex
	 */
	protected void onSuppressedException(Exception ex) {
		synchronized (this.singletonObjects) {
			if (this.suppressedExceptions != null) {
				this.suppressedExceptions.add(ex);
			}
		}
	}

	/**
	 * 从缓存中移除一个指定的beanname实例
	 * 
	 * @param beanName
	 */
	protected void removeSingleton(String beanName) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.remove(beanName);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.remove(beanName);
		}
	}

	@Override
	public boolean containsSingleton(String beanName) {
		return (this.singletonObjects.containsKey(beanName));
	}

	@Override
	public String[] getSingletonNames() {
		synchronized (this.singletonObjects) {
			return StringUtils.toStringArray(this.registeredSingletons);
		}
	}

	@Override
	public int getSingletonCount() {
		synchronized (this.singletonObjects) {
			return this.registeredSingletons.size();
		}
	}

	/**
	 * 在创建实例之前
	 * 
	 * @param beanName
	 */
	protected void beforeSingletonCreation(String beanName) {
		// 判断 这个bean name 不是被创建之前的检查排除了的 ，并且 将这个beanname 添加到当前创建中的缓存。
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * 在创建实例之后
	 * 
	 * @param beanName
	 */
	protected void afterSingletonCreation(String beanName) {
		// 判断 这个bean name 不是被创建之前的检查排除了的 ，并且 将这个beanname 到当前创建中的缓存 移除。
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}

	/**
	 * 设置当前创建的bean name
	 * 
	 * @param beanName
	 * @param inCreation
	 */
	public final void setCurrentlyInCreation(String beanName, boolean inCreation) {
		// 检查没通过的
		if (!inCreation) {
			// 当前创建的检查排除的增加
			this.inCreationCheckExclusions.add(beanName);
		} else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	/**
	 * 判断当前创建的是否是Singleton
	 * 
	 * @param beanName
	 * @return
	 */
	public final boolean isSingletonCurrentlyInCreation(String beanName) {
		return this.singletonsCurrentlyInCreation.contains(beanName);
	}

	/**
	 * 注册可以自由使用的bean
	 * 
	 * @param beanName
	 * @param bean
	 */
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * 注册 一个bean 包含下面的子bean
	 * 
	 * @param containedBeanName
	 *            被包含的bean name
	 * @param containingBeanName
	 *            父bean name
	 */
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		synchronized (this.containedBeanMap) {
			Set<String> containedBeans = this.containedBeanMap.get(containingBeanName);
			if (containedBeans == null) {
				containedBeans = new LinkedHashSet<String>(8);
				this.containedBeanMap.put(containingBeanName, containedBeans);
			}
			containedBeans.add(containedBeanName);
		}
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * 注册一个依赖的bean
	 * 
	 * @param beanName
	 * @param dependentBeanName
	 *            被依赖bean name 父bean name
	 */
	public void registerDependentBean(String beanName, String dependentBeanName) {
		String canonicalName = canonicalName(beanName);
		// 注册 依赖的beanName 的 子bean
		synchronized (this.dependentBeanMap) {
			Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
			if (dependentBeans == null) {
				dependentBeans = new LinkedHashSet<String>(8);
				this.dependentBeanMap.put(canonicalName, dependentBeans);
			}
			dependentBeans.add(dependentBeanName);
		}
		// 注册 被beanName 依赖的 父bean
		synchronized (this.dependenciesForBeanMap) {
			Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(dependentBeanName);
			if (dependenciesForBean == null) {
				dependenciesForBean = new LinkedHashSet<String>(8);
				this.dependenciesForBeanMap.put(dependentBeanName, dependenciesForBean);
			}
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * 判断这个bean name是否被依赖了
	 * 
	 * @param beanName
	 * @return
	 */
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * 获取指定的bean 下所有依赖的bean
	 * 
	 * @param beanName
	 * @return
	 */
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		return StringUtils.toStringArray(dependentBeans);
	}

	/**
	 * 获取指定 被指定bean name 所依赖的bean
	 * 
	 * @param beanName
	 * @return
	 */
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			return new String[0];
		}
		return dependenciesForBean.toArray(new String[dependenciesForBean.size()]);
	}

	/**
	 * 销毁单例容器下的所有的bean
	 */
	public void destroySingletons() {
		if (logger.isInfoEnabled()) {
			logger.info("Destroying singletons in " + this);
		}
		synchronized (this.singletonObjects) {
			// 设置当前的bean 在销毁中
			this.singletonsCurrentlyInDestruction = true;
		}

		String[] disposableBeanNames;
		synchronized (this.disposableBeans) {
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			destroySingleton(disposableBeanNames[i]);
		}

		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		synchronized (this.singletonObjects) {
			this.singletonObjects.clear();
			this.singletonFactories.clear();
			this.earlySingletonObjects.clear();
			this.registeredSingletons.clear();
			this.singletonsCurrentlyInDestruction = false;
		}
	}

	/**
	 * 销毁指定的bean name 的实例 <br>
	 * 从容器中移除，destroySingleton，destroyBean两个方法递归调用，即不但要销毁这个bean，
	 * 其对应的DisposableBean,调用destory()方法。而且如果这个类有依赖类，那么还要继续搜索，销毁其依赖类
	 * 
	 * @param beanName
	 */
	public void destroySingleton(String beanName) {
		// 移除这个bean name
		removeSingleton(beanName);

		// 自由的bean 实例
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
		}
		destroyBean(beanName, disposableBean);
	}

	/**
	 * 销毁任意处理的bean(自由bean)
	 * 
	 * @param beanName
	 * @param bean
	 */
	protected void destroyBean(String beanName, DisposableBean bean) {
		Set<String> dependencies = this.dependentBeanMap.remove(beanName);
		if (dependencies != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
			}
			for (String dependentBeanName : dependencies) {
				destroySingleton(dependentBeanName);
			}
		}

		if (bean != null) {
			try {
				bean.destroy();
			} catch (Throwable ex) {
				logger.error("Destroy method on bean with name '" + beanName + "' threw an exception", ex);
			}
		}

		Set<String> containedBeans = this.containedBeanMap.remove(beanName);
		if (containedBeans != null) {
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it
					.hasNext();) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}
		this.dependenciesForBeanMap.remove(beanName);
	}

	/**
	 * 获取单例缓存
	 * 
	 * @return
	 */
	protected final Object getSingletonMutex() {
		return this.singletonObjects;
	}

}
