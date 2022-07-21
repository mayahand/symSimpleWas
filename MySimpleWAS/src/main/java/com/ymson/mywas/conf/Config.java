package com.ymson.mywas.conf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.AlreadyBoundException;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * 설정 파일을 로드하는 클래스
 * @author dudals
 *
 */
public abstract class Config {
	private static Config me;
	
	public static Config newInstance(File confFile) throws FileNotFoundException, IOException, ParseException, AlreadyBoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		if(me != null) throw new AlreadyBoundException("Already Initialized..");
		me = new ConfigurationImpl(confFile);
		return me;
	}
	
	public static Config getInstance() {
		if(me == null) throw new NullPointerException("Not Initialized Yet..");
		return me;
	}
	
	/**
	 * 설정된 포트 번호를 반환 한다.
	 * @return port - {@link Integer}
	 */
	abstract public int getPort();
	
	/**
	 * 설정된 최대 {@link Thread} 수를  반환 한다.
	 * @return {@link Integer} 값
	 */
	abstract public int getMaxThreads();
	
	/**
	 * 호스트별 HTTP_ROOT 폴더를 반환한다.
	 * @param hostName : {@link String}
	 * @return {@link File} 객체
	 */
	abstract public File getHttpRoot(String hostName);
	
	/**
	 * 호스트 및 에러 코드별로 설정된 경로에 맞는(절대, 상대) 오류페이지 파일은 반환한다.
	 * @param hostName : {@link String} 호스트 네임
	 * @param code : {@link Integer} HTTP 에러 코드
	 * @param currentUrl {@link String} 현재 URL
	 * @return {@link File} 객체
	 */
	abstract public File getErrorPage(String hostName, int code, String currentUrl);
	
	/**
	 * 호스트별로 설정된 경로에 맞는(절대, 상대) index 페이지 파일을 반환한다.
	 * @param hostName : {@link String} 호스트 네임
	 * @param currentUrl : {@link String} 현재 URL
	 * @return {@link File} 객체
	 */
	abstract public File getIndexPage(String hostName, String currentUrl);
	
	/**
	 * 설정파일에 Mapping 규칙이 존재 할 경우 해당 규칙에 따라 요청 URL과 서블릿이 Mapping 된다.
	 * @param target
	 * @return package 및 class 이름
	 */
	abstract public String getMappingClass(String target);
	
	/**
	 * 서블릿 jar파일이 저장되어 있는 폴더를 반환한다.
	 * 이 폰더의 jar파일을 읽어 servlet으로 등록 한다.
	 * @param target
	 * @return package 및 class 이름
	 */
	abstract public File getWebAppRepository();
	
	private final static class ConfigurationImpl extends Config {

		private int serverPort;
		private int max_threads;
		private JSONArray virtureHost;
		private JSONObject mappingInfo;
		private File servlets;
		
		private ConfigurationImpl(File confFile) throws FileNotFoundException, IOException, ParseException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
			JSONObject conf = (JSONObject) (new JSONParser().parse(new FileReader(confFile)));
			JSONObject server = (JSONObject) conf.get("server");
			serverPort = Integer.parseInt((String) server.get("port"));
			max_threads = Integer.parseInt((String) server.get("max_threads"));
			mappingInfo = (JSONObject) server.get("mapping");
			servlets = new File((String) server.get("servlets"));
			if(!servlets.exists() || !servlets.isDirectory()) throw new FileNotFoundException("해당 경로가 없거나 디렉토리가 아닙니다.");
			
			String[] jarFiles = servlets.list(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					if(name.toLowerCase().endsWith("jar")) return true;
					return false;
				}
			});
			
			URLClassLoader loader = (URLClassLoader)ClassLoader.getSystemClassLoader();
			Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
			method.setAccessible(true);
			for(String jarFile : jarFiles){
				File file = new File(servlets, jarFile);
				method.invoke(loader, new Object[]{file.toURI().toURL()});
			}
			
			virtureHost = (JSONArray) conf.get("virtualHost");
			//new File(defaultHost_httpRoot, (String) page.get("page"));
		}
		
		@Override
		public int getPort(){
			return serverPort;
		}
		
		@Override
		public int getMaxThreads(){
			return max_threads;
		}
		
		@Override
		public File getHttpRoot(String hostName){
			File root = null;

			JSONObject host = getVirtureHostJSONObject(hostName);
			if(host == null) return null;

			root = new File((String) host.get("httpRoot"));

			return root.isDirectory() ? root : null;
		}
		
		@Override
		public File getErrorPage(String hostName, int code, String currentUrl){
			File page = null;
			File dir = getHttpRoot(hostName);
			if(dir == null) return null;
			
			JSONObject host = getVirtureHostJSONObject(hostName);
			if(host == null) return null;

			JSONObject errorPages = (JSONObject) host.get("errorPage");
			String errorPageName = (String) errorPages.get("" + code);
			
			if(errorPageName == null || "".equals(errorPageName)) return null;
			
			errorPageName = errorPageName.replace("/", "\\");
			
			if(errorPageName.indexOf(":\\") != -1){
				page = new File(errorPageName);
			}else{
				page = new File(dir, errorPageName);
			}
			
			if(page.exists() && !page.isDirectory()) return page;
			
			return null;
		}
		
		@Override
		public File getIndexPage(String hostName, String url){
			File page = null;
			File dir = new File(getHttpRoot(hostName), url);
			
			JSONObject host = getVirtureHostJSONObject(hostName);
			if(host == null) return null;

			JSONArray indexPages = (JSONArray) host.get("indexPage");
			for(int i=0; i<indexPages.size(); i++){
				String indexPageName = (String) indexPages.get(i);
				page = new File(dir, indexPageName);
				if(page.exists() && !page.isDirectory()) return page;
			}
			
			return null;
		}

		private JSONObject getVirtureHostJSONObject(String hostName){
			for(int i=0; i<virtureHost.size(); i++){
				JSONObject host = (JSONObject) virtureHost.get(i);
				JSONArray hosturls = (JSONArray) host.get("url");
				for(int j=0; j<hosturls.size(); j++){
					Object hosturl = hosturls.get(j);
					if(hostName.equals(hosturl)) return host;
				}
			}

			return null;
		}

		@Override
		public String getMappingClass(String target) {
			String mappedTarget = (String) mappingInfo.get(target);
			if(mappedTarget != null && !"".equals(mappedTarget)) target = mappedTarget;
			return target;
		}

		@Override
		public File getWebAppRepository() {
			return servlets;
		}
	}
}
