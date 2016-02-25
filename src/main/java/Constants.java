/**
 * Created by maheshakya on 2/19/16.
 */
public class Constants {

    // Urls of APIM
    public static final String APIM_LOGIN = "/store/site/blocks/user/login/ajax/login.jag";
    public static final String APIM_APPLICATION_ADD = "/store/site/blocks/application/application-add/ajax/application-add.jag";
    public static final String APIM_SUBSCRIPTION_ADD = "/store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag";
    public static final String APIM_APPLICATION_REMOVE = "/store/site/blocks/application/application-remove/ajax/application-remove.jag";

    // Time intervals (in seconds)
    public static final int INTERVAL_1 = 2;
    public static final int INTERVAL_2 = 6;
    public static final int MILLISECOND_MULTIPLIER = 1000;

    // DAS stream attributes
    public static final String api_name = "api_name";
    public static final String ATTRIBUTE_INTERVAL_1 = "status_code_two_seconds";
    public static final String ATTRIBUTE_INTERVAL_2 = "status_code_six_seconds";
}
