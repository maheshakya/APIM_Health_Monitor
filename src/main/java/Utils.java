import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by maheshakya on 2/18/16.
 */
public class Utils {
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
                                                String callbackUrl, String authorizedDomains, String validityTime) throws IOException, JSONException {
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

    public static String callApiGet(CloseableHttpClient httpClient, String Uri, HashMap<String, String> headers,
                                    HashMap<String, String> params) throws IOException {
        String sc = null;
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
            sc = String.valueOf(response1.getStatusLine().getStatusCode());
            System.out.println(sc);
            HttpEntity entity1 = response1.getEntity();
            // do something useful with the response body
            InputStream is = entity1.getContent();
            String output = IOUtils.toString(is, "UTF-8");
            System.out.println(output);
            // and ensure it is fully consumed
            EntityUtils.consume(entity1);
        } finally {
            response1.close();
        }
        return sc;
    }
}
