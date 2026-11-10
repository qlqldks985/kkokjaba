package com.jjh.kkokjaba.common.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 서버가 살아 있는지 확인하는 가장 단순한 API. 브라우저에서 /api/status 로 확인한다. */
@RestController
public class StatusController {

	@GetMapping("/api/status")
	public Map<String, String> status() {
		return Map.of("service", "kkokjaba", "status", "UP");
	}
}
