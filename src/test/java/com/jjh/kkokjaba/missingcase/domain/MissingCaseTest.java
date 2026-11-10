package com.jjh.kkokjaba.missingcase.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MissingCaseTest {

	private MissingCase shelteredCase(String band) {
		MissingCase c = MissingCase.reportedByGuardian(band, "노랑", "퍼레이드 거리");
		c.assignStaff("박 매니저");
		c.markFound("푸드코트");
		c.moveToShelter();
		return c;
	}

	@Test
	@DisplayName("신고부터 인계까지 순서대로 진행된다")
	void fullFlow() {
		MissingCase c = shelteredCase("B-1137");

		c.handOver("b-1137"); // 대소문자는 구분하지 않는다

		assertThat(c.getStatus()).isEqualTo(CaseStatus.HANDED_OVER);
		assertThat(c.getLastSeenZone()).isEqualTo("푸드코트");
	}

	@Test
	@DisplayName("발견 전에 보호소로 옮길 수 없다 (단계 건너뛰기 금지)")
	void cannotSkipStep() {
		MissingCase c = MissingCase.reportedByGuardian("B-1137", "노랑", "퍼레이드 거리");

		assertThatThrownBy(c::moveToShelter)
				.isInstanceOf(CaseRuleException.class)
				.hasMessageContaining("바로 넘어갈 수 없습니다");
		assertThat(c.getStatus()).isEqualTo(CaseStatus.REPORTED);
	}

	@Test
	@DisplayName("팔찌 번호가 다르면 인계되지 않는다")
	void wrongBandRejected() {
		MissingCase c = shelteredCase("B-1137");

		assertThatThrownBy(() -> c.handOver("B-9999"))
				.isInstanceOf(CaseRuleException.class)
				.hasMessageContaining("일치하지 않습니다");
		assertThat(c.getStatus()).isEqualTo(CaseStatus.SHELTERED);
	}

	@Test
	@DisplayName("캠 감지 건은 보호자를 연결하기 전까지 인계할 수 없다")
	void detectedCaseNeedsGuardian() {
		MissingCase c = MissingCase.detectedByCamera("빨강", "물놀이장");
		c.assignStaff("최 매니저");
		c.markFound("물놀이장");
		c.moveToShelter();

		assertThatThrownBy(() -> c.handOver("B-1137"))
				.isInstanceOf(CaseRuleException.class)
				.hasMessageContaining("보호자 신고가 연결되지 않아");

		c.linkGuardian("B-1137");
		c.handOver("B-1137");
		assertThat(c.getStatus()).isEqualTo(CaseStatus.HANDED_OVER);
	}

	@Test
	@DisplayName("이미 보호자가 있는 사건에 다른 보호자를 연결할 수 없다")
	void cannotLinkTwice() {
		MissingCase c = MissingCase.reportedByGuardian("B-1137", "노랑", "정문 광장");

		assertThatThrownBy(() -> c.linkGuardian("B-2222"))
				.isInstanceOf(CaseRuleException.class);
	}

	@Test
	@DisplayName("빈 값으로는 사건을 만들 수 없다")
	void blankValuesRejected() {
		assertThatThrownBy(() -> MissingCase.detectedByCamera(" ", "물놀이장"))
				.isInstanceOf(CaseRuleException.class)
				.hasMessageContaining("옷 색");
	}
}
