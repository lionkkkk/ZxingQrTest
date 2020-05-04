package com.example.zxingqrtest.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import com.example.zxingqrtest.deeplearning.Classifier;
import com.example.zxingqrtest.deeplearning.TensorFlowImageClassifier;
import com.example.zxingqrtest.deeplearning.Classifier;

import org.opencv.android.Utils;

import org.opencv.core.*;
import org.opencv.imgcodecs.*;
import org.opencv.imgproc.*;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.CvType;
import org.opencv.core.Core;

import java.nio.ByteBuffer;
import java.util.Vector;
import java.lang.Math;
import java.lang.String;


public class PicProcessUtil {
    private static final String TAG = "Main"; //Debug使用

    // 根据Mat 转化成 Bitmap
    public static Bitmap MatToBitmap565(Mat srcImg, String name) {
        if (srcImg.channels() == 1) {
            Imgproc.cvtColor(srcImg, srcImg, Imgproc.COLOR_GRAY2BGR);
        }
        float scale = Math.max(srcImg.rows(), srcImg.cols()) / 1024.0f;
        Imgproc.resize(srcImg, srcImg, new Size(Math.round(srcImg.cols() / scale),
                Math.round(srcImg.rows() / scale)));
        int thickness = srcImg.rows() / 256 + 1;
        Imgproc.putText(srcImg, name, new Point(0, thickness * 6),
                Imgproc.FONT_HERSHEY_SIMPLEX, 1.25,
                new Scalar(255, 255, 0, 0), thickness);
        Bitmap dstBitmap = Bitmap.createBitmap(srcImg.cols(), srcImg.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(srcImg, dstBitmap);
        return dstBitmap;
    }

    // 显示矩形框
    public static Mat showRect_Me2020(Mat rbgImg, Vector<int[]> rectSet) {
        Mat retImg = new Mat();
        rbgImg.copyTo(retImg);
        int height = rbgImg.rows();
        int width = rbgImg.cols();
        int nums = rectSet.size(); //行数即为框数量
        for (int i = 0; i < nums; i++) {
            int[] temp = rectSet.get(i); //提取当前框信息
            int x0 = temp[1];
            int y0 = temp[2];
            int x1 = temp[3];
            int y1 = temp[4];
            if (x0 < 0) x0 = 0; //边界判断
            if (y0 < 0) y0 = 0;
            if (x1 >= width) x1 = width - 1;
            if (y1 >= height) y1 = height - 1;
            if (temp[5] == 1) { // 显示QR码框
                int thickness = (x1 - x0) / 24 + 1;
                //# 画矩形框
                Imgproc.rectangle(retImg, new Point(x0, y0), new Point(x1, y1),
                        new Scalar(255, 0, 0, 255), thickness);
                //# 标注文本
                String text = String.valueOf(temp[0]);
                Imgproc.putText(retImg, text, new Point(x0, y0),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 0.85,
                        new Scalar(0, 255, 0, 0), thickness);
            }
        }
        return retImg;
    }

    // 合成sobel归一化边缘图像，建立累计积分图
    public static void getSobel_Integral_Image(Mat sobel_x, Mat sobel_y,
                                               Mat sobelImg, Mat IntegralImg, double[] gArray) {
        int rows = sobelImg.rows();
        int cols = sobelImg.cols();
        int size = rows * cols;
        short[] xSobel = new short[size];
        short[] ySobel = new short[size];
        byte[] sobel = new byte[size];
        double[] integralImg = new double[IntegralImg.rows() * IntegralImg.cols()];
        sobel_x.get(0, 0, xSobel);
        sobel_y.get(0, 0, ySobel);
        sobelImg.get(0, 0, sobel);
        IntegralImg.get(0, 0, integralImg);
        for (int i = 0; i < rows; i++) {
            double rowSum = 0.0; //行累加
            int index = i * cols;
            int indexLast = i * (cols + 1) + 1;
            int indexNow = (i + 1) * (cols + 1) + 1;
            //integeralImg.get(i,0,rowLast); //加载积分图上一行数据
            for (int j = 0; j < cols; j++) {
                // 计算截断灰度梯度值
                //byte value = (byte)(((Math.abs(xSobel[i*cols+j])+Math.abs(ySobel[i*cols+j]))>>4)&0xff);
                //sobel[i*cols+j] = value;
                byte value = (byte) (((Math.abs(xSobel[index + j]) + Math.abs(ySobel[index + j])) >> 4) & 0xff);
                sobel[index + j] = value;
                // 建立sobel累计积分图
                rowSum += gArray[(int) value & 0xff];
                //integeralImg[(i+1)*(cols+1)+j+1] = integeralImg[i*(cols+1)+j+1] + rowSum;
                integralImg[indexNow + j] = integralImg[indexLast + j] + rowSum;
            }
        }
        sobelImg.put(0, 0, sobel);
        IntegralImg.put(0, 0, integralImg);
        return;
    }

    // 迭代法，阈值获取
    public static float getThr_Loop(int[] grayHist, int T0) {
        int T = T0; //迭代阈值
        while (true) {
            float u1 = 0.0f, u2 = 0.0f;
            int cnt1 = 0, cnt2 = 0;
            for (int i = 0; i < T; i++) {
                u1 += grayHist[i] * i;
                cnt1 += grayHist[i];
            }
            for (int i = T; i < 256; i++) {
                u2 += grayHist[i] * i;
                cnt2 += grayHist[i];
            }
            if (cnt1 > 0) u1 = u1 / cnt1;
            else u1 = 0.0f;
            if (cnt2 > 0) u2 = u2 / cnt2;
            else u2 = 0.0f;
            float Tn = (u1 + u2) / 2;
            if (Math.abs(Tn - T) < 1.0)
                return Tn;
            else
                T = Math.round(Tn);
        }
    }

    // 获取 加权边缘密度块图 归一化并二值化 得到标记结果
    public static void getMar2Dmap_gDensity2Dmap(int grid, Mat IntegralImg,
                                                 Mat mar2Dmap, float a) {
        int height = IntegralImg.rows(); //注意，积分图比原图尺寸多1
        int width = IntegralImg.cols();
        int rows = mar2Dmap.rows();
        int cols = mar2Dmap.cols();
        // 计算 分块 加权边缘密度值
        double[] integralImg = new double[IntegralImg.rows() * IntegralImg.cols()];
        IntegralImg.get(0, 0, integralImg);
        double[] gDensity2Dmap = new double[rows * cols]; //加权边缘密度
        double gDensity_max = 0.0f;
        for (int i = 0; i < rows; i++) {
            int y1 = i * grid;
            int y2 = y1 + grid;
            if (y2 >= height) y2 = height - 1; //边界处理
            for (int j = 0; j < cols; j++) {
                int x1 = j * grid;
                int x2 = x1 + grid;
                if (x2 >= width) x2 = width - 1; //边界处理
                double score = integralImg[y2 * width + x2] + integralImg[y1 * width + x1]
                        - integralImg[y1 * width + x2] - integralImg[y2 * width + x1];
                double pixel = (y2 - y1 + 1) * (x2 - x1 + 1);
                double gDensity = score / pixel; //加权边缘密度值
                gDensity2Dmap[i * cols + j] = gDensity;
                if (gDensity > gDensity_max) gDensity_max = gDensity;
            }
        }
        //Log.i(TAG,"Debug gDensity_max: "+String.valueOf(gDensity_max));
        // 归一化 加权边缘密度 统计分布直方图
        int[] grayHist = new int[256];
        int size = rows * cols;
        for (int i = 0; i < size; i++) {
            int value = (int) (Math.round(gDensity2Dmap[i] / gDensity_max * 255.0) & 0xff);
            grayHist[value]++;
        }
        // 迭代法 计算阈值
        float T = getThr_Loop(grayHist, 15);
        double Thr_gDensity = T * gDensity_max / 255.0 * a;
        //Log.i(TAG,"Debug Thr_gDensity: "+String.valueOf(Thr_gDensity));
        // 二值化 标记结果
        byte[] Mar2Dmap = new byte[size];
        mar2Dmap.get(0, 0, Mar2Dmap);
        for (int i = 0; i < size; i++) {
            if (gDensity2Dmap[i] >= Thr_gDensity)
                Mar2Dmap[i] = (byte) 0xff;
            else
                Mar2Dmap[i] = (byte) 0x00;
        }
        mar2Dmap.put(0, 0, Mar2Dmap);
        return;
    }

    // 初框定+配置初始化
    public static boolean Init_Mar2DmapL1(int L1size0, Mat IntegralImg,
                                          Mat mar2DmapL1, int[] returnMsg) {
        int height = IntegralImg.rows() - 1; //积分图比原灰度图尺寸多1
        int width = IntegralImg.cols() - 1;
        int L1size = L1size0; //待返回 L1分块尺寸
        int nextSize = 0; //待返回 最佳size
        int iterations = 0; //待返回 迭代次数
        // 递增L1size，获取初始化配置
        while (L1size < 12) {
            iterations++;
            // 创建 加权边缘密度块图_标记块图 for L1size
            int grid = L1size;
            int rows, cols;
            if (height % grid == 0) rows = height / grid; //边界处理
            else rows = height / grid + 1;
            if (width % grid == 0) cols = width / grid;
            else cols = width / grid + 1;
            Mat mar2Dmap = new Mat(rows, cols, CvType.CV_8UC1);
            // 获取 加权边缘密度块图 归一化并二值化 得到标记结果
            getMar2Dmap_gDensity2Dmap(grid, IntegralImg, mar2Dmap, 1.00f);
            // 连通域框定
            Mat labels = new Mat();
            Mat stats0 = new Mat();
            Mat centroids = new Mat();
            int nums = Imgproc.connectedComponentsWithStats(mar2Dmap, labels,
                    stats0, centroids, 8);
            labels.release();
            centroids.release(); //不用到，直接丢掉
            //Log.i(TAG,"Debug nums: "+String.valueOf(nums));
            /*nums:聚类数目
              stats:每个集合框信息[x0,y0,width,height,mb]，0号为背景框
                    mb表示markerBlock该框内含标记块数目*/
            // 筛选连通域标记框
            int stats[] = new int[stats0.cols() * stats0.rows()]; //贼烦
            stats0.get(0, 0, stats);
            float scaleNextL1 = 0.0f; //#最佳size=L1size的倍数
            int mbThr = 8; //标记块数阈值
            int sum = 0;
            int cnt = 0;
            for (int i = 1; i < nums; i++) {
                int w = stats[i * 5 + 2], h = stats[i * 5 + 3], mb = stats[i * 5 + 4];
                if (mb > mbThr && Math.abs(w - h) <= 4) {
                    sum += mb;
                    cnt++;
                }
            }
            if (cnt > 10) mbThr = sum / cnt;
            else mbThr = sum / cnt / 2;
            //Log.i(TAG,"Debug mbThr: "+String.valueOf(mbThr));
            int thrFxd = 4; //方形度=|w-h|
            float maxTcb = 0.5f; //填充度=mb/w*h
            for (int i = 1; i < nums; i++) {
                //Log.i(TAG,"Debug i: "+String.valueOf(i));
                if (stats[i * 5 + 4] > mbThr) {
                    int w = stats[i * 5 + 2], h = stats[i * 5 + 3];
                    int mb = stats[i * 5 + 4];
                    float tempTcb = (float) (mb) / (float) (w * h);
                    //Log.i(TAG,"Debug tempTcb: "+String.valueOf(tempTcb));
                    if (tempTcb >= maxTcb && Math.abs(w - h) <= thrFxd) {
                        if (Math.min(w, h) / 4.0f >= 0.75f) {
                            //Log.i(TAG,"Debug i: "+String.valueOf(i));
                            //Log.i(TAG,"Debug w: "+String.valueOf(w));
                            //Log.i(TAG,"Debug h: "+String.valueOf(h));
                            maxTcb = tempTcb;
                            scaleNextL1 = Math.min(w, h) / 4.5f;
                            //Log.i(TAG,"Debug scaleNextL1: "+String.valueOf(scaleNextL1));
                        }
                    }
                }
            }
            //Log.i(TAG,"Debug scaleNextL1: "+String.valueOf(scaleNextL1));
            // 判定最佳nextSize
            if (scaleNextL1 > 0.0) {// 初始化配置成功
                mar2Dmap.copyTo(mar2DmapL1);
                //mar2DmapL1 = mar2Dmap.clone();
                nextSize = Math.round(L1size * scaleNextL1);
                returnMsg[0] = nextSize;//待返回 最佳size
                returnMsg[1] = iterations;//待返回 迭代次数
                return true;
            } else {
                L1size++;
            }
            mar2Dmap.release(); //记得得回收
            stats0.release();
        }
        // 初始化配置失败
        int grid = L1size;
        int rows, cols;
        if (height % grid == 0) rows = height / grid; //边界处理
        else rows = height / grid + 1;
        if (width % grid == 0) cols = width / grid;
        else cols = width / grid + 1;
        mar2DmapL1 = Mat.zeros(rows, cols, CvType.CV_8UC1);
        nextSize = nextSize + 8;
        returnMsg[0] = nextSize;//待返回 最佳size
        returnMsg[1] = iterations;//待返回 迭代次数
        return false;
    }

    // 二次框定+预筛选
    public static int[] getMar2DmapL2(int L2size, Mat IntegralImg, float a,
                                      Mat mar2DmapL2, Vector<int[]> rectSet) {
        int height = IntegralImg.rows() - 1; //积分图比原灰度图尺寸多1
        int width = IntegralImg.cols() - 1;
        // 创建 加权边缘密度块图_标记块图 for L2size
        int grid = L2size;
        int rows, cols;
        if (height % grid == 0) rows = height / grid; //边界处理
        else rows = height / grid + 1;
        if (width % grid == 0) cols = width / grid;
        else cols = width / grid + 1;
        // 获取 加权边缘密度块图 归一化并二值化 得到标记结果
        Mat mar2Dmap = new Mat(rows, cols, CvType.CV_8UC1);
        getMar2Dmap_gDensity2Dmap(grid, IntegralImg, mar2Dmap, a);
        // 连通域框定
        Mat labels = new Mat();
        Mat stats0 = new Mat();
        Mat centroids = new Mat();
        int nums = Imgproc.connectedComponentsWithStats(mar2Dmap, labels,
                stats0, centroids, 8);
        labels.release();
        centroids.release(); //不用到，直接丢掉
        int stats[] = new int[stats0.cols() * stats0.rows()]; //贼烦
        stats0.get(0, 0, stats);
        /*nums:聚类数目
          stats:每个集合框信息[x0,y0,width,height,mb]，0号为背景框
                mb表示markerBlock该框内含标记块数目*/
        // 筛选框 适当外拓 [No,x0,y0,x1,y1,flag](坐标映射回原图size，且做了边界判断)
        float thrFxd = 0.75f; //方形度 = 2*min(w,h)/(w+h)
        float thrTcb = 0.45f; //填充度 = mb/w*h
        int mbThr = 8; //标记块数阈值
        int cntRect = 0; //筛选后 候选QR code框 数量
        //Log.i(TAG,"Debug nums: "+String.valueOf(nums));
        //Log.i(TAG,"Debug stats: "+String.valueOf(stats.length));
        for (int i = 1; i < nums; i++) { //剔除0号整幅图的背景框
            //Log.i(TAG,"Debug i: "+String.valueOf(i));
            if (stats[i * 5 + 4] > mbThr) {
                int w = stats[i * 5 + 2];
                int h = stats[i * 5 + 3];
                int mb = stats[i * 5 + 4];
                if (w > 1 && h > 1 &&
                        (float) mb / (float) (w * h) >= thrTcb &&
                        2.0f * Math.min(w, h) / (float) (w + h) >= thrFxd) {
                    //# 增添候选QRcode框 信息[No,x0,y0,x1,y1,flag]
                    int[] temp = new int[6];
                    temp[0] = ++cntRect; //#[0]编号
                    temp[5] = 1;         //#[5]候选QR框 检测/识别 标志
                    //Log.i(TAG,"Debug cntRect: "+String.valueOf(cntRect));
                    //# 适当外扩 并做边界处理
                    int xSet = 1, ySet = 1;
                    int x0 = stats[i * 5 + 0];
                    int y0 = stats[i * 5 + 1];
                    x0 -= xSet;
                    if (x0 < 0) x0 = 0;
                    temp[1] = x0 * L2size; //#[1]左上角x0
                    y0 -= ySet;
                    if (y0 < 0) y0 = 0;
                    temp[2] = y0 * L2size; //#[2]左上角y0
                    int x1 = x0 + w + (xSet << 1);
                    if (x1 > cols - 1) x1 = width - 1;
                    else x1 = x1 * L2size;
                    temp[3] = x1;        //#[3]右下角x1
                    int y1 = y0 + h + (ySet << 1);
                    if (y1 > rows - 1) y1 = height - 1;
                    else y1 = y1 * L2size;
                    temp[4] = y1;        //#[4]右下角y1
                    rectSet.add(temp);
                    //Log.i(TAG,"Debug rectSet.size(): "+String.valueOf(rectSet.size()));
                }
            }
        }
        int[] retMsg = new int[3];
        retMsg[0] = nums; //筛选前 框数量
        retMsg[1] = cntRect; //筛选后 框数量
        mar2Dmap.copyTo(mar2DmapL2);
        return retMsg;
    }

    // 分类器、鉴别剔除
    public static int delRect_from_model(Mat srcImg, Vector<int[]> rectSet,
                                         Classifier classifier) {
        int nums = rectSet.size();
        if (nums <= 0) return nums;
        Vector<Bitmap> imgBatch = new Vector<Bitmap>();
        // 打包层一整批图片
        for (int i = 0; i < nums; i++) {
            int[] temp = rectSet.get(i);
            Mat subMat = new Mat(srcImg, new Rect(new Point(temp[1], temp[2]),
                    new Point(temp[3], temp[4])));
            Bitmap subBitmap = Bitmap.createBitmap(subMat.cols(), subMat.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(subMat, subBitmap);
            imgBatch.add(subBitmap);
            subMat.release();
        }
        // 整批输入预测
        float[][] predicts = classifier.My_recognizeImage(imgBatch);
        /*String output = ""; //打印分类器输出结果
        for (int i = 0; i < predicts.length; i++) {
            output += "[";
            for (int j = 0; j < predicts[0].length; j++) {
                output += predicts[i][j] + ",";
            }
            output += "]\n\r";
        }
        Log.i(TAG,"Debug predictions: \n\r"+String.valueOf(output));*/
        // 剔除
        for (int i = 0; i < nums; i++) {
            if (predicts[i][0] < predicts[i][1]) {
                rectSet.get(i)[5] = 0;
                nums--;
            }
        }
        return nums;
    }

    // Me2020 批量码识别
    public static Vector<int[]> BatchQRcodeDetect(Bitmap bitmap, Vector<Bitmap> bv,
                                                  Classifier classifier) {
        Mat srcImg = new Mat();
        Utils.bitmapToMat(bitmap, srcImg);
        //==========================================================================================
        //1、预处理+边缘提取
        float scale = Math.max(srcImg.cols(), srcImg.rows()) / 1024.0f;
        int width = Math.round(srcImg.cols() / scale); //压缩后图片尺寸
        int height = Math.round(srcImg.rows() / scale);
        // 灰度变换 及 压缩降采样
        Mat grayImg = new Mat();
        Imgproc.resize(srcImg, srcImg, new Size(width, height));
        Imgproc.cvtColor(srcImg, grayImg, Imgproc.COLOR_RGB2GRAY);
        // 准备 梯度压缩因子
        double gArray[] = new double[256];
        for (int i = 0; i < 256; i++)
            gArray[i] = Math.pow(i, 0.7850);
        // 创建 sobel边缘图
        Mat sobel_x = new Mat();
        Mat sobel_y = new Mat();
        Imgproc.Scharr(grayImg, sobel_x, CvType.CV_16S, 1, 0); //对应short_16位
        Imgproc.Scharr(grayImg, sobel_y, CvType.CV_16S, 0, 1);
        // 创建 sobel累计积分图
        Mat sobelImg = Mat.zeros(height, width, CvType.CV_8UC1); //积分图像多一行一列均为0
        Mat IntegralImg = Mat.zeros(height + 1, width + 1, CvType.CV_64FC1); //对应float_32位
        // 合成sobel归一化边缘图像，建立sobel累计积分图
        getSobel_Integral_Image(sobel_x, sobel_y, sobelImg, IntegralImg, gArray);
        Mat IntegralImg8U = new Mat(); //显示sobel累计积分图
        Core.normalize(IntegralImg, IntegralImg8U, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
        bv.add(MatToBitmap565(grayImg, "grayImg"));
        bv.add(MatToBitmap565(sobelImg, "sobelImg"));
        bv.add(MatToBitmap565(IntegralImg8U, "IntegralImg"));
        grayImg.release();
        sobel_x.release();
        sobel_y.release();
        sobelImg.release();
        IntegralImg8U.release();
        Log.i(TAG, "\n\n===========================================================");
        Log.i(TAG, "1、预处理+边缘提取: " + String.valueOf(true));
        Log.i(TAG, "原图尺寸: " + String.valueOf(srcImg.cols())
                + "*" + String.valueOf(srcImg.rows()));
        Log.i(TAG, "压缩后尺寸: " + String.valueOf(width)
                + "*" + String.valueOf(height));
        //==========================================================================================
        //2、初框定+配置初始化
        Mat mar2DmapL1 = new Mat();
        int InitMsg[] = new int[2];
        int L1size = 2;
        boolean initFlag = Init_Mar2DmapL1(L1size, IntegralImg, mar2DmapL1, InitMsg);
        int L2size = InitMsg[0];
        bv.add(MatToBitmap565(mar2DmapL1, "mar2DmapL1"));
        mar2DmapL1.release();
        Log.i(TAG, "===========================================================");
        Log.i(TAG, "2、初框定+配置初始化: " + String.valueOf(initFlag));
        Log.i(TAG, "迭代次数: " + InitMsg[1]);
        Log.i(TAG, "初框定_分块尺寸: " + String.valueOf(L1size));
        Log.i(TAG, "二次框定_分块尺寸: " + String.valueOf(L2size));
        //==========================================================================================
        //3、二次框定+预筛选
        Mat mar2DmapL2 = new Mat();
        Vector<int[]> rectSet = new Vector<int[]>();
        int[] retMsg = getMar2DmapL2(L2size, IntegralImg, 1.25f, mar2DmapL2, rectSet);
        bv.add(MatToBitmap565(mar2DmapL2, "mar2DmapL2"));
        mar2DmapL2.release();
        Mat showRectImg = showRect_Me2020(srcImg, rectSet);
        bv.add(MatToBitmap565(showRectImg, "showRectImg"));
        showRectImg.release();
        Log.i(TAG, "===========================================================");
        Log.i(TAG, "3、二次框定+预筛选: " + String.valueOf(true));
        Log.i(TAG, "预筛选 前/后 框定数: " + String.valueOf(retMsg[0])
                + "  " + String.valueOf(retMsg[1]));
        //==========================================================================================
        //4、分类器、鉴别剔除
        int cnt = delRect_from_model(srcImg, rectSet, classifier);
        Log.i(TAG, "===========================================================");
        Log.i(TAG, "4、分类器、鉴别剔除: " + String.valueOf(true));
        Log.i(TAG, "鉴别剔除后 框定数: " + String.valueOf(cnt));
        Mat resultImg = showRect_Me2020(srcImg, rectSet);
        bv.add(MatToBitmap565(resultImg, "resultImg"));
        resultImg.release();
        //==========================================================================================
        //5、转换输出，坐标调整（对应回原图尺寸）
        Vector<int[]> result = new Vector<int[]>(); //[No, x0, y0, w, h, flag]
        int w0 = bitmap.getWidth();
        int h0 = bitmap.getHeight();
        int Number = 0;
        for (int i = 0; i < rectSet.size(); i++) {
            int[] rectMsg = rectSet.get(i);
            int isQRcode = rectMsg[5]; //标志
            if (isQRcode == 1) {
                rectMsg[0] = ++Number; //编号
                int x0 = Math.round(rectMsg[1] * scale); //左上角坐标
                int y0 = Math.round(rectMsg[2] * scale);
                int w = Math.round((rectMsg[3] - rectMsg[1] + 1) * scale); //宽
                int h = Math.round((rectMsg[4] - rectMsg[2] + 1) * scale); //高
                if (x0 < 0) x0 = 0; //边界判断
                if (y0 < 0) y0 = 0;
                if (x0 + w >= w0) w = w0 - x0 - 1;
                if (y0 + h >= h0) h = h0 - y0 - 1;
                rectMsg[1] = x0;
                rectMsg[2] = y0;
                rectMsg[3] = w;
                rectMsg[4] = h;
                result.add(rectMsg);
            }
        }
        return result;
    }

    //定位选中的二维码
    public static Bitmap locateChosenCode(Bitmap bm, int[] rect) {
        Mat srcImg = new Mat();
        Utils.bitmapToMat(bm, srcImg);
        int thickness = (rect[3]) / 24 + 1;
        Imgproc.rectangle(srcImg, new Point(rect[1],rect[2]), new Point(rect[1]+rect[3], rect[2]+rect[4]),
                new Scalar(0, 255, 0, 10), thickness);     //找到边框，填满
        return MatToBitmap565(srcImg, "locate");
    }

    public static void rotateP90(byte[] src, byte[] dest, int w, int h) {
        int pos = 0;
        //旋转Y
        int k = 0;
        for (int i = 0; i < w; i++) {
            for (int j = h - 1; j >= 0; j--) {
                dest[k++] = src[j * w + i];
            }
        }
        //旋转U
        pos = w * h;
        for (int i = 0; i < w / 2; i++) {
            for (int j = h / 2 - 1; j >= 0; j--) {
                dest[k++] = src[pos + j * w / 2 + i];
            }
        }
        //旋转V
        pos = w * h * 5 / 4;
        for (int i = 0; i < w / 2; i++) {
            for (int j = h / 2 - 1; j >= 0; j--) {
                dest[k++] = src[pos + j * w / 2 + i];
            }
        }
    }

    public static Bitmap pToBitmap(byte[] data, int w, int h, boolean uv) {
        int plane = w * h;
        int[] colors = new int[plane];
        int off = plane >> 2;
        int yPos = 0, uPos = plane + (uv ? 0 : off), vPos = plane + (uv ? off : 0);
        for(int j = 0; j < h; j++) {
            for(int i = 0; i < w; i++) {
                // YUV byte to RGB int
                final int y1 = data[yPos] & 0xff;
                final int u = (data[uPos] & 0xff) - 128;
                final int v = (data[vPos] & 0xff) - 128;
                final int y1192 = 1192 * y1;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                r = (r < 0) ? 0 : ((r > 262143) ? 262143 : r);
                g = (g < 0) ? 0 : ((g > 262143) ? 262143 : g);
                b = (b < 0) ? 0 : ((b > 262143) ? 262143 : b);
                colors[yPos] = ((r << 6) & 0xff0000) |
                        ((g >> 2) & 0xff00) |
                        ((b >> 10) & 0xff);

                if((yPos++ & 1) == 1) {
                    uPos++;
                    vPos++;
                }
            }
            if((j & 1) == 0) {
                uPos -= (w >> 1);
                vPos -= (w >> 1);
            }
        }
        return Bitmap.createBitmap(colors, w, h, Bitmap.Config.RGB_565);
    }

    //NV21 NV12格式可用此函数转换
    private static Bitmap spToBitmap(byte[] data, int w, int h, int uOff, int vOff) {
        int plane = w * h;
        int[] colors = new int[plane];
        int yPos = 0, uvPos = plane;
        for(int j = 0; j < h; j++) {
            for(int i = 0; i < w; i++) {
                // YUV byte to RGB int
                final int y1 = data[yPos] & 0xff;
                final int u = (data[uvPos + uOff] & 0xff) - 128;
                final int v = (data[uvPos + vOff] & 0xff) - 128;
                final int y1192 = 1192 * y1;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                r = (r < 0) ? 0 : ((r > 262143) ? 262143 : r);
                g = (g < 0) ? 0 : ((g > 262143) ? 262143 : g);
                b = (b < 0) ? 0 : ((b > 262143) ? 262143 : b);
                colors[yPos] = ((r << 6) & 0xff0000) |
                        ((g >> 2) & 0xff00) |
                        ((b >> 10) & 0xff);

                if((yPos++ & 1) == 1) uvPos += 2;
            }
            if((j & 1) == 0) uvPos -= w;
        }
        return Bitmap.createBitmap(colors, w, h, Bitmap.Config.RGB_565);
    }

    //yuv转Bitmap 1号算法
    public static Bitmap yuvToBitmap(byte[] data, int width, int height) {
        int frameSize = width * height;
        int[] rgba = new int[frameSize];
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                int y = (0xff & ((int) data[i * width + j]));
                int u = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 0]));
                int v = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 1]));
                y = y < 16 ? 16 : y;
                int r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128));
                int g = Math.round(1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = Math.round(1.164f * (y - 16) + 2.018f * (u - 128));
                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);
                rgba[i * width + j] = 0xff000000 + (b << 16) + (g << 8) + r;
            }
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0, width, 0, 0, width, height);
        return bmp;
    }

}
