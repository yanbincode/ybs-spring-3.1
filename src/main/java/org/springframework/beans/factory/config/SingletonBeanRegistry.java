package org.springframework.beans.factory.config;

/**
 * 用来定义为用来共享的 bean 实例的注册表，通过它可以使得 BeanFactory 实现统一的方式暴露其单例 bean 管理<br>
 * 允许在运行期间想容器中注册单实例bean
 * 
 * @author yanbin
 * 
 */
public interface SingletonBeanRegistry {

	/**
	 * 注册单例
	 * 
	 * @param beanName
	 * @param singletonObject
	 */
	void registerSingleton(String beanName, Object singletonObject);

	/**
	 * 获取单例的对象
	 * 
	 * @param beanName
	 * @return
	 */
	Object getSingleton(String beanName);

	/**
	 * 这个对象是否包含单例
	 * 
	 * @param beanName
	 * @return
	 */
	boolean containsSingleton(String beanName);

	/**
	 * 获取所有单例的对象name
	 * 
	 * @return
	 */
	String[] getSingletonNames();

	/**
	 * 获取单例对象的总体个数
	 * 
	 * @return
	 */
	int getSingletonCount();

}
