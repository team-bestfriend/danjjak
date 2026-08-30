<div align="center">

<!-- TODO: Add the project logo or cover image at docs/assets/cover.png. -->
<!-- ![단짝 서비스 소개](docs/assets/cover.png) -->

# 단짝 (Danjjak)

**가족이 도와드리는 똑똑한 금융 생활**

고령 사용자가 자주 이용하는 금융 업무를 단축번호와 음성 안내로 쉽게 수행하는 해커톤 MVP

[![Vue](https://img.shields.io/badge/Vue-3.5-42b883)](frontend/package.json)
[![Spring Framework](https://img.shields.io/badge/Spring%20Framework-5.3-6db33f)](backend/build.gradle)
[![Java](https://img.shields.io/badge/Java-17-orange)](backend/build.gradle)
[![Tomcat](https://img.shields.io/badge/Tomcat-9-f8dc75)](backend/README.md)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-6ba539)](contracts/openapi.yaml)

KB IT's Your Life 해커톤

</div>

---

## 📝 프로젝트 소개

- 자주 사용하는 금융 업무를 단축번호로 등록
- TTS와 화면 하이라이트로 금융 절차 안내
- Mock 계좌와 거래 데이터로 송금 흐름 시연
- 메인 송금 기능에서 Rule-based FDS 시연
- 사용 로그를 기반으로 간단한 이용 분석 제공

## ✨ MVP 구현 범위

### 🔢 금융 단축번호

- 자주 사용하는 금융 업무 등록 및 실행
- 단축번호 순서 변경
- 미구현 기능은 비활성 버튼으로 표시

### 💸 안내형 송금

- 출금 계좌 선택
- 수취 계좌 선택 또는 입력
- 송금 금액 입력
- 비밀번호 확인
- Mock 거래 완료

### 🔊 음성 안내

- 단계별 TTS 안내
- 선택할 UI 요소 하이라이트
- 안내 문구 저장 및 조회

### 🚨 이상거래 탐지

- 메인 송금 기능에만 적용
- 신규 수취 계좌 및 고액 송금 규칙 사용
- 위험 사유 표시
- 보호자 확인 흐름 연결

### 📊 이용 분석

- 단계별 소요시간 기록
- 도움 요청 및 재시도 기록
- 어려움을 겪은 단계 요약

## 🧭 주요 시연 흐름

<!-- TODO: Add the demo flow image at docs/assets/demo-flow.png. -->
<!-- ![단짝 주요 시연 흐름](docs/assets/demo-flow.png) -->

### 정상 송금

```text
홈 -> 단축번호 선택 -> 음성 안내 -> 송금 단계 진행 -> 완료
```

### 이상 송금

```text
홈 -> 송금하기 -> 계좌와 금액 입력 -> FDS 판단 -> 재확인 또는 보호자 확인
```

## 🛠 기술 스택

| 영역 | 기술 |
| --- | --- |
| Frontend | Vue 3 · Vite · JavaScript |
| Backend | Java 17 · Spring Framework 5.3 · Spring MVC · MyBatis |
| Runtime | Gradle WAR · Apache Tomcat 9 |
| Database | MySQL · Flyway 예정 |
| Local environment | Docker Compose 예정 |
| API contract | OpenAPI 3.0 · Redocly |
| Logging | Log4j2 |

## 🏗 시스템 구조

<!-- TODO: Add the architecture image at docs/assets/architecture.png. -->
<!-- ![단짝 시스템 구조](docs/assets/architecture.png) -->

```text
Vue frontend
    -> Tomcat 9
        -> Spring MVC controller
            -> Service
                -> MyBatis mapper
                    -> MySQL
```

## 📐 ERD

<!-- TODO: Add the ERD image at docs/assets/erd.png. -->
<!-- ![단짝 ERD](docs/assets/erd.png) -->

- Mock 사용자, 계좌, 금융 패턴, 거래, 실행 로그 중심
- 최종 테이블 구조 확정 후 이미지 추가

## 🚀 시작하기

```powershell
git clone https://github.com/team-bestfriend/danjjak.git
cd danjjak
```

| 항목 | 안내 |
| --- | --- |
| Frontend | [frontend/README.md](frontend/README.md) |
| Backend | [backend/README.md](backend/README.md) |
| API contract | [contracts/openapi.yaml](contracts/openapi.yaml) |

## 📁 프로젝트 구조

```text
danjjak/
|-- frontend/              Vue application
|-- backend/               Spring Framework WAR application
|-- contracts/             OpenAPI contract
|-- infra/                 Docker and database configuration
|-- .agents/skills/        Shared agent instructions
|-- AGENTS.md              Agent routing and shared rules
|-- CONTRIBUTING.md        Collaboration conventions
`-- README.md
```

## 🌐 API 명세

- 원본: [`contracts/openapi.yaml`](contracts/openapi.yaml)
- API 변경 시 명세와 구현을 함께 수정
- 현재 제공: `GET /api/health`, `GET /api/health/db`

## 🤝 협업 규칙

- 커밋 및 코드 컨벤션: [`CONTRIBUTING.md`](CONTRIBUTING.md)
- Agent 공통 규칙: [`AGENTS.md`](AGENTS.md)
- Claude는 [`CLAUDE.md`](CLAUDE.md)를 통해 동일한 규칙 사용

## 👥 팀

<!-- TODO: Add the team member table and profile images. -->

---

<div align="center">

**단짝 (Danjjak)** · KB IT's Your Life 해커톤

<sub>Mock 금융 데이터로 구현하는 시연용 프로젝트입니다. 실제 금융망과 연결되지 않습니다.</sub>

</div>
