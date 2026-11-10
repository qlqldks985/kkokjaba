package com.jjh.kkokjaba.missingcase.web;

import java.time.LocalDateTime;
import java.util.List;

import com.jjh.kkokjaba.missingcase.domain.CaseStatus;
import com.jjh.kkokjaba.missingcase.domain.MissingCase;

/**
 * 응답 모양. 팔찌 번호는 일부러 넣지 않는다.
 * 팔찌 번호는 인계할 때 보호자가 "본인임을 증명하는 값"이라, 화면에 보이면 아무나 인계받을 수 있게 된다.
 * 대신 보호자가 연결됐는지(guardianLinked)만 알려 준다.
 */
public record CaseResponse(
		Long id,
		CaseStatus status,
		String statusLabel,
		List<CaseStatus> nextSteps,
		String clothingColor,
		String lastSeenZone,
		boolean guardianLinked,
		String assignedStaff,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		Long version) { // 화면이 이 값을 들고 있다가 버튼을 누를 때 If-Match 헤더로 돌려보낸다

	public static CaseResponse from(MissingCase c) {
		return new CaseResponse(
				c.getId(),
				c.getStatus(),
				c.getStatus().label(),
				c.getStatus().nextSteps().stream().sorted().toList(), // 화면이 "다음에 누를 수 있는 버튼"을 그릴 때 쓴다
				c.getClothingColor(),
				c.getLastSeenZone(),
				c.hasGuardian(),
				c.getAssignedStaff(),
				c.getCreatedAt(),
				c.getUpdatedAt(),
				c.getVersion());
	}
}
