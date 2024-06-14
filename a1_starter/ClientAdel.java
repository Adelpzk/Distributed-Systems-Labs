import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClientAdel implements Runnable {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static SecureRandom random = new SecureRandom();
    private String host;
    private int port;
    private static List<String> passwordList;

    public ClientAdel(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            TSocket sock = new TSocket(host, port);
            TTransport transport = new TFramedTransport(sock);
            TProtocol protocol = new TBinaryProtocol(transport);
            BcryptService.Client client = new BcryptService.Client(protocol);
            transport.open();
            System.out.println("Process_Starting");

            List<String> hash = client.hashPassword(passwordList, (short) 11);
            System.out.println("Hashing completed");

            List<Boolean> checkResults = client.checkPassword(passwordList, hash);
            System.out.println("Checking completed");

            transport.close();
        } catch (TException e) {
            e.printStackTrace();
        }
    }

    private static List<String> generateRandomPasswords(int length, int count) {
        List<String> passwords = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            passwords.add(generateRandomPassword(length));
        }
        return passwords;
    }

    private static String generateRandomPassword(int length) {
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return password.toString();
    }

    public static void main(String[] args) {
        if (args.length != 5) {
            System.err.println("Usage: java Client FE_host FE_port password_length batch_size num_threads");
            System.exit(-1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int passwordLength = Integer.parseInt(args[2]);
        System.out.println("Password Length: " + passwordLength);
        int batchSize = Integer.parseInt(args[3]);
        System.out.println("Batch Size: " + batchSize);
        int numThreads = Integer.parseInt(args[4]);
        passwordList = generateRandomPasswords(passwordLength, batchSize);
        System.out.println("Passwords Generated");

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            executor.submit(new ClientAdel(host, port));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Execution time: " + executionTime + " milliseconds");
    }
}