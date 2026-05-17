# 나만의 버틀러 — 시퀀스 다이어그램

> **버전:** v1 | **기준:** 서비스 기획서 v0.2 MVP

---

## 1. 회원가입 (온보딩 3단계)

```mermaid
sequenceDiagram
    actor Client
    participant API as Backend API
    participant DB as PostgreSQL
    participant Cache as Redis

    Client->>API: GET /auth/check-username?username=butler_yj
    API->>DB: username 중복 조회
    DB-->>API: 결과 반환
    API-->>Client: { available: true }

    Client->>API: POST /auth/register (이메일·아이디·비밀번호·약관)
    API->>DB: 사용자 생성 (BCrypt 해싱)
    API->>DB: Refresh Token 저장
    DB-->>API: 사용자 ID 반환
    API-->>Client: 201 { access_token, refresh_token, onboarding_completed }
    Client->>Client: refresh_token을 앱 보안 저장소에 저장

    Note over Client,API: Step 2 — 기본 정보 (Authorization 헤더 포함)
    Client->>API: PATCH /users/me/profile (성별·나이대·음주빈도)
    API->>DB: user_profiles 업데이트
    DB-->>API: 저장 완료
    API-->>Client: 200 { gender, ageGroup, drinkingFrequency }

    Note over Client,API: Step 3 — 취향 설정
    Client->>API: POST /users/me/preferences (맛·도수·경험수준)
    API->>DB: user_preferences 저장
    API->>DB: onboarding_completed = true 업데이트
    DB-->>API: 저장 완료
    API-->>Client: 200 { taste_preferences, preferred_abv, experience_level, onboarding_completed: true }
```

---

## 2. 로그인 및 토큰 재발급

```mermaid
sequenceDiagram
    actor Client
    participant API as Backend API
    participant DB as PostgreSQL

    Note over Client,API: 로그인
    Client->>API: POST /auth/login (이메일·비밀번호)
    API->>DB: 사용자 조회 및 BCrypt 검증
    alt 인증 성공
        API->>DB: Refresh Token 저장
        API-->>Client: 200 { access_token, refresh_token, onboarding_completed }
        Client->>Client: refresh_token을 앱 보안 저장소에 저장
        alt onboarding_completed = false
            Client->>Client: 온보딩 화면으로 이동 (Step 2부터)
        else onboarding_completed = true
            Client->>Client: My Bar 메인으로 이동
        end
    else 인증 실패
        API-->>Client: 401 AUTH_001
    end

    Note over Client,API: Access Token 만료 시 재발급
    Client->>API: POST /auth/refresh ({ refresh_token })
    API->>DB: Refresh Token 유효성 검증
    alt 유효
        API->>DB: 기존 Refresh Token 무효화
        API->>DB: 새 Refresh Token 저장 (Rotation)
        API-->>Client: 200 { access_token, refresh_token, onboarding_completed }
        Client->>Client: 새 refresh_token으로 앱 보안 저장소 갱신
    else 만료
        API-->>Client: 401 AUTH_006
        Client->>Client: 로그인 화면으로 이동
    end

    Note over Client,API: 로그아웃
    Client->>API: POST /auth/logout (Authorization 헤더)
    API->>DB: Refresh Token 삭제·무효화
    API-->>Client: 200 { success: true }
```

---

## 3. 재고 등록 — 라벨 스캔 흐름

```mermaid
sequenceDiagram
    actor Client
    participant API as Backend API
    participant OCR as OCR 엔진
    participant DB as PostgreSQL
    participant Cache as Redis

    Client->>API: POST /inventory/scan (multipart: image)
    API->>OCR: 이미지 전달 → 텍스트 추출
    OCR-->>API: rawText 반환
    API->>DB: rawText 기준 자체 DB 재료명 매칭
    alt 매칭 성공
        DB-->>API: 재료 정보 (name, category, abv, capacityMl)
        API-->>Client: 200 { isMatchFound: true, name, category, abv, capacityMl, ocrRawText }
        Client->>Client: 자동 입력 확인 화면 표시
    else 매칭 실패
        DB-->>API: 미매칭
        API-->>Client: 200 { isMatchFound: false, ocrRawText }
        Client->>Client: 경고 배너 + 수기 입력 폼으로 전환
    end

    Client->>API: POST /inventory (재료 정보 JSON)
    API->>DB: inventory_items 저장
    API->>Cache: 레시피 매칭 캐시 무효화
    DB-->>API: 저장된 재고 반환
    API-->>Client: 201 { id, name, category, ... }
```

