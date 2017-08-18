
import com.google.common.collect.Multimap;
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

        String master = "https://....:8443/";
//        KubernetesServiceDiscovery kubernetesServiceDiscovery = new KubernetesServiceDiscovery(master);
//        KubernetesServiceDiscovery kubernetesServiceDiscovery = new KubernetesServiceDiscovery(master,
//                 KubernetesServiceDiscovery.ServiceType.CLUSTERIP);
        KubernetesServiceDiscovery kubernetesServiceDiscovery = new KubernetesServiceDiscovery(master,
                KubernetesServiceDiscovery.ServiceType.NODEPORT);
//        KubernetesServiceDiscovery kubernetesServiceDiscovery = new KubernetesServiceDiscovery(master,
//                KubernetesServiceDiscovery.ServiceType.LOADBALANCER);
//        KubernetesServiceDiscovery kubernetesServiceDiscovery = new KubernetesServiceDiscovery(master,
//                KubernetesServiceDiscovery.ServiceType.EXTERNALNAME);

        Multimap<String,URL> servicesMultimap = kubernetesServiceDiscovery.listServices();

        System.out.println();
        System.out.println();

        for(Map.Entry<String,URL> pair: servicesMultimap.entries()){
            System.out.println(pair.getKey() + "  =  " + pair.getValue());
        }

    }

}