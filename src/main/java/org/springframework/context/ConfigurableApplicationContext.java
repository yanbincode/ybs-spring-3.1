package org.springframework.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * 单个程序启动(SPI)接口，来被大多数的application contexts 实现，提供工具为配置一个application
 * context，在ApplicationContext接口中增加application context的客户方法。
 * 
 * <p>
 * 配置和生命周期方法被封装在这里。为了在客户ApplicationContext明显，目前的方法只能用在startup and shutdown code
 * 
 * @author yanbin
 * 
 */
public interface ConfigurableApplicationContext extends ApplicationContext, Lifecycle {

	/** 任何数量的这些字符是被考虑分割 多个 上下文配置路径，在一个字符串中 */
	String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

	/** 在工厂中 转换服务的名字。如果没有提供，则用默认的应用转换规则 */
	String CONVERSION_SERVICE_BEAN_NAME = "conversionService";

	/** LoadTimeWeaver bean 在factory中的name */
	String LOAD_TIME_WEAVER_BEAN_NAME = "loadTimeWeaver";

	/** Environment 的name */
	String ENVIRONMENT_BEAN_NAME = "environment";

	/** System properties bean 的name */
	String SYSTEM_PROPERTIES_BEAN_NAME = "systemProperties";

	/** System environment 的 name */
	String SYSTEM_ENVIRONMENT_BEAN_NAME = "systemEnvironment";

	/**
	 * 设置ApplicationContext 的唯一id
	 * 
	 * @param id
	 */
	void setId(String id);

	/**
	 * 设置parent applicationcontext
	 * 
	 * @param parent
	 */
	void setParent(ApplicationContext parent);

	/**
	 * 返回一个application context 的配置环境
	 */
	ConfigurableEnvironment getEnvironment();

	void setEnvironment(ConfigurableEnvironment environment);

	/**
	 * 添加一个新的BeanFactoryPostProcessor 能获取应用上下文内部的的bean 工厂 在应用上下文refresh。在任何bean
	 * definitions获取值之前。在context配置期间被调用
	 * 
	 * @param beanFactoryPostProcessor
	 */
	void addBeanFactoryPostProcessor(BeanFactoryPostProcessor beanFactoryPostProcessor);

	/**
	 * 添加一个新的ApplicationListener
	 * 
	 * @param listener
	 */
	void addApplicationListener(ApplicationListener<?> listener);

	/**
	 * 加载或刷新配置的持续表现，也许是xml 文件，properties文件 或者 关系数据库schema
	 * <p>
	 * 这是一个startup方法，他应该销毁所有已经创建的singletons。在调用这个方法之后，所有的singletons都应该被实例化了
	 * 
	 * @throws BeansException
	 * @throws IllegalStateException
	 */
	void refresh() throws BeansException, IllegalStateException;

	/**
	 * 注册一个关闭JVM runtime时的钩子，在JVM 关闭的时关闭这个上下文，除非这个上下文已经关闭了。
	 */
	void registerShutdownHook();

	/**
	 * 关闭这个application context，释放所有的那些实现在占用的资源和锁，这些包括正在销毁的所有的singleton beans的缓存
	 */
	void close();

	/**
	 * 判断应用上下文是否有效的，在最后一次被刷新和还没有被关闭之间是有效的
	 * 
	 * @return
	 */
	boolean isActive();

	/**
	 * 返回应用上下文内部的bean factory 。 能被使用来访问基础的factory的具体函数
	 * <p>
	 * 注意：不要用这个去 后处理 bean factory ； singletons
	 * 在之前已经被实例化了。在bean被触及之前，用一个BeanFactoryPostProcessor去拦截BeanFactory的安装过程
	 * 
	 * @return
	 * @throws IllegalStateException
	 */
	ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

}
