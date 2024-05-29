import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.transport.TNonblockingTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class CheckCallback implements AsyncMethodCallback<List<Boolean>>{
    List<Boolean> res;
    CountDownLatch latch;
    TNonblockingTransport transport;

    public CheckCallback (CountDownLatch latchIn, TNonblockingTransport transportIn) {
        this.res = new ArrayList<Boolean>(); 
        this.latch = latchIn;
        this.transport = transportIn;
	}

	public void onComplete(List<Boolean> response) {
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
