package com.jjh.kkokjaba.missingcase.service;

public class CaseNotFoundException extends RuntimeException {

	public CaseNotFoundException(Long id) {
		super(id + "번 사건을 찾을 수 없습니다.");
	}
}
