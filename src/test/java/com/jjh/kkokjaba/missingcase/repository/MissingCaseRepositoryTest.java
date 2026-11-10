package com.jjh.kkokjaba.missingcase.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.jjh.kkokjaba.missingcase.domain.CaseStatus;
import com.jjh.kkokjaba.missingcase.domain.MissingCase;

/** 가짜 DB(H2)가 아니라 실제 MySQL(kkokjaba_test)에 저장해 본다. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MissingCaseRepositoryTest {

	@Autowired
	private MissingCaseRepository repository;

	@Autowired
	private TestEntityManager em;

	@Test
	@DisplayName("사건을 저장하고 다시 읽으면 상태와 시각이 그대로 남아 있다")
	void saveAndLoad() {
		MissingCase saved = repository.save(MissingCase.reportedByGuardian("B-1137", "노랑", "퍼레이드 거리"));
		em.flush();
		em.clear(); // 1차 캐시를 비워서 정말 DB에서 다시 읽게 한다

		MissingCase loaded = repository.findById(saved.getId()).orElseThrow();
		assertThat(loaded.getStatus()).isEqualTo(CaseStatus.REPORTED);
		assertThat(loaded.getLastSeenZone()).isEqualTo("퍼레이드 거리");
		assertThat(loaded.hasGuardian()).isTrue();
		assertThat(loaded.getCreatedAt()).isNotNull();
	}

	@Test
	@DisplayName("상태 변경이 DB에 반영된다")
	void statusChangeIsStored() {
		MissingCase c = repository.save(MissingCase.detectedByCamera("빨강", "물놀이장"));
		c.assignStaff("박 매니저");
		em.flush();
		em.clear();

		MissingCase loaded = repository.findById(c.getId()).orElseThrow();
		assertThat(loaded.getStatus()).isEqualTo(CaseStatus.SEARCHING);
		assertThat(loaded.getAssignedStaff()).isEqualTo("박 매니저");
	}

	@Test
	@DisplayName("진행 중인 사건만 골라 최근 순으로 가져온다")
	void findOpenCases() {
		MissingCase done = MissingCase.reportedByGuardian("B-0001", "초록", "정문 광장");
		done.assignStaff("최 매니저");
		done.markFound("정문 광장");
		done.moveToShelter();
		done.handOver("B-0001");
		repository.save(done);
		MissingCase stillOpen = repository.save(MissingCase.detectedByCamera("노랑", "푸드코트"));
		em.flush();

		List<MissingCase> open = repository.findByStatusInOrderByCreatedAtDesc(
				EnumSet.complementOf(EnumSet.of(CaseStatus.HANDED_OVER)));

		// 테스트 DB에는 다른 테스트가 커밋한 사건이 남아 있을 수 있다. "목록이 정확히 이것"이 아니라
		// "방금 만든 진행 중 사건은 있고, 끝난 사건은 없다"를 확인해야 남은 데이터에 흔들리지 않는다.
		assertThat(open).extracting(MissingCase::getId).contains(stillOpen.getId()).doesNotContain(done.getId());
		assertThat(open).noneMatch(c -> c.getStatus() == CaseStatus.HANDED_OVER);
	}
}
