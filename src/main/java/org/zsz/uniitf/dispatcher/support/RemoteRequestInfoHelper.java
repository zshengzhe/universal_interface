package org.zsz.uniitf.dispatcher.support;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.zsz.uniitf.dispatcher.RemoteRequestInfo;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * RemoteRequestInfo 助手类
 * @author Zhang Shengzhe
 * @create 2020-06-02 11:01
 */
public final class RemoteRequestInfoHelper {
    /**
     * 为方法创建 RemoteRequestInfo
     * @param method
     * @param handlerType
     * @return
     */
    public RemoteRequestInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RemoteRequestInfo info = createRequestMappingInfo(method);
        if (info != null) {
            // 为类创建 RpcRequestInfo
            RemoteRequestInfo typeInfo = createRequestMappingInfo(handlerType);
            if (typeInfo != null) {
                info = typeInfo.combine(info);
            }
        }
        return info;
    }

    private RemoteRequestInfo createRequestMappingInfo(AnnotatedElement element) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
        return requestMapping != null ? createRequestMappingInfo(requestMapping) : null;
    }

    private RemoteRequestInfo createRequestMappingInfo(RequestMapping requestMapping) {
        String urlPattern = requestMapping.value().length == 0 ? "" : requestMapping.value()[0];
        // 为 null 时 为没有设置配置方法（all http method）
        RequestMethod method = requestMapping.method().length == 0 ? null : requestMapping.method()[0];
        return new RemoteRequestInfo(urlPattern, method);
    }
}
