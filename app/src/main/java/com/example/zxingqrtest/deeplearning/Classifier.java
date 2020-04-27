package com.example.zxingqrtest.deeplearning;

import android.graphics.Bitmap;

import java.util.List;
import java.util.Vector;

/*分类器模型 基类*/
public interface Classifier {

    // 识别结果 封装类
    class Recognition {
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        private final String id;
        /**
         * Display name for the recognition.
         */
        private final String title;
        /**
         * Whether or not the model features quantized or float weights.
         */
        private final boolean quant;
        /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        private final Float confidence;

        public Recognition(final String id,
                           final String title,
                           final Float confidence,
                           final boolean quant) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.quant = quant;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            return resultString.trim();
        }
    }




    // 调用 分类器 进行识别
    public float[][] My_recognizeImage(Vector<Bitmap> images);
    List<Recognition> recognizeImage0(Bitmap bitmap);
    float[][] recognizeImage1(Bitmap bitmap);




    // 释放 分类器 资源
    void close();
}
