package com.jjh.kkokjaba.missingcase.domain;

/** 사건 규칙을 어기는 요청 (단계 건너뛰기, 팔찌 번호 불일치 등). 나중에 API에서 400/409 응답으로 바꾼다. */
public class CaseRuleException extends RuntimeException {

	public CaseRuleException(String message) {
		super(message);
	}
}
