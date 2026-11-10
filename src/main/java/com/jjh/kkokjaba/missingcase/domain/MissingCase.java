package com.jjh.kkokjaba.missingcase.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * 미아 사건 하나.
 * 상태를 바꾸는 방법은 아래 메서드뿐이라서, 어디서 호출하든 같은 규칙이 지켜진다.
 */
@Entity
@Table(name = "missing_case")
public class MissingCase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING) // 숫자(0,1,2)가 아니라 이름으로 저장해야 enum 순서를 바꿔도 안전하다
	@JdbcTypeCode(SqlTypes.VARCHAR) // Hibernate는 MySQL에서 enum을 ENUM 컬럼으로 보려 한다. 우리는 VARCHAR로 만들었다고 알려 준다
	@Column(nullable = false, length = 20)
	private CaseStatus status;

	@Column(nullable = false, length = 20)
	private String clothingColor;

	@Column(nullable = false, length = 40)
	private String lastSeenZone;

	@Column(length = 20) // 보호자 팔찌 번호. 캠 감지로 시작하면 신고 전까지 비어 있다.
	private String guardianBand;

	@Column(length = 40)
	private String assignedStaff;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Version // 직원 두 명이 동시에 같은 사건을 바꾸면 나중 요청을 실패시킨다 (낙관적 락)
	private Long version;

	protected MissingCase() {
		// JPA가 DB에서 읽어 올 때 쓰는 기본 생성자
	}

	private MissingCase(CaseStatus status, String clothingColor, String lastSeenZone, String guardianBand) {
		this.status = status;
		this.clothingColor = requireText(clothingColor, "옷 색");
		this.lastSeenZone = requireText(lastSeenZone, "위치");
		this.guardianBand = guardianBand;
	}

	/** 캠이 보호자 없이 혼자 있는 아이를 감지해 사건을 연다. 신원은 아직 모른다. */
	public static MissingCase detectedByCamera(String clothingColor, String zone) {
		return new MissingCase(CaseStatus.DETECTED, clothingColor, zone, null);
	}

	/** 보호자가 직접 신고해 사건을 연다. */
	public static MissingCase reportedByGuardian(String guardianBand, String clothingColor, String lastSeenZone) {
		return new MissingCase(CaseStatus.REPORTED, clothingColor, lastSeenZone, requireText(guardianBand, "팔찌 번호"));
	}

	/** 캠 감지 건에 보호자 신고를 연결한다. 이미 보호자가 있으면 거절한다. */
	public void linkGuardian(String guardianBand) {
		if (this.guardianBand != null) {
			throw new CaseRuleException("이미 보호자가 연결된 사건입니다.");
		}
		if (status == CaseStatus.HANDED_OVER) {
			throw new CaseRuleException("인계가 끝난 사건에는 보호자를 연결할 수 없습니다.");
		}
		this.guardianBand = requireText(guardianBand, "팔찌 번호");
	}

	public void assignStaff(String staffName) {
		moveTo(CaseStatus.SEARCHING);
		this.assignedStaff = requireText(staffName, "직원 이름");
	}

	public void markFound(String zone) {
		moveTo(CaseStatus.FOUND);
		this.lastSeenZone = requireText(zone, "위치");
	}

	public void moveToShelter() {
		moveTo(CaseStatus.SHELTERED);
	}

	/** 보호자가 보여 준 팔찌 번호가 맞을 때만 인계한다. */
	public void handOver(String presentedBand) {
		if (guardianBand == null) {
			throw new CaseRuleException("보호자 신고가 연결되지 않아 인계할 수 없습니다.");
		}
		if (status != CaseStatus.SHELTERED) {
			throw new CaseRuleException("보호소에서 보호 중인 아이만 인계할 수 있습니다. 지금 단계: " + status.label());
		}
		String band = requireText(presentedBand, "팔찌 번호").toUpperCase();
		if (!band.equals(guardianBand.toUpperCase())) {
			throw new CaseRuleException("팔찌 번호가 일치하지 않습니다.");
		}
		moveTo(CaseStatus.HANDED_OVER);
	}

	private void moveTo(CaseStatus next) {
		if (!status.canMoveTo(next)) {
			throw new CaseRuleException(status.label() + "에서 " + next.label() + "(으)로 바로 넘어갈 수 없습니다.");
		}
		this.status = next;
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new CaseRuleException(name + " 값이 비어 있습니다.");
		}
		return value.trim();
	}

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = this.createdAt;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() { return id; }
	public CaseStatus getStatus() { return status; }
	public String getClothingColor() { return clothingColor; }
	public String getLastSeenZone() { return lastSeenZone; }
	public String getAssignedStaff() { return assignedStaff; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public Long getVersion() { return version; }
	public boolean hasGuardian() { return guardianBand != null; }
}
