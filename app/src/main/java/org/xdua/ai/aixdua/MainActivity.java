package org.xdua.ai.aixdua;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.kymjs.rxvolley.RxVolley;
import com.kymjs.rxvolley.client.HttpCallback;
import com.kymjs.rxvolley.client.HttpParams;
import com.kymjs.rxvolley.toolbox.Loger;
import com.lovearthstudio.duasdk.Dua;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button pushBtn;
    Button switchBtn;
    TextView textView;
    String dua_id;
    ListView listView;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            textView.setText(msg.obj.toString());
        }
    };
    ArrayList<Map<String, Object>> mData1 = new ArrayList<>();
    ArrayList<Map<String, Object>> mData2 = new ArrayList<>();

    boolean isFirstData = true;
    SimpleAdapter simpleAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Dua.init(getApplication());
        pushBtn = (Button) findViewById(R.id.pushButton);
        switchBtn = (Button) findViewById(R.id.switchButton);
        pushBtn.setOnClickListener(this);
        switchBtn.setOnClickListener(this);

        textView = (TextView) findViewById((R.id.textView));
        listView = (ListView) findViewById(R.id.listView);
        simpleAdapter = new SimpleAdapter(this, mData1, android.R.layout.simple_list_item_activated_2,
                new String[]{"title", "text"}, new int[]{android.R.id.text1, android.R.id.text2});
        listView.setAdapter(simpleAdapter);
        dua_id = Long.toString(Dua.getInstance().getCurrentDuaId());
        textView.setText(dua_id);

    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.pushButton:
                duaInfoHandler();
                break;
            case R.id.switchButton:
                changeReport();
                break;
        }
    }

    public void duaInfoHandler() {
        Message message = new Message();
        message.obj = "fetching data...";
        handler.sendMessage(message);
        HttpCallback callback = new HttpCallback() {
            @Override
            public void onSuccess(String t) {
                Loger.debug("请求到的数据:" + t);
                receiveDataHandle(t);
            }

            @Override
            public void onFailure(int errorNo, String strMsg) {
                super.onFailure(errorNo, strMsg);
                Message message = new Message();
                message.obj = "network error";
                handler.sendMessage(message);
                Loger.debug("请求到的数据:" + errorNo);
            }
        };

        HttpParams params = new HttpParams();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("dua_id", dua_id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        params.putJsonParams(jsonObject.toString());
        Loger.debug("待发送的参数： " + params.getJsonParams());

        new RxVolley.Builder()
                .url("http://ai.xdua.org/sexprob")
                .httpMethod(RxVolley.Method.POST) //default GET or POST/PUT/DELETE/HEAD/OPTIONS/TRACE/PATCH
//                .useServerControl(false)
                .shouldCache(false)
                .contentType(RxVolley.ContentType.JSON)//default FORM or JSON
                .params(params)
                .callback(callback)
                .encoding("UTF-8") //default
                .doTask();
    }

    public void receiveDataHandle(String input) {
        try {

            mData1.clear();
            mData2.clear();

            JSONObject jsonObject = new JSONObject(input);
            Double prob = jsonObject.getJSONObject("result").getDouble("prob_result");
            JSONObject catoList = jsonObject.getJSONObject("result").getJSONObject("list");
            Iterator catoIter = catoList.keys();

            DecimalFormat df = new DecimalFormat("0.00");

            while (catoIter.hasNext()) {
                Map<String, Object> item = new HashMap<>();
                String appCato = (String) catoIter.next();
                item.put("title", appCato);
                JSONObject object = catoList.getJSONObject(appCato);
                Integer catoCount = object.getInt("cato_count");
                String catoProb = df.format(object.getDouble("cato_prob"));
                String content = "数量：" + catoCount + "   " + "Male概率： " + catoProb;
                item.put("text", content);
                mData1.add(item);
            }

            //{"com.sec.esdk.elm":{"catos":"","system":1,"name":"ELM Agent"}

            JSONObject brief = jsonObject.getJSONObject("result").getJSONObject("brief");
            Iterator briefIter = brief.keys();
            while (briefIter.hasNext()) {
                Map<String, Object> item = new HashMap<>();
                String pacName = (String) briefIter.next();
                JSONObject object = brief.getJSONObject(pacName);

                String catoArrayString = object.getString("catos");
                StringBuffer catoString = new StringBuffer();
                if (!catoArrayString.equals("")) {
                    JSONArray catoArray = new JSONArray(catoArrayString);
                    for (int i = 0; i < catoArray.length(); i++) {
                        String cato = catoArray.get(i).toString();
                        catoString.append(cato).append(" ");
                    }
                } else {
                    catoString.append("未标记");
                }

                String name = object.getString("name");
                Integer systemFlag = object.getInt("system");
                String system = "System";
                if (systemFlag == 0) {
                    system = "User";
                }
                String title = name + " ( " + system + " )";
                item.put("title", title);
                item.put("text", catoString);
                mData2.add(item);
            }


            //发送概率到主界面
            Message message = new Message();
            message.obj = prob;
            handler.sendMessage(message);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void changeReport() {


        if (isFirstData) {
            simpleAdapter = new SimpleAdapter(this, mData2, android.R.layout.simple_list_item_activated_2,
                    new String[]{"title", "text"}, new int[]{android.R.id.text1, android.R.id.text2});
            isFirstData = false;
        } else {
            simpleAdapter = new SimpleAdapter(this, mData1, android.R.layout.simple_list_item_activated_2,
                    new String[]{"title", "text"}, new int[]{android.R.id.text1, android.R.id.text2});
            isFirstData = true;
        }

        listView.setAdapter(simpleAdapter);
        listView.deferNotifyDataSetChanged();

    }
}
