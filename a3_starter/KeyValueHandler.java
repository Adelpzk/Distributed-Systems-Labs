import java.util.*;
import java.util.concurrent.*;

import org.apache.thrift.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.protocol.*;

import org.apache.curator.framework.*;

public class KeyValueHandler implements KeyValueService.Iface {
    private Map<String, String> myMap;
    private CuratorFramework curClient;
    private String zkNode;
    private String host;
    private int port;
    private volatile boolean isPrimary;

    public KeyValueHandler(String host, int port, CuratorFramework curClient, String zkNode) {
        this.host = host;
        this.port = port;
        this.curClient = curClient;
        this.zkNode = zkNode;
        myMap = new ConcurrentHashMap<String, String>();    
    }

    @Override
    public String get(String key) throws org.apache.thrift.TException {
        String ret = myMap.get(key);
        System.out.println("GET: " + key + " -> " + ret);
        if (ret == null)
            return "";
        else
            return ret;
    }

    @Override
    public void put(String key, String value) throws org.apache.thrift.TException {
        myMap.put(key, value);
        System.out.println("PUT: " + key + " -> " + value);
        // we check if this node (server) is a primary server, then we want to also do the put operation on the backup node
        if (isPrimary) {
            // takes care of putting this key, val in the backup node as well.
            replicatePutToBackups(key, value);
        }
    }

    @Override
    public void setPrimary(boolean isPrimary) {
        // this method is used to update, keep track of the primary node
        this.isPrimary = isPrimary;
        System.out.println("Set primary status to: " + isPrimary + "\n\n\n");
    }

    @Override
    public Map<String, String> getAllData() {
        // gets all the data in the current hashmap and return a copy of it.
        System.out.println("Getting all data for replication \n\n");
        return new HashMap<>(myMap); // Return a copy to avoid concurrency issues
    }

    @Override
    public void loadData(Map<String, String> data) {
        // updates a map with the passed in map.
        myMap.putAll(data);
        System.out.println("Loaded data: " + data);
    }

    private void replicatePutToBackups(String key, String value) {
        try {
            // this first part is just finding the host and port number of the backup node
            List<String> children = curClient.getChildren().forPath(zkNode);
            Collections.sort(children);
            for (int i = 1; i < children.size(); i++) {
                String backupNode = children.get(i);
                byte[] backupData = curClient.getData().forPath(zkNode + "/" + backupNode);
                String strBackupData = new String(backupData);
                String[] backup = strBackupData.split(":");
                String backupHost = backup[0];
                int backupPort = Integer.parseInt(backup[1]);
                
                //using thrift to communicate with the backup node
                TSocket sock = new TSocket(backupHost, backupPort);
                TTransport transport = new TFramedTransport(sock);
                transport.open();
                TProtocol protocol = new TBinaryProtocol(transport);
                KeyValueService.Client backupClient = new KeyValueService.Client(protocol);

                // calling the put method on the backup server/node
                System.out.println("Replicating PUT key: " + key + " value: " + value + " to backup: " + backupHost + ":" + backupPort + "\n\n");
                backupClient.put(key, value);

                transport.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
