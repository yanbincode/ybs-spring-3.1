package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.util.StringUtils;

/**
 * 当一个bean 实例化 name 向beanfactory 找不到definition的时，抛出此异常
 * 
 * @author yanbin
 * 
 */
public class NoSuchBeanDefinitionException extends BeansException {

	/** 找不到bean的name */
	private String beanName;

	/** 必须的bean的类型 */
	private Class beanType;

	public NoSuchBeanDefinitionException(String name) {
		super("No bean named '" + name + "' is defined");
		this.beanName = name;
	}

	public NoSuchBeanDefinitionException(String name, String message) {
		super("No bean named '" + name + "' is defined: " + message);
		this.beanName = name;
	}

	public NoSuchBeanDefinitionException(Class type) {
		super("No unique bean of type [" + type.getName() + "] is defined");
		this.beanType = type;
	}

	public NoSuchBeanDefinitionException(Class type, String message) {
		super("No unique bean of type [" + type.getName() + "] is defined: " + message);
		this.beanType = type;
	}

	public NoSuchBeanDefinitionException(Class type, String dependencyDescription, String message) {
		super("No matching bean of type [" + type.getName() + "] found for dependency"
				+ (StringUtils.hasLength(dependencyDescription) ? " [" + dependencyDescription + "]" : "") + ": "
				+ message);
		this.beanType = type;
	}

	public String getBeanName() {
		return this.beanName;
	}

	public Class getBeanType() {
		return this.beanType;
	}

}
