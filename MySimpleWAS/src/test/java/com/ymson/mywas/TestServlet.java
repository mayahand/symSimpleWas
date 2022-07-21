package com.ymson.mywas;

import java.io.IOException;
import java.io.PrintStream;

import com.ymson.mywas.http.HttpRequest;
import com.ymson.mywas.http.HttpResponse;
import com.ymson.mywas.http.SimpleServlet;

public class TestServlet implements SimpleServlet {

	@Override
	public void service(HttpRequest request, HttpResponse response) throws IOException {
		response.setContentType("text/html; charset=UTF-8");
		
		PrintStream writer = response.getWriter();
		writer.print(this.getClass().getName() + " : Hello, ");
		writer.print(request.getParameter("name"));
	}
}
