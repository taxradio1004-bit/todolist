# Spring Boot에서 JWT 인증 개요

JWT(JSON Web Token)는 서명 기반 토큰으로, 서버가 인증 정보를 유지하는 대신 클라이언트가 토큰을 보관하고 매 요청마다 전송하는 방식입니다. Spring Boot 애플리케이션에서 JWT를 활용하는 흐름과 구현 포인트를 정리했습니다.

## 토큰 구조

`header.payload.signature`의 세 부분으로 구성되며 각 파트는 Base64URL 인코딩됩니다.

- **Header**: 알고리즘(`alg`), 토큰 타입(`typ`) 등 메타 정보.
- **Payload**: 사용자 식별자(sub), 만료(exp), 권한(scope/roles) 같은 클레임.
- **Signature**: `HMACSHA256(base64Url(header) + "." + base64Url(payload), secret)` 형태의 서명. 토큰 위조 여부 검증에 사용됩니다.

## 인증 흐름

```text
Client 로그인 요청 (POST /auth/login)
      |
      v
Authentication Controller
      |
      | 사용자 검증 (DB, 외부 IdP 등)
      v
JWT Provider
      |
      | access token / refresh token 발급
      v
Client 저장 (Authorization 헤더 or 쿠키)
      |
      | subsequent request with Authorization: Bearer <access-token>
      v
JwtAuthenticationFilter (OncePerRequestFilter)
      |
      | 토큰 추출 & 서명 검증
      | 인증 정보(SecurityContext) 저장
      v
Controller (보호된 API)
```

## 핵심 구성 요소

### 1. JWT Provider
토큰 생성/검증을 담당하는 유틸리티 컴포넌트입니다.

```java
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidity;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-validity-ms}") long accessTokenValidity
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
    }

    public String generateAccessToken(Long userId, Collection<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                   .setSubject(userId.toString())
                   .claim("roles", roles)
                   .setIssuedAt(Date.from(now))
                   .setExpiration(Date.from(now.plusMillis(accessTokenValidity)))
                   .signWith(secretKey, SignatureAlgorithm.HS256)
                   .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                   .setSigningKey(secretKey)
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
    }
}
```

### 2. JwtAuthenticationFilter
`OncePerRequestFilter`를 상속받아 매 요청마다 토큰을 검증하고 `SecurityContext`에 인증 객체를 채웁니다.

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;

    // Authorization 헤더에서 Bearer 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtProvider.parseClaims(token);
                UserDetails userDetails = userDetailsService.loadUserById(Long.parseLong(claims.getSubject()));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException ex) {
                // 토큰 검증 실패 시 SecurityContext를 비워두고 다음 필터로 진행
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

### 3. Security Configuration
JWT 필터를 Spring Security 필터 체인에 등록합니다.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

## Refresh Token 전략

- Access Token은 짧게(예: 15분), Refresh Token은 길게(예: 7일) 발급해 재발급 요청 시 사용합니다.
- Refresh Token은 DB/Redis 등에 저장하여 탈취 시 무효화 처리할 수 있도록 합니다.
- 재발급 API 호출 시 Refresh Token의 유효성 및 위변조를 확인한 뒤 새 Access Token을 발급합니다.

## 보안 고려 사항

- **Secret 관리**: HS256 시크릿은 충분히 긴 랜덤 값으로 설정하고, 환경 변수나 시크릿 매니저를 통해 관리합니다.
- **토큰 저장 위치**: XSS 방지를 위해 HttpOnly 쿠키 또는 메모리(stored in memory) 사용을 고려합니다. LocalStorage는 편하지만 공격에 취약합니다.
- **만료 처리**: 만료된 토큰은 `JwtException`으로 검출되며, 클라이언트는 로그인 재요청 또는 Refresh Token 사용 흐름으로 유도합니다.
- **권한 정보**: 토큰에 포함된 `roles` 클레임은 서버에서 신뢰할 수 있도록 서명 검증 후 사용합니다. 민감한 정보는 최대한 넣지 않습니다.

## 테스트 포인트

- 로그인 · 토큰 발급 API 단위 테스트 (`MockMvc` + stubbed user service).
- 인증된 API 요청 통합 테스트: `Authorization` 헤더에 유효/만료/위조 토큰을 넣어 필터 동작 확인.
- Refresh Token 재발급 시나리오 테스트.

이 구조를 적용하면 Spring Boot 애플리케이션에서 세션 상태를 서버에 저장하지 않고도 확장 가능한 인증 시스템을 구현할 수 있습니다.
