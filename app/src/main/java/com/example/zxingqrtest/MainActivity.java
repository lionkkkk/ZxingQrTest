/*
 * 文件名：MainActivity
 * 描    述：主界面
 * 作    者：We Chan
 */
package com.example.zxingqrtest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.zxingqrtest.Utils.DecoderUtil;
import com.example.zxingqrtest.Utils.PicProcessUtil;
import com.example.zxingqrtest.Utils.RealPathFromUriUtils;
import com.example.zxingqrtest.adapter.MyAdapter;
import com.example.zxingqrtest.beans.Informations;
import com.example.zxingqrtest.deeplearning.Classifier;
import com.example.zxingqrtest.deeplearning.TensorFlowImageClassifier;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String[] mPermissionList = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
    public static final int REQUEST_PICK_IMAGE = 11101;
    private ImageView mShowImg;
    private Button btnOpenCamera;
    private Button btnDetect;
    private Button btnBatch;
    private ImageButton btnLast;
    private ImageButton btnNext;
    private String loadPicPath;
    private static final String TAG = "Main";
    private int picIndex = 0;
    //tensorflow相关
    private Classifier classifier; //tf分类器实例
    private static final String MODEL_PATH = "mobileNetV2_0.25_32_float_36.tflite"; //tf模型文件
    private static final String LABEL_PATH = "labels.txt"; //标签文件，在assert文件夹中
    private static final int INPUT_SIZE = 32; //输入图片尺寸
    private static final boolean QUANT = false; //是否为量化版tfModel
    private Executor executor = Executors.newSingleThreadExecutor();
    private Vector<Bitmap> bv = new Vector<Bitmap>();                                     //测试返回切割图片使用
    private Vector<Bitmap> bv1 = new Vector<Bitmap>(10,1); //检测算法使用
    private Vector<int[]> rectMsg = new Vector<int []>(10,1); //检测条码边框信息
    private int nums = 0;                                                                  //检测到的二维码个数，批量检测需要开的线程个数

    private ListView list_result;                           //listview加载数据
    private MyAdapter mAdapter = null;
    private List<Informations> mData = null;
    private Informations minfo=null;
    private String result="unDecode error";
    private Handler handler ;                             //多线程相关
    private ExecutorService service;
    private Handler mainHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);

        }


    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();//初始化UI
        initTensorFlowAndLoadModel();   //加载tensorflow模型

    }

    //初始化各种控件
    private void initView() {
        mShowImg = (ImageView) findViewById(R.id.imageView);       //图片视图
        btnOpenCamera = findViewById(R.id.btnOpenCamera);          //打开相机扫描
        btnDetect = findViewById(R.id.btnDetect);          //识别加载的图片
        btnBatch = findViewById(R.id.btnBatch);
        btnLast = findViewById(R.id.btn_last);
        btnNext = findViewById(R.id.btn_next);

        btnOpenCamera.setOnClickListener(this);           //设置点击事件
        btnBatch.setOnClickListener(this);
        btnDetect.setOnClickListener(this);
        btnLast.setOnClickListener(this);
        btnNext.setOnClickListener(this);

        list_result=findViewById(R.id.list_result);
        mData = new LinkedList<Informations>();           //listview初始化
        mAdapter = new MyAdapter((LinkedList<Informations>) mData,MainActivity.this);
        list_result.setAdapter(mAdapter);       //各种初始化
        list_result.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Bitmap bm1=BitmapFactory.decodeFile(loadPicPath);
                Bitmap bmLocate= PicProcessUtil.locateChosenCode(bm1,rectMsg.get(i));
                mShowImg.setImageBitmap(bmLocate);
//                int[] rect = rectMsg.get(i);
//                Log.i("Main","切割块图信息: \n\r\r"+
//                        String.valueOf(rect[0])+"  "+ String.valueOf(rect[1])+"  "+
//                        String.valueOf(rect[2])+"  "+ String.valueOf(rect[3])+"  "+
//                        String.valueOf(rect[4]));
            }
        });
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

    ///////////////////////////////////////////////-------分隔线-------/////////////////////////////////////////////////
    // 功    能：获取文件读取请求，系统函数
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

    //从相册中获取图片文件
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

    //获取选中的图片的Uri,并显示到主界面
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_IMAGE:
                    if (data != null) {
                        loadPicPath = RealPathFromUriUtils.getRealPathFromUri(this, data.getData()); //存放打开的图片的路径
                        Log.i(TAG, loadPicPath);
                        Bitmap bm = BitmapFactory.decodeFile(loadPicPath);
                        mShowImg.setImageBitmap(bm);
                    } else {
                        Toast.makeText(this, "图片损坏，请重新选择", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    }

    //“图片加载”按钮的点击事件
    public void openAlbum(View view) {
        ActivityCompat.requestPermissions(MainActivity.this, mPermissionList, 100);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnOpenCamera:  //打开相机扫描
                openCameraIntent();
                break;
            case R.id.btnDetect:          //批量检测
                singleDetectIntent();
                if(bv1.isEmpty()==true){
                    Toast.makeText(MainActivity.this,"can't find rect",Toast.LENGTH_SHORT    );
                    break;
                }
                btnNext.setVisibility(View.VISIBLE);
                btnLast.setVisibility(View.VISIBLE);
                picIndex = bv1.size()-1;  //显示结果
                mShowImg.setImageBitmap(bv1.get(picIndex));
                picIndex = 0;
                break;
            case R.id.btnBatch:           //批量识别
                mAdapter.clear();         //清空Listview数据
                if(nums!=0)
                    service = Executors.newFixedThreadPool(nums);
                for(int i=0;i<nums;i++)
                    BatchDetectThread(bv.get(i),i);
                break;
            case R.id.btn_last:  //上一张 切割测试
                if(bv.isEmpty()==true)break;
                if (picIndex == 0) {
                    picIndex = bv.size() - 1;
                } else {
                    picIndex--;
                }
                mShowImg.setImageBitmap(bv.get(picIndex));
                break;
            case R.id.btn_next:  //下一张 切割测试
                if(bv.isEmpty()==true)break;
                if (picIndex == bv.size() - 1) {
                    picIndex = 0;
                } else {
                    picIndex++;
                }
                mShowImg.setImageBitmap(bv.get(picIndex));
                break;
        }
    }

    //单个检测
    private void singleDetectIntent() {
        try {
            Bitmap bm1 = BitmapFactory.decodeFile(loadPicPath);
            bv1.clear(); //调用前，先删掉之前的全部图片，检测算法带回中间调试结果放在bv1中
            rectMsg = PicProcessUtil.BatchQRcodeDetect(bm1, bv1, classifier);
            // 测试用例，根据rectMag切割图片，放在bv中，可选择切换显示
            bv.clear();
            nums = rectMsg.size();
            Log.i("Main","待切割图片数量: "+String.valueOf(nums));
            for(int i=0; i<nums; i++){
                int[] rect = rectMsg.get(i);
                bv.add(Bitmap.createBitmap(bm1, rect[1], rect[2], rect[3], rect[4]));
                Log.i("Main","切割块图信息: \n\r\r"+
                        String.valueOf(rect[0])+"  "+ String.valueOf(rect[1])+"  "+
                        String.valueOf(rect[2])+"  "+ String.valueOf(rect[3])+"  "+
                        String.valueOf(rect[4]));
            }

        } catch (Exception e) {

        }
    }

    //打开Camerax扫描界面
    private void openCameraIntent() {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, CameraxTestActivity.class);
        startActivity(intent);
    }


    //多线程批量识别二维码
    private void BatchDetectThread(final Bitmap bm,int index){
        service.submit(new Runnable(){
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Log.i("Main", ""+Thread.currentThread().getName());
                try {
                    mainHandler.post(new Runnable(){
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            try{
                                result= DecoderUtil.decodeQR(bm).getText();       //识别
                            }catch (Exception e){
                                Log.e("Main", "Exception: "+Log.getStackTraceString(e));
                            }
                            minfo = new Informations(bm, "NO."+index+"->"+result);
                            mAdapter.add(minfo);//listview加载数据
                        }
                    });
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    ///////////////////////////////////////////////////////////-------分隔线-------/////////////////////////////////////////////////
    //OpenCV库加载并初始化成功后的回调函数
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            // TODO Auto-generated method stub
            switch (status) {
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
    public void onResume() {
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