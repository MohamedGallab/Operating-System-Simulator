import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SystemCallHandler {
	public void writeToDisk(String path, String data) {
		try {
			File yourFile = new File(path + ".txt");
			yourFile.createNewFile();
			FileWriter myWriter = new FileWriter(path + ".txt");
			myWriter.write(data);
			myWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String readFromDisk(String path) {

		try (Stream<String> stream = Files.lines(Paths.get(path))) {
			return stream.collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return "";
	}

	public String requestInput() {
		printlnOutput("Please enter a value: ");
		try {
			Scanner sc = new Scanner(System.in);
			return sc.nextLine();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return "";
	}

	public void printOutput(String s) {
		System.out.print(s);
	}

	public void printlnOutput(String s) {
		System.out.println(s);
	}

	public void modifyMemory(String key, String data,
			HashMap<String, String> map) {
		map.put(key, data);
	}

	public String readFromMemory(String key, HashMap<String, String> map) {
		return map.getOrDefault(key, key);
	}
}
