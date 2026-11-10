package com.jjh.kkokjaba.zone.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jjh.kkokjaba.zone.domain.Zone;
import com.jjh.kkokjaba.zone.repository.ZoneRepository;
import com.jjh.kkokjaba.zone.web.ZoneResponse;

@Service
@Transactional(readOnly = true)
public class ZoneService {

	private final ZoneRepository repository;

	public ZoneService(ZoneRepository repository) {
		this.repository = repository;
	}

	/** 지도용. 경계·캠 컬렉션은 지연 로딩이라 트랜잭션 안에서 응답 모양으로 바꿔 내보낸다. */
	public List<ZoneResponse> allZones() {
		return repository.findAllByOrderByDisplayOrderAsc().stream().map(ZoneResponse::from).toList();
	}

	/** 신고 위치·발견 위치는 행사장에 실제로 있는 구역이어야 한다. */
	public Zone requireZone(String name) {
		String trimmed = name == null ? "" : name.trim();
		return repository.findByName(trimmed)
				.orElseThrow(() -> new InvalidZoneException("행사장에 없는 구역입니다: " + trimmed));
	}

	/** 캠 감지는 캠이 설치된 구역에서만 일어날 수 있다. */
	public Zone requireCameraZone(String name) {
		Zone zone = requireZone(name);
		if (!zone.hasCamera()) {
			throw new InvalidZoneException(zone.getName() + "에는 캠이 없어 캠 감지를 받을 수 없습니다.");
		}
		return zone;
	}
}
