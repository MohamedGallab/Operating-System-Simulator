
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

	public OS() {
		addMutex("userInput");
		addMutex("userOutput");
		addMutex("file");
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
				readFile(instruction, 1);
			}
			else if (instruction[1].equals("input")) {
				input(instruction, 1);
			}
			else {
				print(instruction);
			}
			break;
		case "assign":
			if (instruction[2].equals("readFile")) {
				readFile(instruction, 2);
			}
			else if (instruction[2].equals("input")) {
				input(instruction, 2);
			}
			else {
				executingProcess.assign(instruction);
			}
			break;
		case "writeFile":
			if (instruction[2].equals("readFile")) {
				readFile(instruction, 2);
			}
			else if (instruction[2].equals("input")) {
				input(instruction, 2);
			}
			else {
				writeFile(instruction);
			}
			break;
		case "printFromTo":
			if (instruction[1].equals("readFile")) {
				readFile(instruction, 1);
			}
			else if (instruction[1].equals("input")) {
				input(instruction, 1);
			}
			else if (instruction.length > 3 && instruction[3].equals("readFile")) {
				readFile(instruction, 3);
			}
			else if (instruction.length > 3 && instruction[3].equals("input")) {
				input(instruction, 3);
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

	private void writeFile(String[] instruction) {
		try {
			File yourFile = new File(executingProcess.getMap().getOrDefault(instruction[1], instruction[1]) + ".txt");
			yourFile.createNewFile();
			FileWriter myWriter = new FileWriter(
					executingProcess.getMap().getOrDefault(instruction[1], instruction[1]) + ".txt");
			myWriter.write(executingProcess.getMap().getOrDefault(instruction[2], instruction[2]));
			myWriter.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readFile(String[] instruction, int outputPosition) {

		try (Stream<String> stream = Files.lines(Paths.get(executingProcess.getMap()
				.getOrDefault(instruction[outputPosition + 1], instruction[outputPosition + 1])))) {
			instruction[outputPosition] = stream.collect(Collectors.joining(System.lineSeparator()));
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		executingProcess.decrementNextInstruction();
	}

	@SuppressWarnings("resource")
	public void input(String[] instruction, int outputPosition) {
		slowPrint("Please enter a value");
		try {
			Scanner sc = new Scanner(System.in);
			instruction[outputPosition] = sc.nextLine();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		executingProcess.decrementNextInstruction();
	}

	public void print(String[] instruction) {
		System.out.println("--------------------------------------");
		System.out.println(executingProcess.getMap().getOrDefault(instruction[1], instruction[1]));
		System.out.println("--------------------------------------");
	}

	public void printFromTo(String a, String b) {
		System.out.println("--------------------------------------");
		Integer x = Integer.parseInt(executingProcess.getMap().getOrDefault(a, a));
		Integer y = Integer.parseInt(executingProcess.getMap().getOrDefault(b, b));
		for (int i = x; i < y; i++) {
			System.out.print(i + ", ");
		}
		System.out.println(y);
		System.out.println("--------------------------------------");

	}

	public void semWait(Mutex mutex) {
		if (mutex.isValue()) {
			mutex.setOwnerID(executingProcess.getPID());
			mutex.setValue(false);
		}
		else {
			mutex.getBlockedQ().add(executingProcess);
			blockedQ.add(executingProcess);
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
			}
		}
	}

	public static String printQueue(LinkedList<Process> q) {
		String res = "";
		for (int i = 0; i < q.size(); i++) {
			if (i == q.size() - 1)
				res += q.get(i);
			else
				res += q.get(i) + ", ";
		}
		return res;
	}

	public void run() {

		Process nextProcess;

		while (executingProcess != null || !readyQ.isEmpty() || !newQ.isEmpty()) {
			for (int i = 0; i < newQ.size(); i++) {
				if (newQ.get(i).getArrivalTime() == clockCycles) {
					readyQ.add(newQ.get(i));
					newQ.remove(newQ.get(i));
				}
			}
			System.out.println("\nclock : " + clockCycles);
			nextProcess = scheduler.nextProcess(readyQ, executingProcess, clockCycles);

			if (nextProcess != executingProcess) {
				if (executingProcess != null) {
					readyQ.add(executingProcess);
					executingProcess.setTimetolive(timeSlice);
				}
				executingProcess = nextProcess;
				readyQ.remove(nextProcess);
				System.out.println("Ready Queue: [" + printQueue((LinkedList<Process>) readyQ) + "]\n"
						+ "Blocked Queue: [" + printQueue((LinkedList<Process>) blockedQ) + "]");
				System.out.println("Current Process: P" + executingProcess.getPID());
			}

			if (executingProcess != null) {
				if (executingProcess.getTimetolive() == 0) {
					executingProcess.setTimetolive(timeSlice);
					System.out.println("Ready Queue: [" + printQueue((LinkedList<Process>) readyQ) + "]\n"
							+ "Blocked Queue: [" + printQueue((LinkedList<Process>) blockedQ) + "]");
					System.out.println("Current Process: P" + executingProcess.getPID());
				}
				String[] nextInstruction = executingProcess.getNextInstruction();
				String instructionToPrint = "";
				for (int i = 0; i < nextInstruction.length; i++) {
					instructionToPrint += (" " + nextInstruction[i]);
				}
				System.out.println("	Current Instruction: " + instructionToPrint);
				executingProcess.decrementTimeToLive();
				executeInstruction(nextInstruction);
				if (executingProcess != null)
					if (executingProcess.isProcessDone()) {
						executingProcess = null;
					}
			}
			clockCycles++;
			try {
				TimeUnit.SECONDS.sleep(4);
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
		OS os = new OS();
		//os.start();
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
