package com.github.sunnysuperman.pim.region;

import com.github.sunnysuperman.commons.utils.JSONUtil;

public class Region {
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (!(o instanceof Region)) {
			return false;
		}
		Region region = (Region) o;
		return region.getId().equals(id);
	}

	public int hashCode() {
		return id.hashCode();
	}

	public String toString() {
		return JSONUtil.toJSONString(this);
	}

}
