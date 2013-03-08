import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 * Clone client Model Two
 * 
 * @author Danish Shrestha <dshrestha06@gmail.com>
 * 
 */
public class clonecli2 {
	private static Map<String, kvsimple> kvMap = new HashMap<String, kvsimple>();
	private static AtomicLong sequence = new AtomicLong();

	public void run() {
		Context ctx = ZMQ.context(1);
		Socket snapshot = ctx.socket(ZMQ.XREQ);
		snapshot.setLinger(0);
		snapshot.connect("tcp://localhost:5556");

		Socket subscriber = ctx.socket(ZMQ.SUB);
		subscriber.setLinger(0);
		subscriber.connect("tcp://localhost:5557");
		subscriber.subscribe("".getBytes());

		// get state snapshot
		snapshot.send("ICANHAZ?".getBytes(), 0);
		kvsimple kvMsg = null;
		while (true) {
			kvMsg = kvsimple.recv(snapshot);
			sequence.set(kvMsg.getSequence());
			if ("KTHXBAI".equalsIgnoreCase(kvMsg.getKey())) {
				System.out.println("Received snapshot = " + kvMsg.getSequence());
				break; // done
			}

			System.out.println("receiving " + kvMsg.getSequence());
			clonecli2.kvMap.put(kvMsg.getKey(), kvMsg);
		}

		// now apply pending updates, discard out-of-sequence messages
		while (true) {
			kvMsg = null;
			kvMsg = kvsimple.recv(subscriber);

			if (kvMsg != null) {
				if (kvMsg.getSequence() > sequence.get()) {
					sequence.set(kvMsg.getSequence());
					System.out.println("receiving " + sequence);
					clonecli2.kvMap.put(kvMsg.getKey(), kvMsg);
				}
			}
		}
	}

	public static void main(String[] args) {
		new clonecli2().run();
	}
}
