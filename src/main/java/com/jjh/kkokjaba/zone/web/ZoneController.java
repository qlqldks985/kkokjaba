package com.jjh.kkokjaba.zone.web;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jjh.kkokjaba.zone.service.ZoneService;

@RestController
public class ZoneController {

	private final ZoneService service;
	private final String naverMapKeyId;

	public ZoneController(ZoneService service, @Value("${kkokjaba.map.naver-key-id:}") String naverMapKeyId) {
		this.service = service;
		this.naverMapKeyId = naverMapKeyId;
	}

	/** 지도에 그릴 구역 경계와 캠 위치. 화면은 구역을 하드코딩하지 않고 여기서 받아 간다. */
	@GetMapping("/api/zones")
	public List<ZoneResponse> zones() {
		return service.allZones();
	}

	/**
	 * 지도 설정. 네이버 지도 Client ID는 브라우저에 그대로 노출되는 값이라 비밀은 아니지만,
	 * 다른 사람이 내 사용량을 쓰지 않도록 git에는 올리지 않고 로컬 설정(config/local.properties)에서 읽는다.
	 * 값이 없으면 화면은 네이버 지도 대신 간이 지도로 그린다.
	 */
	@GetMapping("/api/map-config")
	public Map<String, Object> mapConfig() {
		return Map.of("naverKeyId", naverMapKeyId.isBlank() ? "" : naverMapKeyId);
	}
}
