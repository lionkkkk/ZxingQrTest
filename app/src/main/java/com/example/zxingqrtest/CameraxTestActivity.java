package com.example.zxingqrtest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import com.google.common.util.concurrent.ListenableFuture;
import com.example.zxingqrtest.view.CameraXCustomPreviewView;
import com.example.zxingqrtest.view.FocusImageView;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;


/**
 * CameraX Demo
 * <p>
 * https://proandroiddev.com/android-camerax-tap-to-focus-pinch-to-zoom-zoom-slider-eb88f3aa6fc6
 *
 * @author
 * @date 2019-05-30
 */
public class CameraxTestActivity  extends AppCompatActivity implements CameraXConfig.Provider, View.OnClickListener {

    private CameraXCustomPreviewView mViewFinder;
    private ImageButton mCaptureButton;
    private ImageButton imgBtnAuto;
    private FocusImageView mFocusView;
    private AppCompatButton mBtnLight;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture mImageCapture;
    private ImageAnalysis mImageAnalysis;
    private Executor executor;
    private CameraInfo mCameraInfo;
    private CameraControl mCameraControl;
    private int count =0 ;
    private float posX[]={250,850,250,850,550};
    private float posY[]={600,600,1400,1400,1000};  //自定义对焦坐标
    private boolean autoTakeFlag = false;
    private ArrayList<String> strListPicPath = new ArrayList<String>(); //用于存放自动拍照地5张图片的URI
    //自动拍照
    Handler handler=new Handler();
    Runnable runnableAuto = new Runnable() {
        @Override
        public void run() {
            setFocusPosition(posX[count],posY[count]);  //设定对焦坐标
            count++;
            saveImage();
            if(count>5){
                Message message = new Message();
                message.what=1;
                handlerStop.sendMessage(message);
            }
            handler.postDelayed(runnableAuto,2000);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camerax_test);
        initEventAndData();
    }

    /*
     * 方法名： initEventAndData()
     * 功    能：初始化
     * 参    数：
     * 返回值：无
     */
    protected void initEventAndData() {
        initView();
        initListenner();
        initCamera();
        initImageAnalysis();
        initImageCapture();
    }

    /*
     * 方法名： initView()
     * 功    能：初始化控件
     * 参    数：
     * 返回值：无
     */
    private void initView(){
        mViewFinder = findViewById(R.id.view_finder);
        mCaptureButton =findViewById(R.id.capture_button);
        mFocusView = findViewById(R.id.focus_view);
        mBtnLight = findViewById(R.id.btn_light);
        imgBtnAuto= findViewById(R.id.btnAutoTake);
    }

    /*
     * 方法名： 初始化监听()
     * 功    能：初始化控件
     * 参    数：
     * 返回值：无
     */
    private  void initListenner(){
        mCaptureButton.setOnClickListener(this);
        mBtnLight.setOnClickListener(this);
        imgBtnAuto.setOnClickListener(this);
    }


