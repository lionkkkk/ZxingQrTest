/*
 * 文件名：MainActivity
 * 描    述：主界面
 * 作    者：We Chan
 */
package com.example.zxingqrtest;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.zxingqrtest.Utils.DecoderUtil;
import com.example.zxingqrtest.Utils.RealPathFromUriUtils;
import com.google.zxing.Result;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private String[] mPermissionList = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
    public static final int REQUEST_PICK_IMAGE = 11101;
    private ImageView mShowImg;
    private Button btnOpenCamera;
    private Button btnDetect;
    private Button btnCvTest;
    private Result result;
    private TextView tv_result;
    private String loadPicPath;

    //tensorflow相关
    private Classifier classifier; //tf分类器实例
    //private static final String MODEL_PATH = "mobileNetV2_0.25_32.tflite"; //tf模型文件
    private static final String MODEL_PATH = "mobileNetV2_0.25_32_quant.tflite"; //tf模型文件
    private static final String LABEL_PATH = "labels.txt"; //标签文件，在assert文件夹中
    private static final int INPUT_SIZE = 32; //输入图片尺寸
    private static final boolean QUANT = false; //是否为量化版tfModel
    private Executor executor = Executors.newSingleThreadExecutor();
    private ArrayList<Bitmap> bmList=new ArrayList<Bitmap>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();//初始化UI
        initTensorFlowAndLoadModel();   //加载tensorflow模型

        btnOpenCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, CameraxTestActivity.class);
                startActivity(intent);
            }
        });

        btnDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
//                    Bitmap bitmap = BitmapFactory.decodeFile(loadPicPath);  //保存加载图片的路径
//                    result = DecoderUtil.decodeQR(bitmap);
//                    tv_result.setText("Result:"+result.getText());
                } catch (Exception e) {

                }
            }
        });

        btnCvTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SlideTestActivity.class);
                startActivity(intent);
            }
        });
    }

    /*
     * 方法名： initView()
     * 功    能：初始化各种控件
     * 参    数：
     * 返回值：无
     */
    private void initView(){
        mShowImg = (ImageView) findViewById(R.id.imageView);       //图片视图
        tv_result = findViewById(R.id.tv_result);                  //识别结果tv
        btnOpenCamera = findViewById(R.id.btnOpenCamera);          //打开相机扫描
        btnDetect = findViewById(R.id.btnDetect);          //识别加载的图片
        btnCvTest = findViewById(R.id.btnCVTest);
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_PATH,
                            LABEL_PATH,
                            INPUT_SIZE,
                            QUANT);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    /*
     * 方法名：onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
     * 功    能：获取文件读取请求，系统函数
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 100:
                boolean writeExternalStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean readExternalStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                Log.e("MainActivity", Arrays.toString(grantResults));
                if (grantResults.length > 0 && writeExternalStorage && readExternalStorage) {
                    getImage();
                } else {
                    Toast.makeText(this, "请设置必要权限", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /*
     * 方法名： getImage()
     * 功    能：从相册中获取图片文件
     */
    private void getImage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"),
                    REQUEST_PICK_IMAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        }
    }

    /*
     * 方法名： onActivityResult()
     * 功    能：获取选中的图片的Uri,并显示到主界面
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_IMAGE:
                    if (data != null) {
                        loadPicPath = RealPathFromUriUtils.getRealPathFromUri(this, data.getData());
                        cutPic2Pieces(loadPicPath);    //切割图片
                        for(int i=0;i<bmList.size();i++)
                        {
                            String output=tensorProcess( bmList.get(i));
                            Log.i("test",output);
                        }

                    } else {
                        Toast.makeText(this, "图片损坏，请重新选择", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    }

    /*
     * 方法名： openAlbum()
     * 功    能：“图片加载”按钮的点击事件
     */
    public void openAlbum(View view) {
        ActivityCompat.requestPermissions(MainActivity.this, mPermissionList, 100);
    }

    /*
     * 方法名： showImg(String path)
     * 功    能：显示图片到界面
     */
    private void showImg(String path){
        Bitmap bm = BitmapFactory.decodeFile(path);
        mShowImg.setImageBitmap(bm);
//        long startTime = System.currentTimeMillis(); //起始时间
//       String output = tensorProcess(bitmap);
//       tv_result.setText(output.toString());
//        long endTime = System.currentTimeMillis(); //结束时间
//        long runTime = endTime - startTime;
//        Log.i("test", String.format("方法使用时间 %d ms", runTime));
    }

    //测试tensorflow接口调用
    private String tensorProcess(Bitmap bm){
        Bitmap bitmap = Bitmap.createScaledBitmap(bm, INPUT_SIZE, INPUT_SIZE, false); //降低采样率
        final float results[][]=classifier.recognizeImage(bitmap);
        String output="";
        for(int i=0;i<results.length;i++){
            output+="[";
            for(int j=0;j<results[0].length;j++)
            {
                output+=results[i][j]+",";
            }
            output+="]\n\r";
        }
        return output;
    }

  //切割图片
    private void cutPic2Pieces(String path){
        Mat m = new Mat();
        Bitmap bm = BitmapFactory.decodeFile(path);
        Utils.bitmapToMat(bm, m);
        for(int i=0;i<bm.getWidth();i+=bm.getWidth()/3)
            for(int j=0;j<bm.getHeight();j+=bm.getHeight()/3){
                Mat r = reSizeMat(m,i,j,200,200);
                Bitmap b = Bitmap.createBitmap(r.cols(), r.rows(),
                        Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(r, b);
                if(bmList.add(b))
                    Log.i("test",i+","+j);
            }
    }

    //根据坐标和宽高切割图片
    private Mat reSizeMat(Mat src,int x,int y,int width, int height){
        Rect rect=new Rect(x,y,width,height);
        Mat result=new Mat(src,rect);
        return result;
    }

    //openCV4Android 需要加载用到
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}