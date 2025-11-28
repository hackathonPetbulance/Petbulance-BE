package com.example.seSAC_Petbulance_BE.global.common.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    TEST_ERROR_CODE(HttpStatus.BAD_REQUEST, "오류가 발생하였습니다."),
    INVALID_FILTER_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 filter 값 입니다.");

    private final HttpStatus status;
    private final String message;
}