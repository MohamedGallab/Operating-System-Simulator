import java.util.Queue;

public class Scheduler {
	private int timeToLive;
	public PCB nextProcess(Queue<PCB> readyQ, PCB executingProcess) {
		if (executingProcess == null){
			return readyQ.peek();
		}
		if (executingProcess.getTimetolive() == 0 && readyQ.isEmpty()){
			return executingProcess;
		}
		if (executingProcess.getTimetolive() == 0 && !readyQ.isEmpty()){
			return readyQ.peek();
		}
		timeToLive--;
		return null;
	}
	public void setTimeToLive(int timeToLive) {
		this.timeToLive = timeToLive;
	}
}
