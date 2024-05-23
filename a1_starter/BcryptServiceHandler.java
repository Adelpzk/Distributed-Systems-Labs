import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.mindrot.jbcrypt.BCrypt;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.log4j.Logger;

public class BcryptServiceHandler implements BcryptService.Iface {
    static Logger log = Logger.getLogger(BcryptServiceHandler.class.getName());  //Added a logger to see if the processes were actually being forwarded to the BE
    private static AtomicInteger nodeIndex = new AtomicInteger(1);

    public List<String> hashPassword(List<String> passwords, short logRounds) throws IllegalArgument, TException { 
       if (FENode.BENodes.isEmpty()) { // no BE nodes available
            return hashPasswords(passwords, logRounds);
        }
        
        // Get the current node index
        int index = nodeIndex.getAndIncrement() % (FENode.BENodes.size() + 1);
        
        if (index == 0) { // FE node takes task
            return hashPasswords(passwords, logRounds);
        } else { // BE node takes task
            return hashPasswordsBE(passwords, logRounds, FENode.BENodes.get(index - 1));
        }
    }

    // function that actually hashes the passwords
    public List<String> hashPasswords(List<String> passwords, short logRounds) throws IllegalArgument {
        log.info("Hashing passwords");
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
            TSocket sock = new TSocket(FENode.hostname, portBE);
            TTransport transport = new TFramedTransport(sock);
            TProtocol protocol = new TBinaryProtocol(transport);
            BcryptService.Client client = new BcryptService.Client(protocol);

            transport.open();
            List<String> hashedPasswords = client.hashPasswords(passwords, logRounds);
            transport.close();

            return hashedPasswords;
        } catch (Exception e) {
            throw new IllegalArgument(e.getMessage());  
        } 
    }

    public List<Boolean> checkPassword(List<String> passwords, List<String> hashes) throws IllegalArgument, TException {
        if (FENode.BENodes.isEmpty()) { // no BE nodes available
            return checkPasswords(passwords, hashes);
        }
        
        // Get the current node index
        int index = nodeIndex.getAndIncrement() % (FENode.BENodes.size() + 1);
        
        if (index == 0) { // FE node takes task
            return checkPasswords(passwords, hashes);
        } else { // BE node takes task
            return checkPasswordsBE(passwords, hashes, FENode.BENodes.get(index - 1));
        }
    }

    // function that actually checks passwords
    public List<Boolean> checkPasswords(List<String> passwords, List<String> hashes) throws IllegalArgument, TException {
        log.info("Checking passwords");
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

    public void pingFE(int port) {
        System.out.println("Received ping from BE port " + port);
        FENode.saveBENode(port);
    }
}
