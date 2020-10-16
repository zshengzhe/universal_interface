package org.zsz.uniitf.dispatcher.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配合 通用接口 {@link } 使用
 * 只有在 {@link org.springframework.stereotype.Controller} 注解的类中生效
 * 仅做标记使用,有此注解可通过通用接口调用
 * @author Zhang Shengzhe
 * @create 2020-05-28 10:31
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteMethod {
}