package com.github.sunnysuperman.pim.cluster;

import com.github.sunnysuperman.pim.protocol.ClientID;
import com.github.sunnysuperman.pim.util.Utils;

public class RouteResult {
	private static final char REGION_SEP_KEY = '-';
	private static final char RESOURCE_SEP_KEY = '/';

	private String region;
	private String server;
	private String resource;
	private String s;

	public RouteResult(String region, String server, String resource) {
		super();
		this.server = Utils.notNull(server);
		this.region = region;
		this.resource = resource;
		if (region == null && resource == null) {
			this.s = server;
		} else {
			int len = server.length();
			if (region != null) {
				len += region.length() + 1;
			}
			if (resource != null) {
				len += resource.length() + 1;
			}
			StringBuilder buf = new StringBuilder(len);
			if (region != null) {
				buf.append(region).append(REGION_SEP_KEY);
			}
			buf.append(server);
			if (resource != null) {
				buf.append(RESOURCE_SEP_KEY).append(resource);
			}
			this.s = buf.toString();
		}
	}

	public String getRegion() {
		return region;
	}

	public String getServer() {
		return server;
	}

	public String getResource() {
		return resource;
	}

	public String toString() {
		return s;
	}

	public boolean isSame(RouteResult another) {
		return another.s.equals(s);
	}

	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (!(o instanceof RouteResult)) {
			return false;
		}
		RouteResult another = (RouteResult) o;
		return isSame(another);
	}

	public int hashCode() {
		return toString().hashCode();
	}

	public boolean matchResourceAndRegion(ClientID clientID, String targetRegion) {
		String targetResource = clientID.getResource();
		// match resource
		if (targetResource != null && resource != null && !targetResource.equals(resource)) {
			return false;
		}
		// match region
		if (targetRegion != null && region != null && !targetRegion.equals(region)) {
			return false;
		}
		return true;
	}

	public static RouteResult fromString(String s) {
		int offset = s.indexOf(REGION_SEP_KEY);
		String region = null;
		if (offset > 0) {
			region = s.substring(0, offset);
			offset++;
		} else {
			offset = 0;
		}
		int offset2 = s.indexOf(RESOURCE_SEP_KEY, offset);
		if (offset2 < 0) {
			return new RouteResult(region, offset > 0 ? s.substring(offset) : s, null);
		} else {
			return new RouteResult(region, s.substring(offset, offset2), s.substring(offset2 + 1));
		}
	}

}
