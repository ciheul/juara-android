package com.ciheul.bigmaps.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class Utility {

    /**
     * Without version
     * 
     * @param baseRequestURI
     * @param version
     * @param resourceURI
     * @param hashParams
     * @return
     */
    public static String constructURI(String baseRequestURI, String resourceURI, HashMap<String, Object> hashParams) {
        String params = "";
        for (Map.Entry<String, Object> entry : hashParams.entrySet()) {
            params += entry.getKey() + "=" + entry.getValue().toString() + "&";
        }
        String result = baseRequestURI + resourceURI + "?" + params;
        result = removeLastCharacter(result);
        return result;
    }

    /**
     * With version
     * 
     * @param baseRequestURI
     * @param version
     * @param resourceURI
     * @param hashParams
     * @return
     */
    public static String constructURI(String baseRequestURI, String version, String resourceURI,
            HashMap<String, Object> hashParams) {
        String params = "";
        for (Map.Entry<String, Object> entry : hashParams.entrySet()) {
            params += entry.getKey() + "=" + entry.getValue().toString() + "&";
        }
        String result = baseRequestURI + version + resourceURI + "?" + params;
        result = removeLastCharacter(result);
        return result;
    }

    public static String removeLastCharacter(String str) {
        if (str.length() > 0) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    public String sendHttpGetRequest(String url) {
        String output = "empty";
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setURI(new URI(url));
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            output = EntityUtils.toString(httpEntity);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return output;
    }

    public String sendHttpsGetRequest(String requestURI) {
        try {
            URL url = new URL(requestURI);
            
            System.setProperty("http.proxyHost", "167.205.22.103");
            System.setProperty("http.proxyPort", "8080");
            
            Authenticator authenticator = new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication("matwinenk132", "xxx".toCharArray()));
                }
            };
            Authenticator.setDefault(authenticator);
            
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.connect();
            
            readStream(urlConnection.getInputStream());
            
            urlConnection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return requestURI;
    }

    private void readStream(InputStream in) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                // System.out.println(line);
                Log.d("BigMaps", "readStream: " + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
