package org.springframework.beans.factory.support;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;

/**
 * 对 FactoryBean 的支持， 获取 FactoryBean 、 FactoryBean 的类型、获取 FactoryBean
 * 曝露的目标对象等，而这些功能都是基于附接口对 Bean 的注册功能的<br>
 * 
 * @author yanbin
 * 
 */
public abstract class FactoryBeanRegistrySupport extends DefaultSingletonBeanRegistry {

	/** 缓存被FactoryBeans 创建的单例对象： FactoryBean name --> object */
	private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<String, Object>();

	/**
	 * 判定factoryBean的类型，返回Object的类型
	 * 
	 * @param factoryBean
	 * @return
	 */
	protected Class getTypeForFactoryBean(final FactoryBean factoryBean) {
		try {
			// 获取系统安全接口
			if (System.getSecurityManager() != null) {
				// TODO：内部类干什么用？
				return AccessController.doPrivileged(new PrivilegedAction<Class>() {
					public Class run() {
						return factoryBean.getObjectType();
					}
				}, getAccessControlContext());
			} else {
				return factoryBean.getObjectType();
			}
		} catch (Throwable ex) {
			logger.warn("FactoryBean threw exception from getObjectType, despite the contract saying "
					+ "that it should return null if the type of its object cannot be determined yet", ex);
			return null;
		}
	}

	/**
	 * 根据FactoryBean 的 beanName 获取Object
	 * 
	 * @param beanName
	 * @return
	 */
	protected Object getCachedObjectForFactoryBean(String beanName) {
		Object object = this.factoryBeanObjectCache.get(beanName);
		return (object != NULL_OBJECT ? object : null);
	}

	/**
	 * 从FactoryBean中获取Object
	 * 
	 * @param factory
	 * @param beanName
	 * @param shouldPostProcess
	 *            bean是否是后置处理后的subject
	 * @return
	 */
	protected Object getObjectFromFactoryBean(FactoryBean factory, String beanName, boolean shouldPostProcess) {
		// 判断这个factory 是不是单例 且包含这个beanname
		if (factory.isSingleton() && containsSingleton(beanName)) {
			// 同步单例对象缓存
			synchronized (getSingletonMutex()) {
				Object object = this.factoryBeanObjectCache.get(beanName);
				if (object == null) {
					object = doGetObjectFromFactoryBean(factory, beanName, shouldPostProcess);
					this.factoryBeanObjectCache.put(beanName, (object != null ? object : NULL_OBJECT));
				}
				return (object != NULL_OBJECT ? object : null);
			}
		} else {
			return doGetObjectFromFactoryBean(factory, beanName, shouldPostProcess);
		}
	}

	/**
	 * 从FactoryBean中获取Object 具体的动作
	 * 
	 * @param factory
	 * @param beanName
	 * @param shouldPostProcess
	 * @return
	 * @throws BeanCreationException
	 */
	private Object doGetObjectFromFactoryBean(final FactoryBean factory, final String beanName,
			final boolean shouldPostProcess) throws BeanCreationException {
		Object object;
		try {
			if (System.getSecurityManager() != null) {
				AccessControlContext acc = getAccessControlContext();
				try {
					object = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
						public Object run() throws Exception {
							return factory.getObject();
						}
					}, acc);
				} catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			} else {
				object = factory.getObject();
			}
		} catch (FactoryBeanNotInitializedException ex) {
			throw new BeanCurrentlyInCreationException(beanName, ex.toString());
		} catch (Throwable ex) {
			throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
		}

		// 判断获取到的object为null 且 这个bean当前正在创建，则抛出异常
		if (object == null && isSingletonCurrentlyInCreation(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName,
					"FactoryBean which is currently in creation returned null from getObject");
		}

		// object不为null 且需要后置处理 ，则调用后置处理
		if (object != null && shouldPostProcess) {
			try {
				object = postProcessObjectFromFactoryBean(object, beanName);
			} catch (Throwable ex) {
				throw new BeanCreationException(beanName, "Post-processing of the FactoryBean's object failed", ex);
			}
		}

		return object;
	}

	/**
	 * TODO： Post-process the given object that has been obtained from the
	 * FactoryBean. The resulting object will get exposed for bean references.
	 * <p>
	 * The default implementation simply returns the given object as-is.
	 * Subclasses may override this, for example, to apply post-processors.
	 * 
	 * @param object
	 * @param beanName
	 * @return
	 * @throws BeansException
	 */
	protected Object postProcessObjectFromFactoryBean(Object object, String beanName) throws BeansException {
		return object;
	}

	/**
	 * 根据 FactoryBean类型的实例对象获取 FactoryBean。就是做了个强转
	 * 
	 * @param beanName
	 * @param beanInstance
	 * @return
	 * @throws BeansException
	 */
	protected FactoryBean getFactoryBean(String beanName, Object beanInstance) throws BeansException {
		if (!(beanInstance instanceof FactoryBean)) {
			throw new BeanCreationException(beanName, "Bean instance of type [" + beanInstance.getClass()
					+ "] is not a FactoryBean");
		}
		return (FactoryBean) beanInstance;
	}

	@Override
	protected void removeSingleton(String beanName) {
		super.removeSingleton(beanName);
		// 同时移除factoryBean中的缓存
		this.factoryBeanObjectCache.remove(beanName);
	}

	/**
	 * 为当前的beanFactory 返回一个安全的 context
	 * 
	 * @return
	 */
	protected AccessControlContext getAccessControlContext() {
		return AccessController.getContext();
	}

}
