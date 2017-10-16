package com.github.sunnysuperman.pim.test;

import java.util.Set;

import com.github.sunnysuperman.commons.bean.Bean;
import com.github.sunnysuperman.commons.config.PropertiesConfig;
import com.github.sunnysuperman.pim.cluster.RedisRouteTable;
import com.github.sunnysuperman.pim.cluster.RedisRouteTable.RedisRouteTableOptions;
import com.github.sunnysuperman.pim.cluster.RouteResult;

import junit.framework.TestCase;

public class RedisRouteTableTest extends TestCase {
    private RedisRouteTable routeTable;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PropertiesConfig config = new PropertiesConfig(
                RedisRouteTableTest.class.getResourceAsStream("redis.properties"));
        RedisRouteTableOptions options = Bean.fromJson(config.getString("db"), new RedisRouteTableOptions());
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
