package com.github.sunnysuperman.pim.util;

import java.util.Date;

import com.github.sunnysuperman.commons.utils.FormatUtil;

public class BeanUtils {

    public static Object parseSimpleType(Object raw, Class<?> destClass) {
        if (raw == null) {
            return null;
        }
        if (raw.getClass().equals(destClass)) {
            return raw;
        }
        // most case
        if (destClass.equals(String.class)) {
            return raw.toString();
        }
        if (destClass.equals(Boolean.class)) {
            return FormatUtil.parseBooleanStrictly(raw);
        }
        if (destClass.equals(boolean.class)) {
            return FormatUtil.parseBoolean(raw, Boolean.FALSE);
        }
        if (destClass.equals(Integer.class)) {
            return FormatUtil.parseInteger(raw);
        }
        if (destClass.equals(int.class)) {
            Integer integer = FormatUtil.parseInteger(raw);
            return integer == null ? 0 : integer.intValue();
        }
        if (destClass.equals(Long.class)) {
            return FormatUtil.parseLong(raw);
        }
        if (destClass.equals(long.class)) {
            Long l = FormatUtil.parseLong(raw);
            return l == null ? 0L : l.longValue();
        }
        if (destClass.equals(Double.class)) {
            return FormatUtil.parseDouble(raw);
        }
        if (destClass.equals(double.class)) {
            Double d = FormatUtil.parseDouble(raw);
            return d == null ? 0d : d.doubleValue();
        }
        if (destClass.equals(Float.class)) {
            return FormatUtil.parseFloat(raw);
        }
        if (destClass.equals(float.class)) {
            Float f = FormatUtil.parseFloat(raw);
            return f == null ? 0f : f.floatValue();
        }
        if (destClass.equals(Date.class)) {
            return FormatUtil.parseDate(raw);
        }
        if (destClass.equals(Short.class)) {
            return FormatUtil.parseShort(raw);
        }
        if (destClass.equals(short.class)) {
            Short s = FormatUtil.parseShort(raw);
            return s == null ? (short) 0 : s.shortValue();
        }
        if (destClass.equals(Byte.class)) {
            return FormatUtil.parseByte(raw);
        }
        if (destClass.equals(byte.class)) {
            Byte s = FormatUtil.parseByte(raw);
            return s == null ? (byte) 0 : s.byteValue();
        }
        if (destClass.equals(Character.class) || destClass.equals(char.class)) {
            return raw.toString().charAt(0);
        }
        return raw;
    }

}
