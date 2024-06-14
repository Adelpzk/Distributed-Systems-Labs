import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.mindrot.jbcrypt.BCrypt;
import org.apache.thrift.TException;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TNonblockingTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.log4j.Logger;


public class BcryptServiceHandler implements BcryptService.Iface {
    static Logger log = Logger.getLogger(BcryptServiceHandler.class.getName());  //Added a logger to see if the processes were actually being forwarded to the BE
    private static AtomicInteger nodeIndex = new AtomicInteger(1);
    private static int threshold = 3;   // # of list elements to warrant a split

    public List<String> hashPassword(List<String> passwords, short logRounds) throws IllegalArgument, TException { 
        
        if(logRounds < 4 || logRounds > 31){
            throw new IllegalArgument("logRounds are out of Range");
        }

        if (FENode.BENodes.isEmpty()) { // no BE nodes available
            return hashPasswords(passwords, logRounds);
        }

        // split passwords among nodes
        if (passwords.size() >= threshold) {
            List<String> ret = new ArrayList<String>();
            try {
                CountDownLatch latch = new CountDownLatch(FENode.BENodes.size());
                List<HashCallback> callbacks = new ArrayList<HashCallback>();
                
                int nodeCount = FENode.BENodes.size();
                int pwPerNode = passwords.size() / (nodeCount + 1);
                int remainder = passwords.size() % (nodeCount + 1);
                int startIndex = pwPerNode;
                
                for (int i = 0; i < FENode.BENodes.size(); i++) {
                    int extraPassword = (i < remainder) ? 1 : 0;
                    int endIndex = startIndex + pwPerNode + extraPassword;
                    List<String> passwordsForNode = passwords.subList(startIndex, endIndex);
                    
                    callbacks.add(hashPasswordsAsync(passwordsForNode, logRounds, FENode.BENodes.get(i), latch));
                    startIndex = endIndex;
                }

                // System.out.println(passwords.subList(0, pwPerNode).toString());
                ret.addAll(hashPasswords(passwords.subList(0, pwPerNode), logRounds));

                latch.await();
                
                for (int i = 0; i < FENode.BENodes.size(); i++) {
                    ret.addAll(callbacks.get(i).res);
                }
            
            } catch (Exception e) {
                System.err.println(e);
            }
            
            return ret;
        }
        else {
            // round robin
            
            // Get the current node index
            int index = nodeIndex.getAndIncrement() % (FENode.BENodes.size() + 1);

            if (index == 0) { // FE node takes task
                return hashPasswords(passwords, logRounds);
            } else { // BE node takes task
                return hashPasswordsBE(passwords, logRounds, FENode.BENodes.get(index - 1));
            }
        }
        
    }

