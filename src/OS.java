import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javafx.util.Pair;

public class OS {
	private Object[] memory = new Object[40];
	private ArrayList<Object> disk = new ArrayList<Object>();
	private LinkedList<Pair<String, Integer>> newQ = new LinkedList<>();
	private Queue<PCB> readyQ = new LinkedList<>();
	private Queue<PCB> blockedQ = new LinkedList<>();
	private HashMap<String, Mutex> mutexes = new HashMap<String, Mutex>();
	private Interpreter interpreter = new Interpreter();
	private Scheduler scheduler = new Scheduler();
	private int clockCycles = 0;
	private int timeSlice = 2;
	private static int pid = 1;
	private SystemCallHandler systemCallHandler = new SystemCallHandler();
	private boolean printQ = false;
	private static OS instance;
	private int pcbPosition;
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
			break;
		case "assign":
			if (instruction[2].equals("readFile")) {
				readFile(instruction, 2);
			} else if (instruction[2].equals("input")) {
				input(instruction, 2);
			} else {
				assign(instruction);
			}
			break;
		case "writeFile":
			if (instruction[2].equals("readFile")) {
				readFile(instruction, 2);
			} else if (instruction[2].equals("input")) {
				input(instruction, 2);
			} else {
				writeFile(instruction);
			}
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
			break;
		case "semWait":
			semWait(mutexes.get(instruction[1]));
			break;
		case "semSignal":
			semSignal(mutexes.get(instruction[1]));
			break;
		default:
			break;
		}
	}

	public void print(String[] instruction) {
		systemCallHandler
				.printlnOutput("--------------------------------------");
		systemCallHandler.printlnOutput(systemCallHandler.readFromMemory(
				instruction[1], executingProcess.getMap()));
		systemCallHandler
				.printlnOutput("--------------------------------------");
	}

	public void assign(String[] instruction) {
		systemCallHandler.modifyMemory(instruction[1], systemCallHandler
				.readFromMemory(instruction[2], executingProcess.getMap()),
				executingProcess.getMap());
	}

	public void input(String[] instruction, int outputPosition) {
		instruction[outputPosition] = systemCallHandler.requestInput();
		executingProcess.decrementNextInstruction();
	}

	public void readFile(String[] instruction, int outputPosition) {
		instruction[outputPosition] = systemCallHandler
				.readFromDisk(systemCallHandler.readFromMemory(
						instruction[outputPosition + 1],
						executingProcess.getMap()));
		executingProcess.decrementNextInstruction();
	}

	public void writeFile(String[] instruction) {
		systemCallHandler.writeToDisk(systemCallHandler.readFromMemory(
				instruction[1], executingProcess.getMap()), systemCallHandler
				.readFromMemory(instruction[2], executingProcess.getMap()));
	}

	public void printFromTo(String a, String b) {
		systemCallHandler
				.printlnOutput("--------------------------------------");
		Integer x = Integer.parseInt(systemCallHandler.readFromMemory(a,
				executingProcess.getMap()));
		Integer y = Integer.parseInt(systemCallHandler.readFromMemory(b,
				executingProcess.getMap()));
		for (int i = x; i < y; i++) {
			systemCallHandler.printOutput(i + ", ");
		}
		systemCallHandler.printlnOutput(y + "");
		systemCallHandler
				.printlnOutput("--------------------------------------");
	}

	public void semWait(Mutex mutex) {
		if (mutex.isValue()) {
			mutex.setOwnerID(executingProcess.getPID());
			mutex.setValue(false);
		} else {
			mutex.getBlockedQ().add(executingProcess);
			blockedQ.add(executingProcess);
			printQ = true;
			executingProcess = null;
		}
	}

	public void semSignal(Mutex mutex) {
		if (mutex.getOwnerID() == executingProcess.getPID()) {
			if (mutex.getBlockedQ().isEmpty())
				mutex.setValue(true);
			else {
				PCB process = mutex.getBlockedQ().remove();
				blockedQ.remove(process);
				mutex.setOwnerID(process.getPID());
				process.setTimetolive(timeSlice);
				readyQ.add(process);
				printQ = true;
			}
		}
	}

	public static String printQueue(LinkedList<PCB> q) {
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
					ArrayList<String[]> instructions = interpreter.parse(newQ.get(i).getKey());
					int position = searchMemory(instructions.size()+4);
					if (position != -1) {
						PCB pcb = new PCB(pid++);
						memory[position] = pcb;
						position+=3;
						for(String[] instruction:instructions){
							memory[position++] = instruction;
						}
	 					readyQ.add(pcb);
						printQ = true;
						newQ.remove(newQ.get(i));
					}
					else{
						
					}
				} catch (IOException e) {
				}
				
			}
		}
	}
	
	public int searchMemory(int size){
		return 0;
	}

	public void run() {
		PCB nextProcess;

		while (pcbPosition!=-1 || !readyQ.isEmpty() || !newQ.isEmpty()) {
			System.out.println("\n\nclock : " + clockCycles);
			printQ = false;
			admitNewProcesses();
			nextProcess = scheduler.nextProcess(readyQ);
			if (nextProcess != null) {
				if (executingProcess != null) {
					readyQ.add(executingProcess);
					executingProcess.setTimetolive(timeSlice);
				}
				executingProcess = nextProcess;
				readyQ.remove(nextProcess);
				printQ = true;
			}
			if (executingProcess != null) {
				String[] nextInstruction = executingProcess
						.getNextInstruction();
				String instructionToPrint = "";
				for (int i = 0; i < nextInstruction.length; i++) {
					instructionToPrint += (" " + nextInstruction[i]);
				}
				System.out.println("	Current Process: " + executingProcess);
				System.out.println("	Current Instruction: "
						+ instructionToPrint);
				executingProcess.decrementTimeToLive();
				executeInstruction(nextInstruction);
				if (printQ) {
					System.out.println("	Ready Queue: "
							+ printQueue((LinkedList<PCB>) readyQ));
					System.out.println("	Blocked Queue: "
							+ printQueue((LinkedList<PCB>) blockedQ));
				}
				if (executingProcess != null
						&& executingProcess.isProcessDone()) {
					executingProcess = null;
				}
			}
			clockCycles++;
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
			try {
				Thread.sleep(70);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public static void main(String[] args) {
		OS os = getInstance();
		os.start();
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
