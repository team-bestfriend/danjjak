package com.bestfriend.danjjak.support.service;

public interface KakaoMessageClient {

    KakaoSendResult sendToMe(String accessToken, String message);

    record KakaoSendResult(boolean success, Integer httpStatus, String detail) {}
}
