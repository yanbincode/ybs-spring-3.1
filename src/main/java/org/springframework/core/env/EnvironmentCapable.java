package org.springframework.core.env;

/**
 * 接口只是一个组件包含和使用可用的Environment对象
 * 
 * @author yanbin
 * 
 */
public interface EnvironmentCapable {

	/**
	 * 返回Environment为this object
	 * 
	 * @return
	 */
	Environment getEnvironment();

}
