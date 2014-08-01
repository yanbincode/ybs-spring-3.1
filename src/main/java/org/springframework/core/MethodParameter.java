package org.springframework.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * 方法参数的包装规范的帮助类
 * 
 * @author yanbin
 * 
 */
public class MethodParameter {

	private final Method method;

	private final Constructor constructor;

	private final int parameterIndex;

	private Class<?> parameterType;

	private Type genericParameterType;

	private Annotation[] parameterAnnotations;

	private ParameterNameDiscoverer parameterNameDiscoverer;

	private String parameterName;

	private int nestingLevel = 1;

	/** map 存放嵌套层次的索引 */
	Map<Integer, Integer> typeIndexesPerLevel;

	Map<TypeVariable, Type> typeVariableMap;

	private int hash = 0;

	/**
	 * 创建一个MethodParameter 为指定的method ， nestingLevel 为1
	 * 
	 * @param method
	 * @param parameterIndex
	 */
	public MethodParameter(Method method, int parameterIndex) {
		this(method, parameterIndex, 1);
	}

	public MethodParameter(Method method, int parameterIndex, int nestingLevel) {
		Assert.notNull(method, "Method must not be null");
		this.method = method;
		this.parameterIndex = parameterIndex;
		this.nestingLevel = nestingLevel;
		this.constructor = null;
	}

	/**
	 * 构造器
	 * 
	 * @param constructor
	 * @param parameterIndex
	 */
	public MethodParameter(Constructor constructor, int parameterIndex) {
		this(constructor, parameterIndex, 1);
	}

	public MethodParameter(Constructor constructor, int parameterIndex, int nestingLevel) {
		Assert.notNull(constructor, "Constructor must not be null");
		this.constructor = constructor;
		this.parameterIndex = parameterIndex;
		this.nestingLevel = nestingLevel;
		this.method = null;
	}

	/**
	 * 复制构造方法
	 * 
	 * @param original
	 */
	public MethodParameter(MethodParameter original) {
		Assert.notNull(original, "Original must not be null");
		this.method = original.method;
		this.constructor = original.constructor;
		this.parameterIndex = original.parameterIndex;
		this.parameterType = original.parameterType;
		this.genericParameterType = original.genericParameterType;
		this.parameterAnnotations = original.parameterAnnotations;
		this.parameterNameDiscoverer = original.parameterNameDiscoverer;
		this.parameterName = original.parameterName;
		this.nestingLevel = original.nestingLevel;
		this.typeIndexesPerLevel = original.typeIndexesPerLevel;
		this.typeVariableMap = original.typeVariableMap;
		this.hash = original.hash;
	}

	public Method getMethod() {
		return this.method;
	}

	public Constructor getConstructor() {
		return this.constructor;
	}

	/**
	 * 返回一个包装的member
	 * 
	 * @return
	 */
	private Member getMember() {
		return this.method != null ? this.method : this.constructor;
	}

	/**
	 * 返回一个注解元素
	 * 
	 * @return
	 */
	private AnnotatedElement getAnnotatedElement() {
		return this.method != null ? this.method : this.constructor;
	}

	/**
	 * 潜在的方法和构造器 获取 生命的类
	 * 
	 * @return
	 */
	public Class getDeclaringClass() {
		return getMember().getDeclaringClass();
	}

	public int getParameterIndex() {
		return this.parameterIndex;
	}

	void setParameterType(Class<?> parameterType) {
		this.parameterType = parameterType;
	}

	/**
	 * 返回构造方法或方法的参数类型
	 * 
	 * @return
	 */
	public Class<?> getParameterType() {
		if (this.parameterType == null) {
			if (this.parameterIndex < 0) {
				this.parameterType = (this.method != null ? this.method.getReturnType() : null);
			} else {
				this.parameterType = (this.method != null ? this.method.getParameterTypes()[this.parameterIndex]
						: this.constructor.getParameterTypes()[this.parameterIndex]);
			}
		}
		return this.parameterType;
	}

	/**
	 * 返回参数的泛型的类型
	 * 
	 * @return
	 */
	public Type getGenericParameterType() {
		if (this.genericParameterType == null) {
			if (this.parameterIndex < 0) {
				this.genericParameterType = (this.method != null ? this.method.getGenericReturnType() : null);
			} else {
				this.genericParameterType = (this.method != null ? this.method.getGenericParameterTypes()[this.parameterIndex]
						: this.constructor.getGenericParameterTypes()[this.parameterIndex]);
			}
		}
		return this.genericParameterType;
	}

