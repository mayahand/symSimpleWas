NHN엔터테인먼트 사전과제(손영민)
==============================

**구현 항목**
------------
** WAS구현은 MySimpleWAS로 이름짓고 작성하였습니다.

** 7번 항목의 SimpleServlet 구현체는 ServerTimeServlet으로 이름짓고 프로젝트를 분리하여 작성하였습니다.

1. HTTP/1.1의 Host 헤더를 해석하여 VirtureHost 기능을 구현하였습니다.
	>* **Virture Host 구현** : wasRoot/conf/conf.json 파일을 이용하여 호스트별로 HTTP_ROOT를 설정 할 수 있습니다.	

2. 스펙에 명시된 사항 및 추가 사항을 설정 파일로 관리 합니다.
	>* **설정 파일 경로**
	>	* wasRoot/conf/conf.json
	>* **설정 파일 내용**
	>	```
	>	{
	>		"server": {
	>			"port": "8080",
	>			"max_threads": "50",
	>			"mapping": {
	>				"serverTime": "com.ymson.myservlet.ServerTimeServlet"
	>			},
	>			"servlets": "./wasRoot/servlets"
	>		},
	>		"virtualHost": [
	>			{
	>				"url": [
	>					"localhost"
	>				],
	>				"httpRoot": "./wasRoot/httpRoot/localhostRoot",
	>				"indexPage": [
	>					"index.htm",
	>					"index.html"
	>				],
	>				"errorPage": {
	>					"403": "./error/error403.htm",
	>					"404": "./error/error404.htm",
	>					"405": "./error/error405.htm"
	>				}
	>			},
	>			{
	>				"url": [
	>					"127.0.0.1"
	>				],
	>				"httpRoot": "./wasRoot/httpRoot/callbackRoot",
	>				"indexPage": [
	>					"index.htm",
	>					"index.html"
	>				],
	>				"errorPage": {
	>					"403": "./error/error403.htm",
	>					"404": "./error/error404.htm",
	>					"405": "./error/error405.htm"
	>				}
	>			}
	>		]
	>	}
	>	```
	>* **서버 설정 방법**
	>	* **포트:** `"port": "8080"`의 8080을 원하는 포트 번호로 변경 한다.
	>	* **스레드:** `"max_threads": "50"`의 50을 원하는 최대 스레드 수로 변경한다.
	>	* **서블릿 매핑:** `"mapping"` 항목에 alias와 서블릿 이름(페이지명 포함)을 추가한다.
	>		```
	>		"mapping": {
	>			"alias1": "com.ymson.myservlet.ServerTimeServlet1",
	>			"alias2": "com.ymson.myservlet.ServerTimeServlet2",
	>		 	"alias3": "com.ymson.myservlet.ServerTimeServlet3"
	>		},
	>		```
	>	* **서블릿 Repository 설정:**
	>		* `"servlets": "./wasRoot/servlets"`의 "./wasRoot/servlets"를 원하는 디렉토리 경로로 설정한다.
	>		* 설정된 위치의 디렉토리안에 있는 jar파일들이 서블릿으로 로드 된다.
	>
	>* **가상호스트 설정 방법**
	>	* **호스트 추가:** `"virtualHost"` 항목에 다음과 같이 가상 호스트 설정을 추가한다
	>	* ','로 구분하여 복수의 Host를 설정 할 수 있다.
	>		```
	>		{
	>			"url": [
	>				"localhost"
	>			],
	>			"httpRoot": "./wasRoot/httpRoot/localhostRoot",
	>			"indexPage": [
	>				"index.htm",
	>				"index.html"
	>			],
	>			"errorPage": {
	>				"403": "./error/error403.htm",
	>				"404": "./error/error404.htm",
	>				"405": "./error/error405.htm"
	>			}
	>		}
	>		```
	>	* **항목 설명**
	>		* **url:** 연상되는 Host주소이며, ',' 로 구분하여 여러개의 Host를 사용 할 수 있다.
	>		* **httpRoot:** 서비스 되는 Host의 HTTP_ROOT 경로를 설정한다.
	>		* **indexPage:** 요청된 URL이 경로인경우(URL 끝이 '/' 인 경우) 설정된 IndexPage를 보여주도록 설정한다. (절대 또는 상대경로로 설정 할 수 있다)
	>		* **errorPage:** 각 HTTP 에러 응답에 보여줄 페이지를 설정 할 수 있다.

3. 403, 404, 500 에러를 처리 합니다.
	>* 403, 404, 500 에러가 발생 할 경우 conf.json에 설정되어 있는 페이지를 응답합니다.

