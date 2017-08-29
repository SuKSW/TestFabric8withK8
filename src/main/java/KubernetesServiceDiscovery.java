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
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
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


class KubernetesServiceDiscovery  extends ServiceDiscovery{
    /*
    *
    * ----------------------------------------------------------------
    * for a service to be discovred
    * port name must be : http/https
    *   //todo ---> if 443 https
    *
    *-----------------------------------------------------------------
    * viewing only service,endpoints,pods so to avoid viewing nodes
    *
    *
    * */

    private final Logger log  = LoggerFactory.getLogger(KubernetesServiceDiscovery.class);
    private KubeServiceDiscoveryConfig kubeServDiscConfig;


    //private OpenShiftClient client;
    private KubernetesClient client;


    KubernetesServiceDiscovery() throws Exception {
        try {
            //this.client = new DefaultOpenShiftClient(buildConfig());
            this.client = new DefaultKubernetesClient(buildConfig());
        } catch (KubernetesClientException e) {
            e.printStackTrace();
        }

    }


    private Config buildConfig(){
        kubeServDiscConfig = new KubeServiceDiscoveryConfig();
        System.setProperty("kubernetes.auth.tryKubeConfig", "false");
        System.setProperty("kubernetes.auth.tryServiceAccount", "true");
        ConfigBuilder configBuilder = new ConfigBuilder().withMasterUrl(kubeServDiscConfig.getMasterUrl()).withTrustCerts(true).withClientCertFile(kubeServDiscConfig.getClientCertLocation());
        Config config;
        if(kubeServDiscConfig.isInsidePod()){
           // config = new ConfigBuilder().withMasterUrl(masterUrl.toString()).build();
            try {
                config = configBuilder.withOauthToken(new String(Files.readAllBytes(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/token")))).build();
            } catch (IOException e) {
                config = null;
                log.error("Token file not found");
                e.printStackTrace();
            }
        }else{
            //config = new ConfigBuilder().withMasterUrl(kubeServDiscConfig.masterUrl).build();
            config = configBuilder.withOauthToken(kubeServDiscConfig.getServiceAccountToken()).build();
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


    private Boolean endpointsAvailable; //when false, will not look for NodePort urls for the remaining ports.

    private void addServicesToJson(ServiceList services, String filerNamespace) throws MalformedURLException {
        JSONArray servicesJsonArray = new JSONArray();
        List<Service> serviceItems = services.getItems();
        for (Service service : serviceItems) {
            String serviceName = service.getMetadata().getName();
            ServiceSpec serviceSpec = service.getSpec();
            JSONArray portsJsonArray = new JSONArray();
            endpointsAvailable = true;
            for (ServicePort servicePort : serviceSpec.getPorts()) {
                String protocol = servicePort.getName();
                if (protocol !=null && (protocol.equals("http") || protocol.equals("https"))) {
                    JSONArray urlsJsonArray = createUrlsJsonArray(serviceName, protocol, filerNamespace,
                            servicePort, service);
                    portsJsonArray.put(createPortJsonObject(servicePort.getPort(),urlsJsonArray));
                }else if(log.isDebugEnabled()){
                    log.debug("Service:{}  Port:{}/{}     Application level protocol not defined.",
                            serviceName, service.getMetadata().getNamespace(),
                            servicePort.getPort(), protocol);
                }
            }
            if(!portsJsonArray.isNull(0)) {
                servicesJsonArray.put(createServiceJsonObject(serviceName, portsJsonArray));
            }
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

            //ExternalName Service
            if(serviceSpec.getType().equals("ExternalName")){
                String externalName = (String)service.getSpec().getAdditionalProperties().get("externalName");
                URL externalNameServiceURL = new URL(protocol + "://" + externalName);
                urlsJsonArray.put(createUrlJsonObject("ExternalName", externalNameServiceURL));
            }
        }
        //NodePort Service
        if(!service.getSpec().getType().equals("ClusterIP") && endpointsAvailable) {
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
                log.debug("Service:{}  Namespace:{}  Port:{}/{} has no loadbalancer ingresses available.",
                        serviceName, service.getMetadata().getNamespace(),
                        servicePort.getPort(), protocol);
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


    private JSONObject createUrlJsonObject(String serviceType, URL serviceUrl){
        JSONObject nodePortUrlJsonObject = new JSONObject();
        nodePortUrlJsonObject.put("type",serviceType);
        nodePortUrlJsonObject.put("url",serviceUrl);
        return nodePortUrlJsonObject;
    }

    private JSONObject createPortJsonObject(int port, JSONArray urls){
        JSONObject portJsonObject = new JSONObject();
        portJsonObject.put("port",port);
        portJsonObject.put("urls",urls);
        return portJsonObject;
    }

    private JSONObject createServiceJsonObject(String serviceName, JSONArray ports){
        JSONObject serviceJsonObject = new JSONObject();
        serviceJsonObject.put("serviceName",serviceName);
        serviceJsonObject.put("ports",ports);
        return serviceJsonObject;
    }



    private URL findNodePortServiceURLForAPort(String filerNamespace, String serviceName,
                                               ServicePort servicePort) throws MalformedURLException {
        URL url;
        String protocol = servicePort.getName();    //kubernetes gives only tcp/udp but we need http/https
        int nodePort = servicePort.getNodePort();
        Endpoints endpoint = findEndpoint(filerNamespace,serviceName);
        List<EndpointSubset> endpointSubsets = endpoint.getSubsets();
        if(endpointSubsets == null) {
            //no endpoints for the service : when LoadBalancer type or pods not selected
            log.debug("Service:{}   No endpoints found for the service.", serviceName);
            endpointsAvailable = false;
            return null;
        }
        for (EndpointSubset endpointSubset : endpointSubsets) {
            List<EndpointAddress> endpointAddresses = endpointSubset.getAddresses();
            //System.out.println(serviceName);
            if (endpointAddresses.isEmpty()) {  //no endpoints for the service : when NodePort type
                log.debug("Service:{}   No endpoints found for the service.", serviceName);
                endpointsAvailable = false;
                return null;
            }
            for (EndpointAddress endpointAddress : endpointAddresses) {
                String podName = endpointAddress.getTargetRef().getName();
                Pod pod = findPod(filerNamespace,podName);
                try {
                    url = new URL(protocol, pod.getStatus().getHostIP(), nodePort, "");
                    return url;
                } catch (NullPointerException e) { //no pods available for this address
                    log.debug("Service:{}  Pod {}  not available", serviceName, podName);
                }
            }
        }
        return null;
    }

    private Endpoints findEndpoint(String filerNamespace, String serviceName){
        Endpoints endpoint;
        if(filerNamespace==null){
            //Below, method ".inAnyNamespace()" did not support ".withName()".
            //Therefore ".withField()" is used.
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
