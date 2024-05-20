import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TFramedTransport;

import java.util.ArrayList;
import java.util.List;


public class FENode {
	static Logger log;
	static Integer BENodePort; //   ---> turned this into just a static variable to simplify logic for forwarding to the BENode
	static List<Integer> BENodes; // list of BE node port numbers
	static Integer counter = 0; // used to track last used node 0: FENode, 1: 1st BENode, 2: 2nd BENode

	public static void saveBENode(int port) {
		// BENodePort = port;
		BENodes.add(port);
		log.info(port + " added to BENodes");
		counter = 0;
	}

	public static void main(String [] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: java FENode FE_port");
			System.exit(-1);
		}

		BENodes = new ArrayList<Integer>();

		// initialize log4j
		BasicConfigurator.configure();
		log = Logger.getLogger(FENode.class.getName());

		int portFE = Integer.parseInt(args[0]);
		log.info("Launching FE node on port " + portFE);

		// launch Thrift server
		BcryptService.Processor processor = new BcryptService.Processor<BcryptService.Iface>(new BcryptServiceHandler());
		TServerSocket socket = new TServerSocket(portFE);
		TSimpleServer.Args sargs = new TSimpleServer.Args(socket);
		sargs.protocolFactory(new TBinaryProtocol.Factory());
		sargs.transportFactory(new TFramedTransport.Factory());
		sargs.processorFactory(new TProcessorFactory(processor));
		TSimpleServer server = new TSimpleServer(sargs);
		server.serve();
    }
}

