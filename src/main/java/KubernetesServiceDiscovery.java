import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.Endpoint;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class KubernetesServiceDiscovery extends ServiceDiscovery {
    /*
    * for a service to be discovred
    * port name must be : http/https/ftp
    *
    * viewing only service,endpoints,pods so to avoid viewing nodes
    *
    * Todo:externalIP
    *
    * */
    //private static final Logger log = LoggerFactory.getLogger(KubernetesServiceDiscovery.class);

    private KubernetesClient client;
    private String loadBalancerDefaultProtocol = "http";
    private String externalNameDefaultProtocol = "http";


    KubernetesServiceDiscovery(String globalEndpoint){
        super(globalEndpoint);
        Config config = new ConfigBuilder().withMasterUrl(globalEndpoint).build();
        try {
            this.client = new DefaultKubernetesClient(config);
        } catch (KubernetesClientException e) {
            e.printStackTrace();
        }
    }

   /* @Override
    HashMap<String, URL> listServices() throws MalformedURLException{
        ServiceList services = client.services().list();
        servicesToHashMap(services);
        return super.listServices();
    }*/

    @Override
    HashMap<String, URL> listServices(String namespace) throws MalformedURLException{
        ServiceList services = client.services().inNamespace(namespace).list();
        servicesToHashMap(services, namespace);
        return super.listServices(namespace);
    }

    @Override
    HashMap<String, URL> listServices(String namespace, HashMap<String, String> criteria) throws MalformedURLException{
        ServiceList services = client.services().inNamespace(namespace).withLabels(criteria).list();
        servicesToHashMap(services,namespace);
        return super.listServices(namespace, criteria);
    }

    /*@Override
    HashMap<String, URL> listServices(HashMap<String, String> criteria) throws MalformedURLException{
        ServiceList services = client.services().withLabels(criteria).list();
        servicesToHashMap(services);
        return super.listServices(criteria);
    }*/


    @Override
    protected void servicesToHashMap(ServiceList services, String namespace) throws MalformedURLException{
        this.services.clear();
        List<Service> serviceItems = services.getItems();
        for (Service service : serviceItems) {
            String serviceName = service.getMetadata().getName();
            ServiceSpec serviceSpec = service.getSpec();
            String serviceType = serviceSpec.getType();
            URL serviceURL;
            switch (serviceType) {
                case "ClusterIp":
                    System.out.println(serviceName + " : "+ "ClusterIP");
                    break;
                case "NodePort":
                    List<ServicePort> servicePorts = serviceSpec.getPorts();
                    //get all ports which includes different NodePorts
                    for (ServicePort servicePort : servicePorts) {
                        serviceURL = findUrlByPort(namespace,serviceName,servicePort);
                        this.services.put(serviceName, serviceURL);
                    }
                    break;
                case "LoadBalancer":
                    LoadBalancerStatus loadBalancerStatus = service.getStatus().getLoadBalancer();
                    Iterator<LoadBalancerIngress> ingressIterator = loadBalancerStatus.getIngress().iterator();
                    if (ingressIterator.hasNext()) {
                        while (ingressIterator.hasNext()) {
                            serviceURL = new URL(loadBalancerDefaultProtocol+"://"+ingressIterator.next().getIp());
                            this.services.put(serviceName, serviceURL);
                        }
                    } else { // when a loadBalancer is not assigned, go with the NodePorts
                        List<ServicePort> servicePorts1 = serviceSpec.getPorts();
                        for (ServicePort servicePort : servicePorts1) {
                            serviceURL = findUrlByPort(namespace,serviceName,servicePort);
                            this.services.put(serviceName, serviceURL);
                        }
                    }
                    break;
                case "ExternalName":
                    String externalName = (String) serviceSpec.getAdditionalProperties().get("externalName");
                    serviceURL = new URL(externalNameDefaultProtocol + "://" +externalName );
                    this.services.put(serviceName, serviceURL);
                    break;
                default: // ClusterIP
                    System.out.println(serviceName + " : "+ "ClusterIP");
                    break;
            }
            List<String> specialExternalIps = service.getSpec().getExternalIPs();
            for(String specialExternalIp:specialExternalIps){ //special case
                serviceURL = new URL("http://"+specialExternalIp);
                this.services.put(serviceName,serviceURL);
            }
        }
    }

    private URL findUrlByPort(String namespace, String serviceName, ServicePort servicePort){
        URL url;
        String protocol = servicePort.getName();    //kubernetes gives only tcp/udp but we need http/https
        if (protocol ==null || (!protocol.equals("http")
                && !protocol.equals("https") && !protocol.equals("ftp")
                && !protocol.equals("dns") && !protocol.equals("irc"))){
            System.out.println(serviceName + " : application level protocol not specified for port:"+servicePort.getNodePort());
            return null;
        } else {
            int portNumber = servicePort.getNodePort();
            Endpoints endpoint = client.endpoints().inNamespace(namespace).withName(serviceName).get();
            List<EndpointSubset> endpointSubsets = endpoint.getSubsets();
            if(endpointSubsets.isEmpty()){  //no endpoints for the service
                return null;
            }
            for(EndpointSubset endpointSubset : endpointSubsets) {
                List<EndpointAddress> endpointAddresses = endpointSubset.getAddresses();
                if(endpointAddresses.isEmpty()){  //no endpoints for the service
                    return null;
                }
                for(EndpointAddress endpointAddress : endpointAddresses) {
                    String podname = endpointAddress.getTargetRef().getName();
                    Pod pod = client.pods().inNamespace(namespace).withName(podname).get();
                    try {
                        url = new URL(protocol, pod.getStatus().getHostIP(), portNumber, "");
                        return url;
                    } catch (NullPointerException e) { //no pods available for this address
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }
}
