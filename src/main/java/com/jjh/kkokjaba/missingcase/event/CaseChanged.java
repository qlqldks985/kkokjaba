package com.jjh.kkokjaba.missingcase.event;

import com.jjh.kkokjaba.missingcase.domain.MissingCase;

/**
 * "사건이 생겼다 / 바뀌었다"는 사실 하나.
 * 서비스는 이 이벤트를 던지기만 하고, 누가 듣는지(SSE, 나중에 문자 알림 등)는 모른다.
 * 덕분에 알림 방식을 추가해도 서비스 코드는 그대로 둘 수 있다.
 */
public record CaseChanged(Kind kind, MissingCase source) {

	public enum Kind {
		OPENED, UPDATED;

		/** SSE 이벤트 이름. 브라우저에서 addEventListener("opened", ...)로 받는다. */
		public String eventName() {
			return name().toLowerCase();
		}
	}
}
