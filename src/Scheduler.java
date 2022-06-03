import java.util.Queue;

public class Scheduler {
	private int quantum;
	private int timeToLive;

	public Scheduler(int quantum){
		this.quantum=quantum-1;
	}

	public String nextProcess(Queue<String> readyQ, Integer pcbPosition) {
		if ((Integer)pcbPosition == null) {
			timeToLive = quantum;
			return readyQ.peek();
		}
		if (timeToLive == 0 && readyQ.isEmpty()) {
			timeToLive = quantum;
			return "No Change";
		}
		if (timeToLive == 0 && !readyQ.isEmpty()) {
			timeToLive = quantum;
			return readyQ.peek();
		}
		timeToLive--;
		return null;
	}

	public void setTimeToLive(int timeToLive) {
		this.timeToLive = timeToLive;
	}

	public void setQuantum(int quantum) {
		this.quantum = quantum;
	}
}
