package com.ymson.mywas.http;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * 클라이언트에 보낼 HTTP 응답 값을 임시로 저장하는 버퍼 역할을 한다.
 * 이 버퍼에 Write를 해도 아직 클라이언트에 응답이 전송된것이 아니다.
 * @author dudals
 *
 */
public interface HttpResponse {
	/**
	 * 클라이언트에 보낼 HTTP 응답 코드dhk 메세지를 설정 한다.</br>
	 * 여러번 호출하여 응답 코드를 업데이트 할 수 있다.</br>
	 * sendError(200, "OK"); 가능</br>
	 * @param code - {@link Integer} HTTP 응답 코드
	 * @param message - {@link String} HTTP 응답 메세지
	 */
	public void sendError(int code, String message);
	
	/**
	 * HTTP 응답 데이터를 보내기 위한 {@link PrintStream} 을 반환한다.
	 * @return {@link PrintStream}
	 */
	public PrintStream getWriter();
	
	/**
	 * 현재 설정되어있는 HTTP 응답 코드를 반환한다.
	 * @return {@link Integer} HTTP 응답 코드
	 */
	public int getCode();
	
	/**
	 * 현재 설정되어있는 HTTP 응답 메세지를 반환한다.
	 * @return {@link String} HTTP 응답 메세지
	 */
	public String getMessage();
	
	/**
	 * 응답될 페이지의 컨텐츠 타입을 설정 할 수 있다.</br>
	 * Ex) setContentType("text/html; charset=utf-8");<br>
	 * @param contentType - {@link String} 컨텐츠 타입
	 */
	public void setContentType(String contentType);
	
	/**
	 * 설정된 응답될 페이지의 컨텐츠 타입을 반환한다.
	 * @return {@link String} 컨텐츠 타입
	 */
	public String getContentType();
	
	/**
	 * 현재까지 저장된 응답 페이지 컨텐츠 내용을 charset에 맞게 인코딩 하여 반환한다.
	 * @param charsetName - {@link String} 인코딩할 Charset Name
	 * @return {@link String} 컨텐츠 내용
	 * @throws UnsupportedEncodingException
	 */
	public String getContents() throws UnsupportedEncodingException;
	
	/**
	 * 현재까지 저장된 응답 페이지 컨텐츠 내용의 바이트 길이를 반환한다.
	 * @return {@link Integer} 컨텐츠 바이트 길이
	 * @throws UnsupportedEncodingException 
	 */
	public int getContentsLength() throws UnsupportedEncodingException;
	
	/**
	 * 인코딩할 Charset을 선택한다.
	 * @param charset
	 */
	public void setCharEncoding(String charset);
}
