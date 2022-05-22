public class PCB {
	int PID;
    ProcessState state;
    int programCounter = 4;
    int[] memoryBoundaries = new int[2];
//	private ArrayList<String[]> instructions;
//	private int timeToLive;
//	private int arrivalTime;
//	private int currentInstruction = 0;
//	private HashMap<String, String> map = new HashMap<String, String>();

	public PCB(int pID) {
		super();
		PID = pID;
	}

	public int getPID() {
		return PID;
	}

	public String toString() {
		return "P" + PID;
	}
}
