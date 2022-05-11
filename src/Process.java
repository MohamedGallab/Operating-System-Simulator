import java.util.ArrayList;
import java.util.HashMap;

public class Process {
	private int PID;
	private ArrayList<String[]> instructions;
	private int timeToLive;
	private int arrivalTime;
	private int currentInstruction = 0;
	private HashMap<String, String> map = new HashMap<String, String>();

	public Process(int pID, ArrayList<String[]> instructions, int timetolive, int arrivalTime) {
		super();
		PID = pID;
		this.instructions = instructions;
		this.timeToLive = timetolive;
		this.arrivalTime = arrivalTime;
	}

	public int getPID() {
		return PID;
	}

	public HashMap<String, String> getMap() {
		return map;
	}

	public void setTimetolive(int timetolive) {
		this.timeToLive = timetolive;
	}

	public int getCurrentInstruction() {
		return currentInstruction;
	}

	public void setCurrentInstruction(int currentInstruction) {
		this.currentInstruction = currentInstruction;
	}

	public void decrementTimeToLive() {
		timeToLive--;
	}

	public int getArrivalTime() {
		return arrivalTime;
	}

	public String[] getNextInstruction() {
		return instructions.get(currentInstruction++);
	}

	public ArrayList<String[]> getInstructions() {
		return instructions;
	}

	public int getTimetolive() {
		return timeToLive;
	}

	public void decrementNextInstruction() {
		currentInstruction--;
	}

	public boolean isProcessDone() {
		return currentInstruction == instructions.size();
	}

	public String toString() {
		return "P" + PID;
	}
}
