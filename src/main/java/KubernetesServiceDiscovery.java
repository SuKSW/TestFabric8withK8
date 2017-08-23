import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;


class KubernetesServiceDiscovery extends ServiceDiscovery {
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
            this.client = new DefaultOpenShiftClient(buildConfig(this.globalEndpoint));
            //this.client = new DefaultKubernetesClient(buildConfig(this.globalEndpoint));
            this.serviceType = defaultServiceType;
        } catch (KubernetesClientException e) {
            e.printStackTrace();
        }

    }

    KubernetesServiceDiscovery(String globalEndpoint, ServiceType serviceType) throws Exception {
        super(globalEndpoint);
        try {
            this.client = new DefaultOpenShiftClient(buildConfig(this.globalEndpoint));
            //this.client = new DefaultKubernetesClient(buildConfig(this.globalEndpoint));
            this.serviceType = serviceType;
        } catch (KubernetesClientException e) {
            e.printStackTrace();
        }
    }

    private Config buildConfig(String globalEndpoint){
        kubeServDiscConfig = new KubeServiceDiscoveryConfig();
        System.setProperty("kubernetes.auth.tryKubeConfig", "false");
        System.setProperty("kubernetes.auth.tryServiceAccount", "true");
        Config config = null;
        if(kubeServDiscConfig.isInsidePod()){
            try {
                config = new ConfigBuilder().withMasterUrl(globalEndpoint).withTrustCerts(true).withClientCertFile(kubeServDiscConfig.getClientCertLocation()).withOauthToken(new String(Files.readAllBytes(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/token")))).build();
            } catch (IOException e) {
                log.error("Token file not found");
                e.printStackTrace();
            }
        }else{
            config = new ConfigBuilder().withMasterUrl(globalEndpoint).withTrustCerts(true).withClientCertFile(kubeServDiscConfig.getClientCertLocation()).withOauthToken(kubeServDiscConfig.getServiceAccountToken()).build();
        }
        return config;
    }


    @Override
    JSONObject listServices() throws MalformedURLException{
        log.debug("Looking for services in all namespaces");
        try{
            ServiceList services = client.services().inAnyNamespace().list();
            addServicesToJson(services, null);
        }catch (KubernetesClientException e){
            e.printStackTrace();
        }
        return super.listServices();
    }

    @Override
    JSONObject listServices(String namespace) throws MalformedURLException{
        log.debug("Looking for services in namespace "+namespace);
        try {
            ServiceList services = client.services().inNamespace(namespace).list();
            addServicesToJson(services, namespace);
        }catch (KubernetesClientException e){
            e.printStackTrace();
        }
        return super.listServices(namespace);
    }

    @Override
    JSONObject listServices(String namespace, HashMap<String, String> criteria) throws MalformedURLException{
        log.debug("Looking for services, with the specified labels, in namespace "+namespace);
        try{
            ServiceList services = client.services().inNamespace(namespace).withLabels(criteria).list();
            addServicesToJson(services, namespace);
        }catch (KubernetesClientException e){
            e.printStackTrace();
        }
        return super.listServices(namespace, criteria);
    }

    @Override
    JSONObject listServices(HashMap<String, String> criteria) throws MalformedURLException{
        log.debug("Looking for services, with the specified labels, in all namespaces");
        try{
            ServiceList services = client.services().withLabels(criteria).list();
            addServicesToJson(services,null);
        }catch (KubernetesClientException e){
            e.printStackTrace();
        }
        return super.listServices(criteria);
    }



    private void addServicesToJson(ServiceList services, String filerNamespace) throws MalformedURLException {
        JSONArray servicesJsonArray = new JSONArray();
        List<Service> serviceItems = services.getItems();
        for (Service service : serviceItems) {
            String serviceName = service.getMetadata().getName();
            ServiceSpec serviceSpec = service.getSpec();
            JSONArray portsJsonArray = new JSONArray();
            for (ServicePort servicePort : serviceSpec.getPorts()) {
                String protocol = servicePort.getName();
                if (protocol !=null && (protocol.equals("http") || protocol.equals("https") ||
                                protocol.equals("ftp") || protocol.equals("irc"))) {
                    JSONArray urlsJsonArray = createUrlsJsonArray(serviceName, protocol, filerNamespace,
                            servicePort, service);
                    portsJsonArray.put(createPortJsonObject(servicePort.getPort(),urlsJsonArray));
                }else if(log.isDebugEnabled()){
                    log.debug("Service:"+serviceName+"  Namespace:"+service.getMetadata().getNamespace()
                            +"  Port:"+servicePort.getPort()+"/"+servicePort.getProtocol()
                            +"  app protocol not defined");
                }
            }
            servicesJsonArray.put(createServiceJsonObject(serviceName,portsJsonArray));
        }
        this.servicesJson = new JSONObject().put("services",servicesJsonArray);
    }


    private JSONArray createUrlsJsonArray(String serviceName, String protocol,
                                          String filerNamespace, ServicePort servicePort,
                                          Service service) throws MalformedURLException {
        JSONArray urlsJsonArray = new JSONArray();
        ServiceSpec serviceSpec = service.getSpec();
        if(kubeServDiscConfig.isInsidePod()){
            //ClusterIP Service
            URL clusterIPServiceURL = new URL(protocol, serviceSpec.getClusterIP(), servicePort.getPort(), "");
            urlsJsonArray.put(createUrlJsonObject("ClusterIP", clusterIPServiceURL));

            //ExternalName Service - todo: check how the protocol works
            if(serviceSpec.getType().equals("ExternalName")){
                String externalName = (String)service.getSpec().getAdditionalProperties().get("externalName");
                URL externalNameServiceURL = new URL(kubeServDiscConfig.getExternalNameDefaultProtocol() + "://" + externalName);
                urlsJsonArray.put(createUrlJsonObject("ExternalName", externalNameServiceURL));
            }
        }
        //NodePort Service
        if(!service.getSpec().getType().equals("ClusterIP")) {
            URL nodePortServiceURL = findNodePortServiceURLForAPort(filerNamespace, serviceName, servicePort);
            if (nodePortServiceURL != null) {
                urlsJsonArray.put(createUrlJsonObject("NodePort", nodePortServiceURL));
            }
        }
        //LoadBlancer Service
        if(service.getSpec().getType().equals("LoadBalancer")) {
            List<LoadBalancerIngress> loadBalancerIngresses = service.getStatus().getLoadBalancer().getIngress();
            if (!loadBalancerIngresses.isEmpty()) {
                URL loadBalancerServiceURL = new URL(protocol,
                        loadBalancerIngresses.get(0).getIp(), servicePort.getPort(), "");
                urlsJsonArray.put(createUrlJsonObject("LoadBalancer", loadBalancerServiceURL));
            } else if (log.isDebugEnabled()) {
                log.debug("Service:" + serviceName + "  Namespace:" + service.getMetadata().getNamespace()
                        + "  Port:" + servicePort.getPort() + "/" + servicePort.getProtocol()
                        + "  has no loadbalancer ingresses available");
            }
        }
        //ExternalName - Special case. Not managed by Kubernetes but the cluster administrator.
        List<String> specialExternalIps = service.getSpec().getExternalIPs();
        if (!specialExternalIps.isEmpty()) {
            URL externalIpServiceURL = new URL(protocol, specialExternalIps.get(0),servicePort.getPort(), "");
            urlsJsonArray.put(createUrlJsonObject("ExternalIP", externalIpServiceURL));
        }
        return urlsJsonArray;
    }


    private JSONObject createUrlJsonObject(String type, URL serviceUrl){
        JSONObject nodePortUrlJsonObject = new JSONObject();
        nodePortUrlJsonObject.put("type",type);
        nodePortUrlJsonObject.put("url",serviceUrl);
        return nodePortUrlJsonObject;
    }

    private JSONObject createServiceJsonObject(String serviceName, JSONArray ports){
        JSONObject serviceJsonObject = new JSONObject();
        serviceJsonObject.put("serviceName",serviceName);
        serviceJsonObject.put("ports",ports);
        return serviceJsonObject;
    }

    private JSONObject createPortJsonObject(int port, JSONArray urls){
        JSONObject portJson = new JSONObject();
        portJson.put("port",port);
        portJson.put("urls",urls);
        return portJson;
    }

    private URL findNodePortServiceURLForAPort(String filerNamespace, String serviceName,
                                               ServicePort servicePort) throws MalformedURLException {
        URL url;
        String protocol = servicePort.getName();    //kubernetes gives only tcp/udp but we need http/https
        int nodePort = servicePort.getNodePort();
        Endpoints endpoint = findEndpoint(filerNamespace,serviceName);
        List<EndpointSubset> endpointSubsets = endpoint.getSubsets();
        if (endpointSubsets.isEmpty()) {
            if(log.isDebugEnabled()){
                log.debug("Service:"+serviceName
                        +"  no endpoints found for the service. EndpointSubset array is empty");
            }
            return null;
        }
        int endpointSubsetIndex = 0;
        for (EndpointSubset endpointSubset : endpointSubsets) {
            endpointSubsetIndex++;
            List<EndpointAddress> endpointAddresses = endpointSubset.getAddresses();
            if (endpointAddresses.isEmpty()) {  //no endpoints for the service
                if(log.isDebugEnabled()){
                    log.debug("Service:"+serviceName+" EndpointSubsetIndex:"+endpointSubsetIndex
                            +"  no endpoints found for the service. EndpointAddress array is empty.");
                }
                return null;
            }
            for (EndpointAddress endpointAddress : endpointAddresses) {
                String podName = endpointAddress.getTargetRef().getName();
                Pod pod = findPod(filerNamespace,podName);
                try {
                    url = new URL(protocol, pod.getStatus().getHostIP(), nodePort, "");
                    return url;
                } catch (NullPointerException e) { //no pods available for this address
                    if(log.isDebugEnabled()){
                        log.debug("Service:"+serviceName +"  Pod "+podName+"  not available");
                    }
                }
            }
        }
        return null;
    }

    private Endpoints findEndpoint(String filerNamespace, String serviceName){
        Endpoints endpoint;
        if(filerNamespace==null){
            //Below, method ".inAnyNamespace()" did not support ".withName()". Therefore ".withField()" is used.
            //It returns a single item list which has the only endpoint created for the service.
            endpoint = client.endpoints().inAnyNamespace()
                    .withField("metadata.name",serviceName).list().getItems().get(0);
        }else {
            endpoint = client.endpoints().inNamespace(filerNamespace).withName(serviceName).get();
        }
        return endpoint;
    }

    private Pod findPod(String filerNamespace, String podName){
        Pod pod;
        if(filerNamespace==null){
            pod = client.pods().inAnyNamespace()
                    .withField("metadata.name",podName).list().getItems().get(0);
        }else {
            pod = client.pods().inNamespace(filerNamespace).withName(podName).get();
        }
        return pod;
    }

}
