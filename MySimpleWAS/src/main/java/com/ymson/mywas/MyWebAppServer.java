package com.ymson.mywas;


import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.rmi.AlreadyBoundException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ymson.mywas.conf.Config;
import com.ymson.mywas.http.HttpRequest;
import com.ymson.mywas.http.HttpResponse;

/**
 * WAS 서버 구동 메인 클래스
 * @author dudals
 *
 */
public class MyWebAppServer implements Runnable{
	private static final Logger logger = LoggerFactory.getLogger(MyWebAppServer.class);
	private static final String defaultConfigFile = "wasRoot/conf/conf.json";
	private final int port;

	private static Thread myThread;
	private static MyWebAppServer was;

	private static boolean isEstablished;
	private static boolean isErrorOccured;

	private static ArrayList<AccessControl> accesscontrol;

	public MyWebAppServer(String configFilePath) throws IOException, ParseException, AlreadyBoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if(configFilePath == null) configFilePath = defaultConfigFile;
		File confFile = new File(configFilePath);
		Config conf = Config.newInstance(confFile);
		logger.info("Server Config File Load success!! - " + confFile.getAbsolutePath());
		this.port = conf.getPort();
		isEstablished = false;
		isErrorOccured = false;

		defineAccessControl();
	}

	/**
	 * 보안 규칙 구현체를 제작
	 * 추후 동적 로딩으로 보안 규칙을 추가 할 수 있도록 수정 가능
	 */
	public static void defineAccessControl(){
		accesscontrol = new ArrayList<AccessControl>();

		//보안규칙 1 - HTTP_ROOT 상위 디렉토리 접근 제한 403 에러
		accesscontrol.add(new AccessControl() {
			@Override
			public boolean isAccessable(HttpRequest request, HttpResponse response) {
				File root = Config.getInstance().getHttpRoot(request.getHost());
				Path rootPath = root.toPath().normalize().toAbsolutePath();
				String rootPathStr = rootPath.toString();
				while(rootPathStr.contains("..\\")){
					rootPath = rootPath.normalize().toAbsolutePath();
					rootPathStr = rootPath.toString();
				}

				File reqPath = new File(root, request.getURL());
				Path path = reqPath.toPath().normalize().toAbsolutePath();
				String pathStr = path.toString();
				while(pathStr.contains("..\\")){
					path = path.normalize().toAbsolutePath();
					pathStr = path.toString();
				}

				if(!pathStr.startsWith(rootPathStr)){
					response.sendError(403, "Forbidden");
					return false;
				}

				return true;
			}
		});

		//보안규칙 2 - 확장자 exe를 요청 할 경우 HTTP 403 에러
		accesscontrol.add(new AccessControl() {
			@Override
			public boolean isAccessable(HttpRequest request, HttpResponse response) {
				if(request.getURL().toLowerCase().endsWith(".exe")) {
					response.sendError(403, "Forbidden");
					return false;
				}
				return true;
			}
		});
	}

	public static ArrayList<AccessControl> getAccessControl(){
		return accesscontrol;
	}

	@Override
	public void run() {
		ServerSocket server = null;
		try {
			Config conf = Config.getInstance();

			ExecutorService pool = Executors.newFixedThreadPool(conf.getMaxThreads());
			server = new ServerSocket(port);

			logger.info("HTTP SERVER PORT : " + server.getLocalPort());
			while (!Thread.currentThread().isInterrupted()) {
				try {
					isEstablished = true;
					Socket request = server.accept();
					Runnable r = new ConnectionProcessor(request);
					pool.submit(r);
				} catch (IOException ex) {
					logger.error("Connection failed..", ex);
				}

				Thread.sleep(1);
			}
		} catch (IOException e) {
			isErrorOccured = true;
			logger.error("Can not start this server(port:"+port+")", e);
		} catch (InterruptedException e) {
			logger.info("Shutdown this server(port:"+port+")");
		}finally {
			if(server != null) try { server.close(); } catch (IOException e) {}
		}
	}

	public static void main(String[] args) {
		try {
			logger.info("Starting the Simple Web Application Server...");
			//구동 시 파라미터로 설정파일의 경로를 지정 할 수 있다.
			if(isEstablished) throw new Exception("이미 구동 중 입니다.");

			if(myThread == null){
				was = new MyWebAppServer((args.length > 0) ? args[0] : null);
				myThread = new Thread(was);
			}

			myThread.start();
		} catch (Exception e) {
			logger.error("Configuration Error", e);
		}

		return;
	}

	public static boolean isEstablished(){
		return isEstablished;
	}

	public static boolean isErrorOccured(){
		return isErrorOccured;
	}

	public static void shutDown(){
		myThread.interrupt();
		isEstablished = false;
	}
}