package org.github.waltz4line.router.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link Router} 가 선언된 Class 내에 Method 에 선언해야 함
 * endpoint 의 HTTP Method GET 요청에 해당하는 method 에 Mapping
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GetMapping {

    /**
     *{@link Router} 에 설정된 path 와 조합하여 최종 endpoint 로 만들어짐
     * @return url path
     */
    String path();

    /**
     * Open API 할성화 시 사용.
     * @return 최종 endpoint 에 대한 설명
     */
    String description() default "";

    /**
     * Open API 할성화 시 사용. (for now)
     * @return 최종 endpoint 에 path parameter 들에 대한 설명
     */
    ParamDescriptor[] pathParams() default {};

    /**
     * Open API 할성화 시 사용. (for now)
     * @return 최종 endpoint 에 query parameter 에 대한 설명
     */
    ParamDescriptor[] queryPrams() default {};

    /**
     * Open API 할성화 시 사용.  (for now)
     * @return status 200 일 경우 응답 Body Class 타입
     */
    Class<?> responseBody() default Void.class;

    /**
     * Open API 할성화 시 사용.  (for now)
     * @return 요청 처리 중 에러 발생 시에 대한 응답 Body 에 대한 설명
     */
    ErrorBody[] errorBody() default {};

}
