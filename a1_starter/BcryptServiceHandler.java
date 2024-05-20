import java.util.ArrayList;
import java.util.List;
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

    @Override
    public List<String> hashPassword(List<String> passwords, short logRounds) throws IllegalArgument, TException {
        // if (FENode.BENodePort == null) {
        //     log.info("there are NO BE Nodes available. Passwords will be processed on FE :(");
        //     return hashPasswordsFE(passwords, logRounds);
        // } else {
        //     log.info("Forwarding password hashing to BE node on port!!!" + FENode.BENodePort);
        //     // Forward tasks to the BE node
        //     return hashPasswordsBE(passwords, logRounds, FENode.BENodePort);
        // }

        // round robin load balancing

        if (FENode.BENodes.size() == 0) { // no BE nodes available
            return hashPasswordsFE(passwords, logRounds);
        }
        else { // at least 1 BE node available
            if (FENode.counter == 0) {  // FE node takes task
                FENode.counter = 1;
                return hashPasswordsFE(passwords, logRounds);
            }
            else if (FENode.counter == 1) { // first or only BE node takes task
                FENode.counter = (FENode.counter + 1) % (FENode.BENodes.size() + 1);
                return hashPasswordsBE(passwords, logRounds, FENode.BENodes.get(0));   
            }
            else {  // second BE node takes task
                FENode.counter = 0;
                return hashPasswordsBE(passwords, logRounds, FENode.BENodes.get(1));   
            }
        }
    }

    private List<String> hashPasswordsFE(List<String> passwords, short logRounds) throws IllegalArgument { //helper function to hash passwords on the FE
        log.info("FE hashing passwords");
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

    private List<String> hashPasswordsBE(List<String> passwords, short logRounds, int portBE) throws TException { //helper function to hash passwords in the BE
        TSocket sock = new TSocket("localhost", portBE);
        TTransport transport = new TFramedTransport(sock);
        TProtocol protocol = new TBinaryProtocol(transport);
        BcryptService.Client client = new BcryptService.Client(protocol);

        transport.open();
        List<String> hashedPasswords = client.hashPassword(passwords, logRounds);
        transport.close();

        return hashedPasswords;
    }

    @Override
    public List<Boolean> checkPassword(List<String> passwords, List<String> hashes) throws IllegalArgument, TException {
        if (FENode.BENodes.size() == 0) { // no BE nodes available
            return checkPasswordsFE(passwords, hashes);
        }
        else { // at least 1 BE node available
            if (FENode.counter == 0) {
                FENode.counter = 1;
                return checkPasswordsFE(passwords, hashes);
            }
            else if (FENode.counter == 1) {
                FENode.counter = (FENode.counter + 1) % (FENode.BENodes.size() + 1);
                return checkPasswordsBE(passwords, hashes, FENode.BENodes.get(0));   
            }
            else {
                FENode.counter = 0;
                return checkPasswordsBE(passwords, hashes, FENode.BENodes.get(1));   
            }
        }
    }

    public List<Boolean> checkPasswordsFE(List<String> passwords, List<String> hashes) throws IllegalArgument, TException {
        log.info("FE hashing passwords");
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

    public List<Boolean> checkPasswordsBE(List<String> passwords, List<String> hashes, int portBE) throws IllegalArgument, TException {
        TSocket sock = new TSocket("localhost", portBE);
        TTransport transport = new TFramedTransport(sock);
        TProtocol protocol = new TBinaryProtocol(transport);
        BcryptService.Client client = new BcryptService.Client(protocol);

        transport.open();
        List<Boolean> checkedPasswords = client.checkPassword(passwords, hashes);
        transport.close();

        return checkedPasswords;
    }

    public void pingFE(int port) {
        System.out.println("Received ping from BE port " + port);
        FENode.saveBENode(port);
    }
}
