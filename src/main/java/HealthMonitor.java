
/**
 * Created by maheshakya on 2/15/16.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.JsonObject;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.eclipse.osgi.internal.signedcontent.Base64;
import org.json.JSONException;
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
import org.yaml.snakeyaml.Yaml;

/**
 * This is the main class that runs the health monitor
 */
public class HealthMonitor {
    public static void main(String[] args) throws IOException {

        System.out.println("Starting.....");
        String currentDir = System.getProperty("user.dir");
        System.setProperty("javax.net.ssl.trustStore", currentDir + "/src/main/resources/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");



        // Test Yaml
        Yaml yaml = new Yaml();
        Map<String, Object> configs = null;

        try {
            InputStream ios = new FileInputStream("src/main/resources/configs.yml");
            // Parse the YAML file and return the output as a series of Maps and Lists
            configs = (Map< String, Object>) yaml.load(ios);
            System.out.println("ok");

        } catch (Exception e) {
            e.printStackTrace();
        }



        String appName = (String) configs.get("applicationName");
        String userName = (String) configs.get("apimUsername");
        String password = (String) configs.get("apimPassword");
        String host = (String) configs.get("apimHost");
        String port = String.valueOf(configs.get("apimPort"));
        String appTier = (String) configs.get("applicationTier");
        String appAuthorizedDomains = (String) configs.get("applicationAuthorizedDomains");
        String appKeyType = (String) configs.get("applicationKeyType");
        String appKeyValidationTime = String.valueOf(configs.get("applicationKeyValidityTime"));

        String apiName = "CalculatorAPI";
        String apiVersion = "1.0";
        String apiProvider = "admin";
        String apiTier = "Unlimited";

        String accessToken = null;

        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            String loginURI = "http://" + host + ":" + port + Constants.APIM_LOGIN;
            String loginUser = userName;
            String loginPassword = password;
            Utils.login(httpclient, loginURI, loginUser, loginPassword);

            String addApplicationUri = "http://" + host + ":" + port + Constants.APIM_APPLICATION_ADD;
            String applicationName = appName;
            String applicationTier = appTier;
            String applicationDescription = "";
            String applicationCallbackUrl = "";
            Utils.addApplication(httpclient, addApplicationUri, applicationName, applicationTier, applicationDescription,
                    applicationCallbackUrl);

            String generateApplicationKeyUri = "http://" + host + ":" + port + Constants.APIM_SUBSCRIPTION_ADD;
            String keyType = appKeyType;
            String authorizedDomains = appAuthorizedDomains;
            String validityTime = appKeyValidationTime;
            accessToken = Utils.generateApplicationKey(httpclient, generateApplicationKeyUri, applicationName, keyType, applicationCallbackUrl, authorizedDomains, validityTime);
        } catch (IOException e) {
            System.err.println("Exception during initializing: " + e.getMessage());
        } finally {
            httpclient.close();
        }

        LinkedHashMap apis = (LinkedHashMap) configs.get("apis");
        if (apis == null) {
            System.err.println("No APIs provided");
        }
        ExecutorService executor = Executors.newFixedThreadPool(apis.size());

        for (Object api: apis.keySet()) {
            LinkedHashMap apiConfig = (LinkedHashMap) apis.get(api);
            Runnable worker = new WorkerThread(host, port, appName, accessToken, userName, password, api.toString(), apiConfig.get("apiVersion").toString(), apiConfig.get("apiProvider").toString(), apiConfig.get("apiTier").toString());
            executor.execute(worker);
        }

//        Runnable worker = new WorkerThread(host, port, appName, accessToken, userName, password, apiName, apiVersion, apiProvider, apiTier);
//        executor.execute(worker);
        executor.shutdown();
        while (!executor.isTerminated()) {

        }

        System.out.println("done.....");
    }

    public static class WorkerThread implements Runnable {

        private String apimHost;
        private String apimPort;
        private String appName;
        private String accessToken;
        private String userName;
        private String password;
        private String apiName;
        private String apiVersion;
        private String apiProvider;
        private String apiTier;


        public WorkerThread(String apimHost, String apimPort, String appName, String accessToken, String userName, String password, String apiName, String apiVersion, String apiProvider, String apiTier){
            this.apimHost = apimHost;
            this.apimPort = apimPort;
            this.appName = appName;
            this.accessToken = accessToken;
            this.userName = userName;
            this.password = password;
            this.apiName = apiName;
            this.apiVersion = apiVersion;
            this.apiProvider = apiProvider;
            this.apiTier = apiTier;
        }
        public void run() {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            try {
                String loginURI = "http://" + apimHost + ":" + apimPort + Constants.APIM_LOGIN;
                String loginUser = userName;
                String loginPassword = password;
                Utils.login(httpclient, loginURI, loginUser, loginPassword);


                String addSubscriptionUri = "http://" + apimHost + ":" + apimPort + Constants.APIM_SUBSCRIPTION_ADD;
                String apiName = this.apiName;
                String apiVersion = this.apiVersion;
                String apiProvider = this.apiProvider;
                String apiTier = this.apiTier;
                Utils.addSubscription(httpclient, addSubscriptionUri, apiName, apiVersion, apiProvider, apiTier, appName);

                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Accept", "application/json");
                headers.put("Authorization", "Bearer " + accessToken);

                HashMap<String, String> params = new HashMap<String, String>();
                params.put("x", "32");
                params.put("y", "4");

                String apiPort = "8243";
                String apiUri = "https://" + apimHost + ":" + apiPort + "/calc/1.0/add";

                // Using http endpoint here, but should be https
                String receiverUrl = "http://localhost:9780/endpoints/healthMonitorReceiver";

                for (int i= 0; i<2; i++) {
                    String sc = Utils.callApiGet(httpclient, apiUri, headers, params);

                    JsonObject event = new JsonObject();
                    JsonObject payLoadData = new JsonObject();

                    payLoadData.addProperty("status_code", sc);

                    event.add("payloadData", payLoadData);

                    String eventString = "{\"event\": " + event + "}";

                    HttpPost postMethod = new HttpPost(receiverUrl);
                    StringEntity entity = new StringEntity(eventString);
                    postMethod.setEntity(entity);
                    // Uncomment when https
//                    postMethod.setHeader("Authorization", "Basic " + Base64.encode((dasUserName + ":" + dasPassword).getBytes()));
                    CloseableHttpResponse response1 = httpclient.execute(postMethod);
                    System.out.println(response1.getStatusLine());

                    HttpEntity entity1 = response1.getEntity();
                    // do something useful with the response body
                    InputStream is = entity1.getContent();
                    String output = IOUtils.toString(is, "UTF-8");
                    System.out.println(output);

                    processmessage();
                    response1.close();
                }

                String removeApplicationUri = "http://" + apimHost + ":" + apimPort + Constants.APIM_APPLICATION_REMOVE;
                Utils.removeApplication(httpclient, removeApplicationUri, appName);

            } catch (IOException e) {
                System.err.println("Exception during monitoring: " + e.getMessage());
            } catch (JSONException e) {
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
