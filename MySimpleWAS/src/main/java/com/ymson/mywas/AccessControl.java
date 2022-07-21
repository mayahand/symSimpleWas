package com.ymson.mywas;

import com.ymson.mywas.http.HttpRequest;
import com.ymson.mywas.http.HttpResponse;

public interface AccessControl {
	public boolean isAccessable(HttpRequest request, HttpResponse reponse);
}
