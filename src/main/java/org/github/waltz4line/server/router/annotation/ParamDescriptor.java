package org.github.waltz4line.server.router.annotation;

/**
 * Path parameter 나 Query parameter 에 대한 설명을 정의한 Annotation
 */
public @interface ParamDescriptor {

    /**
     * @return parameter 이름
     */
    String name();

    /**
     * @return parameter 자료형
     */
    Class<?> type() default void.class;

    /**
     * @return parameter 설명
     */
    String description() default "";
}
