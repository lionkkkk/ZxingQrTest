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
import com.example.zxingqrtest.Utils.PicProcessUtil;
import com.example.zxingqrtest.Utils.RealPathFromUriUtils;
import com.example.zxingqrtest.deeplearning.Classifier;
import com.example.zxingqrtest.deeplearning.TensorFlowImageClassifier;
import com.google.zxing.Result;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

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
    private static final String TAG = "Main";
    //tensorflow相关
    private Classifier classifier; //tf分类器实例
    private static final String MODEL_PATH = "mobileNetV2_0.25_32_quant.tflite"; //tf模型文件
    private static final String LABEL_PATH = "labels.txt"; //标签文件，在assert文件夹中
    private static final int INPUT_SIZE = 32; //输入图片尺寸
    private static final boolean QUANT = false; //是否为量化版tfModel
    private Executor executor = Executors.newSingleThreadExecutor();
    private String[] detectUrl = new String[]{"/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1586677396375.jpg",
            "/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1587016136817.jpg"
       };

    //二维码矩阵测试数据
    private boolean[][] imgMat=new boolean[][]{{true,true,true,true,true,true,true,false,false,true,true,true,true,false,true,true,true,true,true,true,true},
            {true,false,false,false,false,false,true,false,false,true,false,true,false,false,true,false,false,false,false,false,true},
            {true,false,true,true,true,false,true,false,false,true,false,false,false,false,true,false,true,true,true,false,true},
            {true,false,true,true,true,false,true,false,true,true,true,true,false,false,true,false,true,true,true,false,true},
            {true,false,true,true,true,false,true,false,true,false,true,true,true,false,true,false,true,true,true,false,true},
            {true,false,false,false,false,false,true,false,false,false,true,false,true,false,true,false,false,false,false,false,true},
            {true,true,true,true,true,true,true,false,true,false,true,false,true,false,true,true,true,true,true,true,true},
            {false,false,false,false,false,false,false,false,false,true,true,true,true,false,false,false,false,false,false,false,false},
            {true,true,false,false,false,true,true,true,false,true,false,false,true,false,false,false,true,true,false,false,false},
            {false,false,false,true,false,false,false,false,false,false,false,false,true,false,true,false,true,true,true,true,false},
            {false,true,true,false,false,true,true,true,true,true,true,true,false,true,false,false,true,true,false,false,true},
            {true,true,false,false,true,true,false,true,true,true,false,false,false,false,false,false,true,false,true,true,true},
            {false,false,false,false,false,true,true,true,true,false,false,false,false,false,true,false,false,true,false,false,true},
            {false,false,false,false,false,false,false,false,true,true,false,true,true,true,true,false,false,false,true,false,false},
            {true,true,true,true,true,true,true,false,true,true,true,false,true,false,true,true,false,true,false,false,false},
            {true,false,false,false,false,false,true,false,true,false,false,true,true,true,false,false,true,false,true,true,false},
            {true,false,true,true,true,false,true,false,false,false,true,false,true,false,false,true,false,true,true,false,false},
            {true,false,true,true,true,false,true,false,false,true,false,false,true,false,false,false,false,true,true,false,false},
            {true,false,true,true,true,false,true,false,false,true,true,false,false,false,true,true,true,false,true,true,true},
            {true,false,false,false,false,false,true,false,true,false,false,false,false,false,false,true,true,false,true,true,false},
            {true,true,true,true,true,true,true,false,true,true,true,true,false,true,false,true,false,false,true,false,false}};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();//初始化UI
        initTensorFlowAndLoadModel();   //加载tensorflow模型
        //启动相机
        btnOpenCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, CameraxTestActivity.class);
                startActivity(intent);
            }
        });

        //识别按钮点击事件
        btnDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
//                    Bitmap bitmap = BitmapFactory.decodeFile(loadPicPath);
//                    result= DecoderUtil.decodeQR(bitmap);
//                    tv_result.setText("Result:"+result.getText());
                      String [] results=new String[2];
                      for(int i=0;i<detectUrl.length;i++){
                          Bitmap bitmap = BitmapFactory.decodeFile(detectUrl[i]);
                          results[i]=DecoderUtil.decodeQR(bitmap).getText();
                      }
                    Intent intent = new Intent();
                    intent.setClass(MainActivity.this, ListActivity.class);
                    Bundle bd=new Bundle();
                    bd.putStringArray("urlArray",detectUrl);
                    bd.putStringArray("resultsArray",results);   //把数据传给ListActivity
                    intent.putExtras(bd);
                    startActivity(intent);


//                    String res=DecoderUtil.decodeFromBits(imgMat);        //直接根据输入的矩阵进行识别
//                    Log.i(TAG,res);

//                    Bitmap bitmap = BitmapFactory.decodeFile(loadPicPath);  //测试定位截取
//                    Bitmap images = PicProcessUtil.BatchQRcodeDetect(bitmap);
//                    mShowImg.setImageBitmap(images);

                } catch (Exception e) {

                }
            }
        });

        //图片滑动显示
        btnCvTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, ThreadActivity.class);
                startActivity(intent);
            }
        });
    }

    //初始化各种控件
    private void initView(){
        mShowImg = (ImageView) findViewById(R.id.imageView);       //图片视图
        tv_result = findViewById(R.id.tv_result);                  //识别结果tv
        btnOpenCamera = findViewById(R.id.btnOpenCamera);          //打开相机扫描
        btnDetect = findViewById(R.id.btnDetect);          //识别加载的图片
        btnCvTest = findViewById(R.id.btnCVTest);
    }

    //初始化tensorflow模型加载
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
                        loadPicPath = RealPathFromUriUtils.getRealPathFromUri(this, data.getData()); //存放打开的图片的路径
                        Log.i(TAG,loadPicPath);
                        Bitmap bm = BitmapFactory.decodeFile(loadPicPath);
                        mShowImg.setImageBitmap(bm);

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


    //测试tensorflow接口
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


    //OpenCV库加载并初始化成功后的回调函数
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            // TODO Auto-generated method stub
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                    Log.i(TAG, "成功加载");
                    break;
                default:
                    super.onManagerConnected(status);
                    Log.i(TAG, "加载失败");
                    break;
            }
        }
    };
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