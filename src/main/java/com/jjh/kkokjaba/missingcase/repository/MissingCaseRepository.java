package com.jjh.kkokjaba.missingcase.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jjh.kkokjaba.missingcase.domain.CaseStatus;
import com.jjh.kkokjaba.missingcase.domain.MissingCase;

public interface MissingCaseRepository extends JpaRepository<MissingCase, Long> {

	/** 관제실 목록: 아직 끝나지 않은 사건을 최근 순으로. 메서드 이름만으로 쿼리가 만들어진다. */
	List<MissingCase> findByStatusInOrderByCreatedAtDesc(Collection<CaseStatus> statuses);
}
