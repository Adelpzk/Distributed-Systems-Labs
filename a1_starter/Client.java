import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Client implements Runnable {
    private String host;
    private int port;
    private String[] passwords;

    public Client(String host, int port, String[] passwords) {
        this.host = host;
        this.port = port;
        this.passwords = passwords;
    }

    @Override
    public void run() {
        try {
            TSocket sock = new TSocket(host, port);
            TTransport transport = new TFramedTransport(sock);
            TProtocol protocol = new TBinaryProtocol(transport);
            BcryptService.Client client = new BcryptService.Client(protocol);
            transport.open();

            List<String> passwordList = new ArrayList<>();
            for (String password : passwords) {
                passwordList.add(password);
            }

            List<String> hash = client.hashPassword(passwordList, (short) 10);
            client.checkPassword(passwordList, hash);
            
            transport.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        int numThreads = 4; // change this to change the amount of client threads

        if (args.length < 3) {
            System.err.println("Usage: java ClientThread FE_host FE_port password1 [password2 ...]");
            System.exit(-1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String[] passwords = new String[args.length - 2];
        System.arraycopy(args, 2, passwords, 0, args.length - 2);

        // Create a thread pool with four threads
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // Submit tasks to the thread pool
        for (int i = 0; i < numThreads; i++) {
            executor.submit(new Client(host, port, passwords));
        }

        // Shutdown the thread pool after all tasks are complete
        executor.shutdown();

        try {
            // Wait for all tasks to complete or timeout after a certain period
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Execution time: " + executionTime + " milliseconds");
    }
}
