package org.zsz.uniitf.dispatcher.support;

import com.alibaba.fastjson.JSON;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

/**
 * 反射工具类
 * @author Zhang Shengzhe
 * @create 2020-06-05 11:10
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ClassUtil {

    /**
     * 判断一个类是JAVA类型还是用户定义类型
     * @param cls
     * @return
     */
    public static boolean isJavaClass(Class<?> cls) {
        return cls != null && cls.getClassLoader() == null;
    }

    /**
     * String 或 Json 转换
     * @param val
     * @param cls
     * @return
     */
    public static Object converterStringOrJsonValue(String val, Class<?> cls) {
        if (val == null) {
            return null;
        } else if (cls == String.class) {
            return val;
        } else if (cls.isEnum()) {
            return new StringToEnum(ClassUtil.getEnumType(cls)).convert(val);
        } else if (cls == Object.class) {
            return JSON.toJSONString(val);
        } else {
            try {
                return JSON.parseObject(val, cls);
            } catch (Exception e) {
                throw new ClassCastException(e.getMessage());
            }
        }
    }

    private static class StringToEnum<T extends Enum> implements Converter<String, T> {

        private final Class<T> enumType;

        public StringToEnum(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        public T convert(String source) {
            if (source.isEmpty()) {
                // It's an empty enum identifier: reset the enum value to null.
                return null;
            }
            return (T) Enum.valueOf(this.enumType, source.trim());
        }
    }

    public static Class<?> getEnumType(Class<?> targetType) {
        Class<?> enumType = targetType;
        while (enumType != null && !enumType.isEnum()) {
            enumType = enumType.getSuperclass();
        }
        Assert.notNull(enumType, () -> "The target type " + targetType.getName() + " does not refer to an enum");
        return enumType;
    }
}
