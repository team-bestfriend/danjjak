package com.bestfriend.danjjak.auth.dto;

import com.bestfriend.danjjak.user.dto.UserDtos.CurrentUserResponse;

public final class AuthDtos {

    private AuthDtos() {}

    public record SessionResponse(boolean authenticated, CurrentUserResponse user) {}
}
