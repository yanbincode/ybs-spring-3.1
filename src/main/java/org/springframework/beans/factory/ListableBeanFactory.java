package org.springframework.beans.factory;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.beans.BeansException;

/**
 * 罗列beanFactory下的bean列表<br>
 * 能够列举当前 BeanFactory 加载的所有 Bean<br>
 * 列举所有 Bean 的名字或者满足某种类型的 Bean 的名字，根据类型返回所有 Bean 对象 , 等。<br>
 * 但是它无法获取上层 BeanFactory 加载的单例 Bean 。
 * 
 * @author yanbin
 * 
 */
public interface ListableBeanFactory extends BeanFactory {

	/**
	 * 判断指定名字的bean是否包含bean的定义
	 * 
	 * @param beanName
	 * @return
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * 返回beanFactory 定义bean的个数
	 * 
	 * @return
	 */
	int getBeanDefinitionCount();

	/**
	 * 获取所有定义的bean的name
	 * 
	 * @return
	 */
	String[] getBeanDefinitionNames();

	/**
	 * 获取指定类型的所有bean的name
	 * 
	 * @param type
	 * @return
	 */
	String[] getBeanNamesForType(Class<?> type);

	/**
	 * 获取指定类型所有bean的name
	 * 
	 * @param type
	 *            类型
	 * @param includeNonSingletons
	 *            是否包含非单例
	 * @param allowEagerInit
	 *            是否允许渴望初始化的
	 * @return
	 */
	String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit);

	/**
	 * 返回指定类型的bean的实例，map中保存name和实例
	 * 
	 * @param type
	 * @return
	 * @throws BeansException
	 */
	<T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException;

	/**
	 * 返回指定类型的bean的实例，map中保存name和实例
	 * 
	 * @param type
	 * @param includeNonSingletons
	 *            是否包含非单例
	 * @param allowEagerInit
	 *            是否允许渴望初始化的
	 * @return
	 * @throws BeansException
	 */
	<T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException;

	/**
	 * 获取所有类标记了 指定注解的类的实例 ，map中保存name和实例
	 * 
	 * @param annotationType
	 * @return
	 * @throws BeansException
	 */
	Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException;

	/**
	 * 指定bean的名字，获取bean上的注解类
	 * 
	 * @param beanName
	 * @param annotationType
	 * @return
	 */
	<A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType);

}
