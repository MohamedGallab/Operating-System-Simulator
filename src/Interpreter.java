import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Interpreter {
	public ArrayList<String[]> parse(String path) throws IOException 
	{
		ArrayList<String[]> instructions = new ArrayList<String[]>();
		
		
		File file = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String st;
		while ((st = br.readLine()) != null)
			instructions.add(st.split(" "));
		br.close();
		
		return instructions;
	}
}
