package com.github.sunnysuperman.pim.region;

// package com.github.sunnysuperman.im.region;
//
// import io.netty.channel.Channel;
// import io.netty.channel.ChannelHandlerContext;
// import io.netty.channel.nio.NioEventLoopGroup;
//
// import java.util.Collection;
// import java.util.Iterator;
// import java.util.Map.Entry;
// import java.util.concurrent.ConcurrentHashMap;
//
// import com.github.sunnysuperman.commons.config.Config;
// import
// com.github.sunnysuperman.commons.config.Config.ConfigValueChangedListener;
// import com.github.sunnysuperman.commons.utils.CollectionUtil;
// import com.github.sunnysuperman.commons.utils.StringUtil;
// import com.github.sunnysuperman.pimsdk.Packet;
// import com.github.sunnysuperman.pimsdk.PacketType;
// import com.github.sunnysuperman.pimsdk.packet.MessageAck;
// import com.github.sunnysuperman.pimsdk.packet.PongPacket;
// import com.github.sunnysuperman.im.Connection;
// import com.github.sunnysuperman.im.PimPacketHandler;
// import com.github.sunnysuperman.im.PacketSafeSender;
// import com.github.sunnysuperman.im.PacketSafeSenderBuilder;
// import com.github.sunnysuperman.im.PacketSafeSenderMonitor;
// import com.github.sunnysuperman.im.PacketWriter;
// import com.github.sunnysuperman.im.Session;
// import com.github.sunnysuperman.im.packet.CommunicationPacket;
//
// public class DefaultRegionPacketRouter extends PacketSafeSenderBuilder
// implements RegionPacketRouter {
//
// public static class RegionClientConfig extends PacketSafeSenderConfig {
// public RegionClientConfig(Config config, String name) {
// super(config, name);
// }
//
// private volatile Collection<Region> regions;
// private volatile String myRegionId;
// private volatile int port;
//
// @Override
// protected boolean validate(String key, Object value) {
// if (key.equals("regions")) {
// return CollectionUtil.isNotEmpty((Collection<?>) value);
// }
// if (key.equals("myRegionId")) {
// return StringUtil.isNotEmpty((String) value);
// }
// if (key.equals("port")) {
// return ((Integer) value) > 0;
// }
// return super.validate(key, value);
// }
//
// public Collection<Region> getRegions() {
// return regions;
// }
//
// public void setRegions(Collection<Region> regions) {
// this.regions = regions;
// }
//
// public String getMyRegionId() {
// return myRegionId;
// }
//
// public void setMyRegionId(String myRegionId) {
// this.myRegionId = myRegionId;
// }
//
// public int getPort() {
// return port;
// }
//
// public void setPort(int port) {
// this.port = port;
// }
//
// }
//
// private class RegionClientHandler extends PimPacketHandler {
// private String regionId;
//
// public RegionClientHandler(PacketHandlerConfig config, String regionId) {
// super(config);
// this.regionId = regionId;
// }
//
// @Override
// public void channelInactive(ChannelHandlerContext ctx) throws Exception {
// super.channelInactive(ctx);
// LOG.warn("close region connection: " + ctx.channel());
// senderMonitor.alarm();
// }
//
// @Override
// protected boolean handlePacket(Packet packet, ChannelHandlerContext context,
// Session session) {
// try {
// PacketSafeSender sender = senders.get(regionId);
// if (sender == null) {
// LOG.warn("Region sender has been removed");
// return false;
// }
// byte type = packet.getType();
// switch (type) {
// case PacketType.TYPE_PING: {
// Channel currentChannel = sender.getConnection().getChannel();
// Channel contextChannel = context.channel();
// if (currentChannel != contextChannel) {
// LOG.warn("No the same region connection, so do not reply pong, maybe previous
// connection is not closed properly: "
// + contextChannel + ", " + currentChannel);
// return false;
// }
// PacketWriter.write(new PongPacket(), context.channel());
// return true;
// }
// case PacketType.TYPE_MSG_ACK: {
// MessageAck ack = MessageAck.deserialize(packet.getBody());
// sender.onPacketSent(ack.getSequenceID());
// return true;
// }
// default:
// break;
// }
// return true;
// } catch (Throwable t) {
// LOG.error(null, t);
// return true;
// }
// }
//
// @Override
// protected Session newSession() {
// return new Session();
// }
// }
//
// private final byte[] LOCK = new byte[0];
// private final PacketSafeSenderMonitor senderMonitor;
// private final ConcurrentHashMap<String, PacketSafeSender> senders;
//
// public DefaultRegionPacketRouter(RegionClientConfig config) {
// super(config, new NioEventLoopGroup(1));
// this.senders = new ConcurrentHashMap<String,
// PacketSafeSender>(config.getRegions().size() - 1);
// this.senderMonitor = new PacketSafeSenderMonitor(new NioEventLoopGroup(1)) {
//
// @Override
// protected boolean ensureSafeSender() {
// return DefaultRegionPacketRouter.this.ensureSenders();
// }
//
// };
// config.addListener(new ConfigValueChangedListener() {
//
// @Override
// public void onChanged(String key, Object value) {
// if (key.equals("regions")) {
// senderMonitor.alarm();
// } else if (key.equals("writeTimeoutMills") || key.equals("sendMaxTry")) {
// int writeTimeoutMills = getConfig().getWriteTimeoutMills();
// int sendMaxTry = getConfig().getSendMaxTry();
// synchronized (LOCK) {
// for (Iterator<Entry<String, PacketSafeSender>> iter =
// senders.entrySet().iterator(); iter
// .hasNext();) {
// Entry<String, PacketSafeSender> entry = iter.next();
// PacketSafeSender sender = entry.getValue();
// sender.setWriteTimeoutMills(writeTimeoutMills);
// sender.setSendMaxTry(sendMaxTry);
// }
// }
// }
// }
//
// });
// }
//
// @Override
// protected Connection buildConnection(Object context) {
// Region region = (Region) context;
// return build(region.getHost(), getConfig().port, region.getId());
// }
//
// private PacketSafeSender ensureSender(Region region) {
// try {
// PacketSafeSender oldSender = senders.get(region.getId());
// PacketSafeSender sender = ensureSender(oldSender, region);
// if (sender == null) {
// senders.remove(region.getId());
// } else if (sender != oldSender) {
// try {
// senders.put(region.getId(), sender);
// } catch (Throwable putError) {
// LOG.error(null, putError);
// sender.stop(1);
// return null;
// }
// }
// return sender;
// } catch (Throwable t) {
// LOG.error(null, t);
// return null;
// }
// }
//
// private boolean ensureSenders() {
// boolean ok = true;
// synchronized (LOCK) {
// try {
// // 删除无效Connection
// for (Iterator<Entry<String, PacketSafeSender>> iter =
// senders.entrySet().iterator(); iter.hasNext();) {
// Entry<String, PacketSafeSender> entry = iter.next();
// if (findRegion(entry.getKey()) == null) {
// entry.getValue().stop(1);
// iter.remove();
// }
// }
// Collection<Region> regions = getRegions();
// String myRegionId = getMyRegionId();
// for (Region region : regions) {
// if (region.getId().equals(myRegionId)) {
// continue;
// }
// // 新建Connection
// PacketSafeSender sender = ensureSender(region);
// if (ok) {
// ok = (sender != null && sender.isActive());
// }
// }
// return ok;
// } catch (Throwable t) {
// LOG.error(null, t);
// return false;
// }
// }
// }
//
// @Override
// public void stop(int seconds) {
// senderMonitor.stop();
// synchronized (LOCK) {
// for (Iterator<Entry<String, PacketSafeSender>> iter =
// senders.entrySet().iterator(); iter.hasNext();) {
// Entry<String, PacketSafeSender> entry = iter.next();
// entry.getValue().stop(seconds * 1000);
// iter.remove();
// }
// }
// }
//
// @Override
// public boolean routeToRegion(String regionId, CommunicationPacket packet, int
// maxTry) {
// if (LOG.isInfoEnabled()) {
// LOG.info("routeToRegion: " + regionId);
// }
// PacketSafeSender sender = senders.get(regionId);
// if (sender == null) {
// LOG.warn("No sender for region: " + regionId);
// return false;
// }
// return sender.send(packet, maxTry);
// }
//
// @Override
// public boolean routeToAll(CommunicationPacket packet, int maxTry) {
// Collection<Region> regions = getRegions();
// String myRegionId = getMyRegionId();
// boolean allRouted = true;
// for (Region region : regions) {
// if (region.getId().equals(myRegionId)) {
// continue;
// }
// boolean routed = routeToRegion(region.getId(), packet, maxTry);
// if (!routed) {
// allRouted = false;
// }
// }
// return allRouted;
// }
//
// @Override
// public Collection<Region> getRegions() {
// return getConfig().regions;
// }
//
// @Override
// public String getMyRegionId() {
// return getConfig().myRegionId;
// }
//
// @Override
// protected PimPacketHandler newPacketHandler(Object context) {
// return new RegionClientHandler(config, (String) context);
// }
//
// @Override
// protected Connection setupConnection(Channel channel, Object context) {
// return new Connection(channel, getConfig().getCompressThreshold());
// }
//
// private RegionClientConfig getConfig() {
// return (RegionClientConfig) config;
// }
//
// private Region findRegion(String regionId) {
// for (Region region : getRegions()) {
// if (region.getId().equals(regionId)) {
// return region;
// }
// }
// return null;
// }
// }
