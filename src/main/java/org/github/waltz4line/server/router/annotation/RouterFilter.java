package org.github.waltz4line.server.router.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * router handler 에 mapping 하기 위한 annotation.
 * e.g. spring filter 역할 수행
 *
 * {@link Router} 와 달리 endpoint 동작을 mapping 해놓은 메소드들이 아니라
 * {@link BeforeFilter} {@link AfterFilter} 과 같은 필터만 관리하기 위한 Annotation.
 *
 * class Type 에 선언
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RouterFilter {


    /**
     * {@link RouterFilter} 가 선언된 하위 메소드에 {@link BeforeFilter} {@link AfterFilter} 가 있다면
     * filter handler 에서 설정된 path 로 filter 가 적용된다.
     *
     * @return url path
     */
    String path() default "";


}
