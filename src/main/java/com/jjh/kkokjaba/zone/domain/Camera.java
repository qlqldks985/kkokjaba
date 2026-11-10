package com.jjh.kkokjaba.zone.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** 구역에 설치된 캠 한 대. 나중에 YOLO 감지 서버가 이 code로 "어느 캠에서 봤는지"를 알려 주게 된다. */
@Embeddable
public record Camera(
		@Column(nullable = false, length = 20) String code,
		@Column(nullable = false) double lat,
		@Column(nullable = false) double lng) {
}
