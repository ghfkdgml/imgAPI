-- 성능 테스트를 위한 프로젝트 데이터
-- perf.properties의 perf.project-id 값과 일치해야 합니다.
MERGE INTO project (id, code) KEY(id) VALUES (1, 'perf-test-project');