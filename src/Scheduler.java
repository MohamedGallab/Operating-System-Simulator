import java.util.Queue;

public class Scheduler {
	public Process nextProcess(Queue<Process> readyQ, Process executingProcess, int clockCycles) {
		if (!readyQ.isEmpty())
			if (executingProcess == null || executingProcess.getTimetolive() == 0)
				return readyQ.peek();
		return executingProcess;
	}
}
