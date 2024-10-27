package org.github.waltz4line.server.router.annotation;


/**
 * {@link Router} 또한 {@link RouterFilter} 가 선언된 하위 메소드에 {@link BeforeFilter} 가 있다면
 * endpoint operation 처리 전 해당 filter가 실행 됨
 * filter handler 에서 설정된 path 로 filter 가 적용된다.
 */
public @interface BeforeFilter {

    /**
     * {@link Router} 또한 {@link RouterFilter} 설정된 url path 로 적용
     * @return url path
     */
    String path() default "/";

}
