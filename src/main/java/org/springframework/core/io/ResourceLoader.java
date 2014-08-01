package org.springframework.core.io;

import org.springframework.util.ResourceUtils;

/**
 * 为加载资源文件的策略接口 (class path 或者 file system 资源文件) 一个ApplicationContext
 * 必须要提供这个功能，加上继承ResourcePatternResolver的支持
 * 
 * <p>
 * DefaultResourceLoader 是一个 独立的实现 可用在ApplicationContext以为，还可以被ResourceEditor使用
 * <p>
 * 类型Resource的bean属性 和 Resource 数据 能被从strings 迁移
 * 当在ApplicationContext上运行，使用特别的context的resource加载策略
 * 
 * @author yanbin
 * 
 */
public interface ResourceLoader {

	/** 为从class path："classpath:" 加载的伪URL前缀 */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;

	/**
	 * 为指定的resource返回一个Resource的句柄 ， 这个句柄应该总是被resource
	 * descriptor重复使用，总是多个Resource.getInputStream()调用
	 * 
	 * @param location
	 * @return
	 */
	Resource getResource(String location);

	/**
	 * 暴露这个ClassLoader 被 这个ResourceLoader 使用
	 * 
	 * @return
	 */
	ClassLoader getClassLoader();

}
