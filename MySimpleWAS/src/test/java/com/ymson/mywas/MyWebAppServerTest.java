package com.ymson.mywas;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ymson.mywas.conf.Config;

/**
 * 테스트 유닛
 * @author dudals
 *
 */
public class MyWebAppServerTest {
	
private static Config conf;

	private static final String SERVLETS = "./wasRoot/servlets";
	private static final String LOCALHOST_ROOT = "./wasRoot/httpRoot/localhostRoot";
	private static final String CALLBACK_ROOT = "./wasRoot/httpRoot/callbackRoot";
	
	
	@BeforeClass
	public static void setUp(){
		System.out.println("****** Set Up ******");
		MyWebAppServer.main(new String[]{"src/test/resources/conf.json"});
		
		while(!MyWebAppServer.isEstablished() && !MyWebAppServer.isErrorOccured()) try { Thread.sleep(1); } catch (InterruptedException e) {}
		
		conf = Config.getInstance();
	}
	
	@AfterClass
	public static void tearDown(){
		System.out.println("****** Tear Down ******");
		conf = null;
		MyWebAppServer.shutDown();
	}
	
	/**
	 * 테스트용 설정 파일인 src/test/resources/conf.json을 검증하고</br>
	 * Config 인터페이스 구현체의 API가 정상 동작 하는지 확인</br>
	 */
	@Test
	public void configuration_with_json(){
		System.out.println();
		System.out.println("****** configuration_with_json ******");
		
		assertEquals(8080, conf.getPort());
		
		assertEquals(50, conf.getMaxThreads());
		
		assertEquals("TestServlet", conf.getMappingClass("tServlet1"));
		assertEquals("com.ymson.mywas.TestServlet", conf.getMappingClass("tServlet2"));
		assertEquals("com.ymson.myservlet.ServerTimeServlet", conf.getMappingClass("serverTime"));
		
		assertEquals(new File(SERVLETS), conf.getWebAppRepository());
		
		assertEquals(new File(LOCALHOST_ROOT), conf.getHttpRoot("localhost"));
		assertEquals(new File(LOCALHOST_ROOT + "/index.html"), conf.getIndexPage("localhost", "/"));
		assertEquals(new File(LOCALHOST_ROOT + "/error/error403.htm").toPath().normalize(), conf.getErrorPage("localhost", 403, "/").toPath().normalize());
		assertEquals(new File(LOCALHOST_ROOT + "/error/error404.htm").toPath().normalize(), conf.getErrorPage("localhost", 404, "/").toPath().normalize());
		assertEquals(new File(LOCALHOST_ROOT + "/error/error500.htm").toPath().normalize(), conf.getErrorPage("localhost", 500, "/").toPath().normalize());
		
		assertEquals(new File(CALLBACK_ROOT), conf.getHttpRoot("127.0.0.1"));
		assertEquals(new File(CALLBACK_ROOT + "/index.html"), conf.getIndexPage("127.0.0.1", "/"));
		assertEquals(new File(CALLBACK_ROOT + "/error/error403.htm").toPath().normalize(), conf.getErrorPage("127.0.0.1", 403, "/").toPath().normalize());
		assertEquals(new File(CALLBACK_ROOT + "/error/error404.htm").toPath().normalize(), conf.getErrorPage("127.0.0.1", 404, "/").toPath().normalize());
		assertEquals(new File(CALLBACK_ROOT + "/error/error500.htm").toPath().normalize(), conf.getErrorPage("127.0.0.1", 500, "/").toPath().normalize());
	}
	
	/**
	 * - root 경로위치로의 요청 처리 검증(index page 매핑)</br>
	 * - VirtureHost 기능 검증 (연관 테스트 : request_RootURL_response_IndexPage_by_hostname_callback )</br>
	 * - localhost 요청에 대한 응답 처리 검증</br>
	 * @throws IOException
	 */
	@Test
	public void request_RootURL_response_IndexPage_by_hostname_localhost() throws IOException{
		System.out.println();
		System.out.println("****** request_RootURL_response_IndexPage_by_hostname_localhost ******");
		
		URL url = new URL("http://localhost:8080/");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		assertEquals(200, conn.getResponseCode());
		assertEquals("OK", conn.getResponseMessage());
		
		File indexPage = conf.getIndexPage("localhost", "/");
		assertEquals(new File(LOCALHOST_ROOT + "/index.html").toPath().normalize(), indexPage.toPath().normalize());
		
		byte[] exceptedIndexFileData = Files.readAllBytes(indexPage.toPath());
		
		byte[] indexFileData = readInputStream(conn.getInputStream());
		
		assertArrayEquals(exceptedIndexFileData, indexFileData);
	}
	
