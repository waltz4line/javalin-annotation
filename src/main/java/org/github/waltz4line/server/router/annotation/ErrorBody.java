package org.github.waltz4line.router.annotation;

public @interface ErrorBody {

    /**
     * @return HTTP Status Code
     */
    int statusCode();

    /**
     * @return  HTTP Status Code 외에 내부적으로 사용하는 Error Code
     */
    String errorCode() default "";

    /**
     * @return 오류 응답 Body 에 대한 클래스
     */
    Class<?> responseClass() default void.class;

}
