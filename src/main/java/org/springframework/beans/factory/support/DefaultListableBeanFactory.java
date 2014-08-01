package org.springframework.beans.factory.support;

import java.io.Serializable;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * ListableBeanFactory和BeanDefinitionRegistry接口的默认实现：基于bean definition
 * objects上的一个成熟的bean factory
 * 
 * <p>
 * 
 * 
 * @author yanbin
 * 
 */
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements
		ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {

}
