package com.jjh.kkokjaba.common.web;

import java.util.stream.Collectors;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.jjh.kkokjaba.missingcase.domain.CaseRuleException;
import com.jjh.kkokjaba.missingcase.service.CaseNotFoundException;
import com.jjh.kkokjaba.zone.service.InvalidZoneException;

/**
 * 예외를 HTTP 응답으로 바꾸는 곳을 한 군데로 모은다.
 * 응답은 표준 형식(RFC 9457 Problem Details)으로 통일해서, 화면은 항상 detail만 읽어 보여 주면 된다.
 *
 *   400 요청 형식이 틀림 (빈 칸, 깨진 JSON)
 *   404 없는 사건
 *   409 형식은 맞지만 지금 단계에서는 할 수 없는 일 (단계 건너뛰기, 팔찌 불일치, 동시 수정)
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(CaseRuleException.class)
	public ProblemDetail caseRule(CaseRuleException e) {
		return problem(HttpStatus.CONFLICT, "진행할 수 없는 요청", e.getMessage());
	}

	@ExceptionHandler(InvalidZoneException.class)
	public ProblemDetail invalidZone(InvalidZoneException e) {
		return problem(HttpStatus.BAD_REQUEST, "구역 오류", e.getMessage());
	}

	@ExceptionHandler(CaseNotFoundException.class)
	public ProblemDetail notFound(CaseNotFoundException e) {
		return problem(HttpStatus.NOT_FOUND, "사건 없음", e.getMessage());
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ProblemDetail concurrentUpdate(OptimisticLockingFailureException e) {
		return problem(HttpStatus.CONFLICT, "동시 수정",
				"다른 직원이 먼저 이 사건을 수정했습니다. 새로 고친 뒤 다시 시도하세요.");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail invalid(MethodArgumentNotValidException e) {
		String detail = e.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getDefaultMessage())
				.distinct()
				.collect(Collectors.joining(" "));
		return problem(HttpStatus.BAD_REQUEST, "입력값 오류", detail);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail unreadable(HttpMessageNotReadableException e) {
		return problem(HttpStatus.BAD_REQUEST, "입력값 오류", "요청 본문(JSON)을 읽을 수 없습니다.");
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
		pd.setTitle(title);
		return pd;
	}
}
