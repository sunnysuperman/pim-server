package com.github.sunnysuperman.pimserver.test;

import junit.framework.TestCase;

import org.apache.log4j.PropertyConfigurator;

public class BaseTest extends TestCase {

	static {
		PropertyConfigurator.configure(BaseTest.class.getResourceAsStream("log4j.properties"));
	}

}
