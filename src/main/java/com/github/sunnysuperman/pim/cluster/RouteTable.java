package com.github.sunnysuperman.pim.cluster;

import java.util.Set;

public interface RouteTable {

    void init() throws Exception;

    Set<RouteResult> get(String username) throws Exception;

    boolean has(String username) throws Exception;

    boolean add(String username, RouteResult result) throws Exception;

    boolean remove(String username, RouteResult result) throws Exception;

}
