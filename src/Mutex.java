import java.util.LinkedList;
import java.util.Queue;

public class Mutex {
	private String resource;
	private Queue<String> blockedQ = new LinkedList<>();
	private String ownerID;
	private boolean value = true;
	
	public String getOwnerID() {
		return ownerID;
	}

	public void setOwnerID(String ownerID) {
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

	public Queue<String> getBlockedQ() {
		return blockedQ;
	}

}
