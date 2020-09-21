package com.refordom.roletask;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class PostGetUtil {
    private static final String TAG = "PostGetUtil";
    public static String token;
    public static String baseUrl;
    public static String sendJson(String url,String params){
        return sendPost(url,params,true);
    }
    public static String sendPost(String url,String params){
        return sendPost(url,params,false);
    }
    public static String sendPost(String url,String params,boolean isJson){
        StringBuffer sb = new StringBuffer();
        BufferedReader in = null;
        DataOutputStream out = null;
        String fullUrl = baseUrl + url;
        try {
            Log.i(TAG,"url:"+ fullUrl);
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new MyTrustManager()}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new MyHostnameVerifier());

            HttpsURLConnection conn = (HttpsURLConnection)new URL(fullUrl).openConnection();
            conn.setConnectTimeout(5*1000);
            conn.setRequestProperty("Authorization",token);
            conn.setRequestProperty("accept","application/json, text/plain, */*");
            conn.setRequestProperty("accept-encoding","gzip, deflate, br");
            if(isJson)conn.setRequestProperty("content-type","application/json");
           // conn.setRequestProperty("content-length",params.getBytes() + "");
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.connect();
            out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(params);
            out.flush();//输出缓冲区
            out.close();

            int rsCode = conn.getResponseCode();
            if(rsCode == 200) {
                Log.i(TAG,"content length:" +conn.getContentLength());
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = in.readLine()) != null)
                    sb.append(line);
                return sb.toString();
            } else {
                Log.i(TAG,"rsCode:" + rsCode);
                return null;
            }
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        } finally {
            try{
                if(in != null){
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static String sendGET(String url,String params){
        StringBuffer sb = new StringBuffer();
        BufferedReader in = null;
        PrintWriter out = null;
        String fullUrl = baseUrl + url + (params == null ? "" :"?" + params);
        try {
            Log.i(TAG,"url:"+ fullUrl);
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new MyTrustManager()}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new MyHostnameVerifier());

            HttpsURLConnection conn = (HttpsURLConnection)new URL(fullUrl).openConnection();
            conn.setConnectTimeout(5*1000);
            conn.setRequestMethod("GET");
            Log.i(TAG,"httpToken:" + token);
            conn.setRequestProperty("Authorization",token);
            int rsCode = conn.getResponseCode();

            if(rsCode == 200) {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = in.readLine()) != null)
                    sb.append(line);

                Log.i(TAG,"sendPost result:" + sb.toString());
                return sb.toString();
            } else {
                Log.i(TAG,"rsCode:" + rsCode);
                return null;
            }
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        } finally {
            try{
                if(in != null){
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static String createJson(HashMap<String,Object> map){
       return null;
    }
    private static class MyHostnameVerifier implements HostnameVerifier {

        @Override

        public boolean verify(String hostname, SSLSession session) {
            // TODO Auto-generated method stub

            return true;
        }

    }

    private static class MyTrustManager implements X509TrustManager {

        @Override

        public void checkClientTrusted(X509Certificate[] chain, String authType)

                throws CertificateException {
            // TODO Auto-generated method stub


        }
        @Override

        public void checkServerTrusted(X509Certificate[] chain, String authType)

                throws CertificateException {
            // TODO Auto-generated method stub

        }
        @Override

        public X509Certificate[] getAcceptedIssuers() {

            // TODO Auto-generated method stub

            return null;
        }
    }
}
