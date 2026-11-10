package com.jjh.kkokjaba.missingcase.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 관제실 화면이 연결해 두는 주소.
 * 브라우저: new EventSource("/api/cases/stream")
 */
@RestController
public class CaseStreamController {

	private final CaseEventBroadcaster broadcaster;

	public CaseStreamController(CaseEventBroadcaster broadcaster) {
		this.broadcaster = broadcaster;
	}

	@GetMapping(path = "/api/cases/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream() {
		return broadcaster.subscribe();
	}
}
