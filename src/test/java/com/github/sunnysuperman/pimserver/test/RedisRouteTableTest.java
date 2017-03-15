package com.github.sunnysuperman.pimserver.test;

import java.util.Set;

import junit.framework.TestCase;

import com.github.sunnysuperman.commons.config.PropertiesConfig;
import com.github.sunnysuperman.commons.utils.BeanUtil;
import com.github.sunnysuperman.commons.utils.JSONUtil;
import com.github.sunnysuperman.pimserver.cluster.RedisRouteTable;
import com.github.sunnysuperman.pimserver.cluster.RouteResult;
import com.github.sunnysuperman.pimserver.cluster.RedisRouteTable.RedisRouteTableOptions;

public class RedisRouteTableTest extends TestCase {
	private RedisRouteTable routeTable;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		PropertiesConfig config = new PropertiesConfig(
				RedisRouteTableTest.class.getResourceAsStream("redis.properties"));
		RedisRouteTableOptions options = BeanUtil.map2bean(JSONUtil.parseJSONObject(config.getString("db")),
				new RedisRouteTableOptions());
		routeTable = new RedisRouteTable(options);
	}

	public void test_add() throws Exception {
		boolean added = false;
		added = routeTable.add("user.123", new RouteResult(null, "10.0.0.2", null));
		System.out.println(added);
		added = routeTable.add("user.123", new RouteResult(null, "10.0.0.3", null));
		System.out.println(added);
	}

	public void test_get() throws Exception {
		Set<RouteResult> servers = routeTable.get("user.123");
		System.out.println(servers);
	}

	public void test_remove() throws Exception {
		boolean removed = false;
		removed = routeTable.remove("user.123", new RouteResult(null, "10.0.0.2", null));
		System.out.println(removed);
		removed = routeTable.remove("user.123", new RouteResult(null, "10.0.0.3", null));
		System.out.println(removed);
	}
}
