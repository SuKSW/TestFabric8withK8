import com.google.common.collect.Multimap;
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
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class KubernetesServiceDiscovery extends ServiceDiscovery {
    /*
    *----------------------------------------------------------------
    * For service type "A", the url ip retrieved will also be
    * of IP type "A"
    *
    * eg. service type - NodePort
    *     IP type      - Host Ip of a pod
    *
    *     Will not show Nodeport type IPs of LoadBalancer services or ExternalName services either
    *-----------------------------------------------------------------
    * Same service name can repeat with mulitiple service ports
    * due to having different protocols for differents ports
    * Therefore using ArrayListMultimap
    *
    * ----------------------------------------------------------------
    * for a service to be discovred
    * port name must be : http/https/ftp
    *
    *-----------------------------------------------------------------
    * viewing only service,endpoints,pods so to avoid viewing nodes
    *
    *
    * */

    private final Logger log  = LoggerFactory.getLogger(KubernetesServiceDiscovery.class);
    private KubeServiceDiscoveryConfig kubeServDiscConfig;


    private OpenShiftClient client;
    //private KubernetesClient client;

    private ServiceType serviceType;
    private static ServiceType defaultServiceType = ServiceType.LOADBALANCER;

    public enum ServiceType {
        CLUSTERIP, NODEPORT, LOADBALANCER, EXTERNALNAME, EXTERNALIP
    }


    KubernetesServiceDiscovery(String globalEndpoint) throws Exception {
        super(globalEndpoint);
        try {
            this.client = new DefaultOpenShiftClient(buildConfig(globalEndpoint));
            //this.client = new DefaultKubernetesClient(buildConfig(globalEndpoint));
            this.serviceType = defaultServiceType;
        } catch (KubernetesClientException e) {
            e.printStackTrace();
        }

    }

    KubernetesServiceDiscovery(String globalEndpoint, ServiceType serviceType) throws Exception {
        super(globalEndpoint);
        try {
            this.client = new DefaultOpenShiftClient(buildConfig(globalEndpoint));
            //this.client = new DefaultKubernetesClient(buildConfig(globalEndpoint));
            this.serviceType = serviceType;
        } catch (KubernetesClientException e) {
            e.printStackTrace();
        } catch (NullPointerException e){//endpoint not specified
            e.printStackTrace();
        }
    }

    private Config buildConfig(String globalEndpoint){
        kubeServDiscConfig = new KubeServiceDiscoveryConfig();
        if(kubeServDiscConfig.isInsidePod()){
            System.setProperty("kubernetes.auth.tryKubeConfig", "false");
            System.setProperty("kubernetes.auth.tryServiceAccount", "true");
            Config config = null;
            try {
                config = new ConfigBuilder().withMasterUrl(globalEndpoint).withTrustCerts(true).withClientCertFile(kubeServDiscConfig.getClientCertLocation()).withOauthToken(new String(Files.readAllBytes(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/token")))).build();
            } catch (IOException e) {
                log.error("Token file not found");
                e.printStackTrace();
            }
            return config;
        }else{
            System.setProperty("kubernetes.auth.tryKubeConfig", "false");
            System.setProperty("kubernetes.auth.tryServiceAccount", "true");

            //Using token - works
            //Can list services from any namespace since a cluster role was given
            Config config = new ConfigBuilder().withMasterUrl(globalEndpoint).withTrustCerts(true).withClientCertFile(kubeServDiscConfig.getClientCertLocation()).withOauthToken(kubeServDiscConfig.getServiceAccountToken()).build();

            //Without token - does not work.
            //"Message: User "system:anonymous" cannot list services in project "myproject"."
            //Here the project name came from the namespace given in the main method
            //cert is the ca cert from .minikube
            //Config config = new ConfigBuilder().withMasterUrl(globalEndpoint).withTrustCerts(true).withClientCertFile(kubeServDiscConfig.getClientCertLocation()).build();

            //works when system property "kubernetes.auth.tryKubeConfig" set to true
            //Config config = new ConfigBuilder().withMasterUrl(globalEndpoint).build();
            return config;
        }
    }


    @Override
    Multimap<String, URL> listServices() throws MalformedURLException{
        log.debug("Looking for services in all namespaces");
        try{
            ServiceList services = client.services().inAnyNamespace().list();
            addServicesToHashMap(services, null);
        }catch (KubernetesClientException e){
            e.printStackTrace();
        }
        return super.listServices();
    }

    @Override
    Multimap<String, URL> listServices(String namespace) throws MalformedURLException{
        log.debug("Looking for services in namespace "+namespace);
        try {
            ServiceList services = client.services().inNamespace(namespace).list();
            addServicesToHashMap(services, namespace);
        }catch (KubernetesClientException e){
            e.printStackTrace();
        }
        return super.listServices(namespace);
    }

    @Override
    Multimap<String, URL> listServices(String namespace, HashMap<String, String> criteria) throws MalformedURLException{
        log.debug("Looking for services, with the specified labels, in namespace "+namespace);
        try{
            ServiceList services = client.services().inNamespace(namespace).withLabels(criteria).list();
            addServicesToHashMap(services, namespace);
        }catch (KubernetesClientException e){
            e.printStackTrace();
        }
        return super.listServices(namespace, criteria);
    }

    @Override
    Multimap<String, URL> listServices(HashMap<String, String> criteria) throws MalformedURLException{
        log.debug("Looking for services, with the specified labels, in all namespaces");
        try{
            ServiceList services = client.services().withLabels(criteria).list();
            addServicesToHashMap(services,null);
        }catch (KubernetesClientException e){
            e.printStackTrace();
        }
        return super.listServices(criteria);
    }


    protected void addServicesToHashMap(ServiceList services, String namespace) throws MalformedURLException{
        this.servicesMultimap.clear();
        List<Service> serviceItems = services.getItems();
        switch(serviceType){
            case CLUSTERIP:
                log.debug("ServiceType CLUSTERIP selected");
                for (Service service : serviceItems) {
                    if(service.getSpec().getType().equals("ClusterIP")){
                        String serviceName = service.getMetadata().getName();
                        ServiceSpec serviceSpec = service.getSpec();
                        for (ServicePort servicePort : serviceSpec.getPorts()) {
                            String protocol = servicePort.getName();
                            if (protocol !=null && (protocol.equals("http")
                                    || protocol.equals("https") || protocol.equals("ftp") || protocol.equals("irc"))) {
                                URL serviceURL = new URL(protocol, serviceSpec.getClusterIP(), servicePort.getPort(), "");
                                this.servicesMultimap.put(serviceName, serviceURL);
                            }else if(log.isDebugEnabled()){
                                log.debug("Service:"+serviceName+"  Namespace:"+service.getMetadata().getNamespace()+"  Port:"+servicePort.getPort()+"/"+servicePort.getProtocol()+"  app protocol not defined");
                            }
                        }
                    }
                }
                break;
            case NODEPORT:
                log.debug("ServiceType NODEPORT selected");
                for (Service service : serviceItems) {
                    if(service.getSpec().getType().equals("NodePort")){
                        for (ServicePort servicePort : service.getSpec().getPorts()) {
                            URL serviceURL = findNodePortServiceURLsForAPort(namespace,service.getMetadata().getName(),servicePort);
                            if (serviceURL == null){
                                if(log.isDebugEnabled()) {
                                    log.debug("Service:"+service.getMetadata().getName()+"  Namespace:"+service.getMetadata().getNamespace()+"  Port:"+servicePort.getPort()+"/"+servicePort.getProtocol());
                                }
                            }else {
                                this.servicesMultimap.put(service.getMetadata().getName(), serviceURL);
                            }
                        }
                    }
                }
                break;
            case LOADBALANCER:
                log.debug("ServiceType LOADBALANCER selected");
                for (Service service : serviceItems) {
                    if(service.getSpec().getType().equals("LoadBalancer")){
                        String serviceName = service.getMetadata().getName();
                        for (ServicePort servicePort : service.getSpec().getPorts()) {
                            String protocol = servicePort.getName();    //kubernetes gives only tcp/udp but we need http/https
                            if (protocol !=null && (protocol.equals("http")
                                    || protocol.equals("https") || protocol.equals("ftp") || protocol.equals("irc"))) {
                                LoadBalancerStatus loadBalancerStatus = service.getStatus().getLoadBalancer();
                                Iterator<LoadBalancerIngress> ingressIterator = loadBalancerStatus.getIngress().iterator();
                                if (ingressIterator.hasNext()) {
                                    while (ingressIterator.hasNext()) {
                                        URL serviceURL = new URL(protocol,ingressIterator.next().getIp(),servicePort.getPort(), "");
                                        this.servicesMultimap.put(serviceName, serviceURL);
                                    }
                                }
                            }else if(log.isDebugEnabled()){
                                log.debug("Service:"+serviceName+"  Namespace:"+service.getMetadata().getNamespace()+"  Port:"+servicePort.getPort()+"/"+servicePort.getProtocol()+"  app protocol not defined");
                            }
                        }
                    }
                }
                break;
            case EXTERNALNAME:
                log.debug("ServiceType EXTERNALNAME selected");
                for (Service service : serviceItems) {
                    if (service.getSpec().getType().equals("ExternalName")) {
                        String externalName = (String) service.getSpec().getAdditionalProperties().get("externalName");
                        URL serviceURL = new URL(kubeServDiscConfig.getExternalNameDefaultProtocol() + "://" + externalName);
                        this.servicesMultimap.put(service.getMetadata().getName(), serviceURL);
                    }
                }
                break;
            case EXTERNALIP:    // Special case. Not managed by Kubernetes but the cluster administrator.
                log.debug("ServiceType EXTERNALIP selected");
                for (Service service : serviceItems) {
                    String serviceName = service.getMetadata().getName();
                    List<String> specialExternalIps = service.getSpec().getExternalIPs();
                    for (String specialExternalIp : specialExternalIps) { //Not all services have them. Thus protocol checking is done afterwards.
                        for (ServicePort servicePort : service.getSpec().getPorts()) {
                            String protocol = servicePort.getName();    //kubernetes gives only tcp/udp but we need http/https
                            if (protocol !=null && (protocol.equals("http")
                                    || protocol.equals("https") || protocol.equals("ftp") || protocol.equals("irc"))) {
                                URL serviceURL = new URL(protocol,specialExternalIp,servicePort.getPort(), "");
                                this.servicesMultimap.put(service.getMetadata().getName(), serviceURL);
                            }else if(log.isDebugEnabled()){
                                log.debug("Service:"+serviceName+"  Namespace:"+service.getMetadata().getNamespace()+"  Port:"+servicePort.getPort()+"/"+servicePort.getProtocol()+"  app protocol not defined");
                            }
                        }
                    }
                }
                break;
        }

    }

    private URL findNodePortServiceURLsForAPort(String namespace, String serviceName, ServicePort servicePort){
        URL url;
        String protocol = servicePort.getName();    //kubernetes gives only tcp/udp but we need http/https
        if(protocol==null || (!protocol.equals("http")
                && !protocol.equals("https") && !protocol.equals("ftp") && !protocol.equals("irc"))){
            if(log.isDebugEnabled()){
                log.debug("Service:"+serviceName+"  Port:"+servicePort.getPort()+"/"+servicePort.getProtocol()+"  app protocol not defined");
            }
            return null;
        }
        if(namespace==null){
            int nodePort = servicePort.getNodePort();
            /*
                Below, method ".inAnyNamespace()" did not support ".withName()". Therefore ".withField()" is used.
                It returns a single item list which has the only endpoint created for the service.
            */
            Endpoints endpoint = client.endpoints().inAnyNamespace().withField("metadata.name",serviceName).list().getItems().get(0);

            List<EndpointSubset> endpointSubsets = endpoint.getSubsets();
            if (endpointSubsets.isEmpty()) {  //no endpoints for the service
                if(log.isDebugEnabled()){
                    log.debug("Service:"+serviceName+"  no endpoints found for the service. EndpointSubset list is empty");
                }
                return null;
            }
            int endpointSubsetIndex = 0;
            for (EndpointSubset endpointSubset : endpointSubsets) {
                endpointSubsetIndex++;
                List<EndpointAddress> endpointAddresses = endpointSubset.getAddresses();
                if (endpointAddresses.isEmpty()) {  //no endpoints for the service
                    if(log.isDebugEnabled()){
                        log.debug("Service:"+serviceName+" EndpointSubsetIndex:"+endpointSubsetIndex+"  no endpoints found for the service. EndpointAddress list is empty.");
                    }
                    return null;
                }
                for (EndpointAddress endpointAddress : endpointAddresses) {
                    String podname = endpointAddress.getTargetRef().getName();
                    Pod pod = client.pods().inAnyNamespace().withField("metadata.name",podname).list().getItems().get(0); // same as comment 11 lines above
                    try {
                        url = new URL(protocol, pod.getStatus().getHostIP(), nodePort, "");
                        return url;
                    } catch (NullPointerException e) { //no pods available for this address
                        if(log.isDebugEnabled()){
                            log.debug("Service:"+serviceName+"  no pod available for the service");
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        } else {    // namespace given
            int portNumber = servicePort.getNodePort();
            Endpoints endpoint = client.endpoints().inNamespace(namespace).withName(serviceName).get();
            List<EndpointSubset> endpointSubsets = endpoint.getSubsets();
            if (endpointSubsets.isEmpty()) {  //no endpoints for the service
                if(log.isDebugEnabled()){
                    log.debug("Service:"+serviceName+"  no endpoints found for the service. EndpointSubset list is empty");
                }
                return null;
            }
            for (EndpointSubset endpointSubset : endpointSubsets) {
                List<EndpointAddress> endpointAddresses = endpointSubset.getAddresses();
                if (endpointAddresses.isEmpty()) {  //no endpoints for the service
                    if(log.isDebugEnabled()){
                        log.debug("Service:"+serviceName+"  no endpoints found for the service. EndpointAddress list is empty.");
                    }
                    return null;
                }
                for (EndpointAddress endpointAddress : endpointAddresses) {
                    String podname = endpointAddress.getTargetRef().getName();
                    Pod pod = client.pods().inNamespace(namespace).withName(podname).get();
                    try {
                        url = new URL(protocol, pod.getStatus().getHostIP(), portNumber, "");
                        return url;
                    } catch (NullPointerException e) { //no pods available for this address
                        if(log.isDebugEnabled()){
                            log.debug("Service:"+serviceName+"  no pod available for the service");
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }



}
