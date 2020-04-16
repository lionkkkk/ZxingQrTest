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
    private static final String MODEL_PATH = "assets://mobileNetV2_0.25_32_quant.tflite"; //tf模型文件
    private static final String LABEL_PATH = "assets://labels.txt"; //标签文件，在assert文件夹中
    private static final int INPUT_SIZE = 32; //输入图片尺寸
    private static final boolean QUANT = false; //是否为量化版tfModel
    private Executor executor = Executors.newSingleThreadExecutor();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        //
        initTensorFlowAndLoadModel();

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
                    Bitmap bitmap = BitmapFactory.decodeFile(loadPicPath);
                    result = DecoderUtil.decodeQR(bitmap);
                    tv_result.setText("Result:"+result.getText());
                } catch (Exception e) {
                    Log.w("MainActivity", "Cannot detect" , e);
                    tv_result.setText("Can't detect");
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
     * 参    数：
     * 返回值：无
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
     * 参    数：
     * 返回值：无
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
     * 参    数：
     * 返回值：无
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_IMAGE:
                    if (data != null) {
                        loadPicPath = RealPathFromUriUtils.getRealPathFromUri(this, data.getData());
                        Log.e("MainActivity", loadPicPath);
                        showImg(loadPicPath);   //加载图片,同时图片路径存储在loadPicPath中
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
     * 参    数：
     * 返回值：无
     */
    public void openAlbum(View view) {
        ActivityCompat.requestPermissions(MainActivity.this, mPermissionList, 100);
    }

    /*
     * 方法名： showImg(String path)
     * 功    能：显示图片到界面
     * 参    数：图片路径
     * 返回值：无
     */
    public void showImg(String path){
        Log.i("MainActivity",path);
        //压缩测试
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inSampleSize = 2;
//        Bitmap bm = BitmapFactory.decodeFile(path, options);
//        Log.i("MainActivity", "压缩后图片的大小" + (bm.getByteCount() / 1024 / 1024)
//                + "M宽度为" + bm.getWidth() + "高度为" + bm.getHeight());
        Bitmap bm = BitmapFactory.decodeFile(path);
        mShowImg.setImageBitmap(bm);
        final List<Classifier.Recognition> results = classifier.recognizeImage(bm);
//        textViewResult.setText(results.toString());
        try {
            result = DecoderUtil.decodeQR(bm);
//            tv_result.setText("Result:"+result.getText());
            tv_result.setText("Result:"+result.getText()+" tflite:"+results.toString());
        } catch (Exception e) {
            Log.w("MainActivity", "Cannot detect" , e);
            tv_result.setText("Can't detect");
        }
    }

}