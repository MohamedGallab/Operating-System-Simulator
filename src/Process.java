import java.util.ArrayList;
import java.util.HashMap;

public class Process {
	private PCB PCB;
//	private ArrayList<String[]> instructions;
	private int timeToLive;
	private int arrivalTime;
//	private HashMap<String, String> map = new HashMap<String, String>();

	public Process(PCB PCB, ArrayList<String[]> instructions, int timetolive, int arrivalTime) {
		super();
		this.instructions = instructions;
		this.timeToLive = timetolive;
		this.arrivalTime = arrivalTime;
	}

	public int getPID() {
		return PCB.PID;
	}

	public HashMap<String, String> getMap() {
		return map;
	}

	public void setTimetolive(int timetolive) {
		this.timeToLive = timetolive;
	}


	public void decrementTimeToLive() {
		timeToLive--;
	}

	public int getArrivalTime() {
		return arrivalTime;
	}

	public int getNextInstruction() {
		return PCB.getNextInstruction();
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
