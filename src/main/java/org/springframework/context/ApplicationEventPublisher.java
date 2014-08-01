package org.springframework.context;

/**
 * 封装事件发布函数的接口，是ApplicationContext super-interface 服务
 * 
 * @author yanbin
 * 
 */
public interface ApplicationEventPublisher {

	/**
	 * 用一个应用事件通知所有注册的监听器，事件可能是框架的事件（如Requesthandledevent）或应用程序特定的事件。
	 * 
	 * @param event
	 */
	void publishEvent(ApplicationEvent event);

}
