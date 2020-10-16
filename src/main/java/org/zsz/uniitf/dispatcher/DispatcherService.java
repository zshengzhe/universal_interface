package org.zsz.uniitf.dispatcher;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.zsz.uniitf.dispatcher.annotation.RemoteMethod;
import org.zsz.uniitf.dispatcher.dto.Result;
import org.zsz.uniitf.dispatcher.support.RemoteRequestInfoHelper;

import java.lang.reflect.Method;
import java.util.*;

/**
 * mainstay通用接口转发服务
 * @author Zhang Shengzhe
 * @create 2020-05-28 10:46
 */
@Service
@Slf4j
public class DispatcherService implements ApplicationContextAware {

    private static final String SCAN_ERROR_TEMPLATE = "DispatcherService 初始化异常 Controller=[%s],method=[%s] 没有查询到 @RequestMapping 信息";

    private static final String FIND_BEST_MATCH_ERROR_TEMPLATE = "根据url:[%s] method:[%s] 查询最优的匹配[%s]没有找到处理器";

    private static final String FIND_MATCH_ERROR_TEMPLATE = "根据url:[%s] method:[%s] 没有查找到处理器";

    private static final String GENERAL_PREFIX = "/";

    /**
     * 匹配url
     */
    private static final PathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 处理器定义集合
     */
    private List<HandlerDefinition> handlerMappings = new ArrayList<>(50);

    /**
     * key=HandlerDefinition value=HandlerAdapter
     */
    private Map<HandlerDefinition, HandlerAdapter> handlerAdapters = new HashMap<>(50);

    private RemoteRequestInfoHelper helper = new RemoteRequestInfoHelper();

    private void validateInvoke(String url, RequestMethod requestMethod) {
        if (CollectionUtils.isEmpty(handlerMappings)) {
            throw new NullPointerException("映射集合 [handlerMappings] 为空,请检查 @RpcMethod 注解是否标注");
        }
        if (url == null || requestMethod == null) {
            throw new IllegalArgumentException("url 或 requestMethod 不可为空");
        }
    }

    public String invoke(String url, String httpMethod, Map<String, String> argMap) {
        log.info("远程调用信息 url:[{}], method=[{}], 参数:{}", url, httpMethod, JSON.toJSONString(argMap));
        try {
            RequestMethod requestMethod = RequestMethod.valueOf(httpMethod);
            // 校验 url 和 requestMethod
            validateInvoke(url, requestMethod);
            url = processUrl(url);
            return doInvoke(url, requestMethod, argMap);
        } catch (Throwable t) {
            log.error(">>>-- 通用 RPC 方法调用出错 --<<<", t);
            return handleException(t);
        }
    }

