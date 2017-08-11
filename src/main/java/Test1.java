import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.Iterator;
import java.util.List;


public class Test1 {

    private KubernetesClient client;
    //private OpenShiftClient client;

    Test1(String url){
        Config config = new ConfigBuilder().withMasterUrl(url).build();
        try {
            this.client = new DefaultKubernetesClient(config);
            //this.client = new DefaultOpenShiftClient(config);
        } catch (KubernetesClientException e) {
            e.printStackTrace();
        }
    }


    void justPrint(){
//        jp("External ips of hello-minikube service");
//        jp(client.services().inNamespace("default").withName("hello-minikube").get().getSpec().getExternalIPs());
//        jp();
//
//
//        jp("client.namespaces().withName(\"kube-public\").get()");
//        jp(client.namespaces().withName("kube-public").get());
//        jp();
//
//
//        jp("client.pods().withLabel(\"run\", \"hello-node\").list()");
//        jp(client.pods().withLabel("run", "hello-node").list());
//        jp();
//
//
//        jp("client.services().list()");
//        jp(client.services().list()
//        );
//        jp();
//
//
//        jp("client.services().inNamespace(\"default\").list()");
//        jp(client.services().inNamespace("default").list());
//        jp();
//
//
//        jp("client.services().inNamespace(\"default\").withLabel(\"run\", \"hello-node\").list()");
//        jp(client.services().inNamespace("default").withLabel("run", "hello-node").list());
//        jp();
//
//
//        jp("client.services().inNamespace(\"default\").withName(\"hello-minikube\").get()");
//        jp(client.services().inNamespace("default").withName("hello-minikube").get());
//        jp();

        jp("client.endpoints().withName(\"hello-minikube\").get();");
        jp(client.endpoints().inNamespace("default").withName("hello-node").get());
        jp();
    }

    void useServiceClass(){
        List<Service> serviceItems = client.services().list().getItems();
        for (Service service : serviceItems) {
            String name = service.getMetadata().getName();
            String type = service.getSpec().getType();
            String ip;

            jpi(name);
            jpi(type);
            switch (type){
                case "ClusterIp":
                    ip = "Internal ip";
                    jp(ip);
                    break;
                case "NodePort":
                    List<ServicePort> servicePorts = service.getSpec().getPorts();
                    for (ServicePort servicePort : servicePorts) {
                        String protocol = servicePort.getName();
                        if(protocol == null){
                            protocol = servicePort.getProtocol();
                        }
                        String port = servicePort.getNodePort().toString();
                        String podname = client.endpoints().inNamespace("default").withName(name).get().getSubsets().get(0).getAddresses().get(0).getTargetRef().getName();
                        Pod pod = client.pods().inNamespace("default").withName(podname).get();
                        try {
                            ip = protocol +"://"+ pod.getStatus().getHostIP() + ":" + port;
                        } catch (NullPointerException e) {
                            ip = "No pods available";
                        }
                        jp(ip);
                    }
                    break;
                case "LoadBalancer":
                    Iterator<LoadBalancerIngress> ingressIterator = service.getStatus().getLoadBalancer().getIngress().iterator();
                    if(ingressIterator.hasNext()) {
                        while (ingressIterator.hasNext()) {
                            ip = ingressIterator.next().getIp();
                            jp(ip);
                        }
                    }else {
                        ip = "No loadbalancer assigned";
                        jpi(ip);
                        List<ServicePort> servicePorts1 = service.getSpec().getPorts();
                        for (ServicePort servicePort : servicePorts1) {
                            String protocol = servicePort.getName();
                            if(protocol == null || protocol != "http" || protocol != "https" || protocol != "ftp" || protocol != "dns" || protocol != "irc" ){
                                protocol = servicePort.getProtocol();
                            }
                            String port = servicePort.getNodePort().toString();
                            String podname = client.endpoints().inNamespace("default").withName(name).get().getSubsets().get(0).getAddresses().get(0).getTargetRef().getName();
                            Pod pod = client.pods().inNamespace("default").withName(podname).get();
                            try {
                                ip = protocol +"://"+ pod.getStatus().getHostIP() + ":" + port;
                            } catch (NullPointerException e) {
                                ip = "No pods available";
                            }
                            jp(ip);
                        }

                    }
                    break;
                case "ExternalName":
                    ip = (String) service.getSpec().getAdditionalProperties().get("externalName");
                    jp(ip);
                    break;
                default:
                    ip = "Internal ip";
                    jp(ip);
                    break;
            }
        }
    }

    void useServiceClass1(String ns){

        //System.out.println(client.nodes().list());
        //EndpointsList eps = client.endpoints().list();
        List<Service> services = client.services().inNamespace(ns).list().getItems();
        //EndpointsList eps = client.endpoints().list();
        //System.out.println(KubernetesHelper.getServiceMap(client));
        for (Service service : services) {
            //System.out.println(KubernetesHelper.getServiceURL(client,service.getMetadata().getName(),ns,null,null,true));

            System.out.println(service.getMetadata().getName());

                //System.out.println(service.getSpec().getExternalIPs());

        }
        //System.out.println(client.services().inNamespace("default").withName("hello-minikube").get().getSpec());
        //System.out.println(client.services().inNamespace("default").withName("hello-minikube").get().getStatus());
    }

    void jp(Object ms){
        System.out.println(ms);
    }

    void jp(){
        System.out.println();
    }

    void jpi(Object ms){//just print inline
        System.out.print(ms);
        System.out.print("  ");
    }
}
