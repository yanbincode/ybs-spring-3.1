package org.springframework.beans.factory.config;

import org.springframework.beans.factory.ListableBeanFactory;

/**
 * 可配置，可罗列的bean 则实现这个接口。用的不多
 * 
 * @author yanbin
 * 
 */
public interface ConfigurableListableBeanFactory extends ListableBeanFactory, AutowireCapableBeanFactory,
		ConfigurableBeanFactory {

	/**
	 * 忽略指定的类型依赖，来自动装配
	 * 
	 * @param type
	 */
	void ignoreDependencyType(Class<?> type);

	/**
	 * 忽略指定的接口依赖，来自动装配
	 * 
	 * @param ifc
	 */
	void ignoreDependencyInterface(Class<?> ifc);

	/**
	 * 注册一个特殊的依赖，用对应的自动装配类型
	 * 
	 * @param dependencyType
	 * @param autowiredValue
	 */
	void registerResolvableDependency(Class<?> dependencyType, Object autowiredValue);

	/**
	 * 判断指定的bean匹配到其他的bean上面去了。可自动装配候补的资格
	 * 
	 * @param beanName
	 * @param descriptor
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 */
	boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor) throws NoSuchBeanDefinitionException;

	/**
	 * 根据bean name 获取注册的BeanDefinition
	 * 
	 * @param beanName
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 定格，冻结所有的BeanDefinition 。注册的BeanDefinition将不会被修改，并且不会post-processed 任何特征
	 */
	void freezeConfiguration();

	/**
	 * 判断BeanDefinition 是否被冻结/定格了
	 * 
	 * @return
	 */
	boolean isConfigurationFrozen();

	/**
	 * Ensure that all non-lazy-init singletons are instantiated, also
	 * considering {@link org.springframework.beans.factory.FactoryBean
	 * FactoryBeans}. Typically invoked at the end of factory setup, if desired.
	 * 
	 * @throws BeansException
	 *             if one of the singleton beans could not be created. Note:
	 *             This may have left the factory with some beans already
	 *             initialized! Call {@link #destroySingletons()} for full
	 *             cleanup in this case.
	 * @see #destroySingletons()
	 */

	/**
	 * 确保 所有的 non-lazy-init 单例 被实例化 。如果需要的话，通常在工厂的最后调用
	 * 
	 * @throws BeansException
	 */
	void preInstantiateSingletons() throws BeansException;

}
