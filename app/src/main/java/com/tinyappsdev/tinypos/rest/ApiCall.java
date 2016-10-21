package com.tinyappsdev.tinypos.rest;

import android.os.Handler;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ApiCall {
    private final static String TAG = ApiCall.class.getSimpleName();
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static ApiCall sInstance;
    public static ApiCall getInstance() {
        synchronized (ApiCall.class) {
            if(sInstance == null)
                sInstance = new ApiCall();
            return sInstance;
        }
    }

    private OkHttpClient mOkHttpClient = new OkHttpClient();

    public JSONObject callApiSync(String path, Object[][] params) {
        Response response = null;
        try {
            //HttpUrl.Builder builder = HttpUrl.parse(String.format("http://10.0.2.2:8888/")).newBuilder();
            HttpUrl.Builder builder = HttpUrl.parse(String.format("http://192.168.1.109:8888/")).newBuilder();
            builder.addEncodedPathSegments(path);

            if(params != null) {
                for (int i = 0; i < params.length; i++)
                    builder.addQueryParameter(String.valueOf(params[i][0]), String.valueOf(params[i][1]));
            }

            Request request = new Request.Builder().url(builder.build()).build();
            response = mOkHttpClient.newCall(request).execute();

            String res = response.body().string();
            Log.i("PKT", String.format("> %s", request.url().toString()));
            return new JSONObject(res);

        } catch (JSONException e) {
            Log.d(TAG, e.toString());

        } catch (IOException e) {
            Log.d(TAG, e.toString());

        } finally {
            if(response != null) response.close();
        }

        return null;
    }

    public void callApiAsync(String path, final String json, final ApiCallbacks callbacks) {
        HttpUrl.Builder builder = HttpUrl.parse(String.format("http://192.168.1.109:8888/")).newBuilder();
        builder.addEncodedPathSegments(path);
        final Handler mHandler = new Handler();

        Request request = new Request.Builder()
                .url(builder.build())
                .post(RequestBody.create(JSON, json))
                .build();

        mOkHttpClient.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                final String error = e.getMessage();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callbacks.onApiResponse(error, null);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String error = null;
                String json = null;
                try {
                    if (!response.isSuccessful()) {
                        error = response.toString();
                    } else {
                        json = response.body().string();
                    }

                    final String error_ = error;
                    final String json_ = json;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callbacks.onApiResponse(error_, json_);
                        }
                    });
                } finally {
                    response.close();
                }
            }
        });
    }

    public interface ApiCallbacks {
        public void onApiResponse(String error, String json);
    }
}
