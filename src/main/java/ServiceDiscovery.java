import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.fabric8.kubernetes.api.model.ServiceList;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

abstract class ServiceDiscovery {

    protected URL masterUrl;
    protected JSONObject servicesJson;

    ServiceDiscovery(URL masterUrl){
        this.masterUrl = masterUrl;
    }
    JSONObject listServices() throws MalformedURLException{
        return this.servicesJson;
    }
    JSONObject listServices(String namesapce) throws MalformedURLException{
        return this.servicesJson;
    }
    JSONObject listServices(String namesapce, HashMap<String, String> criteria) throws MalformedURLException{
        return this.servicesJson;
    }
    JSONObject listServices(HashMap<String, String> criteria) throws MalformedURLException{
        return this.servicesJson;
    }

}
