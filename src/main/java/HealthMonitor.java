
/**
 * Created by maheshakya on 2/15/16.
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class HealthMonitor {
    public static void main(String[] args) throws IOException {

        System.out.println("Starting.....");
        String currentDir = System.getProperty("user.dir");
        System.setProperty("javax.net.ssl.trustStore", currentDir + "/src/main/resources/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

//        final String host = "localhost";
//        final String port = "9763";
//
//        CloseableHttpClient httpclient = HttpClients.createDefault();
//        try {
//            String loginURI = "http://" + host + ":" + port + "/store/site/blocks/user/login/ajax/login.jag";
//            String loginUser = "admin";
//            String loginPassword = "admin";
//            login(httpclient, loginURI, loginUser, loginPassword);
//
//            String addApplicationUri = "http://" + host + ":" + port + "/store/site/blocks/application/application-add/ajax/application-add.jag";
//            String applicationName = "MyTestApplication";
//            String applicationTier = "Unlimited";
//            String applicationDescription = "";
//            String applicationCallbackUrl = "";
//            addApplication(httpclient, addApplicationUri, applicationName, applicationTier, applicationDescription,
//                    applicationCallbackUrl);
//
//            String generateApplicationKeyUri = "http://" + host + ":" + port + "/store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag";
//            String keyType = "PRODUCTION";
//            String authorizedDomains = "ALL";
//            String validityTime = "-1";
//            String accessToken = generateApplicationKey(httpclient, generateApplicationKeyUri, applicationName, keyType, applicationCallbackUrl, authorizedDomains, validityTime);
//
//            String addSubscritpionUri = "http://" + host + ":" + port + "/store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag";
//            String apiName = "CalculatorAPI";
//            String apiVersion = "1.0";
//            String apiProvider = "admin";
//            String apiTier = "Unlimited";
//            addSubscription(httpclient, addSubscritpionUri, apiName, apiVersion, apiProvider, apiTier, applicationName);
//
//            HashMap<String, String> headers = new HashMap<String, String>();
//            headers.put("Accept", "application/json");
//            headers.put("Authorization", "Bearer " + accessToken);
//
//            HashMap<String, String> params = new HashMap<String, String>();
//            params.put("x", "32");
//            params.put("y", "49");
//
//            String apiPort = "8243";
//            String apiUri = "https://" + host + ":" + apiPort + "/calc/1.0/add";
//            callApiGet(httpclient, apiUri, headers, params);
//
//            String removeApplicationUri = "http://" + host + ":"+ port + "/store/site/blocks/application/application-remove/ajax/application-remove.jag";
//            removeApplication(httpclient, removeApplicationUri, applicationName);
//
//        } finally {
//            httpclient.close();
//        }
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Runnable worker = new WorkerThread("test");
        executor.execute(worker);
        executor.shutdown();
        while (!executor.isTerminated()) {

        }

        System.out.println("done.....");
    }

    public static void login(CloseableHttpClient httpClient, String Uri, String userName, String password)
            throws IOException {
        HttpPost httpPost = new HttpPost(Uri);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("action", "login"));
        nvps.add(new BasicNameValuePair("username", userName));
        nvps.add(new BasicNameValuePair("password", password));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        CloseableHttpResponse response2 = httpClient.execute(httpPost);
        try {
            System.out.println(response2.getStatusLine());
            HttpEntity entity2 = response2.getEntity();
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(entity2);
        } finally {
            response2.close();
        }
    }

    public static void addApplication(CloseableHttpClient httpClient, String Uri, String applicationName, String tier,
            String description, String callbackUrl) throws IOException {
        HttpPost httpPost = new HttpPost(Uri);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("action", "addApplication"));
        nvps.add(new BasicNameValuePair("application", applicationName));
        nvps.add(new BasicNameValuePair("tier", tier));
        nvps.add(new BasicNameValuePair("description", description));
        nvps.add(new BasicNameValuePair("callbackUrl", callbackUrl));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        CloseableHttpResponse response2 = httpClient.execute(httpPost);
        try {
            System.out.println(response2.getStatusLine());
            HttpEntity entity2 = response2.getEntity();
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(entity2);
        } finally {
            response2.close();
        }
    }

    public static void removeApplication(CloseableHttpClient httpClient, String Uri, String applicationName) throws IOException {
        HttpPost httpPost = new HttpPost(Uri);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("action", "removeApplication"));
        nvps.add(new BasicNameValuePair("application", applicationName));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        CloseableHttpResponse response2 = httpClient.execute(httpPost);
        try {
            System.out.println(response2.getStatusLine());
            HttpEntity entity2 = response2.getEntity();
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(entity2);
        } finally {
            response2.close();
        }
    }

    public static String generateApplicationKey(CloseableHttpClient httpClient, String Uri, String applicationName, String keyType,
                                      String callbackUrl, String authorizedDomains, String validityTime) throws IOException {
        HttpPost httpPost = new HttpPost(Uri);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("action", "generateApplicationKey"));
        nvps.add(new BasicNameValuePair("application", applicationName));
        nvps.add(new BasicNameValuePair("keytype", keyType));
        nvps.add(new BasicNameValuePair("callbackUrl", callbackUrl));
        nvps.add(new BasicNameValuePair("AuthorizedDomains", authorizedDomains));
        nvps.add(new BasicNameValuePair("validityTime", validityTime));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        CloseableHttpResponse response2 = httpClient.execute(httpPost);
        try {
            System.out.println(response2.getStatusLine());
            HttpEntity entity1 = response2.getEntity();
            // do something useful with the response body
            InputStream is = entity1.getContent();
            String theString = IOUtils.toString(is, "UTF-8");
            System.out.println(theString);

            JSONObject jsonObject = new JSONObject(theString);
            String accessToken = jsonObject.getJSONObject("data").getJSONObject("key").getString("accessToken");

            System.out.println(accessToken);

            EntityUtils.consume(entity1);

            return accessToken;
        } finally {
            response2.close();
        }
    }

    public static void addSubscription(CloseableHttpClient httpClient, String Uri, String apiName, String apiVersion,
                                      String apiProvider, String tier, String applicationName) throws IOException {
        HttpPost httpPost = new HttpPost(Uri);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("action", "addAPISubscription"));
        nvps.add(new BasicNameValuePair("name", apiName));
        nvps.add(new BasicNameValuePair("version", apiVersion));
        nvps.add(new BasicNameValuePair("provider", apiProvider));
        nvps.add(new BasicNameValuePair("tier", tier));
        nvps.add(new BasicNameValuePair("applicationName", applicationName));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        CloseableHttpResponse response2 = httpClient.execute(httpPost);
        try {
            System.out.println(response2.getStatusLine());
            HttpEntity entity2 = response2.getEntity();
            InputStream is = entity2.getContent();
            String theString = IOUtils.toString(is, "UTF-8");
            System.out.println(theString);

            EntityUtils.consume(entity2);
        } finally {
            response2.close();
        }
    }

    public static void callApiGet(CloseableHttpClient httpClient, String Uri, HashMap<String, String> headers,
                                  HashMap<String, String> params) throws IOException {

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> param: params.entrySet()) {
            nvps.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }
        HttpGet httpGet = new HttpGet(Uri + "?" + URLEncodedUtils.format(nvps, "UTF-8"));
        for (Map.Entry<String, String> header: headers.entrySet()) {
            httpGet.addHeader(header.getKey(), header.getValue());
        }
        System.out.println(httpGet.getURI());
        CloseableHttpResponse response1 = httpClient.execute(httpGet);
        try {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            // do something useful with the response body
            InputStream is = entity1.getContent();
            String theString = IOUtils.toString(is, "UTF-8");
            System.out.println(theString);
            // and ensure it is fully consumed
            EntityUtils.consume(entity1);
        } finally {
            response1.close();
        }

    }

    public static class WorkerThread implements Runnable {
        private String message;
        public WorkerThread(String s){
            this.message=s;
        }
        public void run() {
            final String host = "localhost";
            final String port = "9763";

            CloseableHttpClient httpclient = HttpClients.createDefault();
            try {
                String loginURI = "http://" + host + ":" + port + "/store/site/blocks/user/login/ajax/login.jag";
                String loginUser = "admin";
                String loginPassword = "admin";
                login(httpclient, loginURI, loginUser, loginPassword);

                String addApplicationUri = "http://" + host + ":" + port + "/store/site/blocks/application/application-add/ajax/application-add.jag";
                String applicationName = "MyTestApplication";
                String applicationTier = "Unlimited";
                String applicationDescription = "";
                String applicationCallbackUrl = "";
                addApplication(httpclient, addApplicationUri, applicationName, applicationTier, applicationDescription,
                        applicationCallbackUrl);

                String generateApplicationKeyUri = "http://" + host + ":" + port + "/store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag";
                String keyType = "PRODUCTION";
                String authorizedDomains = "ALL";
                String validityTime = "-1";
                String accessToken = generateApplicationKey(httpclient, generateApplicationKeyUri, applicationName, keyType, applicationCallbackUrl, authorizedDomains, validityTime);

                String addSubscritpionUri = "http://" + host + ":" + port + "/store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag";
                String apiName = "CalculatorAPI";
                String apiVersion = "1.0";
                String apiProvider = "admin";
                String apiTier = "Unlimited";
                addSubscription(httpclient, addSubscritpionUri, apiName, apiVersion, apiProvider, apiTier, applicationName);

                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Accept", "application/json");
                headers.put("Authorization", "Bearer " + accessToken);

                HashMap<String, String> params = new HashMap<String, String>();
                params.put("x", "32");
                params.put("y", "49");

                String apiPort = "8243";
                String apiUri = "https://" + host + ":" + apiPort + "/calc/1.0/add";
                callApiGet(httpclient, apiUri, headers, params);

                String removeApplicationUri = "http://" + host + ":" + port + "/store/site/blocks/application/application-remove/ajax/application-remove.jag";
                removeApplication(httpclient, removeApplicationUri, applicationName);

                processmessage();
            } catch (IOException e) {
                System.err.println("Exception during monitoring: " + e.getMessage());
            } finally {
                try {
                    if (httpclient != null) {
                        httpclient.close();
                    }
                } catch( Exception e ) {
                    System.err.println("Exception during closing http client: " + e.getMessage());
                }
            }
        }
        private void processmessage() {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
