package org.springframework.core;

/**
 * 统一的接口管理别名，提供集成实现
 * 
 * @author yanbin
 * 
 */
public interface AliasRegistry {

	/**
	 * 注册一个别名
	 * 
	 * @param name
	 * @param alias
	 */
	void registerAlias(String name, String alias);

	/**
	 * 移除一个别名
	 * 
	 * @param alias
	 */
	void removeAlias(String alias);

	/**
	 * 判断bean name 是否是一个别名
	 * 
	 * @param beanName
	 * @return
	 */
	boolean isAlias(String beanName);

	/**
	 * 根据bean name 获取所有bean的别名
	 * 
	 * @param name
	 * @return
	 */
	String[] getAliases(String name);

}
