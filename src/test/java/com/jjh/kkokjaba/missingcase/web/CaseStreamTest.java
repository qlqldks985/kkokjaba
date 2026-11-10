package com.jjh.kkokjaba.missingcase.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * 실제 포트로 서버를 띄우고, 브라우저처럼 SSE 연결을 연 채로 다른 요청을 보내 본다.
 * MockMvc 테스트와 달리 @Transactional을 붙이지 않는다. 커밋이 일어나야 알림이 나가기 때문이다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CaseStreamTest {

	@LocalServerPort
	private int port;

	private final HttpClient http = HttpClient.newHttpClient();
	private final BlockingQueue<Map<String, String>> received = new LinkedBlockingQueue<>();
	private Thread reader;
	private Stream<String> body;

	@BeforeEach
	void connect() throws Exception {
		HttpResponse<Stream<String>> res = http.send(
				HttpRequest.newBuilder(url("/api/cases/stream")).header("Accept", "text/event-stream").build(),
				HttpResponse.BodyHandlers.ofLines());
		assertThat(res.statusCode()).isEqualTo(200);
		body = res.body();

		// SSE 글은 "event:이름 / id:... / data:내용 / 빈 줄" 묶음으로 온다. 빈 줄마다 하나씩 큐에 넣는다.
		reader = new Thread(() -> {
			Map<String, String> event = new HashMap<>();
			for (String line : (Iterable<String>) res.body()::iterator) {
				if (line.isEmpty()) {
					if (event.containsKey("event")) {
						received.add(event);
					}
					event = new HashMap<>();
				} else if (!line.startsWith(":")) { // ":ping" 같은 주석은 무시
					int colon = line.indexOf(':');
					event.put(line.substring(0, colon), line.substring(colon + 1));
				}
			}
		});
		reader.setDaemon(true);
		reader.start();
		assertThat(next().get("event")).isEqualTo("connected");
	}

	@AfterEach
	void disconnect() {
		body.close(); // 브라우저 탭을 닫는 것과 같다
		reader.interrupt();
	}

	@Test
	@DisplayName("사건이 생기고 바뀔 때마다 연결된 화면에 바로 알림이 간다")
	void pushesOpenedAndUpdated() throws Exception {
		String created = post("/api/cases/detections", "{\"clothingColor\":\"purple\",\"zone\":\"gate\"}").body();
		String id = created.replaceAll(".*\"id\":(\\d+).*", "$1");

		Map<String, String> opened = next();
		assertThat(opened.get("event")).isEqualTo("opened");
		assertThat(opened.get("data")).contains("\"id\":" + id, "\"status\":\"DETECTED\"");

		post("/api/cases/" + id + "/assign", "{\"staffName\":\"park\"}");

		Map<String, String> updated = next();
		assertThat(updated.get("event")).isEqualTo("updated");
		assertThat(updated.get("data")).contains("\"status\":\"SEARCHING\"", "\"assignedStaff\":\"park\"");
	}

	@Test
	@DisplayName("규칙 위반으로 실패한 요청은 알림을 보내지 않는다")
	void failedRequestSendsNothing() throws Exception {
		String created = post("/api/cases/detections", "{\"clothingColor\":\"red\",\"zone\":\"pool\"}").body();
		String id = created.replaceAll(".*\"id\":(\\d+).*", "$1");
		next(); // opened

		assertThat(post("/api/cases/" + id + "/shelter", "").statusCode()).isEqualTo(409); // 수색 전에 보호소로: 실패

		assertThat(received.poll(1, TimeUnit.SECONDS)).as("실패한 요청 뒤에는 아무 알림도 없어야 한다").isNull();
	}

	private Map<String, String> next() throws InterruptedException {
		Map<String, String> event = received.poll(5, TimeUnit.SECONDS);
		assertThat(event).as("5초 안에 SSE 이벤트가 와야 한다").isNotNull();
		return event;
	}

	private HttpResponse<String> post(String path, String json) throws Exception {
		return http.send(HttpRequest.newBuilder(url(path))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.build(), HttpResponse.BodyHandlers.ofString());
	}

	private URI url(String path) {
		return URI.create("http://localhost:" + port + path);
	}
}
