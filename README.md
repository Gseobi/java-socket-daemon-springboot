# java-socket-daemon-springboot

Spring Boot 기반으로  
**DB 기반 작업 Polling → 암호화 → Socket 통신 → 복호화 → 결과 저장** 흐름을 처리하는  
**장기 실행 Provider 연동형 Java Daemon 구조**를 재구성한 포트폴리오 프로젝트입니다.

단순 Socket 연동 예제가 아니라,  
운영 환경에서 중요한 **설정 분리, 재시도, 타임아웃, 예외 처리, 암·복호화, 결과 반영 흐름**을  
Daemon 관점에서 구조화하는 데 초점을 두었습니다.

</br>

## Overview

이 프로젝트는 외부 Provider와의 Socket 통신이 필요한 환경에서,  
DB에 적재된 작업을 Daemon이 Polling하여 처리하고  
결과를 다시 DB에 반영하는 **실서비스형 백엔드 처리 구조**를 재구성한 프로젝트입니다.

주요 관심사는 단순 송수신이 아니라,  
장기 실행 프로세스에서 필요한 **안정적인 작업 처리 흐름, 설정 분리, 장애 대응, 운영 가능성**입니다.

</br>

## Problem Background

장기 실행 Daemon은 일반적인 요청/응답형 API와 달리  
프로세스 지속 실행, 외부 시스템 연결, 설정 관리, 장애 복구, 재처리 흐름까지 함께 고려해야 합니다.

특히 Provider 연동형 Socket 구조에서는 다음 요소들이 중요합니다.

- 외부 시스템별 연결/설정 분리
- 암호화 규격 차이에 대한 대응
- Polling 기반 작업 처리와 결과 반영
- 타임아웃, 재시도, 예외 발생 시 제어 가능한 흐름
- 운영 중 설정 변경 및 추적 가능성

이 프로젝트는 이러한 문제를 기준으로  
**Provider 연동형 Socket Daemon을 운영 가능한 구조로 정리하는 것**을 목표로 했습니다.

</br>

## Key Design Points

### 1. Long-running Daemon Structure
DB에서 작업을 주기적으로 Polling하고,  
외부 Provider와 통신한 뒤 결과를 다시 반영하는 장기 실행 구조를 전제로 설계했습니다.

### 2. Provider-oriented Configuration
Provider별 IP, PORT, 암호화 방식, 인코딩 규격 등을  
파일 기반 설정으로 분리하여 런타임에서 참조할 수 있도록 구성했습니다.

### 3. Encryption / Decryption Flow
실제 연동 환경을 고려하여  
AES / SHA 기반 암·복호화 처리와 Base64 / HEX 변환 흐름을 반영했습니다.

### 4. Socket Communication Responsibility Separation
DB 작업 조회, Socket 송수신, 암복호화, 결과 반영 책임을 분리하여  
연동 흐름을 추적 가능하게 구성했습니다.

### 5. Retry / Timeout / Exception Handling
운영 환경에서 발생할 수 있는 연결 실패, 응답 지연, 예외 상황을 고려해  
재시도, 타임아웃, 예외 처리 기준을 구조적으로 반영했습니다.

### 6. Repository Abstraction
DB 기반 처리와 테스트/Mock 환경을 분리할 수 있도록  
Repository 계층을 추상화하여 확장 가능성을 고려했습니다.

</br>

## Processing Flow

```text
DB (Stored Procedure)
  ↓
JobRepository (DB / Mock 분리)
  ↓
SocketService
  ├─ Encrypt
  ├─ Socket Connect / Send / Receive
  └─ Decrypt
  ↓
DB (Update Procedure)
```

</br>

## Configuration

보안 및 계약상 이유로 실제 설정 파일은 포함하지 않았습니다.

예시 구조는 아래와 같습니다.

config/provider/PROVIDER_A.conf
```code
IP=127.0.0.1
PORT=9000
ENC_TYPE=AES/CBC/PKCS5Padding
ENC_KEY=sample_key
ENC_IV=sample_iv
ENC_OUT=HEX
```
이처럼 Provider별 연결 정보와 암호화 규격을 외부 설정으로 분리하여
코드 수정 없이 연동 대상을 관리할 수 있도록 설계했습니다.

</br>

## Tech Stack

| 영역            | 기술                       |
| ------------- | ------------------------ |
| Language      | Java 17                  |
| Framework     | Spring Boot 3.2.1        |
| DB Access     | MyBatis                  |
| Communication | TCP Socket               |
| Encryption    | AES / SHA / Base64 / HEX |
| Configuration | File-based Configuration |
| Logging       | Logback                  |

</br>

## Security Considerations
- 실제 Provider 설정 파일(.conf)은 저장소에 포함하지 않았습니다.
- DB 접속 정보는 환경 변수 기반 주입을 전제로 구성했습니다.
- 암호화 Key / IV는 코드에 하드코딩하지 않도록 분리했습니다.
- 실서비스 연동 정보 및 민감 데이터는 포트폴리오용으로 재구성했습니다.

</br>

## Extensibility

이 프로젝트는 향후 아래 방향으로 확장할 수 있도록 고려했습니다.

- Provider별 SocketClient 구현 분리
- 재시도 정책 및 타임아웃 전략 세분화
- Job 상태 관리 및 운영 모니터링 강화
- 테스트용 Mock Provider / Local Simulator 추가
- 배치성 작업과 장기 실행 Daemon의 책임 분리

</br>

## What This Project Focuses On
- 장기 실행 Daemon 구조 설계
- 외부 Provider Socket 연동 흐름 분리
- 암·복호화 처리와 통신 책임 분리
- Polling 기반 작업 처리와 결과 반영 구조
- 재시도 / 타임아웃 / 예외 흐름 설계
- 운영 가능한 설정 분리와 보안 고려
