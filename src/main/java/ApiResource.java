import java.util.HashMap;

/**
 * Information required in a resource to call API
 */
public class ApiResource {
    private String apiName;
    private String apiVersion;
    private String apiContext;
    private String apiResource;
    private String httpMethod;
    private HashMap<String, String> parameters;

    public ApiResource(String apiName, String apiVersion, String apiContext, String apiResource, String httpMethod,
            HashMap<String, String> parameters) {
        this.apiName = apiName;
        this.apiVersion = apiVersion;
        this.apiContext = apiContext;
        this.apiResource = apiResource;
        this.httpMethod = httpMethod;
        this.parameters = parameters;
    }

    public String getApiName() {
        return apiName;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getApiContext() {
        return apiContext;
    }

    public String getApiResource() {
        return apiResource;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public HashMap<String, String> getParameters() {
        return parameters;
    }
}
