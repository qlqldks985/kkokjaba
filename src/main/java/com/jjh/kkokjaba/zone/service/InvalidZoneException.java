package com.jjh.kkokjaba.zone.service;

/** 요청에 들어온 구역이 행사장에 없거나, 그 구역에서는 할 수 없는 요청일 때. 400으로 응답한다. */
public class InvalidZoneException extends RuntimeException {

	public InvalidZoneException(String message) {
		super(message);
	}
}
