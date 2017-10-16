package com.github.sunnysuperman.pim.test;

import java.util.HashMap;

import com.github.sunnysuperman.commons.config.PropertiesConfig;
import com.github.sunnysuperman.commons.config.TypeAndValue;
import com.github.sunnysuperman.pim.bootstrap.ServerConfig;

public class ConfigurationTest extends BaseTest {

    public void test_1() throws Exception {
        PropertiesConfig cfg = new PropertiesConfig(new HashMap<String, Object>());
        cfg.put("port", new TypeAndValue(10112), true);
        cfg.put("readIdleTimeMills", new TypeAndValue(0), true);
        cfg.put("waitPongTimeMills", new TypeAndValue(3000), true);
        ServerConfig config = new ServerConfig(cfg, null);
        System.out.println(config.toString());
    }
}
