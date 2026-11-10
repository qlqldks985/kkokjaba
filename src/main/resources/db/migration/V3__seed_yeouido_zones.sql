-- 여의도 한강공원 축제 시나리오의 구역·캠 배치 (시연용).
-- 좌표는 OpenStreetMap에서 확인한 실제 지점(물빛무대, 마포대교, 여의나루역, 원효대교, 63빌딩) 주변에 잡은 근사값이다.
-- 경계는 단순화를 위해 직사각형으로 두었다. 실제 행사에서는 주최 측 배치도로 교체한다.
-- 보호소는 실제 안내센터가 아니라 축제 때 세우는 임시 천막이라고 가정한다.

INSERT INTO zone (id, name, shelter, display_order) VALUES (1, '물빛무대 앞', 0, 1);
INSERT INTO zone_boundary_point (zone_id, seq, lat, lng) VALUES (1, 0, 37.532300, 126.927800), (1, 1, 37.532300, 126.930000), (1, 2, 37.530900, 126.930000), (1, 3, 37.530900, 126.927800);
INSERT INTO zone_camera (zone_id, code, lat, lng) VALUES (1, 'CAM-01', 37.532000, 126.928300);

INSERT INTO zone (id, name, shelter, display_order) VALUES (2, '강변 산책로 (서쪽)', 0, 2);
INSERT INTO zone_boundary_point (zone_id, seq, lat, lng) VALUES (2, 0, 37.534300, 126.923700), (2, 1, 37.534300, 126.925900), (2, 2, 37.532900, 126.925900), (2, 3, 37.532900, 126.923700);

INSERT INTO zone (id, name, shelter, display_order) VALUES (3, '마포대교 남단', 0, 3);
INSERT INTO zone_boundary_point (zone_id, seq, lat, lng) VALUES (3, 0, 37.530400, 126.930100), (3, 1, 37.530400, 126.932300), (3, 2, 37.529000, 126.932300), (3, 3, 37.529000, 126.930100);
INSERT INTO zone_camera (zone_id, code, lat, lng) VALUES (3, 'CAM-02', 37.530100, 126.930600);

INSERT INTO zone (id, name, shelter, display_order) VALUES (4, '미아 보호소 (임시 천막)', 1, 4);
INSERT INTO zone_boundary_point (zone_id, seq, lat, lng) VALUES (4, 0, 37.529300, 126.932700), (4, 1, 37.529300, 126.934900), (4, 2, 37.527900, 126.934900), (4, 3, 37.527900, 126.932700);
INSERT INTO zone_camera (zone_id, code, lat, lng) VALUES (4, 'CAM-03', 37.529000, 126.933200);

INSERT INTO zone (id, name, shelter, display_order) VALUES (5, '여의나루역 출구 광장', 0, 5);
INSERT INTO zone_boundary_point (zone_id, seq, lat, lng) VALUES (5, 0, 37.527800, 126.931500), (5, 1, 37.527800, 126.933700), (5, 2, 37.526400, 126.933700), (5, 3, 37.526400, 126.931500);
INSERT INTO zone_camera (zone_id, code, lat, lng) VALUES (5, 'CAM-04', 37.527500, 126.932000);
INSERT INTO zone_camera (zone_id, code, lat, lng) VALUES (5, 'CAM-05', 37.527500, 126.933200);

INSERT INTO zone (id, name, shelter, display_order) VALUES (6, '푸드트럭 존', 0, 6);
INSERT INTO zone_boundary_point (zone_id, seq, lat, lng) VALUES (6, 0, 37.527500, 126.934700), (6, 1, 37.527500, 126.936900), (6, 2, 37.526100, 126.936900), (6, 3, 37.526100, 126.934700);
INSERT INTO zone_camera (zone_id, code, lat, lng) VALUES (6, 'CAM-06', 37.527200, 126.935200);

INSERT INTO zone (id, name, shelter, display_order) VALUES (7, '잔디마당', 0, 7);
INSERT INTO zone_boundary_point (zone_id, seq, lat, lng) VALUES (7, 0, 37.525900, 126.937400), (7, 1, 37.525900, 126.939600), (7, 2, 37.524500, 126.939600), (7, 3, 37.524500, 126.937400);
INSERT INTO zone_camera (zone_id, code, lat, lng) VALUES (7, 'CAM-07', 37.525600, 126.937900);

INSERT INTO zone (id, name, shelter, display_order) VALUES (8, '원효대교 남단', 0, 8);
INSERT INTO zone_boundary_point (zone_id, seq, lat, lng) VALUES (8, 0, 37.523500, 126.936100), (8, 1, 37.523500, 126.938300), (8, 2, 37.522100, 126.938300), (8, 3, 37.522100, 126.936100);
INSERT INTO zone_camera (zone_id, code, lat, lng) VALUES (8, 'CAM-08', 37.523200, 126.936600);

INSERT INTO zone (id, name, shelter, display_order) VALUES (9, '63빌딩 앞 강변', 0, 9);
INSERT INTO zone_boundary_point (zone_id, seq, lat, lng) VALUES (9, 0, 37.521900, 126.940900), (9, 1, 37.521900, 126.943100), (9, 2, 37.520500, 126.943100), (9, 3, 37.520500, 126.940900);
INSERT INTO zone_camera (zone_id, code, lat, lng) VALUES (9, 'CAM-09', 37.521600, 126.941400);
