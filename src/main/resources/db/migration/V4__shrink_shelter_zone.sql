-- 실제 지도에서 확인해 보니 미아 보호소 구역의 북동쪽 일부가 강 위에 걸려 있었다.
-- 보호소는 임시 천막 한 동이므로 크기를 줄이고 육지 쪽으로 옮긴다.
-- V3은 이미 적용된 파일이라 고치지 않고, 이렇게 새 버전으로 바로잡는다.

DELETE FROM zone_boundary_point WHERE zone_id = (SELECT id FROM (SELECT id FROM zone WHERE name = '미아 보호소 (임시 천막)') z);

INSERT INTO zone_boundary_point (zone_id, seq, lat, lng)
SELECT id, 0, 37.528900, 126.932900 FROM zone WHERE name = '미아 보호소 (임시 천막)'
UNION ALL SELECT id, 1, 37.528900, 126.934100 FROM zone WHERE name = '미아 보호소 (임시 천막)'
UNION ALL SELECT id, 2, 37.528100, 126.934100 FROM zone WHERE name = '미아 보호소 (임시 천막)'
UNION ALL SELECT id, 3, 37.528100, 126.932900 FROM zone WHERE name = '미아 보호소 (임시 천막)';

UPDATE zone_camera SET lat = 37.528700, lng = 126.933200 WHERE code = 'CAM-03';
