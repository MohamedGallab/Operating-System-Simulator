import java.util.LinkedList;
import java.util.Queue;

public class Mutex {
	private String resource;
	private Queue<PCB> blockedQ = new LinkedList<>();
	private int ownerID;
	private boolean value = true;
	
	public int getOwnerID() {
		return ownerID;
	}

	public void setOwnerID(int ownerID) {
		this.ownerID = ownerID;
	}

	public boolean isValue() {
		return value;
	}

	public void setValue(boolean value) {
		this.value = value;
	}

	public Mutex(String resource) {
		this.resource = resource;
	}

	public Queue<PCB> getBlockedQ() {
		return blockedQ;
	}

}
