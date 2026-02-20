package com.sumu.japdemo.exception;

import com.sumu.japdemo.entity.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PiccqException extends RuntimeException {
    private ErrorCode errorCode;
}
