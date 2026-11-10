package com.jjh.kkokjaba.missingcase.web;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.jjh.kkokjaba.missingcase.event.CaseChanged;

/**
 * 관제실 화면들과 열어 둔 SSE 연결을 들고 있다가, 사건이 바뀌면 모두에게 보낸다.
 *
 * SSE(Server-Sent Events): 브라우저가 연결을 하나 열어 두면 서버가 그 위로 계속 글을 흘려보내는 방식.
 * 서버 → 화면 한 방향만 필요할 때는 WebSocket보다 단순하고, 끊기면 브라우저가 알아서 다시 붙는다.
 */
@Component
public class CaseEventBroadcaster {

	private static final Logger log = LoggerFactory.getLogger(CaseEventBroadcaster.class);
	private static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

	// 보내는 동안 다른 스레드가 연결을 추가·삭제해도 안전한 리스트. 읽기(보내기)가 훨씬 잦을 때 알맞다.
	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	public SseEmitter subscribe() {
		SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError(e -> emitters.remove(emitter));
		emitters.add(emitter);

		// 첫 이벤트를 바로 보내야 응답 헤더가 나가서, 화면이 "연결됨"을 알 수 있다.
		send(emitter, SseEmitter.event().name("connected").data("ok"));
		return emitter;
	}

	/**
	 * AFTER_COMMIT: DB에 진짜로 저장된 뒤에만 알린다.
	 * 커밋 전에 보내면, 저장이 실패해 롤백됐는데도 화면에는 "발견!"이 떠 버릴 수 있다.
	 * 이 시점에는 @Version·updatedAt도 확정된 값이라 화면과 DB가 어긋나지 않는다.
	 */
	@TransactionalEventListener
	public void onCaseChanged(CaseChanged event) {
		CaseResponse payload = CaseResponse.from(event.source());
		for (SseEmitter emitter : emitters) {
			send(emitter, SseEmitter.event()
					.name(event.kind().eventName())
					.id(payload.id() + "-" + payload.updatedAt())
					.data(payload, MediaType.APPLICATION_JSON));
		}
	}

	/**
	 * 아무 일 없이 조용하면 중간의 프록시나 공유기가 연결을 끊어 버리는 경우가 있다.
	 * 20초마다 주석 한 줄(":ping")을 보내 살아 있음을 알린다. 화면에는 이벤트로 잡히지 않는다.
	 */
	@Scheduled(fixedRate = 20_000)
	public void heartbeat() {
		for (SseEmitter emitter : emitters) {
			send(emitter, SseEmitter.event().comment("ping"));
		}
	}

	/**
	 * 서버가 꺼질 때 열린 연결을 먼저 닫아 준다.
	 * 스프링은 "처리 중인 요청이 끝날 때까지 최대 30초 기다렸다가 끈다(graceful shutdown)".
	 * SSE 연결은 끝나지 않는 요청이라, 닫아 주지 않으면 매번 30초를 꽉 채워 기다린다.
	 * @PreDestroy는 그 기다림이 끝난 뒤에야 불리므로 너무 늦다. 종료 신호(ContextClosedEvent)를 받자마자 닫는다.
	 */
	@EventListener(ContextClosedEvent.class)
	public void closeAll() {
		emitters.forEach(SseEmitter::complete);
		emitters.clear();
	}

	public int connectionCount() {
		return emitters.size();
	}

	private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
		try {
			emitter.send(event);
		} catch (IOException | IllegalStateException e) {
			// 탭을 닫은 화면. 목록에서 빼고 넘어간다. 한 화면 때문에 다른 화면에 못 보내면 안 된다.
			emitters.remove(emitter);
			log.debug("SSE 연결 정리: {}", e.getMessage());
		}
	}
}
