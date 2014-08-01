package org.springframework.beans.factory.support;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.AliasRegistry;

/**
 * 注册BeanDefinition<br>
 * Spring配置中每一个bean在spring容器里都通过BeanDefinition表示，向容器提供手工注册BeanDefinition对象的方法
 * 
 * @author yanbin
 * 
 */
public interface BeanDefinitionRegistry extends AliasRegistry {

	/**
	 * 根据bean name 注册BeanDefinition
	 * 
	 * @param beanName
	 * @param beanDefinition
	 * @throws BeanDefinitionStoreException
	 */
	void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionStoreException;

	/**
	 * 移除BeanDefinition
	 * 
	 * @param beanName
	 * @throws NoSuchBeanDefinitionException
	 */
	void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 获取BeanDefinition
	 * 
	 * @param beanName
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	boolean containsBeanDefinition(String beanName);

	/**
	 * 获取指定name的所有BeanDefinition
	 * 
	 * @return
	 */
	String[] getBeanDefinitionNames();

	/**
	 * 获取所有BeanDefinition的数量
	 * 
	 * @return
	 */
	int getBeanDefinitionCount();

	/**
	 * 检查beanname 是否在被使用
	 * 
	 * @param beanName
	 * @return
	 */
	boolean isBeanNameInUse(String beanName);

}
