package org.springframework.beans.factory;

/**
 * 分层BeanFactory<br>
 * 父子级联IoC容器接口<br>
 * 主要是提供父 BeanFactory 的功能，通过它能够获取当前 BeanFactory 的父工厂<br>
 * （ PS: 若在 A 工厂启动并加载 Bean 之前， B 工厂先启动并加载了，那 B 就是 A 的父工厂）<br>
 * 这样就能让当前的 BeanFactory 加载父工厂加载的 Bean 了，弥补了 ListableBeanfactory 欠缺的功能。
 * 
 * @author yanbin
 * 
 */
public interface HierarchicalBeanFactory extends BeanFactory {

	/**
	 * 获取返回parent bean factory 或者返回null
	 * 
	 * @return
	 */
	BeanFactory getParentBeanFactory();

	/**
	 * 判断指定的bean name是否在本地的beanfactory中包含
	 * 
	 * @param name
	 * @return
	 */
	boolean containsLocalBean(String name);

}
