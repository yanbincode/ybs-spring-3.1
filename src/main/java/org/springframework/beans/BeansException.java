package org.springframework.beans;

import org.springframework.core.NestedRuntimeException;
import org.springframework.util.ObjectUtils;

/**
 * 是所有在 beans包和子包下抛出的exception的父类，是一个runtime (unchecked) exception
 * 
 * @author yanbin
 * 
 */
public abstract class BeansException extends NestedRuntimeException {

	public BeansException(String msg) {
		super(msg);
	}

	public BeansException(String msg, Throwable cause) {
		super(msg, cause);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BeansException)) {
			return false;
		}
		BeansException otherBe = (BeansException) other;
		return (getMessage().equals(otherBe.getMessage()) && ObjectUtils.nullSafeEquals(getCause(), otherBe.getCause()));
	}

	@Override
	public int hashCode() {
		return getMessage().hashCode();
	}

}