	/**
	 * - root 경로위치로의 요청 처리 검증(index page 매핑)</br>
	 * - VirtureHost 기능 검증 (연관 테스트 : request_RootURL_response_IndexPage_by_hostname_localhost )</br>
	 * - 127.0.0.1 (callback) 요청에 대한 응답 처리 검증</br>
	 * @throws IOException
	 */
	@Test
	public void request_RootURL_response_IndexPage_by_hostname_callback() throws IOException{
		System.out.println();
		System.out.println("****** request_RootURL_response_IndexPage_by_hostname_callback ******");
		
		URL url = new URL("http://127.0.0.1:8080/");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		assertEquals(200, conn.getResponseCode());
		assertEquals("OK", conn.getResponseMessage());
		
		File indexPage = conf.getIndexPage("127.0.0.1", "/");
		assertEquals(new File(CALLBACK_ROOT + "/index.html").toPath().normalize(), indexPage.toPath().normalize());
		
		byte[] exceptedIndexFileData = Files.readAllBytes(indexPage.toPath());
		
		byte[] indexFileData = readInputStream(conn.getInputStream());
		
		assertArrayEquals(exceptedIndexFileData, indexFileData);
	}
	
	/**
	 * HTTP/1.1 404 File Not Found 응답 검증
	 * @throws IOException
	 */
	@Test
	public void request_exe_response_404_File_Not_Found() throws IOException{
		System.out.println();
		System.out.println("****** request_exe_response_404_File_Not_Found ******");
		
		URL url = new URL("http://localhost:8080/file.test");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		assertEquals(404, conn.getResponseCode());
		assertEquals("File Not Found", conn.getResponseMessage());
		
		File errorPage = conf.getErrorPage("localhost", 404, "/");
		assertTrue(errorPage.exists());
		assertEquals(new File(LOCALHOST_ROOT + "/error/error404.htm").toPath().normalize(), errorPage.toPath().normalize());
		
		byte[] exceptedIndexFileData = Files.readAllBytes(errorPage.toPath());
		
		byte[] indexFileData = readInputStream(conn.getErrorStream());
		
		assertArrayEquals(exceptedIndexFileData, indexFileData);
	}
	
	/**
	 * 서블릿 에러 검증
	 * HTTP/1.1 500 Internal Server Error 응답 검증
	 * @throws IOException
	 */
	@Test
	public void request_exe_response_500_Internal_Server_Error() throws IOException{
		System.out.println();
		System.out.println("****** request_exe_response_500_Internal_Server_Error ******");
		
		URL url = new URL("http://localhost:8080/com.ymson.mywas.TestServletError");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		assertEquals(500, conn.getResponseCode());
		assertEquals("Internal Server Error", conn.getResponseMessage());
		
		File errorPage = conf.getErrorPage("localhost", 500, "/");
		assertTrue(errorPage.exists());
		assertEquals(new File(LOCALHOST_ROOT + "/error/error500.htm").toPath().normalize(), errorPage.toPath().normalize());
		
		byte[] exceptedIndexFileData = Files.readAllBytes(errorPage.toPath());
		
		byte[] indexFileData = readInputStream(conn.getErrorStream());
		
		assertArrayEquals(exceptedIndexFileData, indexFileData);
	}
	
	/**
	 * 보안 규칙 검증 - 확장자 exe 인 파일의 접근 요구
	 * HTTP/1.1 403 Forbidden 응답 검증
	 * @throws IOException
	 */
	@Test
	public void request_exe_response_403_Forbidden() throws IOException{
		System.out.println();
		System.out.println("****** request_exe_response_403_Forbidden ******");
		
		URL url = new URL("http://localhost:8080/run.exe");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		assertEquals(403, conn.getResponseCode());
		assertEquals("Forbidden", conn.getResponseMessage());
		
		File errorPage = conf.getErrorPage("localhost", 403, "/");
		assertTrue(errorPage.exists());
		assertEquals(new File(LOCALHOST_ROOT + "/error/error403.htm").toPath().normalize(), errorPage.toPath().normalize());
		
		byte[] exceptedIndexFileData = Files.readAllBytes(errorPage.toPath());
		
		byte[] indexFileData = readInputStream(conn.getErrorStream());
		
		assertArrayEquals(exceptedIndexFileData, indexFileData);
	}
	
