package com.jjh.kkokjaba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // SSE 연결 유지용 heartbeat(@Scheduled)를 켠다
public class KkokjabaApplication {

	public static void main(String[] args) {
		SpringApplication.run(KkokjabaApplication.class, args);
	}
}
