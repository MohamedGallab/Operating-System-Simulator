import java.util.Queue;

public class Scheduler {
	public Process nextProcess(Queue<Process> readyQ, Process executingProcess, int clockCycles) {
		if (!readyQ.isEmpty())
			if (executingProcess == null || executingProcess.getTimetolive() == 0)
				if (readyQ.peek().getArrivalTime() <= clockCycles)
					return readyQ.peek();
		return executingProcess;
	}
}