---

## 4. 재고 등록 — 직접 입력 흐름

```mermaid
sequenceDiagram
    actor Client
    participant API as Backend API
    participant DB as PostgreSQL
    participant Cache as Redis

    Client->>API: POST /inventory (name, category, abv, levelStatus, isOpened ...)
    API->>DB: inventory_items 저장
    alt isOpened = true
        API->>DB: openedAt = 현재 시각 자동 기록
    end
    API->>Cache: 레시피 매칭 캐시 무효화
    DB-->>API: 저장된 재고 반환
    API-->>Client: 201 { id, name, openedAt, ... }
```

---

## 5. My Bar 메인 화면 로딩

```mermaid
sequenceDiagram
    actor Client
    participant API as Backend API
    participant DB as PostgreSQL
    participant Cache as Redis

    Client->>API: GET /inventory/home?sort=createdAt,desc&page=0
    API->>Cache: 레시피 매칭 캐시 조회
    alt 캐시 히트
        Cache-->>API: 매칭 가능 레시피 수
    else 캐시 미스
        API->>DB: 재고 × 레시피 재료 교집합 계산
        DB-->>API: availableRecipeCount
        API->>Cache: 결과 캐싱
    end
    API->>DB: 인사이트(총 가치·재료 수·유통기한 임박 수) 집계
    API->>DB: 카테고리별 카운트 조회
    API->>DB: 재고 목록 페이징 조회
    DB-->>API: 모든 데이터 반환
    API-->>Client: 200 { insights, categoryCount, inventory(paginated) }
```

---

## 6. 레시피 홈 — 추천 로딩

```mermaid
sequenceDiagram
    actor Client
    participant API as Backend API
    participant DB as PostgreSQL
    participant Cache as Redis

    Client->>API: GET /recipes/home
    
    Note over API,Cache: 재고 기반 추천
    API->>Cache: 재고 기반 추천 캐시 조회
    alt 캐시 히트
        Cache-->>API: 추천 결과
    else 캐시 미스
        API->>DB: 사용자 재고 재료명 목록 조회
        API->>DB: 레시피 재료와 교집합 계산
        DB-->>API: missingCount 기준 분류 결과
        Note right of DB: missingCount=0 → available<br/>missingCount 1~2 → nearlyAvailable<br/>missingCount≥3 → 제외
        API->>Cache: 결과 캐싱
    end

    Note over API,DB: 취향 기반 추천
    API->>DB: 사용자 취향 데이터 조회 (tastePreferences, preferredAbv, experienceLevel)
    API->>DB: AR 완료 별점 데이터 조회
    alt 별점 데이터 없음 (신규 유저)
        API->>DB: 취향 데이터 기반 콘텐츠 필터링
    else 별점 누적
        API->>DB: 콘텐츠 필터링 + 협업 필터링 혼합
    end
    DB-->>API: 취향 기반 추천 목록

    API-->>Client: 200 { byInventory: { available, nearlyAvailable }, byPreference: { content, ... } }
```

---

## 7. AR 주조 가이드 — 완료 → 커뮤니티 자동 업로드

```mermaid
sequenceDiagram
    actor MobileApp as React Native 앱(ARCore)
    participant API as Backend API
    participant DB as PostgreSQL
    participant FileStorage as 로컬 파일 스토리지

    Note over MobileApp,API: AR 세션 시작
    MobileApp->>API: GET /ar/recipes/{recipeId}
    API->>DB: 레시피 재료·단계 조회
    DB-->>API: recipeId, ingredients(amountMl), steps
    API-->>MobileApp: 200 { recipeId, totalVolumeMl, ingredients, steps }

    Note over MobileApp: 클라이언트 내부 처리<br/>(잔 측정 → 재료량 계산 → 단계별 AR 안내)

    Note over MobileApp,API: 주조 완료 제출
    MobileApp->>API: POST /ar/sessions (multipart: recipeId, rating, photo, caption?)
    API->>FileStorage: 사진 파일 저장
    FileStorage-->>API: photoUrl 반환
    API->>DB: ar_sessions 저장 (recipeId, rating, photoUrl, caption)
    
    Note over API,DB: 자동 처리 (트랜잭션)
    API->>DB: posts 생성 (type=PHOTO, isArGenerated=true, recipeTag, rating 포함)
    API->>DB: post_images 저장 (photoUrl)
    API->>DB: recipe_ratings 저장 (userId, recipeId, rating) → 협업 필터링 데이터 누적
    
    DB-->>API: sessionId, postId 반환
    API-->>MobileApp: 201 { sessionId, postId, recipeId, rating, photoUrl, createdAt }
    
    Note over MobileApp: postId로 커뮤니티 게시물 상세 이동 가능
```

