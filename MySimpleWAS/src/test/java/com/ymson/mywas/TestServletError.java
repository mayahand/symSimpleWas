package com.ymson.mywas;

import com.ymson.mywas.http.HttpRequest;
import com.ymson.mywas.http.HttpResponse;
import com.ymson.mywas.http.SimpleServlet;

public class TestServletError implements SimpleServlet {

	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		throw new Exception("HTTP/1.1 500 Internal Server Error");
	}

}
