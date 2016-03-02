import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

/**
 * This is the main class that runs the health monitor
 */
public class HealthMonitor {

    private static Logger logger = Logger.getLogger(HealthMonitor.class);

    public static void main(String[] args) throws IOException {

        logger.info("Starting...");

        Map<String, Object> configs = Utils.readConfigs();

        String trustStorePath = (String) configs.get("trustStorePath");
        String encryptionKey = (String) configs.get("encryptionKey");
        String appName = (String) configs.get("applicationName");
        String userName = (String) configs.get("apimUsername");
        String password = (String) configs.get("apimPassword");
        String clientName = (String) configs.get("clientName"); // not being used because unregister is not here yet.
        String host = (String) configs.get("apimHost");
        String port = String.valueOf(configs.get("apimPort"));
        String gatewayPort = String.valueOf(configs.get("apimGatewayPort"));
        String appTier = (String) configs.get("applicationTier");
        String appAuthorizedDomains = (String) configs.get("applicationAuthorizedDomains");
        String appKeyType = (String) configs.get("applicationKeyType");
        String appKeyValidationTime = String.valueOf(configs.get("applicationKeyValidityTime"));
        String dasUsername = (String) configs.get("dasUsername");
        String dasPassword = (String) configs.get("dasPassword");
        String dasReceiverUrl = (String) configs.get("dasReceiverUrl");

        // validate configs separately???
        if (trustStorePath == null || encryptionKey == null || appName == null || userName == null || password == null
                || clientName == null || host == null || port == null || gatewayPort == null || appTier == null
                || appAuthorizedDomains == null || appKeyType == null || appKeyValidationTime == null
                || dasUsername == null || dasPassword == null || dasReceiverUrl == null) {
            logger.error("One of the required configurations is missing.");
            System.exit(1);
        }

        // Decrypt passwords
        Security security = null;
        try {
            security = new Security(encryptionKey);
        } catch (Exception e) {
            logger.error("Error occurred while initializing security component.", e);
        }
        password = security.decrypt(password);
        dasPassword = security.decrypt(dasPassword);

        // Add client trust store
        String currentDir = System.getProperty("user.dir");
        System.setProperty("javax.net.ssl.trustStore", currentDir + File.separator + trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

        CloseableHttpClient httpClient = HttpClients.createDefault();

        // Get client id and client secret
        // temp client name
        clientName = RandomStringUtils.randomAlphabetic(10);

        String dcrEndpointURL = "http://" + host + ":" + port + Constants.CLIENT_REGISTRATION_PATH + "/register";
        String applicationRequestBody = " {\n" +
                " \"callbackUrl\": \"www.google.lk\",\n" +
                " \"clientName\": \"" + clientName + "\",\n" +
                " \"tokenScope\": \"Production\",\n" +
                " \"owner\": \"" + userName + "\",\n" +
                " \"grantType\": \"password refresh_token\",\n" +
                " \"saasApp\": true\n" +
                " }";

        HashMap<String, String> clientInfo = Utils.registerClient(httpClient, userName, password, dcrEndpointURL,
                applicationRequestBody);
        
        logger.info("Client : " + clientName + " registered.");

        String clientId = clientInfo.get("clientId");
        String clientSecret = clientInfo.get("clientSecret");

        // getting access token from client secret
        String requestBody = "grant_type=password&username=" + userName +
                "&password=" + password +
                "&scope=apim:subscribe";
        String tokenEndpointURL = "https://" + host + ":" + gatewayPort + "/token";

        HashMap<String, String> clientAccessToken = Utils.getAccessToken(httpClient, clientId, clientSecret,
                tokenEndpointURL, requestBody);

        logger.info("Client access token generated.");
        
        String refreshToken = clientAccessToken.get("refreshToken");
        String accessToken = clientAccessToken.get("accessToken");

        // Get application
        String applicationUrl = "http://" + host + ":" + port + Constants.APIM_STORE_PATH + "/applications";
        String applicationId = Utils.getApplicationId(httpClient, applicationUrl, accessToken, appName);
        // If application is not available, create application
        if (applicationId == null) {
            String application = " {\n" +
                    " \"throttlingTier\": \"" + appTier + "\",\n" +
                    " \"name\": \"" + appName + "\",\n" +
                    " \"description\": \"Test description\"\n" +
                    " }";
            applicationId = Utils.createApplication(httpClient, applicationUrl, accessToken, application);
        }

        logger.info("Application : " + appName + " retrieved.");

        // Get application key
        // Application key data and url is being used during API call to refresh tokens, hence initialing early
        String applicationKeyData = " {\n" +
                " \"validityTime\": \"" + appKeyValidationTime + "\",\n" +
                " \"keyType\": \"" + appKeyType + "\",\n" +
                " \"accessAllowDomains\": [\"" + appAuthorizedDomains + "\"]\n" +
                " }";
        String applicationKeyUrl = "http://" + host + ":" + port
                + Constants.APIM_STORE_PATH + "/applications/generate-keys?applicationId=" + applicationId;

        String applicationAccessToken = Utils.getApplicationKey(httpClient, applicationUrl, accessToken, applicationId);
        // If application key is not available, generate application key
        if (applicationAccessToken == null) {
            applicationAccessToken = Utils.generateApplicationKey(httpClient, applicationKeyUrl, accessToken,
                    applicationKeyData);

        }

        logger.info("Application access token generated.");

        // Get APIs
        LinkedHashMap<String, LinkedHashMap> apis = (LinkedHashMap<String, LinkedHashMap>) configs.get("apis");
        if (apis == null) {
            logger.error("No APIs provided.");
            System.exit(1);
        }

        // Subscribe the application to APIs
        LinkedHashMap<String, LinkedHashMap> apiConfig;
        String apiIdentifier;
        ArrayList<String> subscribedApplications;
        String apiSubscriptionUrl = "http://" + host + ":" + port + Constants.APIM_STORE_PATH + "/subscriptions";
        String subscriptionRequestBody;
        for (String api : apis.keySet()) {
            apiConfig = apis.get(api);
            apiIdentifier = apiConfig.get("apiProvider") + "-" + api + "-" + apiConfig.get("apiVersion");
            subscribedApplications = Utils.getSubscribeApplications(httpClient, apiSubscriptionUrl, accessToken,
                    apiIdentifier);
            // If subscribed applications does not contain this application, subscribe this application
            if (!subscribedApplications.contains(applicationId)) {
                subscriptionRequestBody = " {\n" +
                        " \"tier\": \"" + apiConfig.get("apiTier") + "\",\n" +
                        " \"apiIdentifier\": \"" + apiIdentifier + "\",\n" +
                        " \"applicationId\": \"" + applicationId + "\"\n" +
                        " }";
                Utils.addSubscription(httpClient, apiSubscriptionUrl, accessToken, subscriptionRequestBody);
            }

            logger.info("Subscribed to API : " + api + ".");
        }

        // Get all resources of APIs
        ArrayList<ApiResource> apiResources = new ArrayList<ApiResource>();
        for (String api : apis.keySet()) {
            apiConfig = apis.get(api);
            LinkedHashMap<ArrayList<String>, LinkedHashMap> resources = (LinkedHashMap<ArrayList<String>, LinkedHashMap>) apiConfig
                    .get("apiResources");
            for (ArrayList<String> resource : resources.keySet()) {
                LinkedHashMap<String, Object> paramMap = (LinkedHashMap<String, Object>) resources.get(resource)
                        .get("parameters");
                HashMap<String, String> parameters = new HashMap<String, String>();

                for (String param : paramMap.keySet()) {
                    parameters.put(param, String.valueOf(paramMap.get(param)));
                }

                ApiResource apiResource = new ApiResource(api, String.valueOf(apiConfig.get("apiVersion")),
                        String.valueOf(apiConfig.get("apiContext")), resource.get(0), resource.get(1), parameters);
                apiResources.add(apiResource);
            }

        }

        // Create thread pool for the number of API resources
        ExecutorService executor = Executors.newFixedThreadPool(apiResources.size());
        String gatewayUrl = "https://" + host + ":" + gatewayPort;
        for (ApiResource resource : apiResources) {
            Runnable worker = new WorkerThread(accessToken, applicationKeyData, applicationKeyUrl, gatewayUrl,
                    applicationAccessToken, resource.getApiName(), resource.getApiVersion(), resource.getApiContext(),
                    resource.getApiResource(), resource.getHttpMethod(), resource.getParameters(), dasReceiverUrl,
                    dasUsername, dasPassword);
            executor.execute(worker);

            logger.info("Monitoring for API resource : " + resource.getApiName() + " " + resource.getApiVersion() + " "
                    + resource.getApiResource() + " " + resource.getHttpMethod() + " started.");
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        // Delete application
        Utils.removeApplication(httpClient, applicationUrl, applicationId, accessToken);

        httpClient.close();

        logger.info("Exiting...");

    }

    public static class WorkerThread implements Runnable {

        private String clientAccessToken;
        private String applicationKeyData;
        private String applicationKeyUrl;
        private String apiGatewayUrl;
        private String applicationAccessToken;
        private String apiName;
        private String apiVersion;
        private String apiContext;
        private String apiResource;
        private String httpMethod;
        private HashMap<String, String> parameters;

        private String dasReceiverUrl;
        private String dasUsername;
        private String dasPassword;

        public WorkerThread(String clientAccessToken, String applicationKeyData, String applicationKeyUrl,
                String apiGatewayUrl, String applicationAccessToken, String apiName, String apiVersion,
                String apiContext, String apiResource, String httpMethod, HashMap<String, String> parameters,
                String dasReceiverUrl, String dasUsername, String dasPassword) {
            this.clientAccessToken = clientAccessToken;
            this.applicationKeyData = applicationKeyData;
            this.applicationKeyUrl = applicationKeyUrl;
            this.apiGatewayUrl = apiGatewayUrl;
            this.applicationAccessToken = applicationAccessToken;
            this.apiName = apiName;
            this.apiVersion = apiVersion;
            this.apiContext = apiContext;
            this.apiResource = apiResource;
            this.httpMethod = httpMethod;
            this.parameters = parameters;

            this.dasReceiverUrl = dasReceiverUrl;
            this.dasUsername = dasUsername;
            this.dasPassword = dasPassword;
        }

        public void run() {
            CloseableHttpClient httpClient = HttpClients.createDefault();

            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", "Bearer " + applicationAccessToken);

            String apiResourceUrl = apiGatewayUrl + "/" + apiContext + "/" + apiResource;

            for (int i = 0; i < 2; i++) { // For testing only, Needs to run forever!!!
                ArrayList<String> results = Utils.callApi(httpClient, httpMethod, apiResourceUrl, headers, parameters);
                // Check whether there's an authorization error
                if (results.get(0).equals("401")) {
                    String errorCode = Utils.getErrorCode(results.get(1));
                    // If error code is 900901, there application access token has expired
                    if (errorCode.equals("900901")) {
                        // Generate new token
                        applicationAccessToken = Utils.generateApplicationKey(httpClient, applicationKeyUrl,
                                clientAccessToken, applicationKeyData);
                        headers = new HashMap<String, String>();
                        headers.put("Authorization", "Bearer " + applicationAccessToken);
                        // Call API again
                        results = Utils.callApi(httpClient, httpMethod, apiResourceUrl, headers, parameters);
                    }
                }

                HashMap<String, String> payload = new HashMap<String, String>();
                payload.put(Constants.API_NAME, apiName);
                payload.put(Constants.API_VERSION, apiVersion);
                payload.put(Constants.API_RESOURCE, apiResource);
                payload.put(Constants.HTTP_METHOD, httpMethod);
                payload.put(Constants.STATUS_CODE, results.get(0));
                payload.put(Constants.TIME_STAMP, String.valueOf(System.currentTimeMillis()));

                Utils.publishToDas(httpClient, payload, dasReceiverUrl, dasUsername, dasPassword);

                threadSleep(Constants.INTERVAL);
            }

            try {
                httpClient.close();
            } catch (IOException e) {
                logger.error("Error occurred while closing HTTP client.", e);
            }
        }

        private void threadSleep(int milliseconds) {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                logger.error("Error occurred in thread for the api: " + apiResource + "in method: " + httpMethod, e);
            }
        }
    }
}
