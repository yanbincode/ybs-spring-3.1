package org.springframework.context;

import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * 中心interface 为一个应用application 提供配置。当这个应用在跑，这个配置是只读的。不过也可以重新载入如果他的实现支持的话<br>
 * <p>
 * 一个ApplicationContext支持如下：
 * <ul>
 * <li>提供bean factory 方法 为 访问应用components ，继承ListableBeanFactory
 * <li>提供一个通用的方式加载 文件resources （配置文件） 的能力 ，从ResourceLoader继承
 * <li>提供发布监事件注册监听器的能力，继承自ApplicationEventPublisher
 * <li>提供解析messages ，支持国际化的能力，继承自MessageSource
 * <li>继承父context。定义在子context永远是优先权。意味着，举个例子，一个单独的父context能被用在整个web
 * application，虽然每个servlet有他自己的子context，但是和其他的servlet独立的
 * </ul>
 * 
 * <p>
 * 除了针对标准的BeanFactory 生命周期的能力，ApplicationContext 实现检测和调用ApplicationContextAware
 * bean 以及ResourceLoaderAware、ApplicationEventPublisherAware和MessageSourceAware
 * beans
 * 
 * @author yanbin
 * 
 */
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory,
		MessageSource, ApplicationEventPublisher, ResourcePatternResolver {

	/**
	 * 返回这个应用上下文唯一的id
	 * 
	 * @return
	 */
	String getId();

	/**
	 * 为这个context返回一个友好的名称
	 * 
	 * @return
	 */
	String getDisplayName();

	/**
	 * 当这个context被第一次加载的，返回这个加载时间
	 * 
	 * @return
	 */
	long getStartupDate();

	/**
	 * 返回ApplicationContext父应用上下文 或者返回 null
	 * 
	 * @return
	 */
	ApplicationContext getParent();

	/**
	 * 为这个context暴露AutowireCapableBeanFactory的方法功能。
	 * 
	 * @return
	 * @throws IllegalStateException
	 */
	AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;

}
