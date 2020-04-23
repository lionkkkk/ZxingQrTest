package com.example.zxingqrtest;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.zxingqrtest.adapter.MyAdapter;
import com.example.zxingqrtest.beans.Informations;

import java.util.LinkedList;
import java.util.List;

public class ListActivity extends AppCompatActivity implements View.OnClickListener {

    private ListView list_one;
    private MyAdapter mAdapter = null;
    private List<Informations> mData = null;
    private Context mContext = null;
    private Button btn_add;
    private TextView txt_empty;
    private int flag = 0;
    private Informations mData_5 = null;   //用来临时放对象的
    private Informations minfo=null;

    private String[] picUrl = new String[]{"/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1587106356109.jpg",
            "/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1587016136817.jpg",
            "/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1586749412079.jpg",
            "/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1586678359120.jpg",
            "/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1586677396375.jpg"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        mContext = ListActivity.this;
        bindViews();
        mData = new LinkedList<Informations>();
        mAdapter = new MyAdapter((LinkedList<Informations>) mData,mContext);
        list_one.setAdapter(mAdapter);       //各种初始化


        Bundle bd = this.getIntent().getExtras();
        String[] urlStrArr = bd.getStringArray("urlArray");
        String[] resultsArr = bd.getStringArray("resultsArray");
        for(int i = 0; i< urlStrArr.length&&i< resultsArr.length; i++){
            minfo = new Informations(urlStrArr[i], resultsArr[i]);
            mAdapter.add(minfo);
        }//listview显示结果
    }

    private void bindViews(){
        list_one = (ListView) findViewById(R.id.list_one);
        txt_empty = (TextView) findViewById(R.id.txt_empty);
        txt_empty.setText("暂无数据~");
        list_one.setAdapter(mAdapter);
        list_one.setEmptyView(txt_empty);
        btn_add = findViewById(R.id.btn_add);
        btn_add.setOnClickListener(this);
    }


    private void updateListItem(int postion,Informations mData){
        int visiblePosition = list_one.getFirstVisiblePosition();
        View v = list_one.getChildAt(postion - visiblePosition);
        ImageView img = (ImageView) v.findViewById(R.id.img_icon);
        TextView tv = (TextView) v.findViewById(R.id.txt_content);
        Bitmap bm = BitmapFactory.decodeFile(mData.getPicUrl());
        img.setImageBitmap(bm);
        tv.setText(mData.getContent());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_add:
                if(flag == 6)
                    flag=0;
                mData_5 = new Informations(picUrl[flag], "pic " + flag);
                mAdapter.add(mData_5);
                flag++;
                break;
        }
    }

}
