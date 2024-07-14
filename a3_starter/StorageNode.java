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

        // Create an ephemeral sequential node in ZooKeeper
        String createdNodePath = curClient.create()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(zkNode + "/node-", (host + ":" + port).getBytes());

        KeyValueHandler keyValueHandler = new KeyValueHandler(args[0], Integer.parseInt(args[1]), curClient, args[3]);

        KeyValueService.Processor<KeyValueService.Iface> processor = new KeyValueService.Processor<>(keyValueHandler);
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
        
        List<String> children = new ArrayList<>();

        while (children.size() == 0) {
            curClient.sync();
            children = curClient.getChildren().forPath(zkNode);
        }
        
        Collections.sort(children);
    
        // Theres only one node, must be the primary
        if (children.size() == 1) {
            return;
        }

        // Get primary host and port
        byte[] primaryData = curClient.getData().forPath(zkNode + "/" + children.get(children.size() - 2));
        String strPrimaryData = new String(primaryData);
        String[] primary = strPrimaryData.split(":");
        String primaryHost = primary[0];
        int primaryPort = Integer.parseInt(primary[1]);

        // Get the backup host and port
        byte[] backupData = curClient.getData().forPath(zkNode + "/" + children.get(children.size() - 1));
        String strBackupData = new String(backupData);
        String[] backup = strBackupData.split(":");
        String backupHost = backup[0];
        int backupPort = Integer.parseInt(backup[1]);

        // Connect to primary
        TSocket sock = new TSocket(primaryHost, primaryPort);
        TTransport transport = new TFramedTransport(sock);
        transport.open();
        TProtocol protocol = new TBinaryProtocol(transport);
        KeyValueService.Client primaryClient = new KeyValueService.Client(protocol);
        
        // Continually ping the primary 
        while (true) {
            try {
                Thread.sleep(100);
                // This doesnt do anything
                primaryClient.setPrimary(true);
                continue;
            } catch (Exception e) {
                // Primary is dead
                break;
            }
        }

        // Delete the primary from zk
        curClient.delete().forPath(zkNode + "/" + children.get(0));

        // Backup becomes new primary
        sock = new TSocket(backupHost, backupPort);
        transport = new TFramedTransport(sock);
        transport.open();
        protocol = new TBinaryProtocol(transport);
        KeyValueService.Client backupClient = new KeyValueService.Client(protocol);
        
        backupClient.setPrimary(true);

        transport.close();
    }
}
