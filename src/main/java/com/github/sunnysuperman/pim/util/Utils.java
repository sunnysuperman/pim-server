package com.github.sunnysuperman.pim.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class Utils {

	public static <T> T notNull(T t) {
		if (t == null) {
			throw new NullPointerException();
		}
		return t;
	}

	public static String getLocalAddress(String filter) {
		if (filter == null) {
			InetAddress addr;
			try {
				addr = InetAddress.getLocalHost();
				return addr.getHostAddress().toString();
			} catch (UnknownHostException e) {
				throw new RuntimeException("Failed to getLocalAddress", e);
			}
		}
		Pattern filterPattern = Pattern.compile(filter);
		try {
			Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
			while (networks.hasMoreElements()) {
				NetworkInterface network = networks.nextElement();
				Enumeration<InetAddress> addresses = network.getInetAddresses();
				while (addresses.hasMoreElements()) {
					String address = addresses.nextElement().getHostAddress();
					Matcher m = filterPattern.matcher(address);
					if (m.matches()) {
						return address;
					}
				}
			}
		} catch (SocketException e) {
			throw new RuntimeException("Failed to getLocalAddress", e);
		}
		throw new RuntimeException("Failed to getLocalAddress");
	}

	public static String printChannel(ChannelHandlerContext context) {
		return context.channel().toString();
	}

	public static String printChannel(Channel channel) {
		return channel.toString();
	}
}
