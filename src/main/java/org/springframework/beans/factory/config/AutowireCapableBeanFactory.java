package org.springframework.beans.factory.config;

import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;

/**
 * 主要是提供自动 Bean 自动绑定 ( 或者说自动装配 ) 功能。<br>
 * 例如根据自动装配策略 new 一个 bean ，为已有的 bean 装配属性依赖；还有创建 bean 之后的回调功能，为 bean 设置 name 、
 * bean factory 、 bean post processor 等；将 bean post processor 应用到 bean 的初始化，等等
 * 
 * @author yanbin
 * 
 */
public interface AutowireCapableBeanFactory extends BeanFactory {

	int AUTOWIRE_NO = 0;

	int AUTOWIRE_BY_NAME = 1;

	int AUTOWIRE_BY_TYPE = 2;

	int AUTOWIRE_CONSTRUCTOR = 3;

	@Deprecated
	int AUTOWIRE_AUTODETECT = 4;

	/**
	 * 根据bean class 创建bean
	 * 
	 * @param beanClass
	 * @return
	 * @throws BeansException
	 */
	<T> T createBean(Class<T> beanClass) throws BeansException;

	/**
	 * 自动装配已经存在的bean
	 * 
	 * @param existingBean
	 * @throws BeansException
	 */
	void autowireBean(Object existingBean) throws BeansException;

	/**
	 * 配置已经存在的bean，根据beanname
	 * 
	 * @param existingBean
	 * @param beanName
	 * @return
	 * @throws BeansException
	 */
	Object configureBean(Object existingBean, String beanName) throws BeansException;

	// TODO:从属关系
	Object resolveDependency(DependencyDescriptor descriptor, String beanName) throws BeansException;

	/**
	 * 根据bean类型 和 自动装配的模式， 依赖check来创建 对象
	 * 
	 * @param beanClass
	 * @param autowireMode
	 * @param dependencyCheck
	 * @return
	 * @throws BeansException
	 */
	Object createBean(Class beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

	/**
	 * 自动装配
	 * 
	 * @param beanClass
	 * @param autowireMode
	 * @param dependencyCheck
	 * @return
	 * @throws BeansException
	 */
	Object autowire(Class beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

	/**
	 * 自动装配bean属性
	 * 
	 * @param existingBean
	 * @param autowireMode
	 * @param dependencyCheck
	 * @throws BeansException
	 */
	void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck) throws BeansException;

	/**
	 * 应用bean的属性值
	 * 
	 * @param existingBean
	 * @param beanName
	 * @throws BeansException
	 */
	void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException;

	/**
	 * 初始化bean
	 * 
	 * @param existingBean
	 * @param beanName
	 * @return
	 * @throws BeansException
	 */
	Object initializeBean(Object existingBean, String beanName) throws BeansException;

	/**
	 * 应用bean的后处理器，在初始化之前
	 * 
	 * @param existingBean
	 * @param beanName
	 * @return
	 * @throws BeansException
	 */
	Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws BeansException;

	/**
	 * 应用bean的后处理器，在初始化之后
	 * 
	 * @param existingBean
	 * @param beanName
	 * @return
	 * @throws BeansException
	 */
	Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) throws BeansException;

	/**
	 * 解析依赖描述关系
	 * 
	 * @param descriptor
	 * @param beanName
	 * @param autowiredBeanNames
	 * @param typeConverter
	 * @return
	 * @throws BeansException
	 */
	Object resolveDependency(DependencyDescriptor descriptor, String beanName, Set<String> autowiredBeanNames,
			TypeConverter typeConverter) throws BeansException;

}
