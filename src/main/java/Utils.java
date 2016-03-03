import java.io.*;
import java.util.*;

import org.apache.axis2.util.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.yaml.snakeyaml.Yaml;

import com.google.gson.JsonObject;

/**
 * Util methods for health monitor
 */
public class Utils {
    private static Logger logger = Logger.getLogger(Utils.class);

    // New set

    public static Map<String, Object> readConfigs(String path) {
        // Init Yaml
        Yaml yaml = new Yaml();
        Map<String, Object> configs = null;

        try {
            InputStream ios = new FileInputStream(path);
            // Parse the YAML file and return the output as a series of Maps and Lists
            configs = (Map<String, Object>) yaml.load(ios);
            logger.debug("Read configs...");
        } catch (FileNotFoundException e) {
            logger.error("Failed to read configs from file.", e);
            System.exit(-1);
        }

        return configs;
    }

    public static HashMap<String, String> registerClient(CloseableHttpClient httpClient, String userName,
            String password, String dcrEndpointURL, String applicationRequestBody) {

        HashMap<String, String> clientInfo = new HashMap<String, String>();

        HttpPost httpPost = null;
        String basicAuthHeader = userName + ":" + password;
        try {
            httpPost = new HttpPost(dcrEndpointURL);
            httpPost.setHeader("Authorization", "Basic " + Base64.encode(basicAuthHeader.getBytes("UTF-8")));
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(applicationRequestBody));
        } catch (UnsupportedEncodingException e) {
            logger.error("Error occurred while encoding API Manager credentials.", e);
            System.exit(-1);
        }

        CloseableHttpResponse response;
        String responseString = null;
        try {
            response = httpClient.execute(httpPost);
            InputStream is = response.getEntity().getContent();
            responseString = IOUtils.toString(is, "UTF-8");
            response.close();
        } catch (IOException e) {
            logger.error("Error occurred while registering DCR client.", e);
            System.exit(-1);
        }

        JSONObject jsonObject = new JSONObject(responseString);
        clientInfo.put("clientId", jsonObject.getString("clientId"));
        clientInfo.put("clientSecret", jsonObject.getString("clientSecret"));

