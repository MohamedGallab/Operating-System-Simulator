import java.util.Arrays;

public class PCB {
	int PID;
	ProcessState state;
	int programCounter = 4;
	int[] memoryBoundaries = new int[2];

	public PCB(int pID, ProcessState state, int programCounter, int[] memoryBoundaries) {
		super();
		PID = pID;
		this.state = state;
		this.programCounter = programCounter;
		this.memoryBoundaries = memoryBoundaries;
	}

	@Override
	public String toString() {
		return "PCB [PID=" + PID + ", state=" + state + ", programCounter=" + programCounter + ", boundaries="
				+ Arrays.toString(memoryBoundaries) + "]";
	}

	public void setBoundaries(int start, int end) {
		memoryBoundaries[0] = start;
		memoryBoundaries[1] = end;
	}

	public int getNextInstruction() {
		return memoryBoundaries[0] + programCounter++;
	}
}
