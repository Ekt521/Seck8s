package com.seck8s.client;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String DEFAULT_BASE_URL = "http://10.0.2.2:8080";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String baseUrl;
    private String token;
    private LinearLayout content;
    private EditText username;
    private EditText password;
    private TextView sessionLabel;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        SharedPreferences prefs = getSharedPreferences("seck8s", MODE_PRIVATE);
        baseUrl = prefs.getString("baseUrl", DEFAULT_BASE_URL);
        token = prefs.getString("token", "");
        buildUi();
        loadHome();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(247,248,245));
        LinearLayout header = new LinearLayout(this); header.setPadding(24, 22, 18, 16); header.setGravity(View.TEXT_ALIGNMENT_CENTER); header.setBackgroundColor(Color.rgb(12,52,56));
        TextView title = label("SECK8S  票务秒杀", 22, Color.WHITE); header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button account = button(token.isEmpty() ? "登录" : "退出"); account.setOnClickListener(v -> { if (token.isEmpty()) showLogin(); else logout(); }); header.addView(account); root.addView(header);
        ScrollView scroll = new ScrollView(this); content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(22, 22, 22, 30); scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void loadHome() {
        content.removeAllViews(); content.addView(label("正在加载票务与秒杀活动...", 15, Color.DKGRAY));
        get("/api/tickets", tickets -> { content.removeAllViews(); content.addView(label("正在售票", 25, Color.rgb(16,42,51)));
            for (int i = 0; i < tickets.length(); i++) { JSONObject t = tickets.optJSONObject(i); if (t != null) addTicket(t); }
            content.addView(label("秒杀活动", 25, Color.rgb(16,42,51)));
            get("/api/activities/ongoing", activities -> { for (int i = 0; i < activities.length(); i++) { JSONObject a = activities.optJSONObject(i); if (a != null) addActivity(a); } });
            content.addView(label("我的订单", 25, Color.rgb(16,42,51)));
            if (token.isEmpty()) content.addView(label("登录后查看订单", 14, Color.GRAY)); else get("/api/orders", orders -> { for (int i=0;i<orders.length();i++) addOrder(orders.optJSONObject(i)); });
        });
    }

    private void addTicket(JSONObject t) {
        LinearLayout card = card(); String name=t.optString("name","票务"), id=t.optString("ticketId","");
        card.addView(label(name + "\n" + t.optString("description","") + "\n¥" + t.optString("price","--"), 16, Color.rgb(16,42,51)));
        Button detail = button("查看票档"); detail.setOnClickListener(v -> get("/api/tickets/"+id+"/skus", skus -> toast(skuSummary(skus)))); card.addView(detail); content.addView(card);
    }

    private void addActivity(JSONObject a) {
        LinearLayout card=card(); String id=a.optString("activityId",""); card.addView(label(a.optString("name","秒杀活动")+"\n"+a.optString("startTime",""),17,Color.rgb(16,42,51)));
        get("/api/activities/"+id+"/skus", skus -> { for(int i=0;i<skus.length();i++){ JSONObject s=skus.optJSONObject(i); if(s==null)continue; Button b=button(s.optString("skuId")+"  ¥"+s.optString("seckillPrice")+"  立即秒杀"); b.setOnClickListener(v -> seckill(id,s.optString("skuId"))); card.addView(b); } }); content.addView(card);
    }

    private void addOrder(JSONObject o) { if(o==null)return; LinearLayout card=card(); String id=o.optString("orderId"); card.addView(label(id+"\n状态："+o.optString("status"),15,Color.DKGRAY)); if("UNPAID".equals(o.optString("status"))){Button b=button("支付");b.setOnClickListener(v->post("/api/payments/"+id,"",x->toast("已发起支付，请稍后刷新")));card.addView(b);} content.addView(card); }

    private void seckill(String activityId,String skuId){if(token.isEmpty()){showLogin();return;} post("/api/seckill","{\"activityId\":\""+activityId+"\",\"skuId\":\""+skuId+"\",\"quantity\":1}",x->{toast("下单请求已提交");loadHome();});}
    private void showLogin(){LinearLayout box=card();username=new EditText(this);username.setHint("用户名");password=new EditText(this);password.setHint("密码");password.setInputType(129);box.addView(username);box.addView(password);Button b=button("登录");b.setOnClickListener(v->post("/api/users/login","{\"username\":\""+username.getText()+"\",\"password\":\""+password.getText()+"\"}",x->{JSONObject result=x.optJSONObject(0);token=result==null?"":result.optString("token");getSharedPreferences("seck8s",0).edit().putString("token",token).apply();toast("登录成功");buildUi();loadHome();}));box.addView(b);content.removeAllViews();content.addView(box);}
    private void logout(){token="";getSharedPreferences("seck8s",0).edit().remove("token").apply();buildUi();loadHome();}

    private void get(String path, JsonCallback cb){request("GET",path,"",cb);}
    private void post(String path,String body,JsonCallback cb){request("POST",path,body,cb);}
    private void request(String method,String path,String body,JsonCallback cb){executor.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(baseUrl+path).openConnection();c.setRequestMethod(method);c.setRequestProperty("Content-Type","application/json");if(!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);if(!body.isEmpty()){c.setDoOutput(true);try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}}BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();String line;while((line=r.readLine())!=null)s.append(line);JSONObject root=new JSONObject(s.toString());if(root.optInt("code")!=200)throw new Exception(root.optString("message","请求失败"));Object data=root.opt("data");runOnUiThread(()->{try{cb.done(data instanceof JSONArray?(JSONArray)data:new JSONArray().put(data instanceof JSONObject?data:new JSONObject()));}catch(Exception e){toast(e.getMessage());}});}catch(Exception e){runOnUiThread(()->toast(e.getMessage()));}});}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(16,16,16,16);l.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,14);l.setLayoutParams(p);return l;}
    private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,0,0,12);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);return b;}
    private String skuSummary(JSONArray a){StringBuilder s=new StringBuilder();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)s.append(o.optString("name")).append(" ¥").append(o.optString("price")).append("\n");}return s.length()==0?"暂无票档":s.toString();}
    private void toast(String s){Toast.makeText(this,s==null?"请求失败":s,Toast.LENGTH_SHORT).show();}
    private interface JsonCallback{void done(JSONArray data);}
}
