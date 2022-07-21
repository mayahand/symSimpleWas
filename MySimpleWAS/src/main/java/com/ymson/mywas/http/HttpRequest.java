package com.ymson.mywas.http;

/**
 * 요청 헤더를 분석하여 제공하는 클래스를 구현하기 위한 인터 페이스
 * @author dudals
 *
 */
public interface HttpRequest {
	
	/**
	 * 요청 메소드를 반환한다(GET, POST, PUT, HEAD, DELETE, OPTION, 등)
	 * @return {@link String} 메소드
	 */
	abstract public String getMETHOD();
	
	/**
	 * 요청된 URL을 반환한다(HTTP HOST 제외)
	 * @return {@link String} URL
	 */
	abstract public String getURL();
	
	/**
	 * HTTP 버전 정보를 반환한다.
	 * @return {@link String} 버전
	 */
	abstract public String getVersion();
	
	/**
	 * 키 값에 해당하는 요청 파라미터를 반환한다.
	 * @param key - {@link String}
	 * @return {@link String} 요청 파라미터
	 */
	abstract public String getParameter(String key);
	
	/**
	 * 요청 파라미터를 반환한다.
	 * @return {@link String}[] 요청 파라미터의 키값들
	 */
	abstract public String[] getParameterKeys();

	/**
	 * HTTP 헤더의 키값들을 반환한다.
	 * @return {@link String}[] HTTP 헤더의 키값 들
	 */
	abstract public String[] getHeaders();
	
	/**
	 * 키 값에 해당하는 HTTP 헤더 값를 반환한다.
	 * @param key - {@link String}
	 * @return {@link String} HTTP 헤더 값
	 */
	abstract public String getHeaderValue(String key);

	/**
	 * 요청된 HOST 주소를 반환한다.
	 * @return {@link String} HOST 주소
	 */
	abstract public String getHost();
	
	/**
	 * 요청된 BODY 내용을 반환한다.
	 * @return {@link String} BODY 내용
	 */
	abstract public String getBody();
}
