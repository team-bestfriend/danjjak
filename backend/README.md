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

## Tomcat 실행

1. `build/libs/danjjak.war`를 Tomcat의 `webapps`에 복사합니다.
2. `bin/startup.bat`을 실행합니다.
3. <http://localhost:8080/danjjak/api/health>를 확인합니다.
