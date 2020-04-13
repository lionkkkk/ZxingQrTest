package com.example.zxingqrtest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.zxingqrtest.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class GrayTestActivity  extends AppCompatActivity implements View.OnClickListener{
    ImageView img_after;
    TextView text_togray;
    Bitmap srcBitmap;
    Bitmap grayBitmap;
    private static boolean flag = true;
    //private static boolean isFirst = true;
    private static final String TAG = "GrayTest";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gray_test);
        img_after=(ImageView)findViewById(R.id.imageView_after);
        text_togray=(TextView)findViewById(R.id.textView_togray);
        text_togray.setOnClickListener(this);

    }
    //OpenCV库加载并初始化成功后的回调函数
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            // TODO Auto-generated method stub
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                    Log.i(TAG, "成功加载");
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "成功加载！", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;
                default:
                    super.onManagerConnected(status);
                    Log.i(TAG, "加载失败");
                    Toast toast1 = Toast.makeText(getApplicationContext(),
                            "加载失败！", Toast.LENGTH_LONG);
                    toast1.setGravity(Gravity.CENTER, 0, 0);
                    toast1.show();
                    break;
            }

        }
    };

    public void procSrc2Gray(){
        Mat rgbMat = new Mat();
        Mat grayMat = new Mat();
        Mat binaMat = new Mat();
        srcBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pic);
        grayBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.RGB_565);
        Utils.bitmapToMat(srcBitmap, rgbMat);//convert original bitmap to Mat, R G B.
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat
        Imgproc.adaptiveThreshold(grayMat,binaMat,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_OTSU,11,1);
        Utils.matToBitmap(binaMat, grayBitmap); //convert mat to bitmap
        Log.i(TAG, "procSrc2Gray sucess...");
    }
    @Override
    public void onClick(View v)
    {
        switch(v.getId())
        {
            case R.id.textView_togray:
                procSrc2Gray();
                img_after.setImageBitmap(grayBitmap) ;
                break;
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}

