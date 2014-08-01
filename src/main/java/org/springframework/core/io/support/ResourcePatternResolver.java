package org.springframework.core.io.support;

import java.io.IOException;

import org.springframework.core.io.Resource;

/**
 * 为解析一个本地的格式（比如 ant 样式的路径格式）到Resource 对象里的策略接口
 * 
 * <p>
 * 这是ResourceLoader的一个扩展接口，一个通过的ResourceLoader能被checked他是否也实现继承interface
 * 
 * <p>
 * PathMatchingResourcePatternResolver是一个独立的实现，能被ApplicationContext以外的，
 * 也能被ResourceArrayPropertyEditor使用，为填充Resource数组
 * 
 * <p>
 * 能被用来本地的模式的任何排序。输入模式必须匹配策略的实现。这个接口只是指定转换方法，二不是一个具体的格式模式
 * 
 * <p>
 * 这个接口也建议一个新的资源前缀"classpath*:" 为所有匹配的resources 从class path中。
 * 
 * 
 * @author yanbin
 * 
 */
public interface ResourcePatternResolver {

	/**
	 * 为从class path来的所有匹配的resources作为一个伪前缀
	 */
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

	/**
	 * 解析给定的本地模式 到 Resource 对象中
	 * 
	 * @param locationPattern
	 * @return
	 * @throws IOException
	 */
	Resource[] getResources(String locationPattern) throws IOException;

}
