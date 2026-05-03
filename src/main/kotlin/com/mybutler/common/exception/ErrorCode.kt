package com.mybutler.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String,
) {
    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "입력값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_002", "요청한 리소스를 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_003", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_004", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_005", "서버 내부 오류가 발생했습니다."),

    // Auth
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_001", "이미 사용 중인 이메일입니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "AUTH_002", "이미 사용 중인 아이디입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_003", "이메일 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_004", "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_005", "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_006", "리프레시 토큰이 만료되었습니다."),
    PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_007", "비밀번호 재설정 링크가 만료되었습니다."),
    TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "VALID_005", "필수 약관에 동의해야 합니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),

    // My Bar (Ingredient)
    INGREDIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "INGREDIENT_001", "재료를 찾을 수 없습니다."),
    INGREDIENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "INGREDIENT_002", "이미 등록된 재료입니다."),

    // My Bar (Inventory)
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "INVENTORY_001", "재고를 찾을 수 없습니다."),
    INVENTORY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "INVENTORY_002", "해당 재고에 접근할 권한이 없습니다."),
    INVENTORY_ALREADY_OPENED(HttpStatus.CONFLICT, "INVENTORY_003", "이미 열려 있는 재고입니다."),
    INVENTORY_INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "INVENTORY_004", "유효하지 않은 재고 카테고리입니다."),
    INVENTORY_OCR_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "INVENTORY_005", "재고 OCR 처리에 실패했습니다."),

    // Recipe
    RECIPE_NOT_FOUND(HttpStatus.NOT_FOUND, "RECIPE_001", "레시피를 찾을 수 없습니다."),
    RECIPE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "RECIPE_002", "해당 레시피에 접근할 권한이 없습니다."),
    RECIPE_BASE_NOT_DELETABLE(HttpStatus.BAD_REQUEST, "RECIPE_003", "기본 레시피는 삭제할 수 없습니다."),
    RECIPE_INGREDIENT_REQUIRED(HttpStatus.BAD_REQUEST, "RECIPE_004", "재료를 하나 이상 입력해야 합니다."),
    RECIPE_STEP_REQUIRED(HttpStatus.BAD_REQUEST, "RECIPE_005", "단계를 하나 이상 입력해야 합니다."),
    RECIPE_BASE_NOT_UPDATABLE(HttpStatus.BAD_REQUEST, "RECIPE_006", "기본 레시피는 수정할 수 없습니다."),
    RECIPE_STEP_ORDER_DUPLICATE(HttpStatus.BAD_REQUEST, "RECIPE_007", "단계 순서가 중복될 수 없습니다."),

    // Rating
    RECIPE_RATING_NOT_FOUND(HttpStatus.NOT_FOUND, "RATING_001", "평점을 찾을 수 없습니다."),

    // Community
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_001", "해당 게시물을 찾을 수 없습니다."),
    POST_AUTHOR_MISMATCH(HttpStatus.FORBIDDEN, "POST_002", "본인의 게시물만 삭제할 수 있습니다."),
    POST_PHOTO_REQUIRED(HttpStatus.BAD_REQUEST, "POST_003", "PHOTO 타입은 사진이 1장 이상 필요합니다."),
    POST_CAPTION_TOO_LONG(HttpStatus.BAD_REQUEST, "POST_004", "글 내용은 최대 2000자까지 입력할 수 있습니다."),
    POST_LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "POST_005", "이미 좋아요를 누른 게시물입니다."),
    POST_LIKE_NOT_FOUND(HttpStatus.BAD_REQUEST, "POST_006", "좋아요를 누르지 않은 게시물입니다."),
    POST_AR_GENERATED_NOT_DELETABLE(HttpStatus.FORBIDDEN, "POST_007", "AR 자동 생성 게시물은 삭제할 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_001", "해당 댓글을 찾을 수 없습니다."),
    COMMENT_AUTHOR_MISMATCH(HttpStatus.FORBIDDEN, "COMMENT_002", "본인의 댓글만 삭제할 수 있습니다."),
    COMMENT_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "COMMENT_003", "댓글 내용을 입력해주세요."),
    COMMENT_INVALID_TARGET(HttpStatus.BAD_REQUEST, "COMMENT_004", "댓글 경로가 올바르지 않습니다."),
    COMMENT_REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "COMMENT_005", "대댓글에는 다시 답글을 작성할 수 없습니다."),
    POST_IMAGE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE_003", "이미지는 최대 10장까지 업로드할 수 있습니다."),

    // Storage
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_001", "파일 업로드에 실패했습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "STORAGE_002", "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "STORAGE_003", "파일 크기가 초과되었습니다."),
}
