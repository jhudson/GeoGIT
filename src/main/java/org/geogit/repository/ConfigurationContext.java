package org.geogit.repository;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ConfigurationContext extends ClassPathXmlApplicationContext {
	private static ConfigurationContext instance;
	
	public static synchronized ConfigurationContext getInstance() {
		if(instance == null) {
			instance = new ConfigurationContext();
		}
		return instance;
	}
	
	private ConfigurationContext() {
		super("org/geogit/storage/applicationContext.xml");
	}
}
