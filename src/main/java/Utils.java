import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.axis2.util.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonObject;

/**
 * Util methods for health monitor
 */
public class Utils {
    private static Logger logger = Logger.getLogger(Utils.class);

    public static void login(CloseableHttpClient httpClient, String Uri, String userName, String password)
            throws IOException {
        HttpPost httpPost = new HttpPost(Uri);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("action", "login"));
        nvps.add(new BasicNameValuePair("username", userName));
        nvps.add(new BasicNameValuePair("password", password));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        CloseableHttpResponse response = httpClient.execute(httpPost);

        logger.info("Login status: " + response.getStatusLine().getStatusCode());
        response.close();
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
        CloseableHttpResponse response = httpClient.execute(httpPost);

        logger.info("Add application status: " + response.getStatusLine().getStatusCode());
        response.close();
    }

    public static void removeApplication(CloseableHttpClient httpClient, String Uri, String applicationName)
            throws IOException {
        HttpPost httpPost = new HttpPost(Uri);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("action", "removeApplication"));
        nvps.add(new BasicNameValuePair("application", applicationName));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        CloseableHttpResponse response = httpClient.execute(httpPost);

        logger.info("Remove application status: " + response.getStatusLine().getStatusCode());
        response.close();
    }

    public static String generateApplicationKey(CloseableHttpClient httpClient, String Uri, String applicationName,
            String keyType, String callbackUrl, String authorizedDomains, String validityTime)
                    throws IOException, JSONException {
        HttpPost httpPost = new HttpPost(Uri);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("action", "generateApplicationKey"));
        nvps.add(new BasicNameValuePair("application", applicationName));
        nvps.add(new BasicNameValuePair("keytype", keyType));
        nvps.add(new BasicNameValuePair("callbackUrl", callbackUrl));
        nvps.add(new BasicNameValuePair("AuthorizedDomains", authorizedDomains));
        nvps.add(new BasicNameValuePair("validityTime", validityTime));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        CloseableHttpResponse response = httpClient.execute(httpPost);
        try {
            logger.info("Generate application key status" + response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            String theString = IOUtils.toString(is, "UTF-8");
            JSONObject jsonObject = new JSONObject(theString);
            String accessToken = jsonObject.getJSONObject("data").getJSONObject("key").getString("accessToken");
            EntityUtils.consume(entity);
            return accessToken;
        } finally {
            response.close();
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
        CloseableHttpResponse response = httpClient.execute(httpPost);

        logger.info("Add subscription status of API " + apiName + ": " + response.getStatusLine().getStatusCode());
        response.close();
    }

    public static String callApi(CloseableHttpClient httpClient, String apiMethod, String uri,
            HashMap<String, String> headers, HashMap<String, String> params) throws IOException {
        String sc;
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        if (params.size() > 0) {
            for (Map.Entry<String, String> param : params.entrySet()) {
                nvps.add(new BasicNameValuePair(param.getKey(), param.getValue()));
            }
        }

        CloseableHttpResponse response = null;

        if (apiMethod.equalsIgnoreCase("GET")) {
            HttpGet httpGet;
            if (nvps.size() > 0) {
                httpGet = new HttpGet(uri + "?" + URLEncodedUtils.format(nvps, "UTF-8"));
            } else {
                httpGet = new HttpGet(uri);
            }
            for (Map.Entry<String, String> header : headers.entrySet()) {
                httpGet.addHeader(header.getKey(), header.getValue());
            }
            logger.info("API uri: " + httpGet.getURI());
            response = httpClient.execute(httpGet);
        } else {
            // [Need to implement other methods if they are being used]
        }
        sc = String.valueOf(response.getStatusLine().getStatusCode());
        logger.info("API call " + uri + " status code : " + sc);
        response.close();
        return sc;
    }

    public static void publishToDas(CloseableHttpClient httpClient, HashMap<String, String> payload,
            String dasReceiverUrl, String dasUsername, String dasPassword) {
        JsonObject event = new JsonObject();
        JsonObject payLoadData = new JsonObject();

        for (String columnName : payload.keySet()) {
            payLoadData.addProperty(columnName, payload.get(columnName));
        }
        event.add("payloadData", payLoadData);

        String eventString = "{\"event\": " + event + "}";

        HttpPost postMethod = new HttpPost(dasReceiverUrl);
        StringEntity entity = null;
        try {
            entity = new StringEntity(eventString);
        } catch (UnsupportedEncodingException e) {
            logger.error("Error while creating event stream", e);
        }
        postMethod.setEntity(entity);
        postMethod.setHeader("Authorization", "Basic " + Base64.encode((dasUsername + ":" + dasPassword).getBytes()));
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(postMethod);
        } catch (IOException e) {
            logger.error("Error while publishing to DAS", e);
        }
        logger.info("Publishing to DAS status code: " + response.getStatusLine().getStatusCode());
    }
}
