# ✅ Image Management API – Spring Boot Project

본 프로젝트는 이미지 파일 업로드/조회/수정/삭제 CRUD API를 제공하며,  
**대규모 데이터(≥10,000 건) 환경에서도 안정적으로 동작하는 시스템 설계와 성능 검증**을 목표로 개발되었습니다.

---

# 1. 주요 기능 요약

## ✅ 1.1 이미지 업로드 API

- 멀티파트 업로드(여러 장)
- SHA-256 해시 기반 **중복 업로드 방지**
- 원본은 S3/MinIO 저장, DB에는 메타데이터만 저장
- 업로드 직후 **비동기 썸네일 이벤트 발행**

## ✅ 1.2 이미지 목록 조회

✅ Offset 페이징  
✅ Cursor 페이징 (createdAt+id 기반)  
✅ 상태·태그 필터링

10,000건 데이터 기준 p95 성능 비교 포함.

## ✅ 1.3 단건 조회 (Presigned URL 반환)

- 메타데이터 + presigned GET URL 생성

## ✅ 1.4 이미지 수정

- 태그, 메모, 상태 변경

## ✅ 1.5 이미지 삭제

- softDelete=true 처리 (DB만 수정)

## ✅ 1.6 비동기 썸네일 파이프라인

- 업로드 후 비동기 Worker가 썸네일 생성
- 실패 시 **1→2→4초 지수 백오프**, 최대 3회 재시도
- 상태 전이: `PROCESSING → READY | FAILED`

---

# 2. 설치 및 실행 방법

## ✅ 2.1 요구 환경

- JDK 21+
- Docker (MinIO 실행용)
- Gradle 8+
- Spring Boot 3.3+

---

## ✅ 2.2 MinIO 실행

```bash
docker run -d -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minio \
  -e MINIO_ROOT_PASSWORD=minio123 \
  minio/minio server /data --console-address ":9001"
```

## ✅ 2.3 Spring Boot 서버 실행

```bash
./gradlew bootRun
```

---

# 3. API 명세서

## ✅ Swagger UI

http://localhost:8080/swagger-ui/index.html

---

# 4. 성능 테스트 실행 방법

## ✅ 4.1 PerfRunner 실행

```bash
./gradlew bootRun
```

서버 실행한 후 IDE에서 PerfRunnerApplication 실행.
