
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OS {
	private LinkedList<Process> newQ = new LinkedList<>();
	private Queue<Process> readyQ = new LinkedList<>();
	private Queue<Process> blockedQ = new LinkedList<>();
	private HashMap<String, Mutex> mutexes = new HashMap<String, Mutex>();
	private Interpreter interpreter = new Interpreter();
	private Scheduler scheduler = new Scheduler();
	private int clockCycles = 0;
	private Process executingProcess;
	private int timeSlice = 2;
	private static int pid = 1;
	private SystemCallHandler systemCallHandler = new SystemCallHandler();
	private boolean printQ = false;
	private static OS instance;

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
		ArrayList<String[]> instructions;
		try {
			instructions = interpreter.parse(path);
			newQ.add(new Process(pid++, instructions, this.timeSlice, arrivalTime));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void executeInstruction(String[] instruction) {

		switch (instruction[0]) {
		case "print":
			if (instruction[1].equals("readFile")) {
				systemCallHandler.readFile(instruction, 1, executingProcess);
				executingProcess.decrementNextInstruction();
			}
			else if (instruction[1].equals("input")) {
				systemCallHandler.input(instruction, 1);
				executingProcess.decrementNextInstruction();
			}
			else {
				systemCallHandler.print(instruction, executingProcess);
			}
			break;
		case "assign":
			if (instruction[2].equals("readFile")) {
				systemCallHandler.readFile(instruction, 2, executingProcess);
				executingProcess.decrementNextInstruction();
			}
			else if (instruction[2].equals("input")) {
				systemCallHandler.input(instruction, 2);
				executingProcess.decrementNextInstruction();
			}
			else {
				systemCallHandler.assign(instruction, executingProcess);
			}
			break;
		case "writeFile":
			if (instruction[2].equals("readFile")) {
				systemCallHandler.readFile(instruction, 2, executingProcess);
				executingProcess.decrementNextInstruction();
			}
			else if (instruction[2].equals("input")) {
				systemCallHandler.input(instruction, 2);
				executingProcess.decrementNextInstruction();
			}
			else {
				systemCallHandler.writeFile(instruction, executingProcess);
			}
			break;
		case "printFromTo":
			if (instruction[1].equals("readFile")) {
				systemCallHandler.readFile(instruction, 2, executingProcess);
				executingProcess.decrementNextInstruction();
			}
			else if (instruction[1].equals("input")) {
				systemCallHandler.input(instruction, 1);
				executingProcess.decrementNextInstruction();
			}
			else if (instruction.length > 3 && instruction[3].equals("readFile")) {
				systemCallHandler.readFile(instruction, 3, executingProcess);
				executingProcess.decrementNextInstruction();
			}
			else if (instruction.length > 3 && instruction[3].equals("input")) {
				systemCallHandler.input(instruction, 3);
				executingProcess.decrementNextInstruction();
			}
			else {
				if (instruction.length == 3) {
					printFromTo(instruction[1], instruction[2]);
				}
				else if (instruction.length == 4) {
					try {
						Integer x = Integer.parseInt(instruction[2]);
						printFromTo(instruction[1], instruction[2]);
					}
					catch (Exception e) {
						printFromTo(instruction[1], instruction[3]);
					}
				}
				else {
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
	
	public void printFromTo(String a, String b) {
		systemCallHandler.printlnOutput("--------------------------------------");
		Integer x = Integer.parseInt(executingProcess.getMap().getOrDefault(a, a));
		Integer y = Integer.parseInt(executingProcess.getMap().getOrDefault(b, b));
		for (int i = x; i < y; i++) {
			systemCallHandler.printOutput(i + ", ");
		}
		systemCallHandler.printlnOutput(y+"");
		systemCallHandler.printlnOutput("--------------------------------------");
	}

	public void semWait(Mutex mutex) {
		if (mutex.isValue()) {
			mutex.setOwnerID(executingProcess.getPID());
			mutex.setValue(false);
		}
		else {
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
				Process process = mutex.getBlockedQ().remove();
				blockedQ.remove(process);
				mutex.setOwnerID(process.getPID());
				process.setTimetolive(timeSlice);
				readyQ.add(process);
				printQ = true;
			}
		}
	}

	public static String printQueue(LinkedList<Process> q) {
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
			if (newQ.get(i).getArrivalTime() == clockCycles) {
				readyQ.add(newQ.get(i));
				printQ = true;
				newQ.remove(newQ.get(i));
			}
		}
	}

	public void run() {
		Process nextProcess;

		while (executingProcess != null || !readyQ.isEmpty() || !newQ.isEmpty()) {
			System.out.println("\n\nclock : " + clockCycles);
			printQ = false;
			admitNewProcesses();
			nextProcess = scheduler.nextProcess(readyQ, executingProcess);
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
				String[] nextInstruction = executingProcess.getNextInstruction();
				String instructionToPrint = "";
				for (int i = 0; i < nextInstruction.length; i++) {
					instructionToPrint += (" " + nextInstruction[i]);
				}
				System.out.println("	Current Process: " + executingProcess);
				System.out.println("	Current Instruction: " + instructionToPrint);
				executingProcess.decrementTimeToLive();
				executeInstruction(nextInstruction);
				if (printQ) {
					System.out.println("	Ready Queue: " + printQueue((LinkedList<Process>) readyQ));
					System.out.println("	Blocked Queue: " + printQueue((LinkedList<Process>) blockedQ));
				}
				if (executingProcess != null && executingProcess.isProcessDone()) {
					executingProcess = null;
				}
			}
			clockCycles++;
			try {
				TimeUnit.SECONDS.sleep(2);
			}
			catch (InterruptedException e) {
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
					}
					catch (Exception ex) {
						sleep(1000);
						System.out.println("The character you wrote was not a number!");
						sleep(1000);
						slowPrint("Enter the number of slices you want:");
					}
				}
				this.timeSlice = slices;
				break;
			}
			else if (wantsSlices.toLowerCase().equals("n")) {
				break;
			}
			else {
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
			}
			catch (IndexOutOfBoundsException ex) {
				sleep(1000);
				System.out.println("Please enter a number greater than 0!");
				sleep(1000);
				slowPrint("Enter the number of processes you want to add:");
			}
			catch (Exception ex) {
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
				}
				catch (Exception ex) {
					sleep(1000);
					System.out.println("Invalid path!");
					sleep(1000);
					slowPrint("Enter the path for process " + i + ":");
				}
			}
			slowPrint("Enter the time you wish for process " + i + " to arrive:");

			while (true) {
				try {
					sc = new Scanner(System.in);
					time = sc.nextInt();
					if (time < 0)
						throw new IndexOutOfBoundsException();
					if (arrivals.contains(time))
						throw new ArithmeticException();
					break;
				}
				catch (IndexOutOfBoundsException ex) {
					sleep(1000);
					System.out.println("Please enter a positive number!");
					sleep(1000);
					slowPrint("Enter the time you wish for process " + i + " to arrive:");
				}
				catch (ArithmeticException ex) {
					sleep(1000);
					System.out.println("Another process will arrive at this time. Please pick a different time!");
					sleep(1000);
					slowPrint("Enter the time you wish for process " + i + " to arrive:");
				}
				catch (Exception ex) {
					sleep(1000);
					System.out.println("The character you wrote was not a number!");
					sleep(1000);
					slowPrint("Enter the time you wish for process " + i + " to arrive:");
				}
			}
			arrivals.add(time);
			try {
				this.createProcess(path, time);
			}
			catch (Exception ex) {

			}
			sleep(1000);
		}
		slowPrint("Beginning execution.....\n");
	}

	public static void sleep(int x) {
		try {
			Thread.sleep(x);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	public static void slowPrint(String text) {
		for (int i = 0; i < text.length(); i++) {
			System.out.printf("%c", text.charAt(i));
			try {
				Thread.sleep(70);
			}
			catch (InterruptedException ex) {
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
