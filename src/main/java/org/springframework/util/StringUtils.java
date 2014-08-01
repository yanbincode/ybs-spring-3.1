package org.springframework.util;

/**
 * 多方面的string 工具方法
 * 
 * @author yanbin
 * 
 */
public abstract class StringUtils {

	/**
	 * 判断字符串长度是否大于0
	 * 
	 * @param str
	 * @return
	 */
	public static boolean hasLength(String str) {
		return hasLength((CharSequence) str);
	}

	/**
	 * 判断字符序列长度是否大于0
	 * 
	 * @param str
	 * @return
	 */
	public static boolean hasLength(CharSequence str) {
		return (str != null && str.length() > 0);
	}

}