	/**
	 * 보안 규칙 검증 - HTTP_ROOT 상위 디렉토리 요청
	 * HTTP/1.1 403 Forbidden 응답 검증
	 * @throws IOException
	 */
	@Test
	public void request_parent_of_http_root_response_403_Forbidden() throws IOException{
		System.out.println();
		System.out.println("****** request_exe_response_403_Forbidden ******");
		
		URL url = new URL("http://localhost:8080/../../a/b/readme.txt");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		assertEquals(403, conn.getResponseCode());
		assertEquals("Forbidden", conn.getResponseMessage());
		
		File errorPage = conf.getErrorPage("localhost", 403, "/");
		assertTrue(errorPage.exists());
		assertEquals(new File(LOCALHOST_ROOT + "/error/error403.htm").toPath().normalize(), errorPage.toPath().normalize());
		
		byte[] exceptedIndexFileData = Files.readAllBytes(errorPage.toPath());
		
		byte[] indexFileData = readInputStream(conn.getErrorStream());
		
		assertArrayEquals(exceptedIndexFileData, indexFileData);
	}
	
	/**
	 * 서블릿 매핑 검증 - TestServlet 의 매핑 검증
	 * 
	 * @throws IOException
	 */
	@Test
	public void request_servlet_TestServlet() throws IOException{
		System.out.println();
		System.out.println("****** request_servlet_TestServlet ******");
		
		URL url = new URL("http://localhost:8080/TestServlet?name=영민");
		
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		assertEquals(200, conn.getResponseCode());
		assertEquals("OK", conn.getResponseMessage());
		
		byte[] indexFileData = readInputStream(conn.getInputStream());
		
		assertArrayEquals("TestServlet : Hello, 영민".getBytes("UTF-8"), indexFileData);
	}
	
	/**
	 * 서블릿 매핑 검증 - com.ymson.mywas.TestServlet 의 매핑 검증
	 * 
	 * @throws IOException
	 */
	@Test
	public void request_servlet_com_ymson_mywas_TestServlet() throws IOException{
		System.out.println();
		System.out.println("****** request_servlet_com_ymson_mywas_TestServlet ******");
		
		URL url = new URL("http://localhost:8080/com.ymson.mywas.TestServlet?name=영민");
		
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		assertEquals(200, conn.getResponseCode());
		assertEquals("OK", conn.getResponseMessage());
		
		byte[] indexFileData = readInputStream(conn.getInputStream());
		
		assertArrayEquals("com.ymson.mywas.TestServlet : Hello, 영민".getBytes("UTF-8"), indexFileData);
	}
	
	/**
	 * 서블릿 매핑 검증 - 설정파일에 명시된 Mapping 규칙 적용
	 * tServlet1 -> TestServlet
	 * @throws IOException
	 */
	@Test
	public void request_servlet_tServlet1_mapping_to_TestServlet() throws IOException{
		System.out.println();
		System.out.println("****** request_servlet_tServlet1_mapping_to_TestServlet ******");
		
		URL url = new URL("http://localhost:8080/tServlet1?name=영민");
		
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		assertEquals(200, conn.getResponseCode());
		assertEquals("OK", conn.getResponseMessage());
		
		byte[] indexFileData = readInputStream(conn.getInputStream());
		
		assertArrayEquals("TestServlet : Hello, 영민".getBytes("UTF-8"), indexFileData);
	}
	
	/**
	 * 서블릿 매핑 검증 - 설정파일에 명시된 Mapping 규칙 적용
	 * tServlet2 -> com.ymson.mywas.TestServlet
	 * @throws IOException
	 */
	@Test
	public void request_servlet_tServlet2_mapping_to_com_ymson_mywas_TestServlet() throws IOException{
		System.out.println();
		System.out.println("****** request_servlet_tServlet2_mapping_to_com_ymson_mywas_TestServlet ******");
		
		URL url = new URL("http://localhost:8080/tServlet2?name=영민");
		
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		assertEquals(200, conn.getResponseCode());
		assertEquals("OK", conn.getResponseMessage());
		
		byte[] indexFileData = readInputStream(conn.getInputStream());
		
		assertArrayEquals("com.ymson.mywas.TestServlet : Hello, 영민".getBytes("UTF-8"), indexFileData);
	}
	
