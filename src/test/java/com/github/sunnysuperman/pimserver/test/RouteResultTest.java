package com.github.sunnysuperman.pimserver.test;

import com.github.sunnysuperman.pimserver.cluster.RouteResult;

import junit.framework.TestCase;

public class RouteResultTest extends TestCase {

	private void assertRouteResult(String s) {
		RouteResult r1 = RouteResult.fromString(s);
		assertTrue(r1.toString().equals(s));
		System.out.println(r1.getRegion() + ", " + r1.getServer() + ", " + r1.getResource() + ", " + r1.toString());
	}

	public void test_1() {
		assertRouteResult("1-10.0.0.2/device");
		assertRouteResult("1-10.0.0.2");
		assertRouteResult("10.0.0.2/device");
		assertRouteResult("10.0.0.2");
	}

}
