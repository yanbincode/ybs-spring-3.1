package org.springframework.core;

/**
 * 方便的类为封装 RuntimeException 作为最跟的一个
 * 
 * @author yanbin
 * 
 */
public abstract class NestedRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 3967102570403487086L;

	static {
		// 急切的加载NestedExceptionUtils类,避免在OSGI通道 调用getMessage出现 classloader死锁
		NestedExceptionUtils.class.getName();
	}

	public NestedRuntimeException(String msg) {
		super(msg);
	}

	public NestedRuntimeException(String msg, Throwable cause) {
		super(msg, cause);
	}

	@Override
	public String getMessage() {
		return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
	}

	/**
	 * Retrieve the innermost cause of this exception, if any.
	 * 
	 * @return the innermost exception, or <code>null</code> if none
	 * @since 2.0
	 */

	/**
	 * 检索该异常的根原因
	 * 
	 * @return
	 */
	public Throwable getRootCause() {
		Throwable rootCause = null;
		Throwable cause = getCause();
		while (cause != null && cause != rootCause) {
			rootCause = cause;
			cause = cause.getCause();
		}
		return rootCause;
	}

	/**
	 * 检索最多的指定错误
	 * 
	 * @return
	 */
	public Throwable getMostSpecificCause() {
		Throwable rootCause = getRootCause();
		return (rootCause != null ? rootCause : this);
	}

	/**
	 * 检查这个exception是否包含给定的类型
	 * 
	 * @param exType
	 * @return
	 */
	public boolean contains(Class exType) {
		if (exType == null) {
			return false;
		}
		if (exType.isInstance(this)) {
			return true;
		}
		Throwable cause = getCause();
		if (cause == this) {
			return false;
		}
		if (cause instanceof NestedRuntimeException) {
			return ((NestedRuntimeException) cause).contains(exType);
		} else {
			while (cause != null) {
				if (exType.isInstance(cause)) {
					return true;
				}
				if (cause.getCause() == cause) {
					break;
				}
				cause = cause.getCause();
			}
			return false;
		}
	}

}
