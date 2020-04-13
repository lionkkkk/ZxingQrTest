
package com.example.zxingqrtest;


        import android.app.Activity;
        import android.content.Context;
        import android.content.Intent;
        import android.os.Bundle;
        import android.view.View;
        import android.widget.Button;
        import android.widget.Toast;

        import com.google.zxing.integration.android.IntentIntegrator;
        import com.google.zxing.integration.android.IntentResult;

/**
 * 这是调用者
 */

public class PhotoActivity extends Activity {

    private Context context;
    private Button QrOpen;
    private final static int REQUEST_CODE = 1001;
    private Button btnCvTest;
    private Button btnPhotoTest;
    private Button btnLoadPic;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

//        QrOpen = (Button) findViewById(R.id.QrOpen);
//        QrOpen.setOnClickListener(QrOpenListener);
//
//        //CV功能测试
//        btnCvTest =findViewById(R.id.btnGray);
//        btnCvTest.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent();
//                intent.setClass(MainActivity.this, GrayTestActivity.class);//this前面为当前activty名称，class前面为要跳转到得activity名称
//                startActivity(intent);
//            }
//        });
//
//        //拍照功能测试
        btnPhotoTest =findViewById(R.id.btnTake);
        btnPhotoTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(PhotoActivity.this, CameraActivity.class);//this前面为当前activty名称，class前面为要跳转到得activity名称
                startActivity(intent);
            }
        });

//        btnLoadPic=findViewById(R.id.btnLoadPic);
//        btnLoadPic.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent();
//                intent.setClass(MainActivity.this, PhotoActivity.class);//this前面为当前activty名称，class前面为要跳转到得activity名称
//                startActivity(intent);
//            }
//        });

    }

    private View.OnClickListener QrOpenListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //打开扫描界面
            IntentIntegrator intentIntegrator = new IntentIntegrator(PhotoActivity.this);
            intentIntegrator.setOrientationLocked(false);
            intentIntegrator.setCaptureActivity(QRActivity.class); // 设置自定义的activity是QRActivity
            intentIntegrator.setRequestCode(REQUEST_CODE);
            intentIntegrator.initiateScan();
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(resultCode, data);
            final String qrContent = scanResult.getContents();
            Toast.makeText(context, "扫描结果:" + qrContent, Toast.LENGTH_SHORT).show();
        }
    }
}



