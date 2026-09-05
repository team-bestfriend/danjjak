package com.bestfriend.danjjak.common.session;

import com.bestfriend.danjjak.common.error.ApiException;
import javax.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DemoSessionUserResolver {

    public static final String USER_ID_ATTRIBUTE = "userId";
    public static final String KAKAO_ACCESS_TOKEN_ATTRIBUTE = "kakaoAccessToken";
    public static final String KAKAO_REFRESH_TOKEN_ATTRIBUTE = "kakaoRefreshToken";

    public long resolveUserId(HttpSession session) {
        Object value = session.getAttribute(USER_ID_ATTRIBUTE);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        throw new ApiException(
                HttpStatus.UNAUTHORIZED,
                "SESSION_REQUIRED",
                "로그인이 필요합니다. 카카오 로그인 후 다시 시도해 주세요.");
    }

    public String resolveKakaoAccessToken(HttpSession session) {
        Object value = session.getAttribute(KAKAO_ACCESS_TOKEN_ATTRIBUTE);
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}
