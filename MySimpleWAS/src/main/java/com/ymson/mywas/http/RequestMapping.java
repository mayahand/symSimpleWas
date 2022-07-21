package com.ymson.mywas.http;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@link SimpleServlet}을 이용한  서블릿 구현 시,</br>
 * 이 {@link Annotation}을 이용하여 서블릿이 매핑되는 URL을 확장 및 제한 할 수 있다.</br>
 * 이 {@link Annotation}이 없는 서블릿인 경우 요청 URL이 HTTP_ROOT 인경우에만 매핑 된다.</br>
 * @author dudals
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
	String[] value() default {};
}
