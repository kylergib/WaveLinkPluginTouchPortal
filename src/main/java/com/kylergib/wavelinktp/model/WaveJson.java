package com.kylergib.wavelinktp.model;

import org.json.JSONObject;

/**
 * JSON skeleton to send to wave link
 */

public class WaveJson {
    private String jsonrpc = "2.0";
    private String method;
    private int id;
    private JSONObject jsonObject = new JSONObject();
    private JSONObject params;

    public WaveJson(String method, int id) {
        this.method = method;
        this.id = id;
        setJsonObject();
    }
    public void setId(int id) {
        this.id = id;
        setJsonObject();
    }
    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
        setJsonObject();
    }
    public void setMethod(String method){
        this.method = method;
        setJsonObject();
    }
    public void setParams(JSONObject params) {
        this.params = params;
        setJsonObject();
    }

    public JSONObject getJsonObject() {
        return this.jsonObject;
    }

    public String getJsonString() {
        return this.jsonObject.toString();
    }

    public JSONObject getParams() {
        return this.params;
    }

    private void setJsonObject() {
        this.jsonObject.clear();
        this.jsonObject.put("id",this.id);
        this.jsonObject.put("jsonrpc",this.jsonrpc);
        this.jsonObject.put("method", this.method);

        if (params != null ) {
            this.jsonObject.put("params",params);
        }
    }

//    String getAppInfo = "{\"jsonrpc\":\"2.0\",\"method\":\"getApplicationInfo\",\"id\":1}";
}
