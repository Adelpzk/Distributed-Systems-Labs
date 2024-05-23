import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import org.apache.thrift.server.*;
import org.apache.thrift.server.TServer.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.*;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;


public class FENode {
	static Logger log;
	static List<Integer> BENodes; // list of BE node port numbers
	static Integer counter; // used to track last used node 0: FENode, 1: 1st BENode, 2: 2nd BENode
	static String hostname; 

	public static void main(String [] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: java FENode FE_port");
			System.exit(-1);
		}

		BENodes = new ArrayList<Integer>();
		counter = 0;
		hostname = getHostName();

		// initialize log4j
		BasicConfigurator.configure();
		log = Logger.getLogger(FENode.class.getName());

		int portFE = Integer.parseInt(args[0]);
		log.info("Launching FE node on port " + portFE);

		// launch Thrift server
		BcryptService.Processor processor = new BcryptService.Processor<BcryptService.Iface>(new BcryptServiceHandler());
		
		// TNonblockingServerSocket socket = new TNonblockingServerSocket(portFE);
		// THsHaServer.Args sargs = new THsHaServer.Args(socket);
		// sargs.protocolFactory(new TBinaryProtocol.Factory());
		// sargs.transportFactory(new TFramedTransport.Factory());
		// sargs.processorFactory(new TProcessorFactory(processor));
		// sargs.maxWorkerThreads(5);
		// TServer server = new THsHaServer(sargs);
		// server.serve();

		TServerSocket socket = new TServerSocket(portFE);
		TThreadPoolServer.Args sargs = new TThreadPoolServer.Args(socket);
		sargs.protocolFactory(new TBinaryProtocol.Factory());
		sargs.transportFactory(new TFramedTransport.Factory());
		sargs.processorFactory(new TProcessorFactory(processor));
		TThreadPoolServer server = new TThreadPoolServer(sargs);
		server.serve();
    }
	
	static String getHostName()
	{
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			return "localhost";
		}
	}

	public static void saveBENode(int port) {
		BENodes.add(port);
		log.info(port + " added to BENodes");
	}
}

