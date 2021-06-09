package com.example.omenchallengehelper.helper;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.TintContextWrapper;

import com.example.omenchallengehelper.helper.Omen.Challenge;
import com.example.omenchallengehelper.helper.Omen.Login;
import com.example.omenchallengehelper.helper.Utils.JsonUtil;
import com.example.omenchallengehelper.ui.login.LoginActivity;

import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.util.*;

import lombok.Setter;

/**
 * Hello world!
 *
 */
public class App extends Thread
{
    private String email;
    private String pass;
    @Setter
    private TextView resultArea;
    @Setter
    ProgressBar loadingProgressBar;
    public static String ERROR;

    public App(String email, String pass) {
        this.email = email;
        this.pass = pass;
    }

    public void runSub() throws IOException, ParseException {

        String sessionToken;
        Login login = new Login();
        appendMsg("登录准备");
        login.webPrepare();

        login.setEmail(email);
        appendMsg("开始账号检查");
        String checkA = login.idpProvider();
        appendMsg(checkA);

        login.setPass(pass);
        appendMsg("开始登录");
        String localhostUrl = login.webLogin();

        appendMsg("开始模拟Omen登录操作");
        String tokenInfo = login.clientLogin(localhostUrl);
        Map akMap = JsonUtil.string2Obj(tokenInfo, Map.class);

        // 设备处理，按需
        // Device device = new Device((String) akMap.get("access_token"));
        // device.sendInfo();
        // device.sendGetEmpty();
        // device.getDetail();
        // device.register();

        appendMsg("开始获取挑战SESSION");
        sessionToken = login.genSession((String) akMap.get("access_token"));

        // 生成数据
        String applicationId = "6589915c-6aa7-4f1b-9ef5-32fa2220c844";

        Challenge challenge = new Challenge(applicationId, sessionToken);

        appendMsg("获取可参与挑战列表");
        List<String[]> allList = challenge.getAllList();
        appendMsg("可加入的挑战数：" + allList.size());

        for (int i = 0; i < allList.size(); i++) {
            String[] s = allList.get(i);
            challenge.join(s[0], s[1]);
        }

        List<Map<String, Object>> eventList = challenge.currentList();

        if(eventList == null){
            appendMsg("失败！");
            return;
        }
        appendMsg("待完成任务数：" + eventList.size());
        for (int i = 0; i < eventList.size(); i++) {
            Map<String, Object> en = eventList.get(i);
            appendMsg("当前执行任务：" + en.get("eventName") + " - " + en.get("progress") + "%");
            int time = 45;
            time += Math.random() * 20;
            Map<String, Object> result = challenge.doIt((String) en.get("eventName"), time);
            if(result == null){
                appendMsg("失败！");
                return;
            }
            int resultProgress = (int)result.get("progress");
            if(resultProgress == (int)en.get("progress")){
                appendMsg("进度没有变化，你设置的时间不合理！(时间要小于等于当前时间减去上一次提交任务的时间)");
            }else{
                appendMsg("事件：" + en.get("eventName") + " -- 已完成 " + resultProgress + "%");
            }
        }

    }

    public void run(){
        try{
            runSub();
        }catch (Exception e){
            String msg;
            if(e instanceof RuntimeException) {
                StackTraceElement traceElement = e.getStackTrace()[0];
                msg = e.getMessage() + "\n异常来源：" +traceElement.getClassName() + " - line:" + traceElement.getLineNumber();
            }else{
                msg = e.getMessage();
            }
            appendMsg(msg);
        }finally {
            stopLoading();
        }
    }

    public void appendMsg(String msg){
        Context context = resultArea.getContext();

        Activity activity = null;
        if(context instanceof LoginActivity) {
            activity = (Activity) context;
        }else if(context instanceof TintContextWrapper){
            activity = (Activity)((TintContextWrapper) context).getBaseContext();
        }
        if(activity != null)
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    resultArea.append("\n" + msg);
                }
            });
    }

    public void stopLoading(){
        Context context = resultArea.getContext();
        // 4.4 TintContextWrapper
        // 5.1 LoginActivity

        Activity activity = null;
        if(context instanceof LoginActivity) {
            activity = (Activity) context;
        }else if(context instanceof TintContextWrapper){
            activity = (Activity)((TintContextWrapper) context).getBaseContext();
        }

        if(activity != null)
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadingProgressBar.setVisibility(View.INVISIBLE);
                }
            });
    }
}
