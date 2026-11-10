package com.jjh.kkokjaba.missingcase.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jjh.kkokjaba.missingcase.domain.MissingCase;
import com.jjh.kkokjaba.missingcase.service.MissingCaseService;

import jakarta.validation.Valid;

/**
 * 사건 API.
 * 상태를 PATCH {"status":"FOUND"}처럼 아무 값으로나 바꾸게 열지 않고, 행동마다 주소를 따로 둔다.
 * "발견 처리"에는 발견 위치가, "인계"에는 팔찌 번호가 꼭 필요하기 때문이다.
 */
@RestController
@RequestMapping("/api/cases")
public class MissingCaseController {

	private final MissingCaseService service;

	public MissingCaseController(MissingCaseService service) {
		this.service = service;
	}

	@GetMapping
	public List<CaseResponse> openCases() {
		return service.openCases().stream().map(CaseResponse::from).toList();
	}

	@GetMapping("/{id}")
	public CaseResponse get(@PathVariable Long id) {
		return CaseResponse.from(service.get(id));
	}

	/** 보호자 신고로 사건 열기. 201 Created와 함께 새 사건 주소를 Location 헤더에 담는다. */
	@PostMapping("/reports")
	public ResponseEntity<CaseResponse> report(@Valid @RequestBody CaseRequests.Report req) {
		return created(service.report(req.guardianBand(), req.clothingColor(), req.lastSeenZone()));
	}

	/** 캠 감지로 사건 열기. 나중에 Flask/YOLO 서버가 이 주소를 부르게 된다. */
	@PostMapping("/detections")
	public ResponseEntity<CaseResponse> detect(@Valid @RequestBody CaseRequests.Detection req) {
		return created(service.detect(req.clothingColor(), req.zone()));
	}

	// 아래 단계 변경 주소들은 If-Match: "3" 헤더(화면이 마지막으로 본 version)를 선택적으로 받는다.
	// HTTP 표준 헤더라 별도 필드를 만들지 않아도 되고, 본문이 없는 /shelter에도 똑같이 쓸 수 있다.

	@PostMapping("/{id}/guardian")
	public CaseResponse linkGuardian(@PathVariable Long id, @Valid @RequestBody CaseRequests.Guardian req,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
		return CaseResponse.from(service.linkGuardian(id, req.guardianBand(), version(ifMatch)));
	}

	@PostMapping("/{id}/assign")
	public CaseResponse assign(@PathVariable Long id, @Valid @RequestBody CaseRequests.Assign req,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
		return CaseResponse.from(service.assignStaff(id, req.staffName(), version(ifMatch)));
	}

	@PostMapping("/{id}/found")
	public CaseResponse found(@PathVariable Long id, @Valid @RequestBody CaseRequests.Found req,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
		return CaseResponse.from(service.markFound(id, req.zone(), version(ifMatch)));
	}

	@PostMapping("/{id}/shelter")
	public CaseResponse shelter(@PathVariable Long id,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
		return CaseResponse.from(service.moveToShelter(id, version(ifMatch)));
	}

	@PostMapping("/{id}/handover")
	public CaseResponse handOver(@PathVariable Long id, @Valid @RequestBody CaseRequests.Guardian req,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
		return CaseResponse.from(service.handOver(id, req.guardianBand(), version(ifMatch)));
	}

	private ResponseEntity<CaseResponse> created(MissingCase c) {
		return ResponseEntity.created(URI.create("/api/cases/" + c.getId())).body(CaseResponse.from(c));
	}

	/** If-Match 값 "3" 또는 W/"3"에서 숫자만 꺼낸다. 헤더가 없으면 null. */
	private static Long version(String ifMatch) {
		if (ifMatch == null || ifMatch.isBlank()) {
			return null;
		}
		String digits = ifMatch.replace("W/", "").replace("\"", "").trim();
		try {
			return Long.valueOf(digits);
		} catch (NumberFormatException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "If-Match 헤더는 사건 version 숫자여야 합니다.");
		}
	}
}
