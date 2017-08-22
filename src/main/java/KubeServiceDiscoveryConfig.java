import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;


public class KubeServiceDiscoveryConfig {

    private static Boolean insidePod   = true;

    private static String clientCertLocation = System.getProperty("user.dir")+"/src/main/resources/ca.crt";
    private static String serviceAccountToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJteXByb2plY3QiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlY3JldC5uYW1lIjoic2VyZGktdG9rZW4tY3BkZnYiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoic2VyZGkiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiI1NzgxNzkwMy04MjM0LTExZTctOWU2Zi1iYTI2YjVlYTlhODkiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6bXlwcm9qZWN0OnNlcmRpIn0.JopXXkRhTeqWNN6yBosJhaxs2nU9uCZBW_ttnxSYz3pM93AcNAIGvLcrtmbrmEO6nlxvPK-s_qfXk2oJESJNm7Sje-7jvhgwGQdBizy06AlM3AM5cr2L7ap6ri7OQX-B4yvFyIAAIDMC4rifiYijXClV2JPo6S_gXHfwV7kOlJI3KjDwyWEwOjgXMK00ewJ1Xnd-rpTJgvqJPuEer2hbXRZrp2JoMa-fzv219c7l3LuVx7ojLo9ElY9iDbox8VvV1lBRxMJuitC2L8aE0m_jtSAC5Bum9zaOms2OK4T6uyr6HhfpPgLcZoWX1wpAXAOfsmoS2BPA3Ar1LXflT5SLbA";

    private static String externalNameDefaultProtocol = "http";

    public Boolean isInsidePod() {
        return insidePod;
    }

    public String getClientCertLocation() {
        return clientCertLocation;
    }

    public String getServiceAccountToken() {
        return serviceAccountToken;
    }

    public static String getExternalNameDefaultProtocol() {
        return externalNameDefaultProtocol;
    }
}
