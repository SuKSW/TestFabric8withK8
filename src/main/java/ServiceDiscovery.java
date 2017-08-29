import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.fabric8.kubernetes.api.model.ServiceList;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

abstract class ServiceDiscovery {
    JSONObject servicesJson;

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
