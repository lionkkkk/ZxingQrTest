package com.example.zxingqrtest.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import org.opencv.android.Utils;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.CvType;

import java.util.Vector;



public class PicProcessUtil {
    public static Bitmap procSrc2Gray(Bitmap srcBitmap){
        Mat rgbMat = new Mat();
        Mat grayMat = new Mat();
        Mat binaMat = new Mat();
        Bitmap grayBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.RGB_565);
        Utils.bitmapToMat(srcBitmap, rgbMat);//convert original bitmap to Mat, R G B.
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat
        Log.i("Main",rgbMat.toString());
        Log.i("Main",grayMat.toString());
        Imgproc.adaptiveThreshold(grayMat,binaMat,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,11,1);
        Utils.matToBitmap(binaMat, grayBitmap); //convert mat to bitmap
        return grayBitmap;
    }
    //根据坐标和宽高切割图片
    private static  Mat reSizeMat(Mat src,int x,int y,int width, int height){
        Rect rect=new Rect(x,y,width,height);
        Mat result=new Mat(src,rect);
        return result;
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

    // 合成sobel归一化边缘图像，建立累计积分图
    public static void getSobel_Integral_Image(
            Mat sobel_x,Mat sobel_y,Mat sobelImg,Mat IntegralImg,float[] gArray){
        int rows = sobelImg.rows();
        int cols = sobelImg.cols();
        int size = rows*cols;
        short[] xSobel = new short[size];
        short[] ySobel = new short[size];
        sobel_x.get(0, 0, xSobel);
        sobel_y.get(0, 0, ySobel);
        float[] rowLast = new float[cols];
        for(int i=0;i<rows;i++){
            float rowSum = 0.0F; //行累加
            //rowLast = IntegralImg[i,:]
            IntegralImg.get(i,0,rowLast); //加载积分图上一行数据
            for(int j=0;j<cols;j++){
                byte value = (byte)(((Math.abs(xSobel[i])+Math.abs(xSobel[j]))>>4)&0xff);
                sobelImg.put(i,j,value);
                //# 建立累计积分图
                //rowSum += gArray[value]
                //IntegralImg[i+1][j+1] = rowLast[j+1]+rowSum
                rowSum += gArray[(int)value&0xff];
                IntegralImg.put(i+1,j+1,rowLast[j+1]+rowSum);
            }
        }
        return;
    }
    // Me2020 批量码识别
    //public static Vector<Bitmap> BatchQRcodeDetect(Mat srcImg){
    public static Bitmap BatchQRcodeDetect(Bitmap bitmap){
        Mat srcImg = new Mat();
        Utils.bitmapToMat(bitmap,srcImg);
        Log.i("Main",srcImg.toString());
        //梯度压缩因子（预准备）
        float gArray[] = new float[256];//梯度压缩因子
        for(int i=0;i<256;i++)
            gArray[i] = (float)Math.pow(i,0.8125);
        // 灰度变换
        Mat grayImg = new Mat();
        Imgproc.cvtColor(srcImg, grayImg, Imgproc.COLOR_RGB2GRAY);
        // 创建 sobel边缘图
        Mat sobel_x = new Mat();
        Mat sobel_y = new Mat();
        Imgproc.Scharr(grayImg,sobel_x,CvType.CV_16S, 1, 0);
        Imgproc.Scharr(grayImg,sobel_y,CvType.CV_16S, 0, 1);
        // 创建 sobel累计积分图
        int height= grayImg.rows();
        int width = grayImg.cols();// 积分图像多一行一列均为0
        Mat sobelImg = Mat.zeros(height,width,CvType.CV_8UC1);
        Mat IntegralImg = Mat.zeros(height+1,width+1,CvType.CV_64FC1);
        // 合成sobel归一化边缘图像，建立sobel累计积分图
        getSobel_Integral_Image(sobel_x,sobel_y,sobelImg,IntegralImg,gArray);
        //showIntegralImg = np.zeros(IntegralImg.shape,dtype=np.uint8) #显示积分图
        //cv2.normalize(IntegralImg,showIntegralImg,0,255,cv2.NORM_MINMAX,cv2.CV_8U)
        //Mat showIntegralImg = new Mat();
        Bitmap dstBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(sobelImg, dstBitmap); //convert mat to bitmap
        Vector<Bitmap> result = new Vector<Bitmap>();
        result.add(dstBitmap);

        return dstBitmap;
    }




}
