package org.github.waltz4line.server.router.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * router handler 에 endpoint 를 mapping 하기 위한 annotation.
 * e.g. spring controller 역할 수행
 *
 * class Type 에 선언
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Router {

    /**
     * {@link Router} 가 선언된 모든 하위의 mapping 메소드
     * {@link GetMapping} {@link PostMapping} {@link PutMapping} {@link DeleteMapping} 의
     * path 의 prefix 에 해당된다.
     * 또한 {@link Router} 가 선언된 하위 메소드에 {@link BeforeFilter} {@link AfterFilter} 가 있다면
     * filter handler 에서 설정된 path 로 filter 가 적용된다.
     *
     * @return url path
     */
    String path() default "";

    /**
     * 해당 옵션은 Open API 가 사용 활성화 시 동작합니다.
     * Open API 사용 시 endpoint 들을 Grouping 한다.
     * 하위의 모든 {@link GetMapping} {@link PostMapping} {@link PutMapping} {@link DeleteMapping} 매핑 클래스는 같은 그룹으로 묶인다.
     * @return Endpoint Grouping tag name
     */
    String tag() default "";

}
