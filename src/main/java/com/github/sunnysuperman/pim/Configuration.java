package com.github.sunnysuperman.pim;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.commons.config.Config;
import com.github.sunnysuperman.commons.config.Config.ConfigValueChangedListener;
import com.github.sunnysuperman.pim.util.BeanUtils;

public abstract class Configuration implements ConfigValueChangedListener {
    protected static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
    protected ConcurrentLinkedQueue<ConfigValueChangedListener> _listeners = new ConcurrentLinkedQueue<ConfigValueChangedListener>();
    protected Config _config;
    protected String _name;

    public Configuration(Config config, String name) {
        this._config = config;
        this._name = (name == null) ? getClass().getCanonicalName() : name;
        try {
            Collection<Field> fields = getAvailableFields();
            Map<String, Method> setMethods = getAllSetMethods();
            for (Field field : fields) {
                String key = field.getName();
                if (!Modifier.isVolatile(field.getModifiers())) {
                    throw new RuntimeException("Should use volatile modifier for field: " + key);
                }
                String setMethodName = getSetMethodName(key);
                if (!setMethods.containsKey(setMethodName)) {
                    throw new RuntimeException("Does not contains set-method for field: " + key);
                }
                Object value = config.getValue(key);
                if (value == null) {
                    continue;
                }
                Object parsedValue = parseValue(field, value);
                setMethods.get(setMethodName).invoke(this, parsedValue);
            }
            initValues();
            List<Object[]> keyValues = getKeyValues();
            for (Object[] keyValue : keyValues) {
                String key = keyValue[0].toString();
                Object value = keyValue[1];
                if (!validate(key, value)) {
                    throw new RuntimeException("Config is invalid for " + getName() + " at key '" + key + "'");
                }
            }
            if (LOG.isInfoEnabled()) {
                LOG.info(this.toString());
            }
            config.addListener(this);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onChanged(String key, Object value) {
        try {
            if (value == null) {
                return;
            }
            Collection<Field> fields = getAvailableFields();
            Field field = null;
            for (Field f : fields) {
                if (f.getName().equals(key)) {
                    field = f;
                    break;
                }
            }
            if (field == null) {
                LOG.warn("Could not find field of '" + key + "' for " + getName());
                return;
            }
            Map<String, Method> setMethods = getAllSetMethods();
            String setMethodName = getSetMethodName(key);
            Method setMethod = setMethods.get(setMethodName);
            if (setMethod == null) {
                LOG.warn("Could not find set-method of '" + key + "' for " + getName());
                return;
            }
            Object parsedValue = parseValue(field, value);
            boolean invalid = !validate(key, parsedValue);
            if (invalid) {
                LOG.error("Invalid value " + parsedValue + " for key " + key);
                return;
            }
            setMethod.invoke(this, parsedValue);
            if (LOG.isInfoEnabled()) {
                LOG.info(key + " changed, " + this.toString());
            }
            if (!_listeners.isEmpty()) {
                for (ConfigValueChangedListener listener : _listeners) {
                    listener.onChanged(key, parsedValue);
                }
            }
        } catch (Exception e) {
            LOG.error(null, e);
        }
    }

    public String getName() {
        return _name;
    }

    public void addListener(ConfigValueChangedListener listener) {
        _listeners.add(listener);
    }

    public void removeListener(ConfigValueChangedListener listener) {
        _listeners.remove(listener);
    }

    @Override
    public String toString() {
        try {
            List<Object[]> keyValues = getKeyValues();
            StringBuilder buf = new StringBuilder();
            buf.append(getName()).append(":{");
            int i = 0;
            for (Object[] keyValue : keyValues) {
                String key = keyValue[0].toString();
                if (i > 0) {
                    buf.append(",");
                }
                i++;
                buf.append(key).append(":").append(keyValue[1]);
            }
            buf.append("}");
            return buf.toString();
        } catch (Exception e) {
            LOG.error(null, e);
            return null;
        }
    }

    protected abstract void initValues();

    protected abstract boolean validate(String key, Object value);

    protected Object parseValue(Field field, Object value) throws Exception {
        return BeanUtils.parseSimpleType(value, field.getType());
    }

    private List<Object[]> getKeyValues() throws Exception {
        Collection<Field> fields = getAvailableFields();
        Method[] methods = getClass().getMethods();
        Map<String, Method> getMethods = new HashMap<String, Method>();
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith("get") || name.startsWith("is")) {
                getMethods.put(name, method);
            }
        }
        List<Object[]> keyValues = new ArrayList<Object[]>(fields.size());
        for (Field field : fields) {
            String key = field.getName();
            String getMethodName = null;
            if (field.getType() == boolean.class) {
                getMethodName = "is";
            } else {
                getMethodName = "get";
            }
            getMethodName += Character.toUpperCase(key.charAt(0)) + key.substring(1);
            Method getMethod = getMethods.get(getMethodName);
            if (getMethod == null) {
                throw new RuntimeException("No get-method for " + getName() + " at key '" + key + "'");
            }
            Object value = getMethod.invoke(this);
            keyValues.add(new Object[] { key, value });
        }
        return keyValues;
    }

    private void getAllFields(Class<?> clazz, Map<String, Field> fieldMap) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!fieldMap.containsKey(field.getName())) {
                fieldMap.put(field.getName(), field);
            }
        }
        if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            getAllFields(clazz.getSuperclass(), fieldMap);
        }
    }

    private Collection<Field> getAvailableFields() {
        Map<String, Field> fieldMap = new HashMap<String, Field>();
        getAllFields(getClass(), fieldMap);
        Iterator<Entry<String, Field>> iter = fieldMap.entrySet().iterator();
        List<Field> availableFields = new LinkedList<Field>();
        while (iter.hasNext()) {
            Entry<String, Field> entry = iter.next();
            Field field = entry.getValue();
            if (Modifier.isStatic(field.getModifiers()) || field.getName().startsWith("_")) {
                continue;
            }
            availableFields.add(field);
        }
        return availableFields;
    }

    private Map<String, Method> getAllSetMethods() {
        Method[] methods = getClass().getMethods();
        Map<String, Method> setMethods = new HashMap<String, Method>();
        for (Method method : methods) {
            if (!method.getName().startsWith("set")) {
                continue;
            }
            setMethods.put(method.getName(), method);
        }
        return setMethods;
    }

    private String getSetMethodName(String key) {
        String setMethodName = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        return setMethodName;
    }

}
