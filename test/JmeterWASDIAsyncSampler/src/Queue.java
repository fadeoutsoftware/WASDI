import java.util.ArrayList;
import java.util.List;

public class Queue {
	private List<RabbitMessage> queue;
	
	public Queue () {
		this.queue= new ArrayList<RabbitMessage>();
	} 
	
	public  synchronized boolean add(RabbitMessage msg) {
		boolean result = false;
		result = this.queue.add(msg);
		if (this.queue.size()==1) {
			this.notifyAll();
		}
		return result;
	}
	public synchronized RabbitMessage pop() {
		return this.queue.remove(0);
	}
	
	public int getSize() {
		return this.queue.size();
	}

}