    /**
     * 处理 url 上不合适的 /
     * @param url
     * @return
     */
    private String processUrl(String url) {
        // 以 / 结尾 清除最后的 /
        if (url.endsWith(GENERAL_PREFIX) || url.endsWith("?")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * 查找合适的 definition 和 adapter 进行方法invoke
     * @param url
     * @param requestMethod
     * @param argMap
     * @return
     */
    private String doInvoke(String url, RequestMethod requestMethod, Map<String, String> argMap) throws Throwable {
        // 存放已匹配到的 url 后续进行最佳匹配
        List<String> matchingPatterns = new ArrayList<>();
        // key=urlPattern value=index
        Map<String, Integer> matchingIndexMap = new HashMap<>(10);
        for (int index = 0; index < handlerMappings.size(); index++ ) {
            HandlerDefinition definition = handlerMappings.get(index);
            RemoteRequestInfo info = definition.getRequestInfo();
            // 精确匹配
            if (info.getUrlPattern().equals(url)) {
                // 匹配方法
                if (info.getHttpMethod() == null || info.getHttpMethod() == requestMethod) {
                    log.debug("匹配到:[url:{},method={}],开始方法调用", url, requestMethod);
                    HandlerAdapter adapter = handlerAdapters.get(definition);
                    return adapter.process(definition, argMap);
                }
                // 方法不匹配继续寻找
                continue;
            }
            // Pattern match
            if (info.getHttpMethod() == null || requestMethod == info.getHttpMethod()) {
                if (pathMatcher.match(info.getUrlPattern(), url)) {
                    matchingPatterns.add(info.getUrlPattern());
                    // 记录 urlPattern 的 位置
                    matchingIndexMap.put(info.getUrlPattern(), index);
                }
            }
        }
        // 开始匹配最优
        String bestMatch = null;
        Comparator<String> patternComparator = pathMatcher.getPatternComparator(url);
        if (!matchingPatterns.isEmpty()) {
            matchingPatterns.sort(patternComparator);
            bestMatch = matchingPatterns.get(0);
        }
        HandlerDefinition definition;
        if (bestMatch != null) {
            // 根据索引取出 definition
            definition = handlerMappings.get(matchingIndexMap.get(bestMatch));
            if (definition == null) {
                if (bestMatch.endsWith(GENERAL_PREFIX)) {
                    definition = handlerMappings.get(matchingIndexMap.get(bestMatch.substring(0, bestMatch.length() - 1)));
                }
            }
            if (definition == null) {
                String errMsg = String.format(FIND_BEST_MATCH_ERROR_TEMPLATE, url, requestMethod, bestMatch);
                throw new IllegalStateException(errMsg);
            }
        } else {
            String errMsg = String.format(FIND_MATCH_ERROR_TEMPLATE, url, requestMethod);
            throw new IllegalStateException(errMsg);
        }
        // url 中参数处理
        for (String matchingPattern : matchingPatterns) {
            if (patternComparator.compare(bestMatch, matchingPattern) == 0) {
                Map<String, String> vars = pathMatcher.extractUriTemplateVariables(matchingPattern, url);
                argMap.putAll(vars);
            }
        }
        HandlerAdapter adapter = handlerAdapters.get(definition);
        return adapter.process(definition, argMap);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        register(applicationContext);
    }

    private void register(ApplicationContext applicationContext) throws BeansException {
        // 只获取controller中方法
        Map<String, Object> controllerBean = applicationContext.getBeansWithAnnotation(Controller.class);

        if (controllerBean.isEmpty()) {
            log.info("没有在 ApplicationContext 中找到任何 Controller, 已自动返回");
            return;
        }
        log.info("通用 RPC 转发服务扫描开始[仅扫描 @Controller 中 public 方法]");
        for (Object controller : controllerBean.values()) {
            Class<?> handlerType = controller.getClass();
            Method[] methods = handlerType.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(RemoteMethod.class)) {
                    continue;
                }
                log.info("通用 RPC 转发服务扫描到 Controller={}, method={}", handlerType.getName(), method.getName());
                RemoteRequestInfo info = helper.getMappingForMethod(method, handlerType);
                validateRpcInfo(info, handlerType, method);
                log.info("rpc info:{}", JSON.toJSONString(info));
                HandlerDefinition definition = new HandlerDefinition(info, controller, method);
                handlerMappings.add(definition);
                handlerAdapters.put(definition, new HandlerAdapter(definition));
            }
        }
        log.info("通用 RPC 转发服务扫描 RpcMethod 完毕,共{}个方法", handlerMappings.size());
    }

    /**
     * 校验 rpcInfo 是否为空
     * @param info
     * @param cls
     * @param method
     */
    private void validateRpcInfo(RemoteRequestInfo info, Class<?> cls, Method method) {
        if (info == null || StringUtils.isEmpty(info.getUrlPattern())) {
            String errorMsg = String.format(SCAN_ERROR_TEMPLATE, cls.getName(), method.getName());
            throw new IllegalStateException(errorMsg);
        }
    }

    /**
     * 处理异常
     * @param t
     * @return
     */
    private String handleException(Throwable t) {
        log.error("异常原因: {}", t.getMessage());
        Result result = Result.failed(t.getMessage());
        return JSON.toJSONString(result);
    }
}