	/**
	 * 获取嵌套的参数类型
	 * 
	 * @return
	 */
	public Class<?> getNestedParameterType() {
		if (this.nestingLevel > 1) {
			Type type = getGenericParameterType();
			if (type instanceof ParameterizedType) {
				Integer index = getTypeIndexForCurrentLevel();
				Type arg = ((ParameterizedType) type).getActualTypeArguments()[index != null ? index : 0];
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
			return getParameterType();
		}
	}

	/**
	 * 获取方法的所有注解
	 * 
	 * @return
	 */
	public Annotation[] getMethodAnnotations() {
		return getAnnotatedElement().getAnnotations();
	}

	/**
	 * 返回给定类型的annotation
	 * 
	 * @param annotationType
	 * @return
	 */
	public <T extends Annotation> T getMethodAnnotation(Class<T> annotationType) {
		return getAnnotatedElement().getAnnotation(annotationType);
	}

	/**
	 * 返回指定的method/constructor关联的annotation
	 * 
	 * @return
	 */
	public Annotation[] getParameterAnnotations() {
		if (this.parameterAnnotations == null) {
			Annotation[][] annotationArray = (this.method != null ? this.method.getParameterAnnotations()
					: this.constructor.getParameterAnnotations());
			if (this.parameterIndex >= 0 && this.parameterIndex < annotationArray.length) {
				this.parameterAnnotations = annotationArray[this.parameterIndex];
			} else {
				this.parameterAnnotations = new Annotation[0];
			}
		}
		return this.parameterAnnotations;
	}

	/**
	 * 返回给定类型的参数注解
	 * 
	 * @param annotationType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getParameterAnnotation(Class<T> annotationType) {
		Annotation[] anns = getParameterAnnotations();
		for (Annotation ann : anns) {
			if (annotationType.isInstance(ann)) {
				return (T) ann;
			}
		}
		return null;
	}

	/**
	 * 判断参数是否存在注解
	 * 
	 * @return
	 */
	public boolean hasParameterAnnotations() {
		return (getParameterAnnotations().length != 0);
	}

	/**
	 * 判断参数注解中是否存在指定的annotationType类型
	 * 
	 * @param annotationType
	 * @return
	 */
	public <T extends Annotation> boolean hasParameterAnnotation(Class<T> annotationType) {
		return (getParameterAnnotation(annotationType) != null);
	}

	/**
	 * 初始化参数名称discovery 为 方法参数
	 * 
	 * @param parameterNameDiscoverer
	 */
	public void initParameterNameDiscovery(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * 返回method/constructor 的 parameter的name
	 * 
	 * @return
	 */
	public String getParameterName() {
		if (this.parameterNameDiscoverer != null) {
			String[] parameterNames = (this.method != null ? this.parameterNameDiscoverer
					.getParameterNames(this.method) : this.parameterNameDiscoverer.getParameterNames(this.constructor));
			if (parameterNames != null) {
				this.parameterName = parameterNames[this.parameterIndex];
			}
			this.parameterNameDiscoverer = null;
		}
		return this.parameterName;
	}

	/**
	 * 增加parameter的嵌套层级
	 */
	public void increaseNestingLevel() {
		this.nestingLevel++;
	}

	/**
	 * 减少parameter的嵌套层级
	 */
	public void decreaseNestingLevel() {
		getTypeIndexesPerLevel().remove(this.nestingLevel);
		this.nestingLevel--;
	}

	public int getNestingLevel() {
		return this.nestingLevel;
	}

	/**
	 * 设置当前层级的type index
	 * 
	 * @param typeIndex
	 */
	public void setTypeIndexForCurrentLevel(int typeIndex) {
		getTypeIndexesPerLevel().put(this.nestingLevel, typeIndex);
	}

	/**
	 * 返回当前层级的type index
	 * 
	 * @return
	 */
	public Integer getTypeIndexForCurrentLevel() {
		return getTypeIndexForLevel(this.nestingLevel);
	}

	/**
	 * 返回指定嵌套层级的参数类型map index
	 * 
	 * @param nestingLevel
	 * @return
	 */
	public Integer getTypeIndexForLevel(int nestingLevel) {
		return getTypeIndexesPerLevel().get(nestingLevel);
	}

	/**
	 * 获取 每一个level 类型index的map
	 * 
	 * @return
	 */
	private Map<Integer, Integer> getTypeIndexesPerLevel() {
		if (this.typeIndexesPerLevel == null) {
			this.typeIndexesPerLevel = new HashMap<Integer, Integer>(4);
		}
		return this.typeIndexesPerLevel;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof MethodParameter) {
			MethodParameter other = (MethodParameter) obj;

			if (this.parameterIndex != other.parameterIndex) {
				return false;
			} else if (this.getMember().equals(other.getMember())) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = this.hash;
		if (result == 0) {
			result = getMember().hashCode();
			result = 31 * result + this.parameterIndex;
			this.hash = result;
		}
		return result;
	}

	/**
	 * 创建一个新的MethodParameter为给定的方法或者构造器
	 * 
	 * @param methodOrConstructor
	 * @param parameterIndex
	 * @return
	 */
	public static MethodParameter forMethodOrConstructor(Object methodOrConstructor, int parameterIndex) {
		if (methodOrConstructor instanceof Method) {
			return new MethodParameter((Method) methodOrConstructor, parameterIndex);
		} else if (methodOrConstructor instanceof Constructor) {
			return new MethodParameter((Constructor) methodOrConstructor, parameterIndex);
		} else {
			throw new IllegalArgumentException("Given object [" + methodOrConstructor
					+ "] is neither a Method nor a Constructor");
		}
	}

}
