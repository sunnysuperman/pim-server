package com.github.sunnysuperman.pim.cluster;

import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class RedisRouteTable implements RouteTable {
	public static class RedisRouteTableOptions {
		private String host;
		private int port = Protocol.DEFAULT_PORT;
		private String password;
		private int db = Protocol.DEFAULT_DATABASE;
		private int maxWait = 9000;// millsecond
		private int timeout = Protocol.DEFAULT_TIMEOUT; // millsecond
		private int idleTime = 300;// second
		private int minIdle = 2;
		private int maxIdle = 2;
		private int maxTotal = 2;

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public int getDb() {
			return db;
		}

		public void setDb(int db) {
			this.db = db;
		}

		public int getMaxWait() {
			return maxWait;
		}

		public void setMaxWait(int maxWait) {
			this.maxWait = maxWait;
		}

		public int getTimeout() {
			return timeout;
		}

		public void setTimeout(int timeout) {
			this.timeout = timeout;
		}

		public int getIdleTime() {
			return idleTime;
		}

		public void setIdleTime(int idleTime) {
			this.idleTime = idleTime;
		}

		public int getMinIdle() {
			return minIdle;
		}

		public void setMinIdle(int minIdle) {
			this.minIdle = minIdle;
		}

		public int getMaxIdle() {
			return maxIdle;
		}

		public void setMaxIdle(int maxIdle) {
			this.maxIdle = maxIdle;
		}

		public int getMaxTotal() {
			return maxTotal;
		}

		public void setMaxTotal(int maxTotal) {
			this.maxTotal = maxTotal;
		}

	}

	protected JedisPool pool = null;

	private long parseSecondsToMills(long seconds) {
		if (seconds < 0) {
			return -1;
		}
		return seconds * 1000L;
	}

	public RedisRouteTable(RedisRouteTableOptions options) {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMinIdle(options.getMinIdle());
		config.setMaxIdle(options.getMaxIdle());
		config.setMaxTotal(options.getMaxTotal());
		config.setMinEvictableIdleTimeMillis(parseSecondsToMills(options.getIdleTime()));
		config.setMaxWaitMillis(options.getMaxWait());
		pool = new JedisPool(config, options.getHost(), options.getPort(), options.getTimeout(), options.getPassword(),
				options.getDb());
	}

	public void init() throws Exception {
		get("0");
	}

	@Override
	public Set<RouteResult> get(String username) throws Exception {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			Set<String> set = jedis.smembers(username);
			if (set == null || set.isEmpty()) {
				return null;
			}
			Set<RouteResult> results = new HashSet<RouteResult>(set.size());
			for (String s : set) {
				results.add(RouteResult.fromString(s));
			}
			return results;
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	@Override
	public boolean has(String username) throws Exception {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			Set<String> set = jedis.smembers(username);
			return set != null && !set.isEmpty();
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	@Override
	public boolean add(String clientID, RouteResult result) throws Exception {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			Long ret = jedis.sadd(clientID, result.toString());
			return ret > 0 ? true : false;
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public boolean remove(String clientID, RouteResult result) throws Exception {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			Long ret = jedis.srem(clientID, result.toString());
			return ret > 0 ? true : false;
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}
}
