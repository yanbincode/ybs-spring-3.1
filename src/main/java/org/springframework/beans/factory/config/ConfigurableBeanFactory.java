package org.springframework.beans.factory.config;

import java.beans.PropertyEditor;
import java.security.AccessControlContext;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;

/**
 * ConfigurableBeanFactory 就是在 HierarchicalBeanFactory
 * 的基础上增加了可配置的功能，包括注册别名、注册单例等，设置 Classloader 、是否缓存 Bean Metadata 、设置
 * TypeConverter 、 BeanPostProcessor 、配置 Bean 依赖等。
 * 
 * @author yanbin
 * 
 */
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {

	String SCOPE_SINGLETON = "singleton";

	String SCOPE_PROTOTYPE = "prototype";

	/**
	 * 设置父类bean
	 * 
	 * @param parentBeanFactory
	 * @throws IllegalStateException
	 */
	void setParentBeanFactory(BeanFactory parentBeanFactory) throws IllegalStateException;

	/**
	 * 设置bean的classLoader
	 * 
	 * @param beanClassLoader
	 */
	void setBeanClassLoader(ClassLoader beanClassLoader);

	/**
	 * 获取beanClassloader
	 * 
	 * @return
	 */
	ClassLoader getBeanClassLoader();

	/**
	 * 设置临时的ClassLoader
	 * 
	 * @param tempClassLoader
	 */
	void setTempClassLoader(ClassLoader tempClassLoader);

	ClassLoader getTempClassLoader();

	/**
	 * 设置缓存bean 源数据
	 * 
	 * @param cacheBeanMetadata
	 */
	void setCacheBeanMetadata(boolean cacheBeanMetadata);

	/**
	 * 判断是否缓存bean 源数据
	 * 
	 * @return
	 */
	boolean isCacheBeanMetadata();

	/**
	 * bean 表达式的解析器
	 * 
	 * @param resolver
	 */
	void setBeanExpressionResolver(BeanExpressionResolver resolver);

	/**
	 * 获取bean表达式的解析器
	 * 
	 * @return
	 */
	BeanExpressionResolver getBeanExpressionResolver();

	/**
	 * 设置转换服务
	 * 
	 * @param conversionService
	 */
	void setConversionService(ConversionService conversionService);

	/**
	 * 获取转换服务
	 * 
	 * @return
	 */
	ConversionService getConversionService();

	/**
	 * 添加属性编辑注册器
	 * 
	 * @param registrar
	 */
	void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar);

	/**
	 * 注册客户编辑器
	 * 
	 * @param requiredType
	 * @param propertyEditorClass
	 */
	void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass);

	/**
	 * 赋值注册的编辑器到另外一个编辑器
	 * 
	 * @param registry
	 */
	void copyRegisteredEditorsTo(PropertyEditorRegistry registry);

	/**
	 * 设置类型转换
	 * 
	 * @param typeConverter
	 */
	void setTypeConverter(TypeConverter typeConverter);

	/**
	 * 获取类型转换
	 * 
	 * @return
	 */
	TypeConverter getTypeConverter();

	/**
	 * 添加嵌入对象值得转换解析器
	 * 
	 * @param valueResolver
	 */
	void addEmbeddedValueResolver(StringValueResolver valueResolver);

	/**
	 * 解析嵌入对象的值
	 * 
	 * @param value
	 * @return
	 */
	String resolveEmbeddedValue(String value);

	/**
	 * 添加一个信息的BeanPostProcessor
	 * 
	 * @param beanPostProcessor
	 */
	void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

	/**
	 * 获取BeanPostProcessor的个数
	 * 
	 * @return
	 */
	int getBeanPostProcessorCount();

	/**
	 * 注册作用域
	 * 
	 * @param scopeName
	 * @param scope
	 */
	void registerScope(String scopeName, Scope scope);

	/**
	 * 获取所有注册域的名字
	 * 
	 * @return
	 */
	String[] getRegisteredScopeNames();

	/**
	 * 根据名字返回注册的域
	 * 
	 * @param scopeName
	 * @return
	 */
	Scope getRegisteredScope(String scopeName);

	/**
	 * 获取AccessControlContext
	 * 
	 * @return
	 */
	AccessControlContext getAccessControlContext();

	/**
	 * 从另外的 ConfigurableBeanFactory 复制过来
	 * 
	 * @param otherFactory
	 */
	void copyConfigurationFrom(ConfigurableBeanFactory otherFactory);

	/**
	 * 注册一个别名
	 * 
	 * @param beanName
	 * @param alias
	 * @throws BeanDefinitionStoreException
	 */
	void registerAlias(String beanName, String alias) throws BeanDefinitionStoreException;

	/**
	 * 解析所有的别名
	 * 
	 * @param valueResolver
	 */
	void resolveAliases(StringValueResolver valueResolver);

	/**
	 * 根据bean name 返回merged的bean 定义
	 * 
	 * @param beanName
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 */
	BeanDefinition getMergedBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 判断指定的 bean name 是否是 FactoryBean
	 * 
	 * @param name
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 */
	boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException;

	// TODO:
	void setCurrentlyInCreation(String beanName, boolean inCreation);

	boolean isCurrentlyInCreation(String beanName);

	/**
	 * 注册依赖的bean
	 * 
	 * @param beanName
	 * @param dependentBeanName
	 */
	void registerDependentBean(String beanName, String dependentBeanName);

	/**
	 * 获取依赖的beans
	 * 
	 * @param beanName
	 * @return
	 */
	String[] getDependentBeans(String beanName);

	/**
	 * 获取指定beanname的 bean依赖关系
	 * 
	 * @param beanName
	 * @return
	 */
	String[] getDependenciesForBean(String beanName);

	/**
	 * 销毁bean
	 * 
	 * @param beanName
	 * @param beanInstance
	 */
	void destroyBean(String beanName, Object beanInstance);

	/**
	 * 销毁自定义作用域下的bean
	 * 
	 * @param beanName
	 */
	void destroyScopedBean(String beanName);

	/**
	 * 销毁单例的
	 */
	void destroySingletons();

}
