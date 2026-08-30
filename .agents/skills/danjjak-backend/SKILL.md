---
name: danjjak-backend
description: Implement or update the Danjjak Spring Framework backend, MyBatis persistence, Java configuration, or WAR packaging.
---

# Danjjak Backend

## Stack

- JDK 17
- Spring Framework 5.3
- Gradle WAR
- External Tomcat 9
- MyBatis 3.5 with MyBatis-Spring 2.1
- Log4j2

Do not introduce Spring Boot, JPA, or `jakarta.servlet.*`.

## Structure

Create packages only when they are needed.

```text
backend/src/main/
|-- java/com/bestfriend/danjjak/
|   |-- config/
|   |-- common/
|   `-- {domain}/
|       |-- controller/
|       |-- service/
|       |-- mapper/
|       `-- dto/
`-- resources/
    |-- mapper/{domain}/
    |-- application.properties
    `-- log4j2.xml
```

## Rules

- Use Java Config for Spring configuration.
- Keep the flow `controller -> service -> mapper`.
- Keep controllers limited to HTTP input and output.
- Use constructor injection.
- Match each MyBatis mapper interface with its XML statements.
- Do not open raw `SqlSession` instances in application code.
- Use a Spring transaction only when one mock operation performs multiple writes.
- Avoid production banking complexity unless it is an explicit requirement.
- Prefer one primary mapper per feature service.
- Keep mapper methods focused on that feature's persistence needs.
- A feature mapper may access multiple related tables.
- Use another mapper only when it clearly avoids duplicated queries or responsibilities.

## Design

- Give each class and method one clear purpose.
- Keep orchestration in services and move independent business rules into focused classes.
- Split code when responsibilities have different reasons to change.
- Avoid large service classes and generic utility classes.
- Do not create unnecessary wrapper classes only to satisfy SRP.

## Verification

```powershell
cd backend
.\gradlew.bat test war
```
