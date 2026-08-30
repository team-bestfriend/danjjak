# 협업 컨벤션

- 코드 리뷰를 위한 권장 기준입니다.
- Hook이나 CI로 강제하지 않습니다.

## 커밋

```text
<type>: [AREA] <summary> (#issue)
```

- Type: `feat` `fix` `refactor` `docs` `test` `build` `chore`
- Area: `[FE]` `[BE]` `[API]` `[DB]` `[INFRA]` `[DOCS]`
- 이슈 번호는 선택 사항입니다.
- 한 커밋에는 하나의 목적만 담습니다.

```text
feat: [FE] 송금 확인 화면 추가 (#12)
feat: [BE] 위험도 평가 API 추가 (#13)
docs: [API] 이상거래 응답 명세 추가
```

## 브랜치와 PR

- `main`에서 작업 브랜치를 생성합니다.
- `main`에는 직접 Push하지 않습니다.
- 브랜치 하나에는 하나의 이슈 또는 기능만 담습니다.
- PR은 `main`을 대상으로 생성합니다.
- 병합 전 최신 `main`을 반영하고 관련 검증을 실행합니다.
- Squash Merge 후 작업 브랜치를 삭제합니다.

```text
<type>/<issue-number>-<summary>
```

- Type: `feature` `fix` `docs` `chore`
- 이슈가 없다면 이슈 번호를 생략할 수 있습니다.

```text
feature/12-transfer-flow
fix/21-account-selection
docs/readme
```

### 검증

- Frontend (`frontend`): `npm run build`
- Backend (`backend`): `.\gradlew.bat test war`
- API: `npx --yes @redocly/cli@2.47.0 lint contracts/openapi.yaml`
- 문서만 변경한 경우 빌드를 생략할 수 있습니다.

## 코드

### 공통

- `.editorconfig`를 따릅니다.
- 비밀값과 빌드 결과물을 커밋하지 않습니다.
- API 변경 시 `contracts/openapi.yaml`을 함께 수정합니다.

### Backend

- `controller -> service -> mapper` 구조를 따릅니다.
- 생성자 주입을 사용합니다.
- MyBatis 인터페이스와 XML 구문을 일치시킵니다.
- 클래스와 메서드는 하나의 명확한 책임을 갖도록 작성합니다.

### Frontend

- 컴포넌트 이름은 `PascalCase.vue`를 사용합니다.
- 변수와 함수 이름은 `camelCase`를 사용합니다.
- API 호출과 화면 코드를 분리합니다.
