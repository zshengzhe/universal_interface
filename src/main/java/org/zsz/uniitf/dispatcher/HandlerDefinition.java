package org.zsz.uniitf.dispatcher;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 处理器定义
 * @author Zhang Shengzhe
 * @create 2020-05-28 13:54
 */
public class HandlerDefinition {

    /**
     * url and httpMethod
     */
    private RemoteRequestInfo requestInfo;

    /**
     * controller instance
     */
    private Object controller;

    /**
     * invoke method
     */
    private Method method;

    public HandlerDefinition(RemoteRequestInfo requestInfo, Object controller, Method method) {
        this.requestInfo = requestInfo;
        this.controller = controller;
        this.method = method;
    }

    public RemoteRequestInfo getRequestInfo() {
        return requestInfo;
    }

    public Object getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o instanceof HandlerDefinition) {
            return method == ((HandlerDefinition) o).method
                    && ((HandlerDefinition) o).getRequestInfo().equals(requestInfo);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(method);
    }
}