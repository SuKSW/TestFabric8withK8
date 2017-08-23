
import com.google.common.collect.Multimap;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class Main {


    public static void main(String[] args) throws Exception {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");

        URL masterUrl = new URL("https","....", 8443,"/" );
        KubernetesServiceDiscovery kubernetesServiceDiscovery = new KubernetesServiceDiscovery(masterUrl,
                 KubernetesServiceDiscovery.ServiceType.CLUSTERIP);
 //       KubernetesServiceDiscovery kubernetesServiceDiscovery = new KubernetesServiceDiscovery(masterUrl);

        JSONObject services = kubernetesServiceDiscovery.listServices();

        System.out.println();
        System.out.println();

        System.out.println(services);


    }

}