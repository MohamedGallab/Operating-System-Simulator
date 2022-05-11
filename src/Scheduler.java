import java.util.Queue;

public class Scheduler {
	public Process nextProcess(Queue<Process> readyQ, Process executingProcess) {
		if (executingProcess == null)
			return readyQ.peek();

		if (executingProcess.getTimetolive() == 0 && readyQ.isEmpty())
			return executingProcess;

		if (executingProcess.getTimetolive() == 0 && !readyQ.isEmpty())
			return readyQ.peek();
		return null;
	}
}
