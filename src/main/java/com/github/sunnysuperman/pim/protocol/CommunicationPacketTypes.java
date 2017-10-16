package com.github.sunnysuperman.pim.protocol;

import java.util.HashMap;
import java.util.Map;

public class CommunicationPacketTypes {
    private static Map<Class<? extends CommunicationPacket>, Integer> class2intMap = new HashMap<Class<? extends CommunicationPacket>, Integer>(
            0);
    private static Map<Integer, Class<? extends CommunicationPacket>> int2classMap = new HashMap<Integer, Class<? extends CommunicationPacket>>(
            0);

    static {
        CommunicationPacketTypes.register(1, ClientPacket.class);
        CommunicationPacketTypes.register(2, OnlinePacket.class);
        CommunicationPacketTypes.register(3, OfflinePacket.class);
    }

    public static void init() {
        // nope
    }

    public static void register(Integer type, Class<? extends CommunicationPacket> clazz) {
        class2intMap.put(clazz, type);
        int2classMap.put(type, clazz);
    }

    public static Integer getType(Class<? extends CommunicationPacket> clazz) {
        Integer type = class2intMap.get(clazz);
        if (type == null) {
            throw new RuntimeException("Unknown type: " + clazz);
        }
        return type;
    }

    public static Class<? extends CommunicationPacket> getClazz(Integer type) {
        return int2classMap.get(type);
    }
}
