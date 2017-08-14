
import com.google.common.collect.Multimap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Main {


    public static void main(String[] args) throws MalformedURLException {
        String master = "https://..../";

//        KubernetesServiceDiscovery kubernetesServiceDiscovery = new KubernetesServiceDiscovery(master);
        KubernetesServiceDiscovery kubernetesServiceDiscovery = new KubernetesServiceDiscovery(master,
                 KubernetesServiceDiscovery.ServiceType.CLUSTERIP);

       /* KubernetesServiceDiscovery kubernetesServiceDiscovery = new KubernetesServiceDiscovery(master,
                KubernetesServiceDiscovery.ServiceType.NODEPORT);
        */
        /*KubernetesServiceDiscovery kubernetesServiceDiscovery = new KubernetesServiceDiscovery(master,
                KubernetesServiceDiscovery.ServiceType.LOADBALANCER);
        */
         /*KubernetesServiceDiscovery kubernetesServiceDiscovery = new KubernetesServiceDiscovery(master,
                KubernetesServiceDiscovery.ServiceType.EXTERNALNAME);*/
        Multimap<String,URL> servicesMultimap = kubernetesServiceDiscovery.listServices("default");
        System.out.println();
        System.out.println();

        for(Map.Entry<String,URL> pair: servicesMultimap.entries()){
            System.out.println(pair.getKey() + "  =  " + pair.getValue());
        }

        //Test1 t1 = new Test1("https://..../");
        //t1.justPrint();
        //t1.useServiceClass1("default");
        //t1.useServiceClass();

        //URL ulr = new URL("tcp",".....",8443,"/");
        //t1.jp(ulr);

    }

}