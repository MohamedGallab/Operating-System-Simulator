import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.naming.spi.DirStateFactory.Result;
import javax.swing.text.AbstractDocument.BranchElement;

import javafx.util.Pair;

public class OS {
	private Object[] memory = new Object[40];
	private LinkedList<Pair<String, Integer>> newQ = new LinkedList<>();
	private Queue<String> readyQ = new LinkedList<>();
	private Queue<String> blockedQ = new LinkedList<>();
	private HashMap<String, Mutex> mutexes = new HashMap<String, Mutex>();
	private Interpreter interpreter = new Interpreter();
	private Scheduler scheduler = new Scheduler(2);
	private int clockCycles = 0;
	private int timeSlice = 2;
	private static int pid = 1;
	private SystemCallHandler systemCallHandler = new SystemCallHandler();
	private boolean printQ = false;
	private static OS instance;
	private Integer pcbPosition;
	private String onDisk = null;
	private HashMap<String, Integer> pcbIndex = new HashMap<String, Integer>();

	private OS() {
		addMutex("userInput");
		addMutex("userOutput");
		addMutex("file");
	}

	public static OS getInstance() {
		if (instance == null)
			instance = new OS();
		return instance;
	}

	public void addMutex(String mutex) {
		mutexes.put(mutex, new Mutex(mutex));
	}

	public void createProcess(String path, int arrivalTime) {
		newQ.add(new Pair<String, Integer>(path, arrivalTime));
	}

	public void executeInstruction(String[] instruction) {

		switch (instruction[0]) {
		case "print":
			if (instruction[1].equals("readFile")) {
				readFile(instruction, 1);
			} else if (instruction[1].equals("input")) {
				input(instruction, 1);
			} else {
				print(instruction);
			}
			((MyPair) memory[pcbPosition + 2])
					.setValue((Integer
							.parseInt(((MyPair) memory[pcbPosition + 2])
									.getValue()) + 1)
							+ "");
			break;
		case "assign":
			if (instruction[2].equals("readFile")) {
				readFile(instruction, 2);
			} else if (instruction[2].equals("input")) {
				input(instruction, 2);
			} else {
				assign(instruction);
			}
			((MyPair) memory[pcbPosition + 2])
					.setValue((Integer
							.parseInt(((MyPair) memory[pcbPosition + 2])
									.getValue()) + 1)
							+ "");
			break;
		case "writeFile":
			if (instruction[2].equals("readFile")) {
				readFile(instruction, 2);
			} else if (instruction[2].equals("input")) {
				input(instruction, 2);
			} else {
				writeFile(instruction);
			}
			((MyPair) memory[pcbPosition + 2])
					.setValue((Integer
							.parseInt(((MyPair) memory[pcbPosition + 2])
									.getValue()) + 1)
							+ "");
			break;
		case "printFromTo":
			if (instruction[1].equals("readFile")) {
				readFile(instruction, 2);
			} else if (instruction[1].equals("input")) {
				input(instruction, 1);
			} else if (instruction.length > 3
					&& instruction[3].equals("readFile")) {
				readFile(instruction, 3);
			} else if (instruction.length > 3 && instruction[3].equals("input")) {
				input(instruction, 3);
			} else {
				if (instruction.length == 3) {
					printFromTo(instruction[1], instruction[2]);
				} else if (instruction.length == 4) {
					try {
						Integer x = Integer.parseInt(instruction[2]);
						printFromTo(instruction[1], instruction[2]);
					} catch (Exception e) {
						printFromTo(instruction[1], instruction[3]);
					}
				} else {
					printFromTo(instruction[1], instruction[3]);
				}
			}
			((MyPair) memory[pcbPosition + 2])
					.setValue((Integer
							.parseInt(((MyPair) memory[pcbPosition + 2])
									.getValue()) + 1)
							+ "");

			break;
		case "semWait":
			((MyPair) memory[pcbPosition + 2])
					.setValue((Integer
							.parseInt(((MyPair) memory[pcbPosition + 2])
									.getValue()) + 1)
							+ "");
			semWait(mutexes.get(instruction[1]));
			break;
		case "semSignal":
			((MyPair) memory[pcbPosition + 2])
					.setValue((Integer
							.parseInt(((MyPair) memory[pcbPosition + 2])
									.getValue()) + 1)
							+ "");
			semSignal(mutexes.get(instruction[1]));
			break;
		default:
			break;
		}
	}

