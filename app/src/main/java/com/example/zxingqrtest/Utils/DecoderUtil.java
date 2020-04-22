package com.example.zxingqrtest.Utils;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.decoder.Decoder;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class DecoderUtil {
    /**
     * 解析二维码图片
     *
     * @param srcBitmap
     * @return Result
     */
    public static Result decodeQR(Bitmap srcBitmap) {
        Result result = null;
        if (srcBitmap != null) {
            int width = srcBitmap.getWidth();
            int height = srcBitmap.getHeight();
            int[] pixels = new int[width * height];
            srcBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            // 新建一个RGBLuminanceSource对象
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            // 将图片转换成二进制图片
            BinaryBitmap binaryBitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
            QRCodeReader reader = new QRCodeReader();// 初始化解析对象
            try {
                result = reader.decode(binaryBitmap, CodeHints.getDefaultDecodeHints());// 开始解析
            } catch (NotFoundException e) {
                e.printStackTrace();
            } catch (ChecksumException e) {
                e.printStackTrace();
            } catch (FormatException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

//    public static String  getResultByte(Bitmap srcBitmap) throws NotFoundException {
//        BinaryBitmap binaryBitmap=null;
//        if (srcBitmap != null) {
//            int width = srcBitmap.getWidth();
//            int height = srcBitmap.getHeight();
//            int[] pixels = new int[width * height];
//            srcBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
//            // 新建一个RGBLuminanceSource对象
//            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
//            binaryBitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
//        }
//        return binaryBitmap.;
//
//    }

    public static String decodeFromBits(boolean[][] imageMat) {
        Decoder decoder = new Decoder();
        String res="can't detect";

        try {
            res = decoder.decode(imageMat, CodeHints.getDefaultDecodeHints()).getText();// 开始解析
        }  catch (ChecksumException e) {
            e.printStackTrace();
            Log.e("Main", "Exception: "+Log.getStackTraceString(e));
        } catch (FormatException e) {
            e.printStackTrace();
            Log.e("Main", "Exception: "+Log.getStackTraceString(e));
        }
        return res;
    }
}
