import java.util.List;
import java.util.ArrayList;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TFramedTransport;

public class Client {
    public static void main(String [] args) {
        if (args.length < 3) {
            System.err.println("Usage: java Client FE_host FE_port password1 [password2 ...]");
            System.exit(-1);
        }

        try {
            TSocket sock = new TSocket(args[0], Integer.parseInt(args[1]));
            TTransport transport = new TFramedTransport(sock);
            TProtocol protocol = new TBinaryProtocol(transport);
            BcryptService.Client client = new BcryptService.Client(protocol);
            transport.open();

            List<String> password = new ArrayList<>();
            List<String> password1 = new ArrayList<>();
            List<String> password2 = new ArrayList<>();

            for (int i = 2; i < args.length; i++) {
                password.add(args[i]);
                password2.add(args[i]);
                password1.add(args[i]);
            }
            
            List<String> hash = client.hashPassword(password, (short)10);
            List<String> hash1 = client.hashPassword(password1, (short)10);
            List<String> hash2 = client.hashPassword(password2, (short)10);


            // for (int i = 0; i < password.size(); i++) {
            //     System.out.println("Password: " + password.get(i));
            //     System.out.println("Hash: " + hash.get(i));
            // }

            // // Positive check: all passwords should match their hashes
            // System.out.println("Positive check: " + client.checkPassword(password, hash));

            // // Negative check: change the first hash and check
            // hash.set(0, "$2a$14$reBHJvwbb0UWqJHLyPTVF.6Ld5sFRirZx/bXMeMmeurJledKYdZmG");
            // System.out.println("Negative check: " + client.checkPassword(password, hash));

            // try {
            //     hash.set(0, "too short");
            //     List<Boolean> rets = client.checkPassword(password, hash);
            //     System.out.println("Exception check: no exception thrown");
            // } catch (Exception e) {
            //     System.out.println("Exception check: exception thrown");
            // }

            transport.close();
        } catch (TException x) {
            x.printStackTrace();
        } 
    }
}
