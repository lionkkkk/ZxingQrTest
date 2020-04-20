package com.example.zxingqrtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.example.zxingqrtest.view.SlideImageView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;
import java.util.List;

public class SlideTestActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private SlideImageView mSlideImageView;
    private String[] urls = new String[]{"assets://test_1.jpg", "assets://test_2.jpg",
            "assets://test_3.jpg",
            "assets://test_4.jpg",
            "assets://test_5.jpg"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_test);

        mSlideImageView = (SlideImageView) findViewById(R.id.main_siv);
        ImageLoaderConfiguration configuration = ImageLoaderConfiguration
                .createDefault(this);
        ImageLoader.getInstance().init(configuration);
        mSlideImageView.setOnSlideImageListener(new  SlideImageView.OnSlideImageListener() {
            @Override
            public void onShowImage(int index, SlideImageView.ImageInfo imageInfo) {
                Log.i(TAG, String.format("index=%d,image info=%s", index, imageInfo));
            }
        });
        List<SlideImageView.ImageInfo> imageInfoList = new ArrayList<>(5);
        for (int i = 0; i < urls.length; i++) {
            SlideImageView.ImageInfo tempInfo = new SlideImageView.ImageInfo(urls[i], "Image-" + i);
            imageInfoList.add(tempInfo);
        }
        mSlideImageView.setImageInfos(imageInfoList);
    }
}