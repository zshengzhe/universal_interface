package org.zsz.uniitf.dispatcher;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Objects;

/**
 * 远程请求参数信息
 * @author Zhang Shengzhe
 * @create 2020-06-01 16:40
 */
public class RemoteRequestInfo {
    /**
     * url
     */
    private String urlPattern;

    /**
     * method
     */
    private RequestMethod httpMethod;

    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

    public RemoteRequestInfo(String urlPattern, RequestMethod method) {
        this.urlPattern = urlPattern;
        this.httpMethod = method;
    }

    /**
     * 将外部 RemoteRequestInfo 和 this 进行结合
     * @param other
     * @return
     */
    public RemoteRequestInfo combine(RemoteRequestInfo other) {
        // 外部直接覆盖
        if (other.httpMethod != null) {
            this.httpMethod = other.httpMethod;
        }
        if (!StringUtils.isEmpty(other.getUrlPattern())) {
            this.urlPattern = PATH_MATCHER.combine(urlPattern, other.getUrlPattern());
        }
        return this;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public RequestMethod getHttpMethod() {
        return httpMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RemoteRequestInfo that = (RemoteRequestInfo) o;
        return Objects.equals(urlPattern, that.urlPattern) &&
                httpMethod == that.httpMethod;
    }

    @Override
    public int hashCode() {
        return Objects.hash(urlPattern, httpMethod);
    }
}
