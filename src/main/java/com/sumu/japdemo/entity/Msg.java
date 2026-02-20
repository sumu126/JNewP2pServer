package com.sumu.japdemo.entity;

public class Msg {
    private String msg;
    private Integer code;
    private String data;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public static Msg success(){
        Msg msg1 = new Msg();
        msg1.setMsg("success");
        msg1.setCode(200);
        return msg1;
    }
    public static Msg success(String data){
        Msg msg1 = new Msg();
        msg1.setMsg("success");
        msg1.setCode(200);
        msg1.setData(data);
        return msg1;
    }
    public static Msg success(String message,String data){
        Msg msg1 = new Msg();
        msg1.setMsg(message);
        msg1.setCode(200);
        msg1.setData(data);
        return msg1;
    }
    public static Msg success(String msg,Integer code,String data){
        Msg msg1 = new Msg();
        msg1.setMsg(msg);
        msg1.setCode(code);
        msg1.setData(data);
        return msg1;
    }
    public static Msg fail(){
        Msg msg1 = new Msg();
        msg1.setMsg("fail");
        msg1.setCode(500);
        msg1.setData(null);
        return msg1;
    }
    public static Msg fail(String data){
        Msg msg1 = new Msg();
        msg1.setMsg("fail");
        msg1.setCode(500);
        msg1.setData(data);
        return msg1;
    }
    public static Msg fail(String message,String data){
        Msg msg1 = new Msg();
        msg1.setMsg(message);
        msg1.setCode(500);
        msg1.setData(data);
        return msg1;
    }
    public static Msg fail(String msg,Integer code,String data){
        Msg msg1 = new Msg();
        msg1.setMsg(msg);
        msg1.setCode(code);
        msg1.setData(data);
        return msg1;
    }
}
