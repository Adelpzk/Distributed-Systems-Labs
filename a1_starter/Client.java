import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Client implements Runnable {
    private String host;
    private int port;
    private List<String> passwords;

    private static final String CHAR_SET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[]{}|;:,.<>?";
    private static final int PASSWORD_LENGTH = 1024;


    public Client(String host, int port, List<String> passwords) {
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

            List<String> hash;
            int loops = 1; 

            for (int i=0; i<loops; i++) {
                hash = client.hashPassword(passwordList, (short) 4);
                client.checkPassword(passwordList, hash);
            }
            
            
            transport.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String generatePassword(SecureRandom random, int length) {
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHAR_SET.length());
            password.append(CHAR_SET.charAt(index));
        }
        return password.toString();
    }

    private static void job(String[] args, int batchSize, int numThreads) {
        long startTime = System.currentTimeMillis();

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        SecureRandom random = new SecureRandom();
        List<String> passwords = new ArrayList<String>();

        for (int i = 0; i < 16; i++) {
            String password = generatePassword(random, PASSWORD_LENGTH);
            passwords.add(password);
        }

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
        System.out.println(numThreads + " threads w/ batch size " + batchSize + ": " + executionTime + " milliseconds");
    }

    public static void main(String[] args) {
        job(args, 16, 1);
        job(args, 1, 16);
        job(args, 4, 4);

        // long startTime = System.currentTimeMillis();
        // int numThreads = 4; // change this to change the amount of client threads

        // if (args.length < 3) {
        //     System.err.println("Usage: java ClientThread FE_host FE_port password1 [password2 ...]");
        //     System.exit(-1);
        // }

        // String host = args[0];
        // int port = Integer.parseInt(args[1]);
        // // String[] passwords = new String[args.length - 2];
        // // System.arraycopy(args, 2, passwords, 0, args.length - 2);

        // SecureRandom random = new SecureRandom();
        // List<String> passwords = new ArrayList<String>();

        // for (int i = 0; i < 16; i++) {
        //     String password = generatePassword(random, PASSWORD_LENGTH);
        //     passwords.add(password);
        // }

        // // Create a thread pool with four threads
        // ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // // Submit tasks to the thread pool
        // for (int i = 0; i < numThreads; i++) {
        //     executor.submit(new Client(host, port, passwords));
        // }

        // // Shutdown the thread pool after all tasks are complete
        // executor.shutdown();

        // try {
        //     // Wait for all tasks to complete or timeout after a certain period
        //     executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }

        // long endTime = System.currentTimeMillis();
        // long executionTime = endTime - startTime;
        // System.out.println("Execution time: " + executionTime + " milliseconds");
    }
}
