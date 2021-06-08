package com.example.omenchallengehelper.helper.Omen;

import com.example.omenchallengehelper.helper.Omen.Body.AllChallengeListBody;
import com.example.omenchallengehelper.helper.Omen.Body.ChallengePostBody;
import com.example.omenchallengehelper.helper.Omen.Body.CurrentChallengeListBody;
import com.example.omenchallengehelper.helper.Omen.Body.JoinChallengeBody;
import com.example.omenchallengehelper.helper.Utils.HTTP.HttpUtil;
import com.example.omenchallengehelper.helper.Utils.HTTP.HttpUtilEntity;
import com.example.omenchallengehelper.helper.Utils.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author jiyec
 * @Date 2021/5/27 12:21
 * @Version 1.0
 **/
public class Challenge {

    public static final String API = "https://rpc-prod.versussystems.com/rpc";
    public static final Map<String, String> HEADERS = new HashMap<String, String>() {{
        put("Content-Type", "application/json; charset=utf-8");
        put("User-Agent", "");
    }};
    private final String applicationId;
    private final String sessionToken;

    public Challenge(String applicationId, String sessionToken) {
        this.applicationId = applicationId;
        this.sessionToken = sessionToken;
    }

    /**
     * 获取所有挑战，对应[Available Rewards]
     *
     * @return String[] {challengeStructureId, campaignId, relevantEvent}
     */
    public List<String[]> getAllList() {
        Map<String, Object> body = new AllChallengeListBody(applicationId, sessionToken).genBody();
        List<String[]> allList = new ArrayList<>();
        HttpUtilEntity ret = request(body);
        if(ret == null)return allList;

        Map listMap = JsonUtil.string2Obj(ret.getBody(), Map.class);
        List<Map<String, Object>> collection = (List<Map<String, Object>>) (((Map) listMap.get("result")).get("collection"));

        for (Map<String, Object> item : collection) {
            String challengeStructureId = (String) (item.get("challengeStructureId"));
            List<String> relevantEvents = (List<String>) item.get("relevantEvents");
            Map<String, Object> prize = (Map<String, Object>) (item.get("prize"));
            String category = (String) prize.get("category");
            String campaignId = (String) prize.get("campaignId");
            if ("sweepstake".equals(category)) {
                allList.add(new String[]{
                        challengeStructureId,
                        campaignId,
                        relevantEvents.get(0)
                });
            }
        }

        return allList;
    }

    // 参加挑战
    public boolean join(String challengeStructureId, String campaignId) {
        Map<String, Object> body = new JoinChallengeBody(applicationId, sessionToken).genBody(campaignId, challengeStructureId);
        HttpUtilEntity ret = request(body);
        if(ret == null)return false;
        return true;
    }

    // 获取进行中的任务
    public List<Map<String, Object>> currentList(){
        Map<String, Object> body = new CurrentChallengeListBody(applicationId, sessionToken).genBody();
        List<Map<String, Object>> currentList = new ArrayList<>();
        HttpUtilEntity ret = request(body);
        if(ret == null) return null;

        Map listMap = JsonUtil.string2Obj(ret.getBody(), Map.class);
        List<Map<String, Object>> collection = (List<Map<String, Object>>) (((Map) listMap.get("result")).get("collection"));

        for (Map<String, Object> item : collection) {
            List<String> relevantEvents = (List<String>) item.get("relevantEvents");
            Map<String, Object> prize = (Map<String, Object>) (item.get("prize"));
            int progressPercentage = (int)item.get("progressPercentage");
            String category = (String) prize.get("category");
            if ("sweepstake".equals(category)) {
                currentList.add(new HashMap<String, Object>(){{
                    put("eventName", relevantEvents.get(0));
                    put("progress", progressPercentage);
                }});
            }
        }

        return currentList;
    }

    // 执行挑战
    public Map<String, Object> doIt(String eventName, int playTime) {
        Map<String, Object> body = new ChallengePostBody(applicationId, sessionToken).genBody(eventName, playTime);

        // 发送请求
        HttpUtilEntity ret = request(body);
        if(ret == null)return null;

        // 解析结果
        Map<String, Object> retMap = JsonUtil.string2Obj(ret.getBody(), Map.class);
        List<Map<String, Object>> result = (List<Map<String, Object>>) retMap.get("result");

        Map<String, Object> item = result.get(0);
        List<String> relevantEvents = (List<String>) item.get("relevantEvents");
        int percentage = (int) item.get("progressPercentage");

        return new HashMap<String, Object>(){{
            put("progress", percentage);
        }};
    }

    private HttpUtilEntity request(Map<String, Object> body) {
        HttpUtilEntity ret = null;
        try {
            ret = HttpUtil.doStreamPost(
                    API,
                    JsonUtil.obj2String(body).getBytes(StandardCharsets.UTF_8),
                    HEADERS
            );
            int code = ret.getStatusCode();
            if(code == 400){
                Map result = JsonUtil.string2Obj(ret.getBody(), Map.class);
                Map<String, Object> error = (Map<String, Object>) result.get("error");
                throw new RuntimeException("异常 - 响应码：" + ret.getStatusCode() + ", " + error.get("message"));
            }else if (code != 200) {
                throw new RuntimeException("响应异常-响应码：" + ret.getStatusCode() + ", " + ret);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
