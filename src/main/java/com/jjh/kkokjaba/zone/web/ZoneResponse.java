package com.jjh.kkokjaba.zone.web;

import java.util.List;

import com.jjh.kkokjaba.zone.domain.Camera;
import com.jjh.kkokjaba.zone.domain.GeoPoint;
import com.jjh.kkokjaba.zone.domain.Zone;

public record ZoneResponse(
		String name,
		boolean shelter,
		boolean hasCamera,
		List<GeoPoint> boundary,
		List<Camera> cameras) {

	public static ZoneResponse from(Zone z) {
		return new ZoneResponse(z.getName(), z.isShelter(), z.hasCamera(), z.getBoundary(), z.getCameras());
	}
}
