package org.springframework.context;

/**
 * 生命周期，定义了start/stop生命周期的方法。典型的应用就是控制异步处理
 * 
 * <p>
 * 能被 BeanFactory 和 ApplicationContext 两个组件实现。容器将传播启动/停止信号为所有的应用
 * 
 * <p>
 * 能被用在直接调用或为管理操作JMX，在后面的案例中MBeanExporter一个典型的定义InterfaceBasedMBeanInfoAssembler
 * 
 * <p>
 * 注意：这个接口只能支持 最顶层的单例 beans，被发现其他组件则被忽略。另外继承SmartLifecycle接口，能
 * 提供了更先进的集合与容器的启动和关闭功能
 * 
 * @author yanbin
 * 
 */
public interface Lifecycle {

	/**
	 * 开始这个组件，如果这个组件已经在运行了不应该抛出异常。
	 * <p>
	 * 在容器中将启动信息传播到所有组件应用
	 */
	void start();

	/**
	 * 停止这个组件，通常在一个同步的方式，当这个方法返回则这个组件为完全停止。考虑实现SmartLifecycle的stop方法，
	 * 是异步停止行为的是必须的
	 * 
	 * <p>
	 * 当组件还没有启动的是不应该抛出异常
	 * <p>
	 * 在容器中将启动信息传播到所有组件应用
	 * 
	 */
	void stop();

	/**
	 * 检查component 是否当前运行的。
	 * <p>
	 * 只有当所有components 在运行才会返回true
	 * 
	 * @return
	 */
	boolean isRunning();

}
