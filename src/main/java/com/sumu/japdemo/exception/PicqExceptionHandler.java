package com.sumu.japdemo.exception;

import com.sumu.japdemo.entity.Msg;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@ResponseBody
public class PicqExceptionHandler {
    @ExceptionHandler(PiccqException.class)
    public Msg handleException(PiccqException p) {
        p.printStackTrace();
        return Msg.fail();
    }
}
