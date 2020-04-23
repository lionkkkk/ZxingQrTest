package com.example.zxingqrtest;


import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.zxingqrtest.Utils.DecoderUtil;
import com.example.zxingqrtest.adapter.MyAdapter;
import com.example.zxingqrtest.beans.Informations;

public class ThreadActivity extends Activity {
    /** Called when the activity is first created. */

    private Handler handler ;
    private Button bt;
    private String[] picUrl = new String[]{"/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1587624940260.jpg",
            "/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1587624936624.jpg",
            "/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1587016136817.jpg",
            "/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1587624933069.jpg",
            "/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1587016136817.jpg"};
    private ListView list_one;
    private MyAdapter mAdapter = null;
    private List<Informations> mData = null;
    private Context mContext = null;
    private TextView txt_empty;
    private Informations minfo=null;
    private Handler mainHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);

        }


    };
    private ExecutorService service = Executors.newFixedThreadPool(200);  //这里可以根据需要开
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);
        initViews();

        mContext = ThreadActivity.this;
        mData = new LinkedList<Informations>();
        mAdapter = new MyAdapter((LinkedList<Informations>) mData,mContext);
        list_one.setAdapter(mAdapter);       //各种初始化
    }

    private void initViews(){
        bt = (Button)findViewById(R.id.btn_add);
        list_one = (ListView) findViewById(R.id.list_one);
        txt_empty = (TextView) findViewById(R.id.txt_empty);
        txt_empty.setText("暂无数据~");
        list_one.setAdapter(mAdapter);
        list_one.setEmptyView(txt_empty);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                long startTime = System.currentTimeMillis(); // 获取开始时间
                for(int i=0;i<200;i++)
                {
                    BatchDetect(picUrl[0]);
                }
                long endTime = System.currentTimeMillis(); // 获取结束时间
                Log.e("wsy","代码运行时间： " + (endTime - startTime) + "ms");
            }
        });
    }

    //启用多线程进行识别测试
    private void BatchDetect(final String url){
        service.submit(new Runnable(){
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Log.i("Main", ""+Thread.currentThread().getName());
                try {
                    mainHandler.post(new Runnable(){
                        @Override
                        public void run() {//这将在主线程运行
                            // TODO Auto-generated method stub
                            try{
                                Bitmap bitmap = BitmapFactory.decodeFile(url);
                                String result=new String();
                                result= DecoderUtil.decodeQR(bitmap).getText();  //识别
                                minfo = new Informations(url, "result: "+result);
                                mAdapter.add(minfo);//listview加载数据
                            }catch (Exception e){
                                Log.e("Main", "Exception: "+Log.getStackTraceString(e));
                            }

                        }
                    });
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

}
