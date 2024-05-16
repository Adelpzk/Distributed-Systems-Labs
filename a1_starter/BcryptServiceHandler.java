import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

public class BcryptServiceHandler implements BcryptService.Iface {
	public List<String> hashPassword(List<String> password, short logRounds) throws IllegalArgument, org.apache.thrift.TException
	{
		try {
			List<String> ret = new ArrayList<>();
			for (String pwd: password) {
				String hash = BCrypt.hashpw(pwd, BCrypt.gensalt(logRounds));
				ret.add(hash);
			}
			return ret;
		} catch (Exception e) {
			throw new IllegalArgument(e.getMessage());
		}
	}

	public List<Boolean> checkPassword(List<String> password, List<String> hash) throws IllegalArgument, org.apache.thrift.TException
	{
		try {
			List<Boolean> ret = new ArrayList<>();
			for (int i=0; i<password.size(); i++) {
				String pwd = password.get(i);
				String hsh = hash.get(i);
				ret.add(BCrypt.checkpw(pwd, hsh));
			}
			return ret;
		} catch (Exception e) {
			throw new IllegalArgument(e.getMessage());
		}
	}

	public void pingFE(int port) {
        System.out.println("Received ping from BE port " + port);
		FENode.saveBENode(port);
    }
}
