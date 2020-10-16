package org.zsz.uniitf.dispatcher;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.zsz.uniitf.dispatcher.support.ClassUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * 远程调用适配器
 *
 * @author Zhang Shengzhe
 * @create 2020-05-28 11:28
 */
@Slf4j
public class HandlerAdapter {

    private static final String PARAM_ERROR_TEMPLATE = "远程调用url:[%s] 方法[%s] 参数:[%s] 为空";

    private static final String CLASS_CAST_ERROR_TEMPLATE = "转换[%s]类型异常, 原因:[%s]";

    /**
     * 方法是否有参数
     */
    private boolean hasParam;

    /**
     * 方法中参数的信息
     */
    private List<ParamInfo> paramInfoList;

    /**
     * 用于解析param的真实名称
     */
    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public HandlerAdapter(HandlerDefinition definition) {
        this.paramInfoList = new ArrayList<>();
        this.hasParam = false;
        initParams(definition);
    }

    private void initParams(HandlerDefinition definition) {
        String[] paramName = parameterNameDiscoverer.getParameterNames(definition.getMethod());
        Parameter[] parameters = definition.getMethod().getParameters();
        hasParam = parameters != null && parameters.length != 0;

        if (!hasParam) {
            return;
        }

        for (int index = 0; index < parameters.length; index++) {
            Parameter param = parameters[index];
            paramInfoList.add(new ParamInfo(index, paramName[index], param));
        }
    }

    /**
     * 转换参数执行方法
     *
     * @param definition 处理器定义
     * @param argMap     参数map
     * @return 方法执行的结果
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public String process(HandlerDefinition definition, Map<String, String> argMap) throws Throwable {
        // 如果参数列表为空
        if (Objects.isNull(argMap)) {
            argMap = Collections.emptyMap();
        }
        Object[] paramValues = new Object[paramInfoList.size()];
        for (ParamInfo info : paramInfoList) {
            // 必传参数没有传递
            if (info.required && !argMap.containsKey(info.name) && info.defaultValue == null) {
                RemoteRequestInfo requestInfo = definition.getRequestInfo();
                String errMsg = String.format(PARAM_ERROR_TEMPLATE, requestInfo.getUrlPattern(), requestInfo.getHttpMethod(), info.name);
                log.error(errMsg);
                throw new IllegalArgumentException(errMsg);
            }
            Object obj = info.defaultValue;
            try {
                // 自定义类型直接处理整个argMap
                if (info.isSelf) {
                    obj = ClassUtil.converterStringOrJsonValue(JSON.toJSONString(argMap), info.cls);
                }
                // 其它类型
                else {
                    // 如果没有传递 使用默认值
                    String value = !argMap.containsKey(info.name) ? info.defaultValue : argMap.get(info.name);
                    obj = ClassUtil.converterStringOrJsonValue(value, info.cls);
                }
            } catch (ClassCastException e) {
                String errMsg = String.format(CLASS_CAST_ERROR_TEMPLATE, info.cls.getName(), e.getMessage());
                log.error(errMsg);
                throw new ClassCastException(errMsg);
            }
            paramValues[info.index] = obj;
        }
        Object result;
        try {
            result = definition.getMethod().invoke(definition.getController(), paramValues);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
        // 如果已经是 String 类型 无需处理直接返回
        if (result instanceof String) {
            return (String) result;
        }
        return JSON.toJSONString(result, SerializerFeature.WriteDateUseDateFormat);
    }

    private static class ParamInfo {

        /**
         * param name
         */
        private String name;

        /**
         * 索引
         */
        private int index;

        /**
         * 类型名称
         */
        private Class<?> cls;

        /**
         * 是否有必须有值
         */
        private boolean required;

        /**
         * 是否自定义类
         */
        private boolean isSelf;

        /**
         * 参数默认值
         */
        private String defaultValue;

        private ParamInfo(int index, String name, Parameter param) {
            this.name = name;
            this.index = index;
            this.cls = param.getType();
            this.required = false;
            if (param.isAnnotationPresent(RequestParam.class)) {
                RequestParam requestParam = param.getAnnotation(RequestParam.class);
                this.required = requestParam.required();
                this.isSelf = false;
                this.defaultValue = ValueConstants.DEFAULT_NONE.equals(requestParam.defaultValue()) ? null : requestParam.defaultValue();
            }
            // 自定义类(不属于jdk)
            else {
                isSelf = ClassUtil.isJavaClass(cls);
            }
        }
    }
}