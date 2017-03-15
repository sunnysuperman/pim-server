package com.github.sunnysuperman.pimserver.test;

import junit.framework.TestCase;

import org.junit.Test;

import com.github.sunnysuperman.pimsdk.util.GZipUtil;

public class GZipTest extends TestCase {

	@Test
	public void test_1() throws Exception {
		String s = "kjdalkfjdflkjdlkfjdklfjdlkfjlkdjflkdjflddajfkdjfkdfaskf;ldsfk;ldakf;ldskfl;dskf;ld";
		byte[] data = s.getBytes();
		assertTrue(data.length == 82);
		byte[] zdata = GZipUtil.compress(data);
		assertTrue(zdata.length == 67);
	}
}
