package org.springframework.core;

/**
 * exception的帮助类
 * 
 * @author yanbin
 * 
 */
public abstract class NestedExceptionUtils {

	/**
	 * build message
	 * 
	 * @param message
	 * @param cause
	 * @return
	 */
	public static String buildMessage(String message, Throwable cause) {
		if (cause != null) {
			StringBuilder sb = new StringBuilder();
			if (message != null) {
				sb.append(message).append("; ");
			}
			sb.append("nested exception is ").append(cause);
			return sb.toString();
		} else {
			return message;
		}
	}

}
