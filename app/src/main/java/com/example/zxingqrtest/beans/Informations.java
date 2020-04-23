package com.example.zxingqrtest.beans;

public class Informations {
    private String picUrl;
    private String content;

    public Informations() {}

    public Informations(String picUrl, String content) {
        this.picUrl=picUrl;
        this.content = content;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public String getContent() {
        return content;
    }

    public void setPicUrl(String  picUrl) {
        this.picUrl = picUrl;
    }

    public void setContent(String content) {
        this.content = content;
    }
}