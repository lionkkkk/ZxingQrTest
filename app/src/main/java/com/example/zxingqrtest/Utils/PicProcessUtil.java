package com.example.zxingqrtest.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

public class PicProcessUtil {
    public static Bitmap procSrc2Gray(Bitmap srcBitmap){
        Mat rgbMat = new Mat();
        Mat grayMat = new Mat();
        Mat binaMat = new Mat();
        Bitmap grayBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.RGB_565);
        Utils.bitmapToMat(srcBitmap, rgbMat);//convert original bitmap to Mat, R G B.
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat
        Imgproc.adaptiveThreshold(grayMat,binaMat,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,11,1);
        Utils.matToBitmap(binaMat, grayBitmap); //convert mat to bitmap
        return grayBitmap;
    }

//    //切割图片
//    private static void cutPic2Pieces(String path){
//        Mat m = new Mat();
//        Bitmap bm = BitmapFactory.decodeFile(path);
//        Utils.bitmapToMat(bm, m);
//        for(int i=0;i<bm.getWidth();i+=bm.getWidth()/3)
//            for(int j=0;j<bm.getHeight();j+=bm.getHeight()/3){
//                Mat r = reSizeMat(m,i,j,bm.getWidth()/3,bm.getWidth()/3);
//                Bitmap b = Bitmap.createBitmap(r.cols(), r.rows(),
//                        Bitmap.Config.ARGB_8888);
//                Utils.matToBitmap(r, b);
//                if(bmList.add(b))
//                    Log.i("test",i+","+j);
//            }
//    }

    //根据坐标和宽高切割图片
    private static  Mat reSizeMat(Mat src,int x,int y,int width, int height){
        Rect rect=new Rect(x,y,width,height);
        Mat result=new Mat(src,rect);
        return result;
    }
}
