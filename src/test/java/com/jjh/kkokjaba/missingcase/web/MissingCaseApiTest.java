package com.jjh.kkokjaba.missingcase.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * 컨트롤러 → 서비스 → 도메인 → MySQL까지 전부 진짜로 연결해서, HTTP 요청만 가짜(MockMvc)로 보낸다.
 * 테스트마다 트랜잭션을 롤백하므로 kkokjaba_test DB에 데이터가 남지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MissingCaseApiTest {

	@Autowired
	private MockMvc mvc;

	@Test
	@DisplayName("신고부터 인계까지 HTTP로 끝까지 진행된다")
	void fullFlowOverHttp() throws Exception {
		long id = report("B-1137", "노랑", "퍼레이드 거리");

		send("/api/cases/" + id + "/assign", "{\"staffName\":\"박 매니저\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SEARCHING"))
				.andExpect(jsonPath("$.statusLabel").value("수색 중"));
		send("/api/cases/" + id + "/found", "{\"zone\":\"회전목마 앞\"}")
				.andExpect(jsonPath("$.lastSeenZone").value("회전목마 앞"));
		send("/api/cases/" + id + "/shelter", null)
				.andExpect(jsonPath("$.nextSteps[0]").value("HANDED_OVER"));
		send("/api/cases/" + id + "/handover", "{\"guardianBand\":\"b-1137\"}") // 소문자도 통과
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("HANDED_OVER"));

		// 끝난 사건은 관제실 목록에서 빠진다
		mvc.perform(get("/api/cases"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
	}

	@Test
	@DisplayName("사건을 만들면 201과 Location 헤더를 돌려주고, 팔찌 번호는 응답에 없다")
	void createReturns201WithoutBand() throws Exception {
		send("/api/cases/reports", "{\"guardianBand\":\"B-2001\",\"clothingColor\":\"빨강\",\"lastSeenZone\":\"정문\"}")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", containsString("/api/cases/")))
				.andExpect(jsonPath("$.guardianLinked").value(true))
				.andExpect(jsonPath("$.guardianBand").doesNotExist());
	}

	@Test
	@DisplayName("단계를 건너뛰면 409와 이유를 돌려준다")
	void skipStepReturns409() throws Exception {
		long id = report("B-3001", "파랑", "푸드코트");

		send("/api/cases/" + id + "/found", "{\"zone\":\"푸드코트\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("보호자 신고에서 발견(으)로 바로 넘어갈 수 없습니다."));
	}

	@Test
	@DisplayName("팔찌 번호가 다르면 인계되지 않는다")
	void wrongBandReturns409() throws Exception {
		long id = report("B-4001", "초록", "물놀이장");
		send("/api/cases/" + id + "/assign", "{\"staffName\":\"최 매니저\"}");
		send("/api/cases/" + id + "/found", "{\"zone\":\"물놀이장\"}");
		send("/api/cases/" + id + "/shelter", null);

		send("/api/cases/" + id + "/handover", "{\"guardianBand\":\"B-9999\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("팔찌 번호가 일치하지 않습니다."));
	}

	@Test
	@DisplayName("다른 직원이 먼저 바꾼 사건을 옛날 version으로 바꾸려 하면 409")
	@Transactional(propagation = Propagation.NOT_SUPPORTED) // 요청마다 실제로 커밋돼야 version이 올라간다
	void staleVersionReturns409() throws Exception {
		long id = report("B-5001", "하늘", "기념품샵"); // 새 사건 version = 0

		// 직원 B가 먼저 배정 → version 1
		send("/api/cases/" + id + "/assign", "{\"staffName\":\"직원 B\"}", "\"0\"")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.version").value(1));

		// 직원 A는 아직 version 0 화면을 보고 "발견"을 누름 → 거절
		send("/api/cases/" + id + "/found", "{\"zone\":\"기념품샵\"}", "\"0\"")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("동시 수정"));
	}

	@Test
	@DisplayName("필수 값이 비면 400")
	void blankFieldReturns400() throws Exception {
		send("/api/cases/detections", "{\"clothingColor\":\"\",\"zone\":\"정문\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("옷 색을 입력하세요."));
	}

	@Test
	@DisplayName("없는 사건은 404")
	void unknownCaseReturns404() throws Exception {
		mvc.perform(get("/api/cases/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("999999번 사건을 찾을 수 없습니다."));
	}

	private long report(String band, String color, String zone) throws Exception {
		String body = send("/api/cases/reports",
				"{\"guardianBand\":\"%s\",\"clothingColor\":\"%s\",\"lastSeenZone\":\"%s\"}".formatted(band, color, zone))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private ResultActions send(String url, String json) throws Exception {
		return send(url, json, null);
	}

	private ResultActions send(String url, String json, String ifMatch) throws Exception {
		var req = post(url).characterEncoding(StandardCharsets.UTF_8);
		if (json != null) {
			req.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		if (ifMatch != null) {
			req.header("If-Match", ifMatch);
		}
		return mvc.perform(req);
	}
}
