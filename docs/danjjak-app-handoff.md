# danjjak-app 설계 이관

## 합의한 범위

| 항목 | 결정 |
| --- | --- |
| 원본 저장소 | https://github.com/team-bestfriend/danjjak |
| 원본 브랜치 | codex/specs-design-handoff |
| 로컬 참고 경로 | C:/Users/flami/workspace/danjjak |
| 대상 | https://github.com/team-bestfriend/danjjak-app |
| 대상 상태 | 2026-09-07 확인 시 빈 저장소. 작업 시작 시 다시 확인 |
| 산출물 | 명세·Agent 라우팅·저장소 구성·BE 설계 골격 |
| BE 구조 | 현재 프로젝트의 패키지·계층·기능 분리 방식 유지 |
| Java 파일 | package와 class/interface 선언, 상단 설계 설명만 |
| 제외 | 필드·메서드 시그니처·구현 본문·실행 가능한 금융/연동 코드 |
| 언어 | 사람용 기능·설계 설명·Javadoc: 한국어 / Agent 작업 지침: 영어 |
| Issue | 기능별 관련 명세·구현 대상·완료 조건으로 계획. 실제 등록은 별도 요청 범위 확인 |

이관 기준은 위 원본 브랜치의 명세·AGENTS.md·스킬이다. 새 작업에서는 해당 브랜치를 체크아웃하고 원격 커밋을 확인한다. 기본 브랜치만 복제해 작업하지 않는다. 로컬 참고 경로는 필수가 아니다.

## Agent Task

Prepare the target repository as a design scaffold using the source repository and branch above. This is not functional BE implementation.

### Read

1. Source AGENTS.md and [requirements index](specs/requirements.md).
2. Relevant architecture, feature specs, decisions, and scenarios linked from the index.
3. Current backend package/file inventory and relevant class declarations to recover the existing organization and responsibilities.

### Transfer

- Use the published source branch for the latest docs and routing. Verify the checked-out ref and working-tree status before copying; do not assume the default branch contains these changes.
- Preserve source files and uncommitted work. Work in a separate target checkout; never repoint the source remote.
- Carry requirements, concise repository instructions, relevant skills, and basic documentation into the target.
- Adapt skill references and paths to the target. Replace source-only implementation dependencies with target design links; do not blindly copy references to absent code or migrations.
- Reuse the current BE package/layer organization. Do not introduce a new domain layout or ORM merely to create a design scaffold.
- Inventory existing class/interface roles, then reconcile them with updated requirements. Reusing the layout does not mean retaining outdated behavior.
- Create only package/type declarations and Korean top-level design comments. No fields, method signatures/bodies, runtime annotations, executable configuration, SQL, seed scripts, or implementation tests in this design deliverable.
- Each type comment identifies purpose, responsibility, collaborators, and relevant requirement IDs/documents. Link detailed business rules rather than duplicating them.
- Draft feature-level Issue mappings after the file inventory is stable. Do not use one Issue per class by default or claim planned Issues have been created.

### Preserve Decisions

- Four tabs: home, shortcuts, analysis, settings.
- Number voice input opens the existing pre-execution confirmation; Start begins the flow.
- Kakao CTA: 보호자에게 카톡 보내기. Actual demo delivery: logged-in user's message-to-self, with accurate disclosure.
- Multiple recipient accounts per person; explicit confirmation even for one account.
- Pre-start and step voice editing live inside pattern editing.
- Mock owned-account import; no real banking or MyData integration.
- Preserve all FR/UX/NFR/SC/CH/D IDs and proposal/optional status, especially FR-061 and D-01–05.

### Completion

- Report source-to-target package/file mapping and any requirement-driven design changes.
- Every generated Java file contains only the agreed declarations and design comments.
- Every required document is reachable from target AGENTS.md or its linked index; all local links resolve.
- No original business implementation, credentials, build artifacts, runtime SQL/configuration, or completed-test claims are copied.
- Validate document links, requirement coverage, skill format, and declaration-only content. A runnable application build is not this task's acceptance criterion.
- Record completed work and the next step in the target so continuation does not require the original conversation.

### Preparation Boundary

- The event guidance places backend code writing after the event starts. Whether declaration-only Java files are permitted beforehand has not been confirmed by the organizer.
- Preserve that distinction; do not label this scaffold organizer-approved or infer authorization to implement functional code.
