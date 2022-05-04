
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class OS {
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
	public static void sleep(int x){
		try{
		    Thread.sleep(x);
		}
		catch(InterruptedException ex){
		    Thread.currentThread().interrupt();
		}
	}
	public static void slowPrint(String text){
		for(int i = 0; i < text.length(); i++){
		    System.out.printf("%c", text.charAt(i));
		    try{
		        Thread.sleep(70);
		    }catch(InterruptedException ex){
		        Thread.currentThread().interrupt();
		    }
		}
	}
	public void addMutex(String mutex) {
		mutexes.put(mutex, new Mutex(mutex));
	}

	public void createProcess(String path, int arrivalTime) throws IOException {
		ArrayList<String[]> instructions = interpreter.parse(path);
		readyQ.add(new Process(pid++, instructions, this.timeSlice, arrivalTime));
	}

	public void executeInstruction(String[] instruction) {

		switch (instruction[0]) {
		case "print":
			if (instruction[1].equals("readFile")) {
				read(instruction);
				//check ttl and next instruction and shit man tomorrow yeah ?
			} else if (instruction[1].equals("input")) {
				input(instruction);
			} else {
				System.out.println(executingProcess.getMap().getOrDefault(instruction[1],instruction[1]));
			}
			break;
		case "assign":

			break;
		case "writeFile":

			break;

		case "printFromTo":
//			if(instruction[1].equals("readFile"))
//			{
//				
//			}
//			else if(instruction[3].equals("readFile"))
//			{
//				
//			}
//			else
//			printFromTo(instruction[1], instruction[2]);
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
//		print
//		assign
//		writeFile
//		readFile
//		printFromTo
//		semWait
//		semSignal
	}
	
	public void read(String[] instruction) {
		executingProcess.decrementNextInstruction();
	}

	public void input(String[] instruction) {
		executingProcess.decrementNextInstruction();
	}

	public void print() {

	}
	
	public void printFromTo(String a, String b) {

	}

	public void semWait(Mutex mutex) {
		if (mutex.isValue()) {
			mutex.setOwnerID(executingProcess.getPID());
			mutex.setValue(false);
		} else {
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
				mutex.setOwnerID(process.getPID());
				readyQ.add(process);
			}
		}
	}

	public void run() {

		Process nextProcess;

		while (executingProcess != null || !readyQ.isEmpty()) {

			nextProcess = scheduler.nextProcess(readyQ, executingProcess, clockCycles);
			if (nextProcess != executingProcess) {
				readyQ.add(executingProcess);
				executingProcess = nextProcess;
				readyQ.remove(nextProcess);
			}

			if (executingProcess != null) {

				String[] nextInstruction = executingProcess.getNextInstruction();
				executingProcess.decrementTimeToLive();
				executeInstruction(nextInstruction);
				if (executingProcess != null)
					if (executingProcess.isProcessDone()) {
						executingProcess = null;
					}
			}
			while (readyQ.remove(null)) {
	        }
			clockCycles++;
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}		}
	}

	public static void main(String[] args) {
		OS os = new OS();
		Scanner sc = new Scanner(System.in); 
		System.out.println("Hello and welcome to SteakHolderOS");
		sleep(1000);
		slowPrint("Do you want to specify the amount of time slices allowed per process? Default is 2.(Y/N)");
	    String wantsSlices = sc.nextLine();
	    if(wantsSlices.toLowerCase().equals("y")){
		    int slices;
		slowPrint("Enter the number of slices you want:");
			while(true){
			try{
			    sc = new Scanner(System.in); 
				slices = sc.nextInt();
				
				break;
			}
			catch(Exception ex){
				sleep(1000);
				System.out.println("The character you wrote was not a number!");
				sleep(1000);
				slowPrint("Enter the number of slices you want:");
			}
			}
			os.timeSlice = slices;
	    }
	    slowPrint("Enter the number of processes you want to add:");
	    int numOfProcesses;
		while(true){
		try{
		    sc = new Scanner(System.in);
			numOfProcesses = sc.nextInt();
			if (numOfProcesses<1)
				throw new IndexOutOfBoundsException();
			break;
		}
		catch(IndexOutOfBoundsException ex){
			sleep(1000);
			System.out.println("Please enter a number greater than 0!");
			sleep(1000);
			slowPrint("Enter the number of processes you want to add:");
		}
		catch(Exception ex){
			sleep(1000);
			System.out.println("The character you wrote was not a number!");
			sleep(1000);
			slowPrint("Enter the number of processes you want to add:");
		}
		}
		ArrayList<Integer> arrivals = new ArrayList<Integer>();

		for (int i = 1;i<numOfProcesses+1;i++){
			slowPrint("Enter the path for process "+i+":" );
			String path = "";
			int time = 0;
			while(true){
				try{
					sc = new Scanner(System.in); 
					path = sc.nextLine();
					os.interpreter.parse(path);
					break;
				}
				catch(Exception ex){
					sleep(1000);
					System.out.println("Invalid path!");
					sleep(1000);
					slowPrint("Enter the path for process "+i+":" );				}
			}		
				slowPrint("Enter the time you wish for process "+i+" to arrive:" );

				while(true){
				try{
				    sc = new Scanner(System.in); 
					time = sc.nextInt();
					if (time < 0)
						throw new IndexOutOfBoundsException();
					if (arrivals.contains(time))
						throw new ArithmeticException();
					break;
				}
				catch(IndexOutOfBoundsException ex){
					sleep(1000);
					System.out.println("Please enter a positive number!");
					sleep(1000);
					slowPrint("Enter the time you wish for process "+i+" to arrive:" );					
				}
				catch(ArithmeticException ex){
					sleep(1000);
					System.out.println("Another process will arrive at this time. Please pick a different time!");
					sleep(1000);
					slowPrint("Enter the time you wish for process "+i+" to arrive:" );	
				}
				catch(Exception ex){
					sleep(1000);
					System.out.println("The character you wrote was not a number!");
					sleep(1000);
					slowPrint("Enter the time you wish for process "+i+" to arrive:" );	
				}
				}
				arrivals.add(time);
				try{
					os.createProcess(path, time);
				}
				catch(Exception ex){
					
				}
				sleep(1000);
		}
		Collections.sort((List<Process>) os.readyQ, Comparator.comparingInt(Process -> ((Process) Process).getArrivalTime()));
		slowPrint("Beginning execution....." + "\n" );
		os.run();
		slowPrint("\n"+ "All processes have been executed successfully!" );
		sleep(1000);
		System.out.println("\n"+"𝒞𝓇ℯ𝒹𝒾𝓉𝓈 :");
		sleep(1000);
		System.out.println("𝒜𝓂𝓇 ℳℴ𝒽𝒶𝓂ℯ𝒹");
		sleep(1000);
		System.out.println("ℳℴ𝒽𝒶𝓂ℯ𝒹 𝒲𝒶ℯ𝓁");
		sleep(1000);
		System.out.println("ℳℴ𝒽𝒶𝓂ℯ𝒹 𝒪𝓈𝒶𝓂𝒶");	
		sleep(1000);
		System.out.println("𝒴ℴ𝓊𝓈𝓈ℯ𝒻 ℳℴ𝒶𝓉𝒶𝓏");
	}
}
