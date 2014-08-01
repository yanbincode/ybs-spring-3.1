package org.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 为方法或者构造器发现参数名称的接口。
 * 
 * @author yanbin
 * 
 */
public interface ParameterNameDiscoverer {

	/**
	 * 为这个方法返回参数名
	 * 
	 * @param method
	 * @return
	 */
	String[] getParameterNames(Method method);

	/**
	 * 为这个构造器返回参数名
	 * 
	 * @param ctor
	 * @return
	 */
	String[] getParameterNames(Constructor ctor);

}
