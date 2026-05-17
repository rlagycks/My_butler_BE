# 나만의 버틀러 — 테이블 설명 (Text)

> **기준:** `docs/archive/erd.md` (서비스 기획서 v0.2 MVP, PostgreSQL)

---

## Auth

### `users`
- 목적: 계정(로그인)과 서비스 이용 동의, 온보딩 완료 여부를 보관하는 **최상위 사용자 테이블**.
- 핵심 컬럼: `email`(로그인 ID), `username`(닉네임), `password_hash`(BCrypt), `onboarding_completed`.
- 제약/규칙: `email`, `username`은 `UNIQUE`.
- 관계: 대부분의 도메인 테이블이 `user_id`로 `users.id`를 참조.

### `refresh_tokens`
- 목적: 모바일 앱 인증을 위한 **리프레시 토큰 저장소**(서버 측 추적/무효화).
- 핵심 컬럼: `user_id`, `token`, `expires_at`, `created_at`.
- 제약/규칙: `token`은 `UNIQUE`, Rotation 시 기존 토큰 레코드를 제거하고 새 토큰을 저장.
- 관계: `refresh_tokens.user_id` → `users.id`.

### `password_reset_tokens`
- 목적: 비밀번호 재설정 시 **1회성 토큰**(이메일 발송용)을 저장.
- 핵심 컬럼: `user_id`, `token`, `is_used`, `expires_at`.
- 제약/규칙: `token`은 `UNIQUE`, 사용 후 `is_used=true`.
- 관계: `password_reset_tokens.user_id` → `users.id`.

---

## User

### `user_profiles`
- 목적: 온보딩 Step 2의 **기본 정보**(성별/나이대/음주 빈도)를 1:1로 저장.
- 핵심 컬럼: `user_id`, `gender`, `age_group`, `drinking_frequency`.
- 제약/규칙: `user_id`는 `UNIQUE`(사용자당 1개).
- 관계: `user_profiles.user_id` → `users.id`.

### `user_preferences`
- 목적: 온보딩 Step 3의 **취향 요약 값**(선호 도수/경험 수준)을 1:1로 저장.
- 핵심 컬럼: `user_id`, `preferred_abv`, `experience_level`.
- 제약/규칙: `user_id`는 `UNIQUE`(사용자당 1개).
- 관계: `user_preferences.user_id` → `users.id`.

### `user_taste_preferences`
- 목적: 복수 선택 가능한 **선호 맛**을 정규화하여 저장(사용자 1명 : taste N개).
- 핵심 컬럼: `user_id`, `taste`.
- 관계: `user_taste_preferences.user_id` → `users.id`.

---

## Inventory

### `inventory_items`
- 목적: 사용자의 My Bar에 등록된 **재료/술병 재고**를 저장.
- 핵심 컬럼: `user_id`, `name`(표준 재료명), `category`, `abv`, `capacity_ml`, `level_status`, `is_opened`, `opened_at`.
- 인덱스(조회/배치): `(user_id, category)`, `(user_id, level_status)`, `(user_id, opened_at)`.
- 관계: `inventory_items.user_id` → `users.id`.
- 비고: 레시피 매칭은 `recipe_ingredients.name`과 `inventory_items.name`의 “표준명” 매칭을 전제로 함.

---

## Recipe

### `recipes`
- 목적: 기본 레시피 + 사용자 커스텀 레시피를 통합 저장.
- 핵심 컬럼: `created_by`(커스텀 작성자), `is_custom`, `source`, `name`, `category`, `difficulty`.
- 제약/규칙: 기본 레시피는 `created_by=NULL`, 커스텀 레시피는 `is_custom=true`.
- 인덱스: `(is_custom, created_by)`(내 레시피), `name`(검색).
- 관계: `recipes.created_by` → `users.id`(NULL 가능).

### `recipe_ingredients`
- 목적: 레시피의 **재료 목록/순서/용량**을 저장.
- 핵심 컬럼: `recipe_id`, `order_num`, `name`, `amount`, `unit`.
- 관계: `recipe_ingredients.recipe_id` → `recipes.id`.

### `recipe_steps`
- 목적: 레시피의 **단계별 텍스트 가이드**를 저장.
- 핵심 컬럼: `recipe_id`, `order_num`, `description`.
- 관계: `recipe_steps.recipe_id` → `recipes.id`.

### `recipe_ratings`
- 목적: AR 완료 후 누적되는 **레시피 별점**(협업 필터링 입력값)을 저장.
- 핵심 컬럼: `user_id`, `recipe_id`, `rating`, `created_at`.
- 제약/규칙: `UNIQUE(user_id, recipe_id)`(동일 레시피 1회).
- 관계: `recipe_ratings.user_id` → `users.id`, `recipe_ratings.recipe_id` → `recipes.id`.

---

## AR Guide

### `ar_sessions`
- 목적: AR 주조 가이드 완료 1회를 나타내는 **세션 기록**(사진/별점/캡션 포함).
- 핵심 컬럼: `user_id`, `recipe_id`, `rating`, `photo_url`, `caption`, `created_at`.
- 관계: `ar_sessions.user_id` → `users.id`, `ar_sessions.recipe_id` → `recipes.id`.
- 연동: AR 완료 시 커뮤니티에 게시물이 자동 생성되며, 연결용으로 `post_id`를 보관할 수 있음.

---

## Community

### `posts`
- 목적: 커뮤니티 게시물(사진/텍스트)을 저장. AR 자동 업로드 게시물도 동일 테이블에 저장.
- 핵심 컬럼: `user_id`, `type`(`PHOTO`/`TEXT`), `caption`, `rating`(AR 게시물만), `is_ar_generated`, `recipe_id`(태그), `ar_session_id`(연결), `like_count`, `comment_count`.
- 인덱스(피드/프로필): `(created_at DESC)`, `(like_count DESC, comment_count DESC, created_at DESC)`, `(user_id, created_at DESC)`.
- 관계: `posts.user_id` → `users.id`, `posts.recipe_id` → `recipes.id`(NULL 가능), `posts.ar_session_id` → `ar_sessions.id`(NULL 가능).

### `post_images`
- 목적: 게시물의 **이미지 목록**을 순서대로 저장.
- 핵심 컬럼: `post_id`, `image_url`, `order_num`.
- 관계: `post_images.post_id` → `posts.id`.

### `comments`
- 목적: 게시물의 **댓글**(1단계)을 저장.
- 핵심 컬럼: `post_id`, `user_id`, `content`, `reply_count`.
- 관계: `comments.post_id` → `posts.id`, `comments.user_id` → `users.id`.

### `replies`
- 목적: 댓글의 **대댓글**(1레벨만 지원)을 저장.
- 핵심 컬럼: `comment_id`, `user_id`, `content`.
- 관계: `replies.comment_id` → `comments.id`, `replies.user_id` → `users.id`.

### `likes`
- 목적: 게시물 좋아요를 저장(유저-게시물 N:M).
- 핵심 컬럼: `post_id`, `user_id`, `created_at`.
- 제약/규칙: `UNIQUE(post_id, user_id)`(중복 좋아요 방지).
- 관계: `likes.post_id` → `posts.id`, `likes.user_id` → `users.id`.

---

## Notification

### `notifications`
- 목적: 앱 내 벨 아이콘에서 보는 **인앱 알림**을 저장(푸시 미제공 MVP).
- 핵심 컬럼: `user_id`, `type`, `message`, `link_screen`, `link_target_id`, `is_read`, `created_at`.
- 인덱스: `(user_id, is_read, created_at DESC)`(미확인 알림/최근 알림).
- 관계: `notifications.user_id` → `users.id`.
