package com.example.zxingqrtest.beans;

import android.graphics.Bitmap;

public class Informations {
    private String picUrl;
    private Bitmap bm;
    private String content;

    public Informations() {}

    public Informations(String picUrl, String content) {
        this.picUrl=picUrl;
        this.content = content;
    }

    public Informations(Bitmap bm, String content) {
        this.bm = bm;
        this.content = content;
    }

    public String getPicUrl() {
        return picUrl;
    }
    public Bitmap getBm(){return bm;}

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