	/**
	 * 서블릿 동적 로딩 검증
	 * webapps 항목으로 설정된 폴더 하위의 jar파일을 동적으로 Class Loader에 등록하여 서블릿을 동적으로 로드 하는 기능에 대한 검증
	 * ServerTimeServlet : 현재 서버의 시간을 출력하는 서블릿
	 * @throws IOException
	 */
	@Test
	public void request_ServerTimeServlet_dynamic_load() throws IOException{
		System.out.println();
		System.out.println("****** request_ServerTimeServlet_dynamic_load ******");
		
		URL url = new URL("http://localhost:8080/com.ymson.myservlet.ServerTimeServlet");
		
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		assertEquals(200, conn.getResponseCode());
		assertEquals("OK", conn.getResponseMessage());
		
		byte[] indexFileData = readInputStream(conn.getInputStream());
		
		assertFalse((0 == indexFileData.length));
	}
	
	/**
	 * 서블릿 동적 로딩 매핑 설정 검증
	 * 매핑 설정 테스트 : 동적 로드 되는 서블릿도 설정 파일을 이용하여 매핑
	 * ServerTimeServlet : 현재 서버의 시간을 출력하는 서블릿
	 * serverTime -> com.ymson.myservlet.ServerTimeServlet;
	 * @throws IOException
	 */
	@Test
	public void request_ServerTimeServlet_dynamic_load_and_mapping() throws IOException{
		System.out.println();
		System.out.println("****** request_ServerTimeServlet_dynamic_load_and_mapping ******");
		
		URL url = new URL("http://localhost:8080/serverTime");
		
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		assertEquals(200, conn.getResponseCode());
		assertEquals("OK", conn.getResponseMessage());
		
		byte[] indexFileData = readInputStream(conn.getInputStream());
		
		assertFalse((0 == indexFileData.length));
	}
	
	/**
	 * 서블릿 @RequestMapping Annotation 사용시 URL Mapping 테스트
	 * 해당 서블릿은 @RequestMapping({"/", "/APP"}) 을 사용하여 구현되었다.
	 * URL이 / 또는 /APP 인경우 매핑이 가능하며, 다른 URL로 요청될 경우 매핑할 수 없다.
	 * URL이 / 인 경우에 대한 검증은 다음 테스트에서 검증 된것으로 간주 한다.
	 * request_ServerTimeServlet_dynamic_load, request_ServerTimeServlet_dynamic_load_and_mapping
	 * URL이 /APP인 경우에 대한 검증을 한다.
	 * @throws IOException
	 */
	@Test
	public void request_ServerTimeServlet_dynamic_load_and_mapping_by_RequestMapping_Annotation() throws IOException{
		System.out.println();
		System.out.println("****** request_ServerTimeServlet_dynamic_load_and_mapping ******");
		
		//@RequestMapping 으로 명시한 URL 요청시
		URL url = new URL("http://localhost:8080/APP/serverTime");
		
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		assertEquals(200, conn.getResponseCode());
		assertEquals("OK", conn.getResponseMessage());
		
		byte[] indexFileData = readInputStream(conn.getInputStream());
		
		assertFalse((0 == indexFileData.length));
		
		//@RequestMapping 으로 명시 하지 않은 URL 요청시
		url = new URL("http://localhost:8080/AAA/serverTime");
		
		conn = (HttpURLConnection) url.openConnection();
		assertEquals(404, conn.getResponseCode());
		assertEquals("File Not Found", conn.getResponseMessage());
		
		File errorPage = conf.getErrorPage("localhost", 404, "/");
		assertTrue(errorPage.exists());
		assertEquals(new File(LOCALHOST_ROOT + "/error/error404.htm").toPath().normalize(), errorPage.toPath().normalize());
		
		byte[] exceptedIndexFileData = Files.readAllBytes(errorPage.toPath());
		
		indexFileData = readInputStream(conn.getErrorStream());
		
		assertArrayEquals(exceptedIndexFileData, indexFileData);
	}
	
	/**
	 * 응답 스트립 데이터를 읽어서 바이트 배열로 반환한다.
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private byte[] readInputStream(InputStream in) throws IOException{
		while(in.available() <= 0) try { Thread.sleep(1); } catch (InterruptedException e) {}
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		while(in.available() > 0){
			byte[] readed = new byte[in.available()];
			in.read(readed);
			bos.write(readed);
		}
		
		return bos.toByteArray();
	}
}