	public void print(String[] instruction) {
		systemCallHandler
				.printlnOutput("--------------------------------------");
		int i;
		for (i = pcbPosition + 5; i < pcbPosition + 8; i++) {
			if (((MyPair) systemCallHandler.readFromMemory(memory, i)).getKey()
					.equals(instruction[1]))
				break;
		}
		if (i < pcbPosition + 8)
			systemCallHandler
					.printlnOutput((String) ((MyPair) systemCallHandler
							.readFromMemory(memory, i)).getValue());
		else
			systemCallHandler.printlnOutput(instruction[1]);
		systemCallHandler
				.printlnOutput("--------------------------------------");
	}

	public void assign(String[] instruction) {
		for (int i = pcbPosition + 5; i < pcbPosition + 8; i++) {
			if (((MyPair) systemCallHandler.readFromMemory(memory, i))
					.getValue().equals("None")) {
				systemCallHandler.modifyMemory(memory, i, new MyPair(
						instruction[1], instruction[2]));
				break;
			}
		}
	}

	public void input(String[] instruction, int outputPosition) {
		instruction[outputPosition] = systemCallHandler.requestInput();
		((MyPair) memory[pcbPosition + 2]).setValue((Integer
				.parseInt(((MyPair) memory[pcbPosition + 2]).getValue()) - 1)
				+ "");
	}

	public void readFile(String[] instruction, int outputPosition) {
		int i;
		for (i = pcbPosition + 5; i < pcbPosition + 8; i++) {
			if (((MyPair) systemCallHandler.readFromMemory(memory, i)).getKey()
					.equals(instruction[outputPosition + 1]))
				break;
		}
		if (i < pcbPosition + 8)
			instruction[outputPosition] = systemCallHandler
					.readFromDisk((String) ((MyPair) systemCallHandler
							.readFromMemory(memory, i)).getValue());
		else
			instruction[outputPosition] = systemCallHandler
					.readFromDisk(instruction[outputPosition + 1]);

		((MyPair) memory[pcbPosition + 2]).setValue((Integer
				.parseInt(((MyPair) memory[pcbPosition + 2]).getValue()) - 1)
				+ "");
	}

	public void writeFile(String[] instruction) {
		int i;
		int j;
		for (i = pcbPosition + 5; i < pcbPosition + 8; i++) {
			if (((MyPair) systemCallHandler.readFromMemory(memory, i)).getKey()
					.equals(instruction[1]))
				break;
		}
		for (j = pcbPosition + 5; j < pcbPosition + 8; j++) {
			if (((MyPair) systemCallHandler.readFromMemory(memory, j)).getKey()
					.equals(instruction[2]))
				break;
		}
		String a = "";
		String b = "";

		systemCallHandler.writeToDisk(a, b);
	}

	public void printFromTo(String a, String b) {
		int i;
		int j;
		for (i = pcbPosition + 5; i < pcbPosition + 8; i++) {
			try {
				if (((MyPair) systemCallHandler.readFromMemory(memory, i))
						.getKey().equals(a))
					break;
			} catch (Exception e) {
				i = pcbPosition + 8;
				break;
			}
		}
		for (j = pcbPosition + 5; j < pcbPosition + 8; j++) {
			try {
				if (((MyPair) systemCallHandler.readFromMemory(memory, j))
						.getKey().equals(b))
					break;
			} catch (Exception e) {
				i = pcbPosition + 8;
				break;
			}

		}
		systemCallHandler
				.printlnOutput("--------------------------------------");
		Integer x;
		Integer y;
		if (i >= pcbPosition + 8)
			x = Integer.parseInt(a);
		else
			x = Integer.parseInt((String) ((MyPair) systemCallHandler
					.readFromMemory(memory, i)).getValue());
		if (j >= pcbPosition + 8)
			y = Integer.parseInt(b);
		else
			y = Integer.parseInt((String) ((MyPair) systemCallHandler
					.readFromMemory(memory, j)).getValue());

		for (int k = x; k < y; k++) {
			systemCallHandler.printOutput(k + ", ");
		}
		systemCallHandler.printlnOutput(y + "");
		systemCallHandler
				.printlnOutput("--------------------------------------");
	}

