import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SystemCallHandler {
	public void writeFile(String[] instruction, Process executingProcess) {
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

	public void readFile(String[] instruction, int outputPosition, Process executingProcess) {

		try (Stream<String> stream = Files.lines(Paths.get(executingProcess.getMap()
				.getOrDefault(instruction[outputPosition + 1], instruction[outputPosition + 1])))) {
			instruction[outputPosition] = stream.collect(Collectors.joining(System.lineSeparator()));
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void input(String[] instruction, int outputPosition) {
		System.out.println("Please enter a value");
		try {
			Scanner sc = new Scanner(System.in);
			instruction[outputPosition] = sc.nextLine();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void print(String[] instruction, Process executingProcess) {
		System.out.println("--------------------------------------");
		System.out.println(executingProcess.getMap().getOrDefault(instruction[1], instruction[1]));
		System.out.println("--------------------------------------");
	}

	public void printOutput(String s) {
		System.out.print(s);
	}

	public void printlnOutput(String s) {
		System.out.println(s);
	}

	public void assign(String[] instruction, Process executingProcess) {
		executingProcess.getMap().put(instruction[1],
				executingProcess.getMap().getOrDefault(instruction[2], instruction[2]));
	}
}
