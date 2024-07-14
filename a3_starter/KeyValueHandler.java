import java.util.*;
import java.util.concurrent.*;

import org.apache.thrift.*;
import org.apache.thrift.transport.*;
import org.apache.zookeeper.*;
import org.apache.thrift.protocol.*;

import org.apache.curator.framework.*;
import org.apache.curator.framework.api.*;

import java.util.concurrent.locks.*;
import com.google.common.util.concurrent.Striped;

public class KeyValueHandler implements KeyValueService.Iface, CuratorWatcher {
    private Map<String, String> myMap;
    private CuratorFramework curClient;
    private String zkNode;
    private String host;
    private int port;
    private volatile boolean isPrimary = false;
    private ReentrantLock copyLock = new ReentrantLock();
    private Striped<Lock> stripedLock = Striped.lock(64);
    private volatile ConcurrentLinkedQueue<KeyValueService.Client> backupPool = null;

    public KeyValueHandler(String host, int port, CuratorFramework curClient, String zkNode) throws Exception {
        this.host = host;
        this.port = port;
        this.curClient = curClient;
        this.zkNode = zkNode;
        myMap = new ConcurrentHashMap<String, String>();   

        curClient.sync();
        List<String> children = curClient.getChildren().usingWatcher(this).forPath(zkNode);

        // Assign primary or backup
        if (children.size() == 1) {
            // System.out.println("This is Primary");
            this.isPrimary = true;
        } else {
            Collections.sort(children);
            
            byte[] primaryData = curClient.getData().forPath(zkNode + "/" + children.get(children.size() - 2));
            String strPrimaryData = new String(primaryData);
            String[] primary = strPrimaryData.split(":");
            String primaryHost = primary[0];
            int primaryPort = Integer.parseInt(primary[1]);

            // Check if this is primary
            if (primaryHost.equals(host) && primaryPort == port) {
                // System.out.println("This is Primary");
                this.isPrimary = true;
            } else {
                // System.out.println("This is Backup");
                this.isPrimary = false;
            }
        }
    }

    @Override
    public String get(String key) throws org.apache.thrift.TException {
        if (isPrimary == false) {
            throw new org.apache.thrift.TException("Backup should not get");
        }
        
        String ret = myMap.get(key);
        if (ret == null)
            return "";
        else
            return ret;
    }

    @Override
    public void put(String key, String value) throws org.apache.thrift.TException {
        Lock lock = stripedLock.get(key);
        lock.lock();

        // Check copy lock. Prevent puts while copying the data
        while (copyLock.isLocked());
        
        try {
            myMap.put(key, value);
            
            // we check if this node (server) is a primary server, then we want to also do the put operation on the backup node
            if (isPrimary && this.backupPool != null) {
                KeyValueService.Client backupClient = null;

                while(backupClient == null) {
                    backupClient = backupPool.poll();
                }
    
                backupClient.put(key, value);

                this.backupPool.offer(backupClient);
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.backupPool = null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setPrimary(boolean isPrimary) {
        // this method is used to update, keep track of the primary node
        this.isPrimary = isPrimary;
    }

    @Override
    public void loadData(Map<String, String> data) {
        // updates a map with the passed in map.
        this.myMap = new ConcurrentHashMap<String, String>(data); 
        // System.out.println("Copied to backup!");
    }

    // Called whenever a change is detected in the znode tree
    synchronized public void process(WatchedEvent event) throws Exception {
        // System.out.println("Event happened");
        curClient.sync();
        List<String> children = curClient.getChildren().usingWatcher(this).forPath(zkNode);

        if (children.size() == 1) {
            // System.out.println("This is primary");
            this.isPrimary = true;
            return;
        }
        
        Collections.sort(children);
        byte[] backupData = curClient.getData().forPath(zkNode + "/" + children.get(children.size() - 1));
        String strBackupData = new String(backupData);
        String[] backup = strBackupData.split(":");
        String backupHost = backup[0];
        int backupPort = Integer.parseInt(backup[1]);

        // check if this is primary
        if (backupHost.equals(host) && backupPort == port) {
            // System.out.println("This is backup");
            this.isPrimary = false;
        } else {
            // System.out.println("This is primary");
            this.isPrimary = true;
        }
        
        // New primary needs to copy data to new backup
        if (this.isPrimary && this.backupPool == null) {
            // System.out.println("Initiating copy to backup");
            KeyValueService.Client backupClient = null;
            
            while(backupClient == null) {
                try {
                    TSocket sock = new TSocket(backupHost, backupPort);
                    TTransport transport = new TFramedTransport(sock);
                    transport.open();
                    TProtocol protocol = new TBinaryProtocol(transport);
                    backupClient = new KeyValueService.Client(protocol);
                } catch (Exception e) {
                    // System.out.println("Can't reach a backup. Retrying...");
                }
            }

            // Copy data to new backup    
            copyLock.lock();

            backupClient.loadData(this.myMap);

            // Create backup pool for new primary
            this.backupPool = new ConcurrentLinkedQueue<KeyValueService.Client>();

            for(int i = 0; i < 32; i++) {
                TSocket sock = new TSocket(backupHost, backupPort);
                TTransport transport = new TFramedTransport(sock);
                transport.open();
                TProtocol protocol = new TBinaryProtocol(transport);
        
                this.backupPool.add(new KeyValueService.Client(protocol));
            }

            copyLock.unlock();
        }
        else {
            this.backupPool = null;
        }
    }
}
