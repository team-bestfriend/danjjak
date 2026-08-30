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

## Tomcat 실행

1. `build/libs/danjjak.war`를 Tomcat의 `webapps`에 복사합니다.
2. `bin/startup.bat`을 실행합니다.
3. <http://localhost:8080/danjjak/api/health>를 확인합니다.
