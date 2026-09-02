package com.bestfriend.danjjak.common.session;

import javax.servlet.http.HttpSession;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DemoSessionUserResolver {

    public static final String USER_ID_ATTRIBUTE = "userId";
    public static final String KAKAO_ACCESS_TOKEN_ATTRIBUTE = "kakaoAccessToken";

    private final long demoUserId;

    public DemoSessionUserResolver(Environment environment) {
        this.demoUserId = environment.getRequiredProperty("demo.user-id", Long.class);
    }

    public long resolveUserId(HttpSession session) {
        Object value = session.getAttribute(USER_ID_ATTRIBUTE);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return demoUserId;
    }

    public String resolveKakaoAccessToken(HttpSession session) {
        Object value = session.getAttribute(KAKAO_ACCESS_TOKEN_ATTRIBUTE);
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}