4. 다음과 같은 보안 규칙을 둡니다.
	>* **보안규칙 1: ** HTTP_ROOT 디렉터리의 상위 디렉터리에 대한 접근
	>* **보안규칙 2: ** 확장자가 exe인 파일에 대한 접근
	>* **AccessControl Adaptor**를 Implements하여 보안 규칙을 만들고 등록 할 수 있도록 하였습니다.
	>	```
	>	package com.ymson.mywas;
	>
	>	import com.ymson.mywas.http.HttpRequest;
	>	import com.ymson.mywas.http.HttpResponse;
	>
	>	public interface AccessControl {
	>		public boolean isAccessable(HttpRequest request, HttpResponse reponse);
	>	}
	>	```

5. logback 프레임워크를 이용하여 다음의 로깅 작업을 합니다.
	>* 로그 파일을 하루 단위로 분리 합니다.
	>	- wasRoot/log/ymson-was.log 에 당일 로그를 남기고 날자가 지나가면 연월일을 붙여 분리 합니다.
	>* 로그 내용에 따라 적절한 레벨을 적용합니다. (error, debug, info 세종류의 로그를 출력합니다.)
	>	- 빌드 전에 src/main/resources/logback.xml을 설정하여 기록될 레벨을 설정 할 수 있습니다.
	>* Exception 발생시 해당 StackTrace 전체를 로그파일에 남깁니다.

6. 간단한 WAS를 구현 합니다.
	>* SimpleServlet, HttpRequest, HttpResponse를 구현 하였고 다음과 같이 Servlet을 Implements 할 수 있습니다.
	>
	>	```
	>	package com.ymson.mywas;
	>
	>	import java.io.IOException;
	>	import java.io.PrintStream;
	>	import com.ymson.mywas.http.HttpRequest;
	>	import com.ymson.mywas.http.HttpResponse;
	>	import com.ymson.mywas.http.SimpleServlet;
	>
	>	public class TestServlet implements SimpleServlet {
	>
	>		@Override
	>		public void service(HttpRequest request, HttpResponse response) throws IOException {
	>			response.setContentType("text/html; charset=UTF-8");
	>
	>			PrintStream writer = response.getWriter();
	>			writer.print("Hello, ");
	>			writer.print(request.getParameter("name"));
	>		}
	>	}
	>	```
	>* conf.json 설정 파일을 이용하여 서블릿의 alias를 설정 할 수 있습니다.
	>
	>		```
	>		"mapping": {
	>			"alias1": "com.ymson.myservlet.ServerTimeServlet1",
	>			"alias2": "com.ymson.myservlet.ServerTimeServlet2",
	>		 	"alias3": "com.ymson.myservlet.ServerTimeServlet3"
	>		},
	>		```
	>* RequestMapping Annotation을 이용하여 매핑될 서블릿의 URL 주소를 설정 할 수 있습니다.
	>	- 아래와 같이 구현되는 Servlet에 @RequestMapping({"/", "/APP"})을 주게 되면,
	>	'http:/hostname/com.ymson.myservlet.ServerTimeServlet ' 또는,
	>	'http:/hostname/APP/com.ymson.myservlet.ServerTimeServlet '인 경우에만  매핑됩니다.
	>		```
	>		package com.ymson.myservlet;
	>
	>		import java.io.IOException;
	>		import java.util.Date;
	>
	>		import com.ymson.mywas.http.HttpRequest;
	>		import com.ymson.mywas.http.HttpResponse;
	>		import com.ymson.mywas.http.RequestMapping;
	>		import com.ymson.mywas.http.SimpleServlet;
	>
	>		/**
	>		 * 샘플 서블릿
	>		 * @author dudals
	>		 *
	>		 */
	>		@RequestMapping({"/", "/APP"})
	>		public class ServerTimeServlet implements SimpleServlet {
	>			@Override
	>			public void service(HttpRequest request, HttpResponse response) throws IOException {
	>				Date date = new Date();
	>				response.getWriter().print(date);
	>			}
	>		}
	>		```

7. 현재 시각을 출력하는 SimpleServlet 구현체 작성
	>* ServerTimeServlet이라는 프로젝트로 따로 분리하여 구현하였습니다.
	>* MySimpleWAS 프로젝트의 wasRoot/servlets/ServerTimeServlet.jar로 생성되어 있습니다.
	>* WAS 구동 후 'http://localhost:8080/com.ymson.myservlet.ServerTimeServlet' 또는 'http://localhost:8080/serverTime' 으로 접속 하면 현재 시간을 출력합니다.

8. 구현한 스펙을 검증하는 테스트 케이스를 JUnit4를 이용해서 작성 하였습니다.
	>* com.ymson.mywas.MyWebAppServerTest class에서 테스트를 수행합니다
	>* 프로젝트 폴더 내에서 mvn clean package 며령을 수행하면 테스트를 진행 한 후 이상이 없다면 build를 완료 합니다.
	>* 각 테스트 케이스들은 WAS를 직접 구동 후 HttpURLConnection API를 이용하여 테스트를 하도록 작성되었습니다.
