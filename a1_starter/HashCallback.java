import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.transport.TNonblockingTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class HashCallback implements AsyncMethodCallback<List<String>>{
    List<String> res;
    CountDownLatch latch;
    TNonblockingTransport transport;

    public HashCallback (CountDownLatch latchIn, TNonblockingTransport transportIn) {
        this.res = new ArrayList<String>(); 
        this.latch = latchIn;
        this.transport = transportIn;
	}

	public void onComplete(List<String> response) {
        this.res = response;
        transport.close();
        this.latch.countDown();
	}
    
	public void onError(Exception e) {
        System.err.println(e);
        transport.close();
        this.latch.countDown();
	}
}
