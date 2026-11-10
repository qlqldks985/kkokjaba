package com.jjh.kkokjaba.zone.domain;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * 행사장 구역 하나. 데이터는 Flyway 마이그레이션(V3)으로 넣고, 앱은 읽기만 한다.
 * 경계 꼭짓점과 캠은 구역 없이는 의미가 없으므로 별도 엔티티가 아니라 값 컬렉션(@ElementCollection)으로 둔다.
 */
@Entity
@Table(name = "zone")
public class Zone {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 40)
	private String name;

	@Column(nullable = false)
	private boolean shelter;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@ElementCollection
	@CollectionTable(name = "zone_boundary_point", joinColumns = @JoinColumn(name = "zone_id"))
	@OrderColumn(name = "seq") // 꼭짓점은 순서가 곧 모양이다
	@BatchSize(size = 50) // 구역 9개의 경계를 9번이 아니라 한 번의 쿼리로 읽는다 (N+1 방지)
	private List<GeoPoint> boundary = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "zone_camera", joinColumns = @JoinColumn(name = "zone_id"))
	@BatchSize(size = 50)
	private List<Camera> cameras = new ArrayList<>();

	protected Zone() {
	}

	public boolean hasCamera() {
		return !cameras.isEmpty();
	}

	public Long getId() { return id; }
	public String getName() { return name; }
	public boolean isShelter() { return shelter; }
	public int getDisplayOrder() { return displayOrder; }
	public List<GeoPoint> getBoundary() { return List.copyOf(boundary); }
	public List<Camera> getCameras() { return List.copyOf(cameras); }
}
