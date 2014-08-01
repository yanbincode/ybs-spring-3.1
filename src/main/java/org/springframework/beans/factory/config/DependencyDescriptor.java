package org.springframework.beans.factory.config;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.Assert;

/**
 * 依赖描述器，描述其为了一个指定的即将注入的依赖。包装构造的参数，方法参数或是一个字段。实现Serializable，可持久化
 * 
 * @author yanbin
 * 
 */
public class DependencyDescriptor implements Serializable {

	/** transient：不被持久化，方法参数 */
	private transient MethodParameter methodParameter;

	/** 属性 */
	private transient Field field;

	/** 申明的类 */
	private Class declaringClass;

	/** 方法名 */
	private String methodName;

	/** 参数类型 */
	private Class[] parameterTypes;

	/** 参数索引 */
	private int parameterIndex;

	/** 属性名 */
	private String fieldName;

	/** 是否必要 */
	private final boolean required;

	/** 是否急切加载 */
	private final boolean eager;

	/** 嵌套的层次 */
	private int nestingLevel = 1;

	/** 字段的注解 */
	private transient Annotation[] fieldAnnotations;

	/**
	 * 为构造方法或方法的参数创建一个Descriptor，考虑这个依赖是'eager'的
	 * 
	 * @param methodParameter
	 * @param required
	 */
	public DependencyDescriptor(MethodParameter methodParameter, boolean required) {
		this(methodParameter, required, true);
	}

	public DependencyDescriptor(MethodParameter methodParameter, boolean required, boolean eager) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		this.methodParameter = methodParameter;
		this.declaringClass = methodParameter.getDeclaringClass();
		// 如果方法参数为空则参数类型指定的是构造方法的类型，不为空则取传入的方法参数的相关内容
		if (this.methodParameter.getMethod() != null) {
			this.methodName = methodParameter.getMethod().getName();
			this.parameterTypes = methodParameter.getMethod().getParameterTypes();
		} else {
			this.parameterTypes = methodParameter.getConstructor().getParameterTypes();
		}
		this.parameterIndex = methodParameter.getParameterIndex();
		this.required = required;
		this.eager = eager;
	}

	/**
	 * 为属性创建Descriptor，指定依赖为'eager'的
	 * 
	 * @param field
	 * @param required
	 */
	public DependencyDescriptor(Field field, boolean required) {
		this(field, required, true);
	}

	public DependencyDescriptor(Field field, boolean required, boolean eager) {
		Assert.notNull(field, "Field must not be null");
		this.field = field;
		this.declaringClass = field.getDeclaringClass();
		this.fieldName = field.getName();
		this.required = required;
		this.eager = eager;
	}

	/**
	 * copy的构造
	 * 
	 * @param original
	 */
	public DependencyDescriptor(DependencyDescriptor original) {
		this.methodParameter = (original.methodParameter != null ? new MethodParameter(original.methodParameter) : null);
		this.field = original.field;
		this.declaringClass = original.declaringClass;
		this.methodName = original.methodName;
		this.parameterTypes = original.parameterTypes;
		this.parameterIndex = original.parameterIndex;
		this.fieldName = original.fieldName;
		this.required = original.required;
		this.eager = original.eager;
		this.nestingLevel = original.nestingLevel;
		this.fieldAnnotations = original.fieldAnnotations;
	}

	public MethodParameter getMethodParameter() {
		return this.methodParameter;
	}

	public Field getField() {
		return this.field;
	}

	public boolean isRequired() {
		return this.required;
	}

	public boolean isEager() {
		return this.eager;
	}

	/**
	 * 增加这个描述器的嵌套层次
	 */
	public void increaseNestingLevel() {
		this.nestingLevel++;
		if (this.methodParameter != null) {
			this.methodParameter.increaseNestingLevel();
		}
	}

	/**
	 * 初始化参数name发现 为潜在的方法参数。 这个方法实际不尝试在这个点上检索参数名，他只是允许当在application call的时候发生
	 * 
	 * @param parameterNameDiscoverer
	 */
	public void initParameterNameDiscovery(ParameterNameDiscoverer parameterNameDiscoverer) {
		if (this.methodParameter != null) {
			this.methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
		}
	}

	/**
	 * 确定包装的field/Parameter的名称
	 * 
	 * @return
	 */
	public String getDependencyName() {
		return (this.field != null ? this.field.getName() : this.methodParameter.getParameterName());
	}

	/**
	 * 确定包装的field/Parameter 生命的类型（非泛型）
	 * 
	 * @return
	 */
	public Class<?> getDependencyType() {
		if (this.field != null) {
			if (this.nestingLevel > 1) {
				Type type = this.field.getGenericType();
				if (type instanceof ParameterizedType) {
					Type arg = ((ParameterizedType) type).getActualTypeArguments()[0];
					if (arg instanceof Class) {
						return (Class) arg;
					} else if (arg instanceof ParameterizedType) {
						arg = ((ParameterizedType) arg).getRawType();
						if (arg instanceof Class) {
							return (Class) arg;
						}
					}
				}
				return Object.class;
			} else {
				return this.field.getType();
			}
		} else {
			return this.methodParameter.getNestedParameterType();
		}
	}

	/**
	 * 确定包装的集合中泛型的元素类型
	 * 
	 * @return
	 */
	public Class<?> getCollectionType() {
		return (this.field != null ? GenericCollectionTypeResolver
				.getCollectionFieldType(this.field, this.nestingLevel) : GenericCollectionTypeResolver
				.getCollectionParameterType(this.methodParameter));
	}

	/**
	 * 确定包装的map parameter/field 的 泛型key类型
	 * 
	 * @return
	 */
	public Class<?> getMapKeyType() {
		return (this.field != null ? GenericCollectionTypeResolver.getMapKeyFieldType(this.field, this.nestingLevel)
				: GenericCollectionTypeResolver.getMapKeyParameterType(this.methodParameter));
	}

	/**
	 * 确定包装的map parameter/field 的 泛型value类型
	 * 
	 * @return
	 */
	public Class<?> getMapValueType() {
		return (this.field != null ? GenericCollectionTypeResolver.getMapValueFieldType(this.field, this.nestingLevel)
				: GenericCollectionTypeResolver.getMapValueParameterType(this.methodParameter));
	}

	/**
	 * 获得包装的parameter/field 相关的注解
	 * 
	 * @return
	 */
	public Annotation[] getAnnotations() {
		if (this.field != null) {
			if (this.fieldAnnotations == null) {
				this.fieldAnnotations = this.field.getAnnotations();
			}
			return this.fieldAnnotations;
		} else {
			return this.methodParameter.getParameterAnnotations();
		}
	}

	// ---------------------------------------------------------------------
	// 序列化支持
	// ---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依赖于默认的序列化，在序列化后，只是初始化了状态
		ois.defaultReadObject();

		// 恢复反射处理（遗憾的是不能序列化）
		try {
			if (this.fieldName != null) {
				this.field = this.declaringClass.getDeclaredField(this.fieldName);
			} else {
				if (this.methodName != null) {
					this.methodParameter = new MethodParameter(this.declaringClass.getDeclaredMethod(this.methodName,
							this.parameterTypes), this.parameterIndex);
				} else {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredConstructor(this.parameterTypes), this.parameterIndex);
				}
				for (int i = 1; i < this.nestingLevel; i++) {
					this.methodParameter.increaseNestingLevel();
				}
			}
		} catch (Throwable ex) {
			throw new IllegalStateException("Could not find original class structure", ex);
		}
	}

}