	public void semWait(Mutex mutex) {
		if (mutex.isValue()) {
			mutex.setOwnerID("P" + ((MyPair) memory[pcbPosition]).getValue());
			mutex.setValue(false);
		}else{
			String ID = "P" + ((MyPair) memory[pcbPosition]).getValue();
			if(onDisk!=null){
			if (onDisk.equals(ID)) {
				String Result = "\n";
				try {
					BufferedReader reader = new BufferedReader(new FileReader(
							"Disk.txt"));
					String br = "";
					while ((br = reader.readLine()) != null) {
						if (br.equals("State Ready-Disk")||br.equals("State Blocked-Memory"))
							Result += "State Blocked-Disk" + "\n";
						else
							Result +=   br + "\n";
					}
					reader.close();
					systemCallHandler.writeToDisk("Disk", Result);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
			else {
				((MyPair) memory[pcbPosition+1]).setValue("Blocked-Memory");
			}
			}
			else {
				((MyPair) memory[pcbPosition+1]).setValue("Blocked-Memory");
			}
				mutex.getBlockedQ().add(
						"P" + ((MyPair) memory[pcbPosition]).getValue() + "");
				blockedQ.add("P" + ((MyPair) memory[pcbPosition]).getValue()
						+ "");
				printQ = true;
				pcbPosition = null;

			}
		}
	

	public void semSignal(Mutex mutex) {
		if (mutex.getOwnerID().equals(
				("P" + ((MyPair) memory[pcbPosition]).getValue()))) {
			if (mutex.getBlockedQ().isEmpty())
				mutex.setValue(true);
			else {
				String ID = mutex.getBlockedQ().remove();
				if(onDisk!=null){
				if (onDisk.equals(ID)) {
					String Result = "\n";
					try {
						BufferedReader reader = new BufferedReader(
								new FileReader("Disk.txt"));
						String br = "";
						while ((br = reader.readLine()) != null) {
							if (br.equals("State Blocked-Disk"))
								Result += "State Ready-Disk"+ "\n";
							else
								Result += br + "\n";
						}
						reader.close();
						systemCallHandler.writeToDisk("Disk", Result);
					} catch (IOException e) {
						e.printStackTrace();
					}
					}
				} else {
					int temp = pcbPosition;
					pcbPosition = pcbIndex.get(ID);
					((MyPair) memory[pcbPosition + 1]).setValue("Ready-Memory");
					pcbPosition = temp;
				}
				blockedQ.remove(ID);
				mutex.setOwnerID(ID);
				readyQ.add(ID);
				printQ = true;
			}
		}
	}

	public static String printQueue(LinkedList<String> q) {
		String res = "[";
		for (int i = 0; i < q.size(); i++) {
			if (i == q.size() - 1)
				res += (q.get(i));
			else
				res += q.get(i) + ", ";
		}
		return res += "]";
	}

	public void admitNewProcesses() {
		for (int i = 0; i < newQ.size(); i++) {
			if (newQ.get(i).getValue() == clockCycles) {
				try {
					ArrayList<String[]> instructions = interpreter.parse(newQ
							.get(i).getKey());
					Integer position = searchMemory(instructions.size());
					if (position != null) {
						int[] boundaries = { position,
								position + 7 + instructions.size() };
						readyQ.add("P" + pid);
						memory[position] = new MyPair("PID", (pid++) + "");
						pcbIndex.put("P" + (pid - 1), position);
						memory[position + 1] = new MyPair("State",
								"Ready-Memory");
						memory[position + 2] = new MyPair("ProgramCounter", "8");
						memory[position + 3] = new MyPair("LowerBound",
								boundaries[0] + "");
						memory[position + 4] = new MyPair("UpperBound",
								boundaries[1] + "");
						memory[position + 5] = new MyPair("Uninitialized",
								"None");
						memory[position + 6] = new MyPair("Uninitialized",
								"None");
						memory[position + 7] = new MyPair("Uninitialized",
								"None");
						position += 8;
						int k = 1;
						for (String[] instruction : instructions) {
							memory[position++] = new MyPairInstruction("I" + k,
									instruction);
							k++;
						}

						printQ = true;
						newQ.remove(newQ.get(i));
						return;
					} else {
						int[] boundaries = new int[2];
						if (blockedQ.size() > 0) {
							boundaries[0] = Integer
									.parseInt((((MyPair) memory[pcbIndex
											.get(blockedQ.peek()) + 3])
											.getValue()));
							boundaries[1] = Integer
									.parseInt((((MyPair) memory[pcbIndex
											.get(blockedQ.peek()) + 4])
											.getValue()));
							onDisk = blockedQ.peek();
						} else {
							boundaries[0] = Integer
									.parseInt((((MyPair) memory[pcbIndex
											.get(readyQ.peek()) + 3])
											.getValue()));
							boundaries[1] = Integer
									.parseInt((((MyPair) memory[pcbIndex
											.get(readyQ.peek()) + 4])
											.getValue()));
							onDisk = readyQ.peek();
						}
						String diskInstruction = "";
						for (int j = boundaries[0] + 8; j < boundaries[1] + 1; j++) {
							for (int k = 0; k < ((MyPairInstruction) memory[j])
									.getValue().length; k++) {
								diskInstruction += ((MyPairInstruction) memory[j])
										.getValue()[k] + " ";
							}
							diskInstruction += "\n";
							memory[j] = null;
						}
						ArrayList<String> buffer = new ArrayList<String>();
						for (int j = boundaries[0]; j < boundaries[0] + 8; j++) {
							if (j == boundaries[0]+1) {
								if(((MyPair) memory[j]).getValue().equals("Ready-Memory"))
									((MyPair) memory[j]).setValue("Ready-Disk");
								else 
									((MyPair) memory[j]).setValue("Blocked-Disk");
							}
							buffer.add(((MyPair) memory[j]).toString());
							memory[j] = null;
						}
						String data = "";
						for (int j = 0; j < buffer.size(); j++) {
							data += buffer.get(j) + "\n";
						}
						systemCallHandler
								.printlnOutput("---------------------------------------");
						System.out.println("Swapping P" + (pid)
								+ " from Disk to Memory with " + onDisk);
						systemCallHandler
								.printlnOutput("---------------------------------------");
						systemCallHandler.writeToDisk("Disk", data
								+ diskInstruction);
						i = -1;
					}
				} catch (IOException e) {
					return;
				}
			}
		}
	}

	public Integer searchMemory(int size) {
		Integer result = null;
		int j = 0;
		boolean first = true;
		int position = 0;
		for (int i = 0; i < memory.length; i++) {
			if (memory[i] == null && first) {
				position = i;
				j++;
				first = false;
			} else if (memory[i] == null)
				j++;
			else {
				j = 0;
				first = true;
			}
		}
		if (j >= 8 + size)
			result = position;
		return result;
	}

	public void swap() {
		boolean written = false;
		BufferedReader reader;
		ArrayList<String[]> pcbData = new ArrayList<String[]>();
		ArrayList<String[]> instructions = new ArrayList<String[]>();
		try {
			reader = new BufferedReader(new FileReader("Disk.txt"));
			for (int i = 0; i < 8; i++) {
				String br = reader.readLine();
				if(br.equals("")){
					i--;
					continue;
				}
				pcbData.add(br.split(" "));
			}
			String br = null;
			while ((br = reader.readLine()) != null) {
				instructions.add(br.split(" "));
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Integer position = searchMemory(instructions.size());
		if (position != null) {
			int[] boundaries = { position, position + 7 + instructions.size() };
			memory[position] = new MyPair("PID", pcbData.get(0)[1]);
			pcbIndex.remove("P" + pcbData.get(0)[1]);
			pcbIndex.put("P" + pcbData.get(0)[1], position);
			memory[position + 1] = new MyPair("State", "Running");
			memory[position + 2] = new MyPair("ProgramCounter",
					pcbData.get(2)[1]);
			memory[position + 3] = new MyPair("LowerBound", boundaries[0] + "");
			memory[position + 4] = new MyPair("UpperBound", boundaries[1] + "");
			memory[position + 5] = new MyPair(pcbData.get(5)[0],
					pcbData.get(5)[1]);
			memory[position + 6] = new MyPair(pcbData.get(6)[0],
					pcbData.get(6)[1]);
			memory[position + 7] = new MyPair(pcbData.get(7)[0],
					pcbData.get(7)[1]);
			position += 8;
			int k = 1;
			for (String[] instruction : instructions) {
				memory[position++] = new MyPairInstruction("I" + k, instruction);
				k++;
			}
			if (!written) {
				onDisk = null;
				systemCallHandler.writeToDisk("Disk", "");
			}
			return;
		} else {
			int[] boundaries = new int[2];
			if (blockedQ.size() > 0) {
				boundaries[0] = Integer.parseInt((((MyPair) memory[pcbIndex
						.get(blockedQ.peek()) + 3]).getValue()));
				boundaries[1] = Integer.parseInt((((MyPair) memory[pcbIndex
						.get(blockedQ.peek()) + 4]).getValue()));
				onDisk = blockedQ.peek();
			} else {
				boundaries[0] = Integer.parseInt((((MyPair) memory[pcbIndex
						.get(readyQ.peek()) + 3]).getValue()));
				boundaries[1] = Integer.parseInt((((MyPair) memory[pcbIndex
						.get(readyQ.peek()) + 4]).getValue()));
				onDisk = readyQ.peek();
			}
			String diskInstruction = "";
			for (int j = boundaries[0] + 8; j < boundaries[1] + 1; j++) {
				for (int k = 0; k < ((MyPairInstruction) memory[j]).getValue().length; k++) {
					diskInstruction += ((MyPairInstruction) memory[j])
							.getValue()[k] + " ";
				}
				diskInstruction += "\n";
				memory[j] = null;
			}
			ArrayList<String> buffer = new ArrayList<String>();
			for (int j = boundaries[0]; j < boundaries[0] + 8; j++) {
				if (j == boundaries[0]+1) {
					if(((MyPair) memory[j]).getValue().equals("Ready-Memory"))
						((MyPair) memory[j]).setValue("Ready-Disk");
					else 
						((MyPair) memory[j]).setValue("Blocked-Disk");
				}
				buffer.add(((MyPair) memory[j]).toString());
				memory[j] = null;
			}
			String data = "";
			for (int j = 0; j < buffer.size(); j++) {
				data += buffer.get(j) + "\n";
			}
			systemCallHandler.writeToDisk("Disk", data + diskInstruction);
			position = searchMemory(instructions.size());
			int[] boundaries2 = { position, position + 7 + instructions.size() };
			memory[position] = new MyPair("PID", pcbData.get(0)[1]);
			pcbIndex.remove("P" + pcbData.get(0)[1]);
			pcbIndex.put("P" + pcbData.get(0)[1], position);
			memory[position + 1] = new MyPair("State", "Running");
			memory[position + 2] = new MyPair("ProgramCounter",
					pcbData.get(2)[1]);
			memory[position + 3] = new MyPair("LowerBound", boundaries2[0] + "");
			memory[position + 4] = new MyPair("UpperBound", boundaries2[1] + "");
			memory[position + 5] = new MyPair(pcbData.get(5)[0],
					pcbData.get(5)[1]);
			memory[position + 6] = new MyPair(pcbData.get(6)[0],
					pcbData.get(6)[1]);
			memory[position + 7] = new MyPair(pcbData.get(7)[0],
					pcbData.get(7)[1]);
			position += 8;
			int k = 1;
			for (String[] instruction : instructions) {
				memory[position++] = new MyPairInstruction("I" + k, instruction);
				k++;
			}
			written = true;
		}
	}

	public void run() {
		String ID = "";
		while (pcbPosition != null || !readyQ.isEmpty() || !newQ.isEmpty()) {
			System.out.println("\n\nclock : " + clockCycles);
			printQ = false;
			admitNewProcesses();
			String testID = scheduler.nextProcess(readyQ, pcbPosition);
			if (testID != "No Change" && testID != null){
				if (pcbPosition!=null)
					((MyPair)memory[pcbPosition+1]).setValue("Ready-Memory");
				ID = testID;
			}
			if (ID != null) {
				if (pcbPosition != null) {
					if ("P" + ((MyPair) memory[pcbPosition]).getValue() == onDisk)
						((MyPair) memory[pcbPosition + 1])
								.setValue("Ready-Disk");
					else
						((MyPair) memory[pcbPosition + 1])
								.setValue("Ready-Memory");
					readyQ.add("P" + ((MyPair) memory[pcbPosition]).getValue());
				}
				pcbPosition = pcbIndex.get(ID);
				readyQ.remove(ID);
				printQ = true;
			}
			if (ID.equals(onDisk)) {
				systemCallHandler
						.printlnOutput("---------------------------------------");
				System.out.print("Swapping " + onDisk + " from Disk to Memory");
				swap();
				pcbPosition = pcbIndex.get(ID);
				System.out.println(" with " + onDisk);
				systemCallHandler
						.printlnOutput("---------------------------------------");
			}
			if (pcbPosition != null) {
				((MyPair) memory[pcbPosition + 1]).setValue("Running");
				String[] nextInstruction = ((MyPairInstruction) memory[Integer
						.parseInt(((MyPair) memory[pcbPosition + 2]).getValue())
						+ pcbPosition]).getValue();
				String instructionToPrint = "";
				for (int i = 0; i < nextInstruction.length; i++) {
					instructionToPrint += (" " + nextInstruction[i]);
				}
				System.out.println("	Current Process: P"
						+ ((MyPair) memory[pcbPosition]).getValue() + "");
				System.out.println("	Current Instruction: "
						+ instructionToPrint);
				executeInstruction(nextInstruction);
				if (printQ) {
					System.out.println("	Ready Queue: "
							+ printQueue((LinkedList<String>) readyQ));
					System.out.println("	Blocked Queue: "
							+ printQueue((LinkedList<String>) blockedQ));
				}
				if (pcbPosition != null
						&& ((Integer
								.parseInt(((MyPair) memory[pcbPosition + 2])
										.getValue()) + pcbPosition) > Integer
								.parseInt(((MyPair) memory[pcbPosition + 4])
										.getValue()))) {
					int start = (Integer
							.parseInt(((MyPair) memory[pcbPosition + 3])
									.getValue()));
					int end = (Integer
							.parseInt(((MyPair) memory[pcbPosition + 4])
									.getValue()));
					for (int i = start; i <= end; i++) {
						memory[i] = null;
					}
					pcbPosition = null;
				}
			}
			System.out.println("	Memory : [" + printArray(memory) + "]");
			String Result = "\n";
			try {
				BufferedReader reader = new BufferedReader(new FileReader(
						"Disk.txt"));
				String br = "";
				while ((br = reader.readLine()) != null) {
					Result += "        " + br + "\n";
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.print("        Disk currently has : " + onDisk
					+ "\n        Data on Disk : " + Result);
			clockCycles++;
			 try {
			 TimeUnit.SECONDS.sleep(15);
			 } catch (InterruptedException e) {
			 e.printStackTrace();
			 }
			
		}
	}

	public String printArray(Object[] Mem) {
		String res = "";
		try {
			for (int i = 0; i < Mem.length - 1; i++) {
				if (Mem[i] != null) {
					try {
						res += ((MyPair) Mem[i]).toStringColon() + ", ";
					} catch (Exception e) {
						res += ((MyPairInstruction) Mem[i]).toStringColon()
								+ ", ";
					}
				} else {
					res += "null, ";
				}
			}
			if (Mem[Mem.length - 1] != null)
				res += (((MyPairInstruction) Mem[Mem.length - 1])
						.toStringColon());
			else {
				res += "null";
			}
		} catch (Exception e) {
		}
		;
		return res;
	}

	public void start() {
		System.out.println("Hello and welcome to SteakHolderOS");
		slowPrint("Do you want to specify the amount of time slices allowed per process? Default is 2.(Y/N)");
		Scanner sc = new Scanner(System.in);
		sleep(1000);
		while (true) {
			String wantsSlices = sc.nextLine();
			if (wantsSlices.toLowerCase().equals("y")) {
				int slices;
				slowPrint("Enter the number of slices you want:");
				while (true) {
					try {
						sc = new Scanner(System.in);
						slices = sc.nextInt();

						break;
					} catch (Exception ex) {
						sleep(1000);
						System.out
								.println("The character you wrote was not a number!");
						sleep(1000);
						slowPrint("Enter the number of slices you want:");
					}
				}
				this.timeSlice = slices;
				break;
			} else if (wantsSlices.toLowerCase().equals("n")) {
				break;
			} else {
				sleep(1000);
				System.out.println("Please choose either Y or N!");
				sleep(1000);
				slowPrint("Do you want to specify the amount of time slices allowed per process? Default is 2.(Y/N)");
			}
		}
		slowPrint("Enter the number of processes you want to add:");
		int numOfProcesses;
		while (true) {
			try {
				sc = new Scanner(System.in);
				numOfProcesses = sc.nextInt();
				if (numOfProcesses < 1)
					throw new IndexOutOfBoundsException();
				break;
			} catch (IndexOutOfBoundsException ex) {
				sleep(1000);
				System.out.println("Please enter a number greater than 0!");
				sleep(1000);
				slowPrint("Enter the number of processes you want to add:");
			} catch (Exception ex) {
				sleep(1000);
				System.out.println("The character you wrote was not a number!");
				sleep(1000);
				slowPrint("Enter the number of processes you want to add:");
			}
		}
		ArrayList<Integer> arrivals = new ArrayList<Integer>();

		for (int i = 1; i < numOfProcesses + 1; i++) {
			slowPrint("Enter the path for process " + i + ":");
			String path = "";
			int time = 0;
			while (true) {
				try {
					sc = new Scanner(System.in);
					path = sc.nextLine();
					this.interpreter.parse(path);
					break;
				} catch (Exception ex) {
					sleep(1000);
					System.out.println("Invalid path!");
					sleep(1000);
					slowPrint("Enter the path for process " + i + ":");
				}
			}
			slowPrint("Enter the time you wish for process " + i
					+ " to arrive:");

			while (true) {
				try {
					sc = new Scanner(System.in);
					time = sc.nextInt();
					if (time < 0)
						throw new IndexOutOfBoundsException();
					if (arrivals.contains(time))
						throw new ArithmeticException();
					break;
				} catch (IndexOutOfBoundsException ex) {
					sleep(1000);
					System.out.println("Please enter a positive number!");
					sleep(1000);
					slowPrint("Enter the time you wish for process " + i
							+ " to arrive:");
				} catch (ArithmeticException ex) {
					sleep(1000);
					System.out
							.println("Another process will arrive at this time. Please pick a different time!");
					sleep(1000);
					slowPrint("Enter the time you wish for process " + i
							+ " to arrive:");
				} catch (Exception ex) {
					sleep(1000);
					System.out
							.println("The character you wrote was not a number!");
					sleep(1000);
					slowPrint("Enter the time you wish for process " + i
							+ " to arrive:");
				}
			}
			arrivals.add(time);
			try {
				this.createProcess(path, time);
			} catch (Exception ex) {

			}
			sleep(1000);
		}
		slowPrint("Beginning execution.....\n");
	}

	public static void sleep(int x) {
		try {
			Thread.sleep(x);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	public static void slowPrint(String text) {
		for (int i = 0; i < text.length(); i++) {
			System.out.printf("%c", text.charAt(i));
			// try {
			// // Thread.sleep(70);
			// } catch (InterruptedException ex) {
			// Thread.currentThread().interrupt();
			// }
		}
	}

	public static void main(String[] args) {
		OS os = getInstance();
		// os.start();
		os.createProcess("Program_1.txt", 0);
		os.createProcess("Program_2.txt", 1);
		os.createProcess("Program_3.txt", 4);
		os.run();
		slowPrint("\nAll processes have been executed successfully!");
		sleep(1000);
		System.out.println("\nCredits :");
		sleep(1000);
		System.out.println("Amr Mohamed");
		sleep(1000);
		System.out.println("Mohamed Wael");
		sleep(1000);
		System.out.println("Mohamed Osama");
		sleep(1000);
		System.out.println("Yousef Moataz");
	}
}
