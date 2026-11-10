package com.jjh.kkokjaba.zone.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** 위도·경도 한 점. 따로 테이블을 갖지 않고, 구역 경계 테이블의 컬럼으로 들어간다(@Embeddable). */
@Embeddable
public record GeoPoint(
		@Column(nullable = false) double lat,
		@Column(nullable = false) double lng) {
}
