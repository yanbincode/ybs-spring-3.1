package org.springframework.beans.factory;

import org.springframework.beans.BeansException;

/**
 * bean的最最父的接口，基本接口
 * 
 * @author yanbin
 * 
 */
public interface BeanFactory {

	String FACTORY_BEAN_PREFIX = "&";

	/**
	 * 根据对象名字，获取对象实例
	 * 
	 * @param name
	 * @return
	 * @throws BeansException
	 */
	Object getBean(String name) throws BeansException;

	/**
	 * 根据对象名字，制定类型，返回指定类型对象
	 * 
	 * @param name
	 * @param requiredType
	 * @return
	 * @throws BeansException
	 */
	<T> T getBean(String name, Class<T> requiredType) throws BeansException;

	/**
	 * 根据对象类型，获取指定对象
	 * 
	 * @param requiredType
	 * @return
	 * @throws BeansException
	 */
	<T> T getBean(Class<T> requiredType) throws BeansException;

	/**
	 * 根据名字，获取指定多个对象的匹配
	 * 
	 * @param name
	 * @param args
	 * @return
	 * @throws BeansException
	 */
	Object getBean(String name, Object... args) throws BeansException;

	/**
	 * 判断beanFactory中是否包含指定类名的类对象
	 * 
	 * @param name
	 * @return
	 */
	boolean containsBean(String name);

	/**
	 * 判断beanFactory中指定类名的类对象是否是单例
	 * 
	 * @param name
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 */
	boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 判断beanFactory中指定类名的类对象，是否是其他对象的属性
	 * 
	 * @param name
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 */
	boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 判断beanFactory中指定类名的类对象，是否和指定的目标对象的类型匹配
	 * 
	 * @param name
	 * @param targetType
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 */
	boolean isTypeMatch(String name, Class<?> targetType) throws NoSuchBeanDefinitionException;

	/**
	 * 根据指定名字，获取类对象的类型
	 * 
	 * @param name
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 */
	Class<?> getType(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 获取指定的名字的对象的别名
	 * 
	 * @param name
	 * @return
	 */
	String[] getAliases(String name);

}
