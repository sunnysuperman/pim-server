package com.github.sunnysuperman.pim.bootstrap;

import java.util.List;

public class ServerOptions {
    private int bossThreads;
    private int workerThreads;
    private List<ServerConfig> listens;

    public int getBossThreads() {
        return bossThreads;
    }

    public void setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public List<ServerConfig> getListens() {
        return listens;
    }

    public void setListens(List<ServerConfig> listens) {
        this.listens = listens;
    }
}
