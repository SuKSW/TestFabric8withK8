import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.fabric8.kubernetes.api.model.ServiceList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by subhashinie on 7/28/17.
 */
public abstract class ServiceDiscovery {

    protected String globalEndpoint;
    protected Multimap<String, URL> servicesMultimap;

    ServiceDiscovery(String globalEndpoint){
        this.servicesMultimap = ArrayListMultimap.create();
        this.globalEndpoint = globalEndpoint;
    }
    Multimap<String, URL> listServices() throws MalformedURLException{
        return this.servicesMultimap;
    }
    Multimap<String, URL> listServices(String namesapce) throws MalformedURLException{
        return this.servicesMultimap;
    }
    Multimap<String, URL> listServices(String namesapce, HashMap<String, String> criteria) throws MalformedURLException{
        return this.servicesMultimap;
    }
    Multimap<String, URL> listServices(HashMap<String, String> criteria) throws MalformedURLException{
        return this.servicesMultimap;
    }

    protected void servicesToHashMap(ServiceList services, String namespace) throws MalformedURLException{}
}