        return clientInfo;
    }

    public static HashMap<String, String> getAccessToken(CloseableHttpClient httpClient, String clientId,
            String clientSecret, String tokenEndpointURL, String requestBody) {

        HashMap<String, String> accessTokenInfo = new HashMap<String, String>();

        HttpPost httpPost = null;
        String basicAuthHeader = clientId + ":" + clientSecret;
        try {
            httpPost = new HttpPost(tokenEndpointURL);
            httpPost.setHeader("Authorization", "Basic " + Base64.encode(basicAuthHeader.getBytes("UTF-8")));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setEntity(new StringEntity(requestBody));
        } catch (UnsupportedEncodingException e) {
            logger.error("Error occurred while encoding client secrets.", e);
            System.exit(-1);
        }

        CloseableHttpResponse response;
        String responseString = null;
        try {
            response = httpClient.execute(httpPost);
            InputStream is = response.getEntity().getContent();
            responseString = IOUtils.toString(is, "UTF-8");
            response.close();
        } catch (IOException e) {
            logger.error("Error occurred while obtaining client access token.", e);
            System.exit(-1);
        }

        JSONObject jsonObject = new JSONObject(responseString);
        accessTokenInfo.put("refreshToken", jsonObject.getString("refresh_token"));
        accessTokenInfo.put("accessToken", jsonObject.getString("access_token"));

        return accessTokenInfo;
    }

    public static String getApplicationId(CloseableHttpClient httpClient, String applicationUrl, String accessToken,
            String applicationName) {
        String applicationId = null;

        String getApplicationUrl = applicationUrl + "?query=" + applicationName;
        HttpGet httpGet = new HttpGet(getApplicationUrl);
        httpGet.setHeader("Authorization", "Bearer " + accessToken);

        CloseableHttpResponse response;
        String responseString = null;
        try {
            response = httpClient.execute(httpGet);
            InputStream is = response.getEntity().getContent();
            responseString = IOUtils.toString(is, "UTF-8");
            response.close();
        } catch (IOException e) {
            logger.error("Error occurred while retrieving application.", e);
            System.exit(-1);
        }
        JSONObject fullList = new JSONObject(responseString);
        JSONArray list = (JSONArray) fullList.get("list");

        if (list.length() == 0) {
            return applicationId;
        } else {
            JSONObject application = (JSONObject) list.get(0);
            applicationId = application.getString("applicationId");
            return applicationId;
        }

    }

    public static String createApplication(CloseableHttpClient httpClient, String applicationUrl, String accessToken,
            String applicationRequestBody) {
        String result;

        HttpPost httpPost = new HttpPost(applicationUrl);
        httpPost.setHeader("Authorization", "Bearer " + accessToken);
        httpPost.setHeader("Content-Type", "application/json");
        try {
            httpPost.setEntity(new StringEntity(applicationRequestBody));
        } catch (UnsupportedEncodingException e) {
            logger.error("Error occurred while encoding application credentials.", e);
            System.exit(-1);
        }

        CloseableHttpResponse response;
        String responseString = null;
        try {
            response = httpClient.execute(httpPost);
            InputStream is = response.getEntity().getContent();
            responseString = IOUtils.toString(is, "UTF-8");
            response.close();
        } catch (IOException e) {
            logger.error("Error occurred while creating application.", e);
            System.exit(-1);
        }
        JSONObject jsonObject = new JSONObject(responseString);
        result = jsonObject.getString("applicationId");

        return result;
    }

    public static String getApplicationKey(CloseableHttpClient httpClient, String applicationUrl, String accessToken,
            String applicationId) {
        String applicationKey = null;

        String getApplicationUrl = applicationUrl + "/" + applicationId;
        HttpGet httpGet = new HttpGet(getApplicationUrl);
        httpGet.setHeader("Authorization", "Bearer " + accessToken);

        CloseableHttpResponse response;
        String responseString = null;
        try {
            response = httpClient.execute(httpGet);
            InputStream is = response.getEntity().getContent();
            responseString = IOUtils.toString(is, "UTF-8");
            response.close();
        } catch (IOException e) {
            logger.error("Error occurred while retrieving application details.", e);
            System.exit(-1);
        }
        JSONObject fullList = new JSONObject(responseString);
        JSONArray keys = (JSONArray) fullList.get("keys");

        if (keys.length() == 0) {
            return applicationKey;
        } else {
            JSONObject applicationKeys = (JSONObject) keys.get(0);
            JSONObject token = applicationKeys.getJSONObject("token");
            applicationKey = token.getString("accessToken");
            return applicationKey;
        }
    }

    public static String generateApplicationKey(CloseableHttpClient httpClient, String applicationKeyUrl,
            String accessToken, String applicationKeyRequestBody) {
        String result;

        HttpPost httpPost = new HttpPost(applicationKeyUrl);
        httpPost.setHeader("Authorization", "Bearer " + accessToken);
        httpPost.setHeader("Content-Type", "application/json");
        try {
            httpPost.setEntity(new StringEntity(applicationKeyRequestBody));
        } catch (UnsupportedEncodingException e) {
            logger.error("Error occurred while encoding application credentials.", e);
            System.exit(-1);
        }

        CloseableHttpResponse response;
        String responseString = null;
        try {
            response = httpClient.execute(httpPost);
            InputStream is = response.getEntity().getContent();
            responseString = IOUtils.toString(is, "UTF-8");
            response.close();
        } catch (IOException e) {
            logger.error("Error occurred while generating application key.", e);
            System.exit(-1);
        }
        JSONObject jsonObject = new JSONObject(responseString);
        result = jsonObject.getJSONObject("token").getString("accessToken");

        return result;
    }

    public static ArrayList<String> getSubscribeApplications(CloseableHttpClient httpClient, String subscribeUrl,
            String accessToken, String apiId) {
        ArrayList<String> subscribedApplications = new ArrayList<String>();

        String getApplicationUrl = subscribeUrl + "?apiId=" + apiId;
        HttpGet httpGet = new HttpGet(getApplicationUrl);
        httpGet.setHeader("Authorization", "Bearer " + accessToken);

        CloseableHttpResponse response;
        String responseString = null;
        try {
            response = httpClient.execute(httpGet);
            InputStream is = response.getEntity().getContent();
            responseString = IOUtils.toString(is, "UTF-8");
            response.close();
        } catch (IOException e) {
            logger.error("Error occurred while retrieving application details.", e);
            System.exit(-1);
        }
        JSONObject fullList = new JSONObject(responseString);
        JSONArray list = (JSONArray) fullList.get("list");

        for (int i = 0; i < list.length(); i++) {
            JSONObject app = list.getJSONObject(i);
            subscribedApplications.add(app.getString("applicationId"));
        }

        return subscribedApplications;
    }

    public static void addSubscription(CloseableHttpClient httpClient, String apiSubscriptionUrl, String accessToken,
            String subscriptionRequestBody) {
        HttpPost httpPost = new HttpPost(apiSubscriptionUrl);
        httpPost.setHeader("Authorization", "Bearer " + accessToken);
        httpPost.setHeader("Content-Type", "application/json");
        try {
            httpPost.setEntity(new StringEntity(subscriptionRequestBody));
        } catch (UnsupportedEncodingException e) {
            logger.error("Error occurred while encoding application keys.", e);
            System.exit(-1);
        }

        CloseableHttpResponse response;
        try {
            response = httpClient.execute(httpPost);
            response.close();
        } catch (IOException e) {
            logger.error("Error occurred while subscribing for the API.", e);
            System.exit(-1);
        }
    }

    public static void removeApplication(CloseableHttpClient httpClient, String applicationUrl, String applicationId,
            String accessToken) {
        HttpDelete httpDelete = new HttpDelete(applicationUrl + "/" + applicationId);
        httpDelete.setHeader("Authorization", "Bearer " + accessToken);

        CloseableHttpResponse response;
        try {
            response = httpClient.execute(httpDelete);
            response.close();
        } catch (IOException e) {
            logger.error("Error occurred while removing application: " + applicationId, e);
        }
    }

    public static ArrayList<String> callApi(CloseableHttpClient httpClient, String apiMethod, String uri,
            HashMap<String, String> headers, HashMap<String, String> params) {
        ArrayList<String> results = new ArrayList<String>();

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        if (params.size() > 0) {
            for (Map.Entry<String, String> param : params.entrySet()) {
                nvps.add(new BasicNameValuePair(param.getKey(), param.getValue()));
            }
        }

        HttpRequestBase httpRequestBase;
        if (apiMethod.equalsIgnoreCase("GET")) {
            if (nvps.size() > 0) {
                httpRequestBase = new HttpGet(uri + "?" + URLEncodedUtils.format(nvps, "UTF-8"));
            } else {
                httpRequestBase = new HttpGet(uri);
            }
        } else if (apiMethod.equalsIgnoreCase("HEAD")) {
            if (nvps.size() > 0) {
                httpRequestBase = new HttpHead(uri + "?" + URLEncodedUtils.format(nvps, "UTF-8"));
            } else {
                httpRequestBase = new HttpHead(uri);
            }
        } else if (apiMethod.equalsIgnoreCase("OPTIONS")) {
            if (nvps.size() > 0) {
                httpRequestBase = new HttpOptions(uri + "?" + URLEncodedUtils.format(nvps, "UTF-8"));
            } else {
                httpRequestBase = new HttpOptions(uri);
            }
        } else {
            logger.error(
                    "Unsupported HTTP method: " + apiMethod + ". Only GET, HEAD and OPTIONS methods are supported.");
            return results;
        }

        for (Map.Entry<String, String> header : headers.entrySet()) {
            httpRequestBase.addHeader(header.getKey(), header.getValue());
        }        

        CloseableHttpResponse response;
        try {
            response = httpClient.execute(httpRequestBase);
            InputStream is = response.getEntity().getContent();
            results.add(String.valueOf(response.getStatusLine().getStatusCode()));
            results.add(IOUtils.toString(is, "UTF-8"));
            response.close();
        } catch (IOException e) {
            logger.error("Error occurred while calling API: " + uri + " for method: " + apiMethod, e);
        }

        logger.debug(String.format("API uri: %s, status code: %s, entity: %s", httpRequestBase.getURI(), results.get(0),
                results.get(1)));
        
        return results;
    }

    public static String getErrorCode(String content) {
        String errorCode;
        JSONObject jsonObject = XML.toJSONObject(content);
        errorCode = String.valueOf(jsonObject.getJSONObject("ams:fault").get("ams:code"));
        return errorCode;
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
        CloseableHttpResponse response;
        try {
            response = httpClient.execute(postMethod);
            logger.debug("Publishing to DAS status code: " + response.getStatusLine().getStatusCode());
            response.close();
        } catch (IOException e) {
            logger.error("Error occurred while publishing to DAS", e);
        }

    }

    public static void writeToFile(String filePath, HashMap<String, String> entries) {
        Yaml yaml = new Yaml();
        FileWriter writer = null;
        try {
            writer = new FileWriter(filePath);
        } catch (IOException e) {
            logger.error("Error occurred while writing to file.", e);
        }
        yaml.dump(entries, writer);
    }

    public static boolean validateApiConfig(LinkedHashMap<String, LinkedHashMap> api) {
        boolean isValid = true;
        // TODO
        return isValid;
    }
}
