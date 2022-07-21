package com.ymson.mywas;

import java.io.*;
import java.net.Socket;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ymson.mywas.conf.Config;
import com.ymson.mywas.http.HttpRequest;
import com.ymson.mywas.http.HttpResponse;
import com.ymson.mywas.http.RequestMapping;
import com.ymson.mywas.http.SimpleServlet;

/**
 * 클라이언트(인터넷 브라우저)의 접속을 처리하는 프로세서 클래스
 * @author dudals
 *
 */
public class ConnectionProcessor implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ConnectionProcessor.class);
    private Socket connection;

    public ConnectionProcessor(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
    	BufferedReader in = null;
    	PrintStream out = null;
        try {

        	HttpRequestImpl req = null;
        	HttpResponseImpl res = new HttpResponseImpl();
        	
        	File responsePage = null;
			String responseBody = null;
			
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			out = new PrintStream(connection.getOutputStream(), false, "UTF-8");
			
			String get = in.readLine();
			get = URLDecoder.decode(get, "UTF-8");
			if("".equals(get)) return;
			
			logger.debug("**** REQ 1si Line : " + get);
			
			req = new HttpRequestImpl(get);
			while(in.ready()){
				get = in.readLine();
				if("".equals(get)) break;
				req.setHeaders(get);
			}
			
			while(in.ready()){
				get = in.readLine();
				req.appendBody(get);
			}
			
			// Logging Request Info Start
			logger.info("*********" + connection.getRemoteSocketAddress().toString() + "- [HTTP/1.1 Request Header Info] *********");
			logger.info("METHOD: " + req.getMETHOD());
			logger.info("Request URL: " + req.getURL());
			logger.info("Version: " + req.getVersion());
			
			String[] keys = req.getParameterKeys();
			StringBuffer sb = new StringBuffer("Request Parameter : [");
			for(String k: keys){
				sb.append(k+"=" + req.getParameter(k)+", ");
			}
			if(sb.toString().endsWith(", ")) sb.replace(sb.length()-2, sb.length(), "]");
			else sb.append("]");
			
			logger.info(sb.toString());
			
			keys = req.getHeaders();
			for(String k: keys){
				logger.info(k+": " + req.getHeaderValue(k));
			}
			// Logging Request Info End
			
			int targetIndex = req.getURL().lastIndexOf("/");
			String path = req.getURL().substring(0, targetIndex+1);
			String target = req.getURL().substring(targetIndex+1, req.getURL().length());
			
			try {
				
				switch((byte)0xFF){
					default:{
						
						/*
						 * 스텝 1 - 서블릿 처리
						 * target이 존재한다면
						 *  - target이 servlet으로 매핑 가능한지 확인 후 가능하면 처리 불가능 하면 스텝 2으로 이동
						 *  - 검색되는 클래스가 순수하게 Class인지와 SimpleServlet Interface를 Implements 했는지를 검사하여 참이면 처리하고 거짓이면 다음 스텝으로 넘어간다.
						 *  - 서블릿 매핑이 정상적인 경우라면 보안규칙은 만족한 것으로 볼 수 있다.
						 */
						if(!"".equals(target)){
							Class<?> cls = null;
							try {cls = Class.forName(Config.getInstance().getMappingClass(target));} catch (Exception e) {}
							
							if(cls != null && !cls.isInterface() && !cls.isAnnotation()){
								//Annotation이 존재하는 경우 path에 대한 mapping여부를 체크한다.
								if(cls.isAnnotationPresent(RequestMapping.class)){
									RequestMapping rm = cls.getAnnotation(RequestMapping.class);
									String[] pathforMaps = rm.value();
									boolean isFound = false;
									for(String pathforMap: pathforMaps){
										
										if(!pathforMap.endsWith("/")) pathforMap += "/";
										
										if(path.equals(pathforMap)) {
											((SimpleServlet) cls.newInstance()).service(req, res);
											isFound = true;
											break;
										}
									}
									
									//매핑되어 처리된 경우 step3의 skip을 위해 break;
									if(isFound) break;
								}
								//Annotation이 존재하지 않는 경우 path가 HTTP_ROOT인 경우만 매핑한다.
								else if("/".equals(path)){
									((SimpleServlet) cls.newInstance()).service(req, res);
									break;
								}
							}
						}
						
						//스텝 2 - 접근 제어
						ArrayList<AccessControl> accCtrls = MyWebAppServer.getAccessControl();
						boolean isAccessable = true;
						for(AccessControl accCtrl : accCtrls){
							if(!accCtrl.isAccessable(req, res)) {
								isAccessable = false;
								break;
							}
						}
						if(!isAccessable) break;
						
						//스텝 3 - 경로 처리1 : 경로만 요청된 경우 index 페이지 매핑
						//target가 빈 문자열이면 URL 경로만 요청된 것으로 판단 해당 경로에 config에 정의된 index 파일이 존재하면 처리 
						//index 파일이 존재하지 않는다면 HTTP 404 File Not Found 응답
						if("".equals(target)){
							responsePage = Config.getInstance().getIndexPage(req.getHost(), path);
							if(responsePage == null) res.sendError(404, "File Not Found");
							// HttpResponse에 대한 준비가 완성 되었으니 스텝2 와 스텝3을 차리하지 않기 위해 break처리 
							break;
						}else{
							File httpRoot = Config.getInstance().getHttpRoot(req.getHost());
							File currentDir = new File(httpRoot, path);
							responsePage = new File(currentDir, target);
							
							if(responsePage.exists() && !responsePage.isDirectory()) break;
						}
						
						//URL 최종 매핑 실패 시 HTTP 404 에러
						res.sendError(404, "File Not Found");
						break;
					}	
				}
				
			} catch (Exception e) {
	            logger.error("Internal Server Error " + connection.getRemoteSocketAddress(), e);
	            res.sendError(500, "Internal Server Error");
			}
			
			switch(res.getCode()){
			case 200:
				if(responsePage != null) {
					List<String> lines = Files.readAllLines(responsePage.toPath());
					for(int i=0; i<lines.size(); i++) {
						String line = lines.get(i);
						if(i<lines.size()-1) res.getWriter().println(line);
						else res.getWriter().print(line);
					}
					res.setContentType(URLConnection.getFileNameMap().getContentTypeFor(responsePage.getAbsolutePath()));
				}
				
				break;
			default:
				res.cleanContents();
				responsePage = Config.getInstance().getErrorPage(req.getHost(), res.getCode(), path);
			    if(responsePage != null) {
			    	List<String> lines = Files.readAllLines(responsePage.toPath());
					for(int i=0; i<lines.size(); i++) {
						String line = lines.get(i);
						if(i<lines.size()-1) res.getWriter().println(line);
						else res.getWriter().print(line);
					}
			    	res.setContentType(URLConnection.getFileNameMap().getContentTypeFor(responsePage.getAbsolutePath()));
			    }else{
			    	responseBody = new StringBuilder("<HTML>\r\n")
			                .append("<HEAD><TITLE>"+res.getMessage()+"</TITLE>\r\n")
			                .append("</HEAD>\r\n")
			                .append("<BODY>")
			                .append("<H1>HTTP Error "+res.getCode()+": "+res.getMessage()+"</H1>\r\n")
			                .append("</BODY>\r\n")
			                .append("</HTML>\r\n")
			                .toString();
			    	res.getWriter().print(responseBody);
			    }
				break;
			}
			
            sendHeader(out, res);
            if(responsePage != null) logger.debug("Response Page: " + responsePage.getAbsolutePath());
            logger.debug("Response Body: \r\n" + res.getContents());
            out.print(URLDecoder.decode(res.getContents(), "UTF-8"));
            
        } catch (IOException ex) {
            logger.error("Error talking to " + connection.getRemoteSocketAddress(), ex);
        } finally {
            try {
                if(out != null) out.flush();
            	connection.close();
            } catch (IOException ex) {
            }
        }
    }
    
    /**
     * 설정된 HttpResponse 객체의 내용을 바탕으로 HTTP Response 헤더를 생성하여 클라이언트에 전송한다.
     * 반드시 컨텐츠를 보내기 전에 보내야 한다.
     * @param out - {@link PrintStream} 클라이언트에 응답을 전송 할 수 있도록 해주는 객체
     * @param res - {@link HttpResponse} 클라이언트레 전송할 응답을 임시로 저장하는 객체
     * @throws IOException
     */
    private void sendHeader(PrintStream out, HttpResponse res)
            throws IOException {
    	
    	logger.info("*********" + connection.getRemoteSocketAddress().toString() + "- [HTTP/1.1 Response Header Info] *********");
    	
    	out.println("HTTP/1.1 "+res.getCode()+" "+res.getMessage());
    	logger.info("HTTP/1.1 "+res.getCode()+" "+res.getMessage());
    	
        Date now = new Date();
        
        out.println("Date: " + now);
        logger.info("Date: " + now);
        
        out.println("Server: JHTTP 2.0");
        logger.info("Server: JHTTP 2.0");
        
        out.println("Content-length: " + res.getContentsLength());
        logger.info("Content-length: " + res.getContentsLength());
        
        out.println("Content-type: " + res.getContentType());
        logger.info("Content-type: " + res.getContentType());
        
        out.println();
    }
    
    private final static class HttpRequestImpl implements HttpRequest {
    	private static final int METHOD = 0;
    	private static final int URL = 1;
    	private static final int VERSION = 2;
		
		private String[] requestInfo;
		private HashMap<String, String> requestParam;
		private HashMap<String, String> requestHeaders;
		
		private BufferedWriter body;
		
		private HttpRequestImpl(String initValue){
			requestHeaders = new HashMap<String, String>();
			body = new BufferedWriter(new StringWriter());
			requestInfo = initValue.split(" ");
			requestParam = new HashMap<String, String>();
			try {
				int paramSepIndex = requestInfo[URL].indexOf("?");
				String partOfUrl = requestInfo[URL];
				String partOfParam = "";
				if(paramSepIndex > 0){
					partOfUrl = requestInfo[URL].substring(0, paramSepIndex);
					partOfParam = requestInfo[URL].substring(paramSepIndex+1, requestInfo[URL].length());
					requestInfo[URL] = partOfUrl;
				}
				
				if(partOfParam.length() > 0){
					if(partOfParam.contains("&")){
						String[] params = partOfParam.split("&");
						for(String param : params){
							int vIndex = param.indexOf("=");
							String k = param.substring(0, vIndex);
							String v = param.substring(vIndex+1, param.length());
							requestParam.put(k, v);
						}
					}else{
						int vIndex = partOfParam.indexOf("=");
						String k = partOfParam.substring(0, vIndex);
						String v = partOfParam.substring(vIndex+1, partOfParam.length());
						requestParam.put(k, v);
					}
				}
			} catch (Exception e) {
				logger.error("Make a HttpRequest Object failed..", e);
			}
		}
		
		@Override
		public String getMETHOD(){
			return requestInfo[METHOD].trim();
		}
		
		@Override
		public String getURL(){
			return requestInfo[URL].trim();
		}
		
		@Override
		public String getVersion(){
			return requestInfo[VERSION].trim();
		}
		
		@Override
		public String getParameter(String key){
			return requestParam.get(key);
		}
		
		public String[] getParameterKeys(){
			return requestParam.keySet().toArray(new String[]{});
		}
		
		public void setHeaders(String... headerLines){
			for(String header: headerLines){
				String[] headerParts = header.split(":");
				requestHeaders.put(headerParts[0].trim(), headerParts[1].trim());
			}
		}
		
		@Override
		public String[] getHeaders(){
			Set<String> keys = requestHeaders.keySet();
			return keys.toArray(new String[]{});
		}
		
		@Override
		public String getHeaderValue(String key){
			return requestHeaders.get(key);
		}
		
		@Override
		public String getHost(){
			return requestHeaders.get("Host");
		}
		
		public void appendBody(CharSequence... b) throws IOException{
			for(CharSequence s : b){
				body.append(s);
				body.newLine();
			}
			body.flush();
		}
		
		@Override
		public String getBody(){
			return body.toString();
		}
	}
    
    private static final class HttpResponseImpl implements HttpResponse {
    	
    	@Override
		public String toString() {
			return writer.toString();
		}
    	
		private PrintStream writer;
		private ByteArrayOutputStream bos;
    	private int code;
    	private String message;
    	private String contentType;
    	private String charset;
    	private HttpResponseImpl() throws UnsupportedEncodingException{
    		bos = new ByteArrayOutputStream();
    		writer = new PrintStream(bos, true, "UTF-8");
    		contentType = "text/html; charset=utf-8";
    		charset = "UTF-8";
    		code = 200;
    		message = "OK";
    	}
    	
    	private void cleanContents(){
    		try {
        		bos.reset();
				bos.flush();
			} catch (IOException e) {
			}
    	}
    	
		@Override
		public void sendError(int code, String message) {
			this.code = code;
			this.message = message;
		}
		
		@Override
		public PrintStream getWriter() {
			return writer;
		}

		@Override
		public int getCode() {
			return code;
		}

		@Override
		public String getMessage() {
			return message;
		}

		@Override
		public void setContentType(String ct) {
			if(!ct.toLowerCase().endsWith("; charset=utf-8")) ct += "; charset=" + charset;
			contentType = ct;
		}

		@Override
		public String getContentType() {
			return contentType;
		}
		
		@Override
		public String getContents() throws UnsupportedEncodingException{
    		return bos.toString("UTF-8");
		}

		@Override
		public int getContentsLength() throws UnsupportedEncodingException {
			return bos.toString("UTF-8").getBytes("UTF-8").length;
		}

		@Override
		public void setCharEncoding(String charset) {
			this.charset = charset;
		}
    }
}