package org.springframework.context;

import java.util.Locale;

/**
 * 为解析messages的策略接口， 提供这些消息的参数化和国际化的支持
 * 
 * @author yanbin
 * 
 */
public interface MessageSource {

	/**
	 * 
	 * 尝试解析这个message，如果没有message被找到，则返回一个默认的message。
	 * 
	 * @param code
	 * @param args
	 * @param defaultMessage
	 * @param locale
	 * @return
	 */
	String getMessage(String code, Object[] args, String defaultMessage, Locale locale);

	/**
	 * 尝试解析message ，如果消息不能被发现的则抛出错误
	 * 
	 * @param code
	 * @param args
	 * @param locale
	 * @return
	 * @throws NoSuchMessageException
	 */
	String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException;

}
