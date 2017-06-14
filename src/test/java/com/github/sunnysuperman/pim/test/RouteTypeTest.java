package com.github.sunnysuperman.pim.test;

import junit.framework.TestCase;

public class RouteTypeTest extends TestCase {
	public static final int ROUTE_LOCAL = (1 << 0);
	public static final int ROUTE_REGION = (1 << 1);
	public static final int ROUTE_GLOBAL = (1 << 2);

	public void test1() {
		System.out.println(ROUTE_LOCAL);
		System.out.println(ROUTE_REGION);
		System.out.println(ROUTE_GLOBAL);
	}

	public void test2() {
		{
			int ret = 0;
			ret |= 0;
			System.out.println(ret);
		}
		{
			int ret = 0;
			ret |= ROUTE_LOCAL;
			ret |= 0;
			System.out.println(ret);
		}
		{
			int ret = 0;
			ret |= ROUTE_LOCAL;
			ret |= ROUTE_REGION;
			System.out.println(ret);
		}
		{
			int ret = 0;
			ret |= ROUTE_LOCAL;
			ret |= ROUTE_REGION;
			ret |= ROUTE_REGION;
			System.out.println(ret);
		}
		{
			int ret = 0;
			ret |= ROUTE_LOCAL;
			ret |= ROUTE_REGION;
			ret |= ROUTE_REGION;
			ret |= ROUTE_GLOBAL;
			ret |= ROUTE_GLOBAL;
			System.out.println(ret);
		}
	}
}
