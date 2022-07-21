package com.ymson.mywas.http;

import java.io.IOException;

/**
 * 서블릿 구현을 위한 인터페이스
 * @author dudals
 *
 */
public interface SimpleServlet {
	public void service(HttpRequest request, HttpResponse response) throws IOException, Exception;
}