    /*
     * 方法名： initCamera()
     * 功    能：初始化相机
     * 参    数：
     * 返回值：无
     */
    private void initCamera() {
        executor = ContextCompat.getMainExecutor(this);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {

            }
        }, executor);

    }

    /*
     * 方法名： bindPreview()
     * 功    能：绑定预览界面
     * 参    数：
     * 返回值：无
     */
    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, mImageCapture, mImageAnalysis, preview);
        mCameraInfo = camera.getCameraInfo();
        mCameraControl = camera.getCameraControl();
        preview.setSurfaceProvider(mViewFinder.createSurfaceProvider(mCameraInfo));
        initCameraListener();
    }


    /**
     * 自定义对焦
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initCameraListener() {
        LiveData<ZoomState> zoomState = mCameraInfo.getZoomState();
        float maxZoomRatio = zoomState.getValue().getMaxZoomRatio();
        float minZoomRatio = zoomState.getValue().getMinZoomRatio();
        mViewFinder.setCustomTouchListener(new CameraXCustomPreviewView.CustomTouchListener() {
            @Override
            public void zoom() {
                float zoomRatio = zoomState.getValue().getZoomRatio();
                if (zoomRatio < maxZoomRatio) {
                    mCameraControl.setZoomRatio((float) (zoomRatio + 0.1));
                }
            }

            @Override
            public void ZoomOut() {
                float zoomRatio = zoomState.getValue().getZoomRatio();
                if (zoomRatio > minZoomRatio) {
                    mCameraControl.setZoomRatio((float) (zoomRatio - 0.1));
                }
            }
            //手动对焦
            @Override
            public void click(float x, float y) {
                // TODO 对焦
                setFocusPosition(x,y);
            }

            //双击放大
            @Override
            public void doubleClick(float x, float y) {
                float zoomRatio = zoomState.getValue().getZoomRatio();
                if (zoomRatio > minZoomRatio) {
                    mCameraControl.setLinearZoom(0f);
                } else {
                    mCameraControl.setLinearZoom(0.5f);
                }
            }
            @Override
            public void longClick(float x, float y) {

            }
        });
    }

    /**
     * 图像分析
     */
    private void initImageAnalysis() {

        mImageAnalysis = new ImageAnalysis.Builder()
                // 分辨率
                .setTargetResolution(new Size(1280, 720))
                // 非阻塞模式
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        mImageAnalysis.setAnalyzer(executor, image -> {
            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            // byte[] b = new byte[buffer.remaining()];
            // LogUtils.e(b);
            // TODO: 分析完成后关闭图像参考，否则会阻塞其他图像的产生
            // image.close();
        });
    }

    /**
     * 构建图像捕获用例
     */
    private void initImageCapture() {
        // 构建图像捕获用例
        mImageCapture = new ImageCapture.Builder()
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();
        // 旋转监听
        OrientationEventListener orientationEventListener = new OrientationEventListener((Context) this) {
            @Override
            public void onOrientationChanged(int orientation) {
                int rotation;
                // Monitors orientation values to determine the target rotation value
                if (orientation >= 45 && orientation < 135) {
                    rotation = Surface.ROTATION_270;
                } else if (orientation >= 135 && orientation < 225) {
                    rotation = Surface.ROTATION_180;
                } else if (orientation >= 225 && orientation < 315) {
                    rotation = Surface.ROTATION_90;
                } else {
                    rotation = Surface.ROTATION_0;
                }
                mImageCapture.setTargetRotation(rotation);
            }
        };
        orientationEventListener.enable();
    }

    @NonNull
    @Override
    public CameraXConfig getCameraXConfig() {
        return Camera2Config.defaultConfig();
    }

    //设置对焦
    private void setFocusPosition(float x,float y){
        MeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(1.0f, 1.0f);
        MeteringPoint point = factory.createPoint(x, y);
        FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                // auto calling cancelFocusAndMetering in 3 seconds
                .setAutoCancelDuration(2, TimeUnit.SECONDS)
                .build();

        mFocusView.startFocus(new Point((int) x, (int) y));
        ListenableFuture future = mCameraControl.startFocusAndMetering(action);
        future.addListener(() -> {
            try {
                FocusMeteringResult result = (FocusMeteringResult) future.get();
                if (result.isFocusSuccessful()) {
                    mFocusView.onFocusSuccess();
                } else {
                    mFocusView.onFocusFailed();
                }
            } catch (Exception e) {
            }
        }, executor);
    }
    //保存图片
    public void saveImage() {
        //新建图片
        File file = new File(getExternalMediaDirs()[0],System.currentTimeMillis() + ".jpg");
        //从capture流保存图片
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        mImageCapture.takePicture(outputFileOptions, executor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String msg = "图片保存成功: " + file.getAbsolutePath();
                        Log.i("CameraxTestActivity",msg);
                        Uri contentUri = Uri.fromFile(new File(file.getAbsolutePath()));
                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
                        sendBroadcast(mediaScanIntent);
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        String msg = "图片保存失败: " + exception.getMessage();
                    }
                }
        );
    }

    //按钮点击
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.capture_button:
                Log.i("CameraxTestActivty","take pic");
                saveImage();
                Log.i("CameraxTestActivty","stop auto take");
                break;
            case R.id.btn_light:
                switch (mImageCapture.getFlashMode()) {
                    case ImageCapture.FLASH_MODE_AUTO:
                        mImageCapture.setFlashMode(ImageCapture.FLASH_MODE_ON);
                        mBtnLight.setText("闪光灯：开");
                        break;
                    case ImageCapture.FLASH_MODE_ON:
                        mImageCapture.setFlashMode(ImageCapture.FLASH_MODE_OFF);
                        mBtnLight.setText("闪光灯：关");
                        break;
                    case ImageCapture.FLASH_MODE_OFF:
                        mImageCapture.setFlashMode(ImageCapture.FLASH_MODE_AUTO);
                        mBtnLight.setText("闪光灯：自动");
                        break;
                }
                break;
            case R.id.btnAutoTake:
                handler.post(runnableAuto);
                autoTakeFlag=true;
                Log.i("CameraxTestActivty","auto take");
                break;
        }
    }

    //消息队列,用于终止连拍
    final Handler handlerStop = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    count = 0;
                    handler.removeCallbacks(runnableAuto);
                    break;
            }
            super.handleMessage(msg);
        }
    };

}
