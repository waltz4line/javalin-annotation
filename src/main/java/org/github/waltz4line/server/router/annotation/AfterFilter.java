package org.github.waltz4line.server.router.annotation;

/**
 * {@link Router} 또한 {@link RouterFilter} 가 선언된 하위 메소드에 {@link AfterFilter} 가 있다면
 *  Endpoint operation 처리 후 해당 필터가 적용
 *  filter handler 에서 설정된 path 로 filter 가 적용된다.
 */
public @interface AfterFilter {

    /**
     * {@link Router} 또는 {@link RouterFilter} 설정된 url path 로 적용
     * @return url path
     */
    String path() default "/";
}
