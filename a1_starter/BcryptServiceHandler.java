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
        if (FENode.BENodePort == null) {
            log.info("there are NO BE Nodes available. Passwords will be processed on FE :(");
            return hashPasswordsFE(passwords, logRounds);
        } else {
            log.info("Forwarding password hashing to BE node on port!!!" + FENode.BENodePort);
            // Forward tasks to the BE node
            return hashPasswordsOnBE(passwords, logRounds, FENode.BENodePort);
        }
    }

    private List<String> hashPasswordsFE(List<String> passwords, short logRounds) throws IllegalArgument { //helper function to hash passwords on the FE
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

    private List<String> hashPasswordsOnBE(List<String> passwords, short logRounds, int portBE) throws TException { //helper function to hash passwords in the BE
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

    @Override
    public void pingFE(int port) {
        System.out.println("Received ping from BE port " + port);
        FENode.saveBENode(port);
    }
}
