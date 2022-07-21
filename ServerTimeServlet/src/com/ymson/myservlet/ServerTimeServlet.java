package com.ymson.myservlet;

import java.io.IOException;
import java.util.Date;

import com.ymson.mywas.http.HttpRequest;
import com.ymson.mywas.http.HttpResponse;
import com.ymson.mywas.http.RequestMapping;
import com.ymson.mywas.http.SimpleServlet;

/**
 * 샘플 서블릿
 * @author dudals
 *
 */
@RequestMapping({"/", "/APP"})
public class ServerTimeServlet implements SimpleServlet {
	
	@Override
	public void service(HttpRequest request, HttpResponse response) throws IOException {
		Date date = new Date();
		response.getWriter().print(date);
	}

}
