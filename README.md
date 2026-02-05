# Java Socket Daemon (Spring Boot)

운영 환경에서 사용되는 **Provider 연동형 Java Daemon 구조** 를
재구성한 포트폴리오 프로젝트입니다.  
DB 기반 작업 폴링 → 암호화 → Socket 통신 → 복호화 → 결과 저장까지의  
**실제 서비스형 백엔드 처리 흐름**을 중심으로 설계되었습니다.

---

## 주요 목적

- 장기 실행 Daemon 아키텍처 설계
- 외부 시스템(Socket) 연동 구조 구현
- AES / SHA 기반 데이터 암·복호화 처리
- 파일 기반 설정 로딩 및 런타임 반영
- DB 프로시저 기반 Job 처리 모델
- 재시도/타임아웃/예외 처리 설계

---

## 처리 흐름
DB (Stored Procedure)
↓
JobRepository (DB / Mock 분리)
↓
SocketService
├─ Encrypt
├─ Socket Connect
├─ Decrypt
↓
DB (Update Procedure)

---

## 설정 파일

보안 및 계약상 이유로 실제 설정 파일은 포함되어 있지 않습니다.

예시 구조: 
config/provider/PROVIDER_A.conf
- IP=127.0.0.1
- PORT=9000
- ENC_TYPE=AES/CBC/PKCS5Padding
- ENC_KEY=sample_key
- ENC_IV=sample_iv
- ENC_OUT=HEX

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.1 |
| DB | MyBatis |
| 통신 | TCP Socket |
| 암호화 | AES / SHA / Base64 / HEX |
| 설정 관리 | 파일 기반 설정 |
| Logging | Logback |

---

## 보안 설계

- 실제 Provider 설정 파일(.conf) 제외
- DB 접속 정보는 환경 변수로만 주입
- 암호화 키/IV는 코드에 저장하지 않음