---

## 8. 커뮤니티 피드 및 게시물 작성

```mermaid
sequenceDiagram
    actor Client
    participant API as Backend API
    participant DB as PostgreSQL
    participant FileStorage as 로컬 파일 스토리지

    Note over Client,API: 피드 목록 조회
    Client->>API: GET /posts?sort=LATEST&page=0
    API->>DB: posts 페이징 조회 (정렬 기준 적용)
    Note right of DB: LATEST: createdAt DESC<br/>POPULAR: 좋아요+댓글 가중 합산
    DB-->>API: 게시물 목록 + isLiked 여부
    API-->>Client: 200 { content, page, totalElements, ... }

    Note over Client,API: 사용자 직접 게시물 작성 (사진형)
    Client->>API: POST /posts (multipart: type=PHOTO, images[], caption?, recipeId?)
    API->>FileStorage: 이미지 파일 저장
    FileStorage-->>API: imageUrls 반환
    API->>DB: posts 저장
    API->>DB: post_images 저장
    DB-->>API: 생성된 게시물 반환
    API-->>Client: 201 { id, type, imageUrls, caption, createdAt }

    Note over Client,API: 좋아요
    Client->>API: POST /posts/{id}/likes
    API->>DB: likes 중복 확인
    alt 중복 없음
        API->>DB: likes 저장
        API->>DB: post.likeCount 증가
        API->>DB: 게시물 작성자 알림 생성 (POST_LIKE)
        API-->>Client: 200 { postId, likeCount, isLiked: true }
    else 중복
        API-->>Client: 409 POST_005
    end

    Note over Client,API: 댓글 작성
    Client->>API: POST /posts/{id}/comments (content)
    API->>DB: comments 저장
    API->>DB: 게시물 작성자 알림 생성 (POST_COMMENT)
    API-->>Client: 201 { id, content, createdAt }
```

---

## 9. 유통기한 알림 배치 처리

```mermaid
sequenceDiagram
    participant Scheduler as 배치 스케줄러 (매일 새벽)
    participant DB as PostgreSQL

    Scheduler->>DB: 개봉된 재고 중 dDay <= -7 항목 조회 (openedAt 기준, D-7 이상 경과 = 경고·위험 대상)
    DB-->>Scheduler: 대상 재고 목록 (userId, inventoryId, dDay)
    
    loop 각 재고 항목
        alt dDay <= -14
            Scheduler->>DB: notifications 생성 (type=EXPIRY_DANGER)
        else dDay <= -7
            Scheduler->>DB: notifications 생성 (type=EXPIRY_WARNING)
        end
    end
    
    Note over Scheduler,DB: 앱 진입 시 클라이언트가 폴링으로 뱃지 갱신
```

---

## 10. 비밀번호 재설정

```mermaid
sequenceDiagram
    actor Client
    participant API as Backend API
    participant DB as PostgreSQL
    participant Email as 이메일 서버

    Client->>API: POST /auth/password/reset-request (email)
    API->>DB: 이메일로 사용자 조회
    Note over API: 보안상 이메일 미존재 여부와 동일 응답 반환
    alt 사용자 존재
        API->>DB: password_reset_tokens 저장 (token, expiresAt)
        API->>Email: 재설정 링크 이메일 발송
    end
    API-->>Client: 200 { message: "재설정 링크 발송" }

    Client->>API: POST /auth/password/reset (token, newPassword)
    API->>DB: token 유효성·만료 여부 검증
    alt 유효
        API->>DB: 비밀번호 BCrypt 업데이트
        API->>DB: password_reset_tokens 삭제
        API-->>Client: 200 { message: "비밀번호 변경 완료" }
    else 무효·만료
        API-->>Client: 400 AUTH_007
    end
```
