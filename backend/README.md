# Danjjak Backend

## 요구 환경

- JDK 17
- Apache Tomcat 9
- MySQL

## 빌드

```powershell
.\gradlew.bat test war
```

- 결과: `build/libs/danjjak.war`

## DB 설정

| 환경변수 | 기본값 |
| --- | --- |
| `DANJJAK_DB_URL` | `jdbc:mysql://localhost:3306/danjjak?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul` |
| `DANJJAK_DB_USERNAME` | `danjjak` |
| `DANJJAK_DB_PASSWORD` | `danjjak_local` |
| `DANJJAK_DB_POOL_SIZE` | `10` |

MySQL은 `infra/`의 Docker Compose로 띄웁니다. [infra/README.md](../infra/README.md)를 참고하세요.
기본값 그대로 사용하면 환경변수를 따로 설정하지 않아도 접속됩니다.
`infra/.env`에서 `MYSQL_PORT`를 바꾼 경우에만 `DANJJAK_DB_URL`을 함께 맞추면 됩니다.

## TTS 설정

실제 음성 안내를 생성하려면 `OPENAI_API_KEY` 환경변수가 필요합니다.
IntelliJ에서 Tomcat을 실행할 때는 Tomcat Run Configuration의 `Environment variables`에 설정하세요.
API key가 없거나 OpenAI API 크레딧을 사용할 수 없으면 실제 TTS 호출이 실패할 수 있습니다.
실제 secret 값은 코드, 설정 파일, 로그에 작성하지 않습니다.

## 가족 음성 저장

V6 마이그레이션을 적용한 뒤 새 WAR를 사용합니다. 녹음은 기본적으로 Tomcat 실행 사용자의
`~/.danjjak/voices`에 저장합니다. `DANJJAK_VOICE_DIR`로 쓰기 가능한 다른 폴더를 지정할 수
있습니다. 재배포 때 삭제되는 `webapps`나 임시 폴더는 피하세요.

`GET /api/patterns/{patternId}/guidance`로 시작 안내와 단계별 문구·음성 선택·녹음 상태를
읽습니다. 대상은 시작 안내의 `start` 또는 템플릿의 `stepCode`입니다. 대상별 `PUT`은
문구와 음성 선택을, `POST .../{target}/audio`는 multipart의 `file`을 저장합니다.
음성은 10MB 이하 WebM, OGG, MP4, MP3, WAV를 지원합니다. 재생 URL은 같은 사용자의
로그인 세션을 요구합니다. 문구 변경은 기존 녹음을 유지하며 재녹음 필요 상태를 남깁니다.

## 카카오 OAuth·나에게 보내기 설정

로컬 카카오 로그인과 HIGH 이상거래의 실제 `나에게 보내기`를 사용하려면 다음 사용자
환경변수를 준비합니다.

| 환경변수 | 용도 |
| --- | --- |
| `KAKAO_REST_API_KEY` | 카카오 REST API 앱 키 |
| `KAKAO_CLIENT_SECRET` | 카카오 보안의 클라이언트 시크릿 |
| `KAKAO_REDIRECT_URI` | `http://localhost:8080/danjjak/api/auth/kakao/callback` |
| `KAKAO_MESSAGE_LINK_URL` | 카카오 메시지에서 열 유효한 HTTPS 링크 |

카카오 Developers 앱에는 Web 플랫폼의 `http://localhost:5173`, 위 Redirect URI,
카카오 로그인 활성화, `talk_message` 동의 항목이 필요합니다. 액세스·리프레시 토큰은
현재 Tomcat 세션에만 두고 DB나 로그에 저장하지 않습니다.

## Tomcat 실행

1. `build/libs/danjjak.war`를 Tomcat의 `webapps`에 복사합니다.
2. `bin/startup.bat`을 실행합니다.
3. <http://localhost:8080/danjjak/api/health>를 확인합니다.
