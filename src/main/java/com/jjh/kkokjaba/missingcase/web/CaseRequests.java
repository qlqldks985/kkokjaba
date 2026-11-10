package com.jjh.kkokjaba.missingcase.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 요청 본문(JSON) 모양. 엔티티를 그대로 받지 않고 요청마다 필요한 칸만 따로 정의한다.
 * 그래야 클라이언트가 status나 id 같은 값을 몰래 끼워 넣어도 무시된다.
 * 길이 제한은 DB 컬럼 길이와 맞춰 두어, DB 오류가 나기 전에 400으로 걸러 낸다.
 */
public final class CaseRequests {

	private CaseRequests() {
	}

	public record Report(
			@NotBlank(message = "팔찌 번호를 입력하세요.") @Size(max = 20) String guardianBand,
			@NotBlank(message = "옷 색을 입력하세요.") @Size(max = 20) String clothingColor,
			@NotBlank(message = "마지막으로 본 위치를 입력하세요.") @Size(max = 40) String lastSeenZone) {
	}

	public record Detection(
			@NotBlank(message = "옷 색을 입력하세요.") @Size(max = 20) String clothingColor,
			@NotBlank(message = "감지된 구역을 입력하세요.") @Size(max = 40) String zone) {
	}

	public record Guardian(
			@NotBlank(message = "팔찌 번호를 입력하세요.") @Size(max = 20) String guardianBand) {
	}

	public record Assign(
			@NotBlank(message = "직원 이름을 입력하세요.") @Size(max = 40) String staffName) {
	}

	public record Found(
			@NotBlank(message = "발견 위치를 입력하세요.") @Size(max = 40) String zone) {
	}
}
