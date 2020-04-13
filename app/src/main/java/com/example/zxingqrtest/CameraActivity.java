package com.example.zxingqrtest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.zxingqrtest.Utils.PollingUtil;
import com.google.android.cameraview.CameraView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;



public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraTest";
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private CameraView mCameraView;

    //用于在子线程中处理图片数据
    private Handler mBackgroundHandler;
    private static Handler mHandler=new Handler();
    private FloatingActionButton fabFlash;
    private FloatingActionButton fabTake;
    private FloatingActionButton fabDetect;
    private PollingUtil pollingUtil;
    private int count=0;
    private LinearLayout linTouch;
    //自动拍照
    Handler handler = new Handler();
    Runnable runnableAuto = new Runnable() {
        @Override
        public void run() {
            if (mCameraView != null) {
                mCameraView.takePicture();
            }
            handler.postDelayed(runnableAuto,500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mCameraView = (CameraView) findViewById(R.id.camera);
        fabTake = (FloatingActionButton) findViewById(R.id.take_picture);
        fabFlash=findViewById(R.id.fabFlash);
        fabDetect=findViewById(R.id.fab_detect);

        //添加相机监听回调
        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }

        //拍照按钮
        if (fabTake != null) {
            fabTake.setOnClickListener(mOnClickListener);
        }

        //闪光开关
        if (fabFlash != null) {
            fabFlash.setOnClickListener(mOnClickListener);
        }

        //多帧拍照测试
        if(fabDetect!=null){
            fabDetect.setOnClickListener(mOnClickListener);
        }


    }

    //点击事件集合
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.take_picture:   //单独拍一张照片
                    handler.removeCallbacks(runnableAuto);  //停止连拍的线程
                    if (mCameraView != null) {
//                        Camera1
                        mCameraView.takePicture();
                    }
                    break;
                case R.id.fabFlash:  //闪光灯设置
                    Log.d(TAG, "flash");
                    if(mCameraView.getFlash()==CameraView.FLASH_ON){
                        mCameraView.setFlash(CameraView.FLASH_OFF);
                        fabFlash.setImageResource(R.drawable.ic_flash_off);
                    }
                    else if(mCameraView.getFlash()==CameraView.FLASH_OFF){
                        mCameraView.setFlash(CameraView.FLASH_ON);
                        fabFlash.setImageResource(R.drawable.ic_flash_on);
                    } else if(mCameraView.getFlash()==CameraView.FLASH_AUTO){
                        mCameraView.setFlash(CameraView.FLASH_OFF);
                        fabFlash.setImageResource(R.drawable.ic_flash_off);
                        Log.d(TAG, "auto");
                    }
                    break;
                case R.id.fab_detect:
                    handler.post(runnableAuto);
                    break;
//                case R.id.camera:

                    }

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //检查权限，如果有权限就启动相机，没有就去请求权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            mCameraView.start();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onPause() {
        //关闭相机
        mCameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    //获取权限
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (permissions.length != 1 || grantResults.length != 1) {
                    throw new RuntimeException("Error on requesting camera permission.");
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.camera_permission_not_granted,
                            Toast.LENGTH_SHORT).show();
                }
                // No need to start camera here; it is handled by onResume
                break;
        }
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    //相机 监听回调事件
    private CameraView.Callback mCallback = new CameraView.Callback() {
        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }
        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        //拍照回调函数，并将数据保存到本地
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);
            Toast.makeText(cameraView.getContext(), R.string.picture_taken, Toast.LENGTH_SHORT)
                    .show();
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    //在子线程中保存图片,并以时间命名
                    Date date = new Date();
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                            date.toString()+".jpg");
                    OutputStream moutputStream = null;
                    try {
                        moutputStream = new FileOutputStream(file);
                        moutputStream.write(data);
                        moutputStream.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Cannot write to " + file, e);
                    } finally {
                        if (moutputStream != null) {
                            try {
                                moutputStream.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }
                }
            });
        }
    };


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

