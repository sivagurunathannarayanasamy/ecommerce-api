# Java Conventions

## Stack
- Java 21
- Spring Boot 4.1.0
- Maven
- MySQL

## Package Structure
Base package: `com.siva.ecommerce`

Sub-packages: 
- `controller`
- `service`
- `repository`
- `entity`
- `dto`
- `exception`
- `config`

## Architecture Rules
- Always use DTOs in controllers - never expose entities directly
- All business logic lives in the service layer - keep controllers thin
- Centralised exception handling via `GlobalExceptionHandler`
- All endpoints prefixed with `/api/v1/`

## Lombok 
- `@RequiredArgsConstructor` for injection everywhere (with `final` fields). No `@Autowired`
- Entities: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`


## Dependency Injection
- Always use `@RequiredArgsConstructor` with `final` fields.
- Never use `@Autowired`.

## Entities
- Annotate with `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`.
- Never use `@Data` on entities.

## DTOs
- Annotate with `@Data @Builder @NoArgsConstructor @AllArgsConstructor`.

## Money
- Always use `BigDecimal` for money fields.
- Never use `double` or `float`.

## Tests
- JUnit 5, Mockito, AssertJ, MockMvc.
- Always use AssertJ `assertThat`.
- Never use `assertEquals`.

## Communication Style
- Show diffs, not full rewrites.
- Ask before deleting any file.
- Be direct and technical.
