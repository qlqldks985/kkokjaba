package com.jjh.kkokjaba.missingcase.domain;

import java.util.Set;

/**
 * 미아 사건의 진행 단계.
 * 각 단계는 "다음으로 갈 수 있는 단계"를 스스로 알고 있어서, 단계를 건너뛰는 변경을 막는다.
 *
 * 접수(DETECTED / REPORTED) → 수색 중 → 발견 → 보호 중 → 인계 완료
 */
public enum CaseStatus {
	DETECTED("캠 감지"),
	REPORTED("보호자 신고"),
	SEARCHING("수색 중"),
	FOUND("발견"),
	SHELTERED("보호 중"),
	HANDED_OVER("인계 완료");

	private final String label;

	CaseStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	/** 이 단계에서 바로 넘어갈 수 있는 다음 단계들. */
	public Set<CaseStatus> nextSteps() {
		return switch (this) {
			case DETECTED, REPORTED -> Set.of(SEARCHING);
			case SEARCHING -> Set.of(FOUND);
			case FOUND -> Set.of(SHELTERED);
			case SHELTERED -> Set.of(HANDED_OVER);
			case HANDED_OVER -> Set.of();
		};
	}

	public boolean canMoveTo(CaseStatus next) {
		return nextSteps().contains(next);
	}
}
