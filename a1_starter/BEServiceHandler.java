import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;
import org.apache.thrift.TException;
import org.apache.log4j.Logger;

public class BEServiceHandler implements BcryptService.Iface {
    static Logger log = Logger.getLogger(BcryptServiceHandler.class.getName()); //Added a logger to see if the processes were actually being forwarded to the BE

    @Override
    public List<String> hashPassword(List<String> passwords, short logRounds) throws IllegalArgument, TException {
        log.info("BENode (" + BENode.port + ") hashing password(s)");
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

    @Override
    public List<Boolean> checkPassword(List<String> passwords, List<String> hashes) throws IllegalArgument, TException {
        log.info("BENode (" + BENode.port + ") checking password(s)");
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
        // does nothing
    }
}
