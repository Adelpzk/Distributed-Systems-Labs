import java.io.*;
import java.util.*;

import org.apache.thrift.*;
import org.apache.thrift.server.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.protocol.*;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.*;
import org.apache.curator.*;
import org.apache.curator.retry.*;
import org.apache.curator.framework.*;

import org.apache.log4j.*;

public class StorageNode {
    static Logger log;

    static String host;
    static String port;
    static String zkConnectString;
    static String zkNode;
    static volatile boolean isPrimary;

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        log = Logger.getLogger(StorageNode.class.getName());

        if (args.length != 4) {
            System.err.println("Usage: java StorageNode host port zkconnectstring zknode");
            System.exit(-1);
        }

        host = args[0];
        port = args[1];
        zkConnectString = args[2];
        zkNode = args[3];

        CuratorFramework curClient = CuratorFrameworkFactory.builder()
                .connectString(args[2])
                .retryPolicy(new RetryNTimes(10, 1000))
                .connectionTimeoutMs(1000)
                .sessionTimeoutMs(10000)
                .build();

        curClient.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                curClient.close();
            }
        });

        KeyValueHandler keyValueHandler = new KeyValueHandler(args[0], Integer.parseInt(args[1]), curClient, args[3]);

        KeyValueService.Processor<KeyValueService.Iface> processor = new KeyValueService.Processor<>(
                keyValueHandler);
        TServerSocket socket = new TServerSocket(Integer.parseInt(args[1]));
        TThreadPoolServer.Args sargs = new TThreadPoolServer.Args(socket);
        sargs.protocolFactory(new TBinaryProtocol.Factory());
        sargs.transportFactory(new TFramedTransport.Factory());
        sargs.processorFactory(new TProcessorFactory(processor));
        sargs.maxWorkerThreads(64);
        TServer server = new TThreadPoolServer(sargs);
        log.info("Launching server");

        new Thread(new Runnable() {
            public void run() {
                server.serve();
            }
        }).start();

        // Create an ephemeral sequential node in ZooKeeper
        String createdNodePath = curClient.create()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(zkNode + "/node-", (host + ":" + port).getBytes());
        System.out.println("Created znode: " + createdNodePath);

        while (true) {
            // continuosly running loop
            // in here we keep checking to determine the role of this server, whether it is a primary node or a back up node
            determineRole(curClient, zkNode, createdNodePath, keyValueHandler);
            Thread.sleep(1000);
        }
    }

    private static void determineRole(CuratorFramework curClient, String zkNode, String createdNodePath, KeyValueHandler keyValueHandler)
            throws Exception {
        List<String> children = curClient.getChildren().forPath(zkNode);
        Collections.sort(children);
        System.out.println("Children nodes: " + children);

        String primaryNode = children.get(0);
        // if this node is supposed to be primary node
        if (createdNodePath.endsWith(primaryNode)) {
            // but currently is not, then update it to be the primary node
            if (!isPrimary) {
                isPrimary = true;
                keyValueHandler.setPrimary(true);
                log.info("This node is now the primary: " + createdNodePath);
                System.out.println("This node is now the primary: " + createdNodePath + "\n\n");
            }
        } 
        // if this node is not a primary node
        else {
            // but currently it is set to a primary node, update it to the backup/not primary node
            if (isPrimary) {
                isPrimary = false;
                keyValueHandler.setPrimary(false);
                log.info("This node is now a backup: " + createdNodePath);
                System.out.println("This node is now a backup: " + createdNodePath + "\n\n");
                // calling the syncFromPrimary method to perform an initial data synchronization from the primary node.
                syncFromPrimary(curClient, zkNode, keyValueHandler, children.get(0));
            }
            System.out.println("This node is Backup: " + createdNodePath + "\n");
        }
        
    }

    private static void syncFromPrimary(CuratorFramework curClient, String zkNode, KeyValueHandler keyValueHandler, String primaryNode) throws Exception {
        byte[] primaryData = curClient.getData().forPath(zkNode + "/" + primaryNode);
        String strPrimaryData = new String(primaryData);
        String[] primary = strPrimaryData.split(":");
        String primaryHost = primary[0];
        int primaryPort = Integer.parseInt(primary[1]);

        TSocket sock = new TSocket(primaryHost, primaryPort);
        TTransport transport = new TFramedTransport(sock);
        transport.open();
        TProtocol protocol = new TBinaryProtocol(transport);
        KeyValueService.Client primaryClient = new KeyValueService.Client(protocol);

        // get all key-value pairs from the primary
        Map<String, String> data = primaryClient.getAllData();
        // load the map with data recieved from primary node
        keyValueHandler.loadData(data);

        transport.close();
        System.out.println("Initial data sync from primary completed" + "\n\n");
    }
}
