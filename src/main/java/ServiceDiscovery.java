import io.fabric8.kubernetes.api.model.ServiceList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by subhashinie on 7/28/17.
 */
public abstract class ServiceDiscovery {

    protected String globalEndpoint;
    protected HashMap<String, URL> services;
    //ifServiceSSL protocol = "https://";

    ServiceDiscovery(String globalEndpoint){
        this.services = new HashMap<String, URL>();
        this.globalEndpoint = globalEndpoint;
    }
    HashMap<String, URL> listServices() throws MalformedURLException{
        return this.services;
    }
    HashMap<String, URL> listServices(String namesapce) throws MalformedURLException{
        return this.services;
    }
    HashMap<String, URL> listServices(String namesapce, HashMap<String, String> criteria) throws MalformedURLException{
        return this.services;
    }
    HashMap<String, URL> listServices(HashMap<String, String> criteria) throws MalformedURLException{
        return this.services;
    }

    protected void servicesToHashMap(ServiceList services, String namespace) throws MalformedURLException{}
}
