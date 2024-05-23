import java.net.InetAddress;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import org.apache.thrift.server.*;
import org.apache.thrift.server.TServer.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.*;


public class BENode {
	static Logger log;
	static Integer port;

	public static void main(String [] args) throws Exception {
		if (args.length != 3) {
			System.err.println("Usage: java BENode FE_host FE_port BE_port");
			System.exit(-1);
		}

		// initialize log4j
		BasicConfigurator.configure();
		log = Logger.getLogger(BENode.class.getName());

		String hostFE = args[0];
		int portFE = Integer.parseInt(args[1]);
		int portBE = Integer.parseInt(args[2]);
		
		port = portBE;

		log.info("Launching BE node on port " + portBE + " at host " + getHostName());

		// ping FE
		while (true) {
			try {
				TSocket sock = new TSocket(hostFE, portFE);
				TTransport transport = new TFramedTransport(sock);
				TProtocol protocol = new TBinaryProtocol(transport);
				BcryptService.Client client = new BcryptService.Client(protocol);
				transport.open();

				client.pingFE(portBE);

				transport.close();
				break;
			} catch (Exception e) {
				log.error("Error connecting to FE. Retrying in 5 seconds...");
                Thread.sleep(5000);
			}
		}

		// launch Thrift server
		BcryptService.Processor processor = new BcryptService.Processor<BcryptService.Iface>(new BcryptServiceHandler());
		
		// TNonblockingServerSocket socket = new TNonblockingServerSocket(portBE);
		// THsHaServer.Args sargs = new THsHaServer.Args(socket);
		// sargs.protocolFactory(new TBinaryProtocol.Factory());
		// sargs.transportFactory(new TFramedTransport.Factory());
		// sargs.processorFactory(new TProcessorFactory(processor));
		// sargs.maxWorkerThreads(5);
		// TServer server = new THsHaServer(sargs);
		// server.serve();

		TServerSocket socket = new TServerSocket(portBE);
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
}