    private HashCallback hashPasswordsAsync(List<String> passwords, short logRounds, int portBE, CountDownLatch latch) {
        // System.out.println(passwords.toString());
        try {
            TNonblockingTransport transport = new TNonblockingSocket(FENode.hostname, portBE);
            TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
            BcryptService.AsyncClient client = new BcryptService.AsyncClient(protocolFactory, FENode.clientManager, transport);
            HashCallback cb = new HashCallback(latch, transport);
            
            client.hashPasswords(passwords, logRounds, cb);

            return cb;
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }

    // function that actually hashes the passwords
    public List<String> hashPasswords(List<String> passwords, short logRounds) throws IllegalArgument {
        if(logRounds < 4 || logRounds > 31){
            throw new IllegalArgument("logRounds are out of Range");
        }
        // log.info("Hashing passwords");

        List<String> ret = new ArrayList<>();
        try {
            for (String pwd : passwords) {
                String hash = BCrypt.hashpw(pwd, BCrypt.gensalt(logRounds));
                ret.add(hash);
            }
        } catch (Exception e) {
            throw new IllegalArgument(e.getMessage());
        }
        return ret;
    }

    // function that calls hashPasswords on a BE node
    private List<String> hashPasswordsBE(List<String> passwords, short logRounds, int portBE) throws TException {
        try { 
            if(logRounds < 4 || logRounds > 31){
                throw new IllegalArgument("logRounds are out of Range");
            }
            TSocket sock = new TSocket(FENode.hostname, portBE);
            TTransport transport = new TFramedTransport(sock);
            TProtocol protocol = new TBinaryProtocol(transport);
            BcryptService.Client client = new BcryptService.Client(protocol);

            transport.open();
            List<String> hashedPasswords = client.hashPasswords(passwords, logRounds);
            transport.close();

            return hashedPasswords;
        } catch (TException e) {
            throw new IllegalArgument(e.getMessage());  
        } 
    }

    public List<Boolean> checkPassword(List<String> passwords, List<String> hashes) throws IllegalArgument, TException {
        if (passwords.size() != hashes.size()){
            throw new IllegalArgument("Passwords and Hashes are of Unequal length.");
        }

        malformedHashCheck(hashes);

        if (FENode.BENodes.isEmpty()) { // no BE nodes available
            return checkPasswords(passwords, hashes);
        }

        // split passwords among nodes
        if (passwords.size() >= threshold) {
            List<Boolean> ret = new ArrayList<Boolean>();
            try {
                CountDownLatch latch = new CountDownLatch(FENode.BENodes.size());
                List<CheckCallback> callbacks = new ArrayList<CheckCallback>();
                
                int nodeCount = FENode.BENodes.size();
                int pwPerNode = passwords.size() / (nodeCount + 1);
                int remainder = passwords.size() % (nodeCount + 1);
                int startIndex = pwPerNode;
                
                for (int i = 0; i < FENode.BENodes.size(); i++) {
                    int extraPassword = (i < remainder) ? 1 : 0;
                    int endIndex = startIndex + pwPerNode + extraPassword;
                    List<String> passwordsForNode = passwords.subList(startIndex, endIndex);
                    List<String> hashesForNode = hashes.subList(startIndex, endIndex);
                    
                    callbacks.add(checkPasswordsAsync(passwordsForNode, hashesForNode, FENode.BENodes.get(i), latch));
                    startIndex = endIndex;
                }

                // System.out.println(passwords.subList(0, pwPerNode).toString());
                ret.addAll(checkPasswords(passwords.subList(0, pwPerNode), hashes.subList(0, pwPerNode)));

                latch.await();
                
                for (int i = 0; i < FENode.BENodes.size(); i++) {
                    ret.addAll(callbacks.get(i).res);
                }
            
            } catch (Exception e) {
                System.err.println(e);
            }
            
            return ret;
        } 
        else {
            // Get the current node index
            int index = nodeIndex.getAndIncrement() % (FENode.BENodes.size() + 1);
            
            if (index == 0) { // FE node takes task
                return checkPasswords(passwords, hashes);
            } else { // BE node takes task
                return checkPasswordsBE(passwords, hashes, FENode.BENodes.get(index - 1));
            }
        }

    }

    private CheckCallback checkPasswordsAsync(List<String> passwords, List<String> hashes, int portBE, CountDownLatch latch) {
        // System.out.println(passwords.toString());
        try {
            TNonblockingTransport transport = new TNonblockingSocket(FENode.hostname, portBE);
            TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
            BcryptService.AsyncClient client = new BcryptService.AsyncClient(protocolFactory, FENode.clientManager, transport);
            CheckCallback cb = new CheckCallback(latch, transport);
            
            client.checkPasswords(passwords, hashes, cb);

            return cb;
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }

    // function that actually checks passwords
    public List<Boolean> checkPasswords(List<String> passwords, List<String> hashes) throws IllegalArgument, TException {
        // log.info("Checking passwords");
        try {
            List<Boolean> ret = new ArrayList<>();
            for (int i = 0; i < passwords.size(); i++) {
                String pwd = passwords.get(i);
                String hsh = hashes.get(i);
                ret.add(BCrypt.checkpw(pwd, hsh));
            }
            return ret;
        } catch (Exception e) {
            throw new IllegalArgument(e.getMessage());  
        }
    }

    // function that calls checkPasswords on a BE node
    private List<Boolean> checkPasswordsBE(List<String> passwords, List<String> hashes, int portBE) throws IllegalArgument, TException {
        malformedHashCheck(hashes);
        try {
            TSocket sock = new TSocket(FENode.hostname, portBE);
            TTransport transport = new TFramedTransport(sock);
            TProtocol protocol = new TBinaryProtocol(transport);
            BcryptService.Client client = new BcryptService.Client(protocol);

            transport.open();
            List<Boolean> checkedPasswords = client.checkPasswords(passwords, hashes);
            transport.close();

            return checkedPasswords;
        } catch (Exception e) {
            throw new IllegalArgument(e.getMessage());  
        } 
    }

    private void malformedHashCheck(List<String> hashes) throws IllegalArgument, TException{
        for(int i = 0; i<= hashes.size()-1; i++){
            String hash = hashes.get(i);
            if(hash.length() != 60 || !hash.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$")){
                throw new IllegalArgument("Malformed hash: " + hash);
            }
        }
    }

    public void pingFE(int port) {
        // System.out.println("Received ping from BE port " + port);
        FENode.saveBENode(port);
    }
}
