
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

public class Main {


    public static void main(String[] args) throws MalformedURLException {
        String master = "https://......../";

        KubernetesServiceDiscovery kubernetesServiceDiscovery = new KubernetesServiceDiscovery(master);
        HashMap<String,URL> serviceURLs = kubernetesServiceDiscovery.listServices("default");
        Iterator it = serviceURLs.entrySet().iterator();
        System.out.println();
        System.out.println();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            System.out.println(pair.getKey() + "  =  " + pair.getValue());
            it.remove();
        }
        //Test1 t1 = new Test1("https://.../");
        //t1.justPrint();
        //t1.useServiceClass1("default");
        //t1.useServiceClass();

        //URL ulr = new URL("tcp",".....",8443,"/");
        //t1.jp(ulr);

    }

}