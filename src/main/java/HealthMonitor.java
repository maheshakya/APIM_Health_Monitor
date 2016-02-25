import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.yaml.snakeyaml.Yaml;

/**
 * This is the main class that runs the health monitor
 */
public class HealthMonitor {

    private static Logger logger = Logger.getLogger(HealthMonitor.class);

    public static void main(String[] args) throws IOException {

        logger.info("Starting.....");
        String currentDir = System.getProperty("user.dir");
        System.setProperty("javax.net.ssl.trustStore", currentDir + "/src/main/resources/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

        // Init Yaml
        Yaml yaml = new Yaml();
        Map<String, Object> configs = null;

        try {
            InputStream ios = new FileInputStream("src/main/resources/configs.yml");
            // Parse the YAML file and return the output as a series of Maps and Lists
            configs = (Map<String, Object>) yaml.load(ios);
            logger.info("Read configs...");
        } catch (Exception e) {
            logger.error("Failed to read configs from file...");
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
        String dasUsername = (String) configs.get("dasUsername");
        String dasPassword = (String) configs.get("dasPassword");
        String dasReceiverUrl = (String) configs.get("dasReceiverUrl");

        // Validate main configs
        if (appName == null || userName == null || password == null || host == null || port == null || appTier == null
                || appAuthorizedDomains == null || appKeyType == null || appKeyValidationTime == null
                || dasUsername == null || dasPassword == null || dasReceiverUrl == null) {
            logger.error("Config info missing ...");
        }

        String accessToken = null;

        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            // Login to APIM
            String loginURI = "http://" + host + ":" + port + Constants.APIM_LOGIN;
            Utils.login(httpclient, loginURI, userName, password);

            // Add application for monitoring
            String addApplicationUri = "http://" + host + ":" + port + Constants.APIM_APPLICATION_ADD;
            Utils.addApplication(httpclient, addApplicationUri, appName, appTier, "", "");

            // Generate application key
            String generateApplicationKeyUri = "http://" + host + ":" + port + Constants.APIM_SUBSCRIPTION_ADD;
            accessToken = Utils.generateApplicationKey(httpclient, generateApplicationKeyUri, appName, appKeyType, "",
                    appAuthorizedDomains, appKeyValidationTime);
        } catch (IOException e) {
            logger.error("Exception during initializing: " + e.getMessage());
        } catch (JSONException e) {
            logger.error("Exception during initializing: " + e.getMessage());
        }

        // Get APIs
        LinkedHashMap<String, LinkedHashMap> apis = (LinkedHashMap<String, LinkedHashMap>) configs.get("apis");
        if (apis == null) {
            logger.error("No APIs provided");
        }

        // Create thread pool for the number of APIs
        ExecutorService executor = Executors.newFixedThreadPool(apis.size());

        for (String api : apis.keySet()) {
            LinkedHashMap<String, Object> apiConfig = (LinkedHashMap<String, Object>) apis.get(api);
            HashMap<String, String> parameters = new HashMap<String, String>();
            LinkedHashMap<String, String> parameterConfig = (LinkedHashMap<String, String>) apiConfig.get("parameters");
            if (parameterConfig != null) {
                for (String param : parameterConfig.keySet()) {
                    parameters.put(param, String.valueOf(parameterConfig.get(param)));
                }
            }
            // Need to validate API configs!!!
            Runnable worker = new WorkerThread(host, port, appName, accessToken, userName, password, api,
                    apiConfig.get("apiMethod").toString(), String.valueOf(apiConfig.get("apiVersion")),
                    apiConfig.get("apiProvider").toString(), apiConfig.get("apiTier").toString(),
                    apiConfig.get("apiUrl").toString(), parameters, dasUsername, dasPassword, dasReceiverUrl);
            executor.execute(worker);
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        String removeApplicationUri = "http://" + host + ":" + port + Constants.APIM_APPLICATION_REMOVE;
        Utils.removeApplication(httpclient, removeApplicationUri, appName);
        httpclient.close();

        logger.info("Done.....");
    }

    public static class WorkerThread implements Runnable {

        private String apimHost;
        private String apimPort;
        private String appName;
        private String accessToken;
        private String userName;
        private String password;
        private String apiName;
        private String apiMethod;
        private String apiVersion;
        private String apiProvider;
        private String apiTier;
        private String apiUrl;
        private HashMap<String, String> apiParameters;
        private String dasUsername;
        private String dasPassword;
        private String dasReceiverUrl;

        private String lastTwoSecondsStatus;
        private String lastSixSecondsStatus;

        public WorkerThread(String apimHost, String apimPort, String appName, String accessToken, String userName,
                String password, String apiName, String apiMethod, String apiVersion, String apiProvider,
                String apiTier, String apiUrl, HashMap<String, String> apiParameters, String dasUsername,
                String dasPassword, String dasReceiverUrl) {
            this.apimHost = apimHost;
            this.apimPort = apimPort;
            this.appName = appName;
            this.accessToken = accessToken;
            this.userName = userName;
            this.password = password;
            this.apiName = apiName;
            this.apiMethod = apiMethod;
            this.apiVersion = apiVersion;
            this.apiProvider = apiProvider;
            this.apiTier = apiTier;
            this.apiUrl = apiUrl;
            this.apiParameters = apiParameters;
            this.dasUsername = dasUsername;
            this.dasPassword = password;
            this.dasReceiverUrl = dasReceiverUrl;

        }

        public void run() {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            try {
                String loginURI = "http://" + apimHost + ":" + apimPort + Constants.APIM_LOGIN;
                Utils.login(httpclient, loginURI, userName, password);

                String addSubscriptionUri = "http://" + apimHost + ":" + apimPort + Constants.APIM_SUBSCRIPTION_ADD;
                Utils.addSubscription(httpclient, addSubscriptionUri, apiName, apiVersion, apiProvider, apiTier,
                        appName);

                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Bearer " + accessToken);

                HashMap<String, String> params = apiParameters;

                HashMap<String, String> payloads;
                int totalTime = 0;
                int interval_1 = Constants.INTERVAL_1;
                int interval_2 = Constants.INTERVAL_2;

                lastTwoSecondsStatus = Utils.callApi(httpclient, apiMethod, apiUrl, headers, params);
                lastSixSecondsStatus = lastTwoSecondsStatus;

                // Loop should be continued until user terminates
                for (int i = 0; i < 2; i++) {
                    payloads = new HashMap<String, String>();
                    payloads.put(Constants.api_name, apiName);
                    payloads.put(Constants.ATTRIBUTE_INTERVAL_1, lastTwoSecondsStatus);
                    payloads.put(Constants.ATTRIBUTE_INTERVAL_2, lastSixSecondsStatus);

                    Utils.publishToDas(httpclient, payloads, dasReceiverUrl, dasUsername, dasPassword);

                    threadSleep(interval_1 * Constants.MILLISECOND_MULTIPLIER);
                    totalTime += interval_1;
                    lastTwoSecondsStatus = Utils.callApi(httpclient, apiMethod, apiUrl, headers, params);
                    if (totalTime % interval_2 == 0) {
                        lastSixSecondsStatus = lastTwoSecondsStatus;
                    }
                }

            } catch (IOException e) {
                logger.error("Exception during monitoring: " + e.getMessage());
            } finally {
                try {
                    if (httpclient != null) {
                        httpclient.close();
                    }
                } catch (Exception e) {
                    logger.error("Exception during closing http client: " + e.getMessage());
                }
            }
        }

        private void threadSleep(int milliseconds) {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                logger.error("Error occurred in threads...");
            }
        }
    }
}
