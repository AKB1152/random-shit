import java.io.File;
import java.io.PrintStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Scanner;

public class BFInterpreter {

	private Stack<Integer> openBracketStack = new Stack<>();
	private HashMap<Integer, Integer> bracketPair = new HashMap<>();
	private Scanner in = new Scanner (System.in);

	private String prog;

	private int data_index; 
	private int prog_index;
	private int[] data; 
	
	public static void main(String...args) throws IOException {
		File f = new File ("log.yaml");
		f.createNewFile(); 
		var ps = new PrintStream (f, StandardCharsets.UTF_8);
		System.setErr (ps);
		if (args[0].equalsIgnoreCase("-f"))
			new BFInterpreter(readFile(new File(args[1])));
		else 
			new BFInterpreter(concatAll(args));
	}

	public static String readFile (File f) throws IOException {
		String s = ""; 
		Scanner fr = new Scanner (f, StandardCharsets.UTF_8);
		while (fr.hasNextLine())
			s += fr.nextLine() + "\n";
		fr.close(); 
		return s; 
	}

	public static String concatAll (String...args) {
		String q = "";
		for (String s : args)
			q += s;
		return q; 
	}

	public static void printDebug (BFInterpreter bfi) {
		var bmap = "";
		for (var mapping : bfi.bracketPair.entrySet())
			bmap += String.format("\n\t - %2d -> %2d", mapping.getKey(), mapping.getValue());

		System.err.printf("""
				[\u001b[31mDEBUG|ERROR\u001b[0m]
				prog_index: %d (%c)
				data_index: %d (%d %x | %c)
				data: 
					(0x00): %02x %02x %02x %02x | %02x %02x %02x %02x | %c %c %c %c  %c %c %c %c 
					(0x08): %02x %02x %02x %02x | %02x %02x %02x %02x | %c %c %c %c  %c %c %c %c 
					(0x10): %02x %02x %02x %02x | %02x %02x %02x %02x | %c %c %c %c  %c %c %c %c 
					(0x18): %02x %02x %02x %02x | %02x %02x %02x %02x | %c %c %c %c  %c %c %c %c 
				
				obstack: %s 
				bmap: %s
				prog:
					%s
				
				""",
			bfi.prog_index, bfi.prog.charAt(bfi.prog_index),
			bfi.data_index, bfi.data[bfi.data_index], bfi.data[bfi.data_index], (char)(bfi.data[bfi.data_index] < 0x20? 0x2e : bfi.data[bfi.data_index]),
			bfi.data[00], bfi.data[01], bfi.data[02], bfi.data[03], bfi.data[04], bfi.data[05], bfi.data[06], bfi.data[07], 
			bfi.data[00] < 0x20? 0x2e : bfi.data[00], bfi.data[01]< 0x20? 0x2e : bfi.data[01], bfi.data[02]< 0x20? 0x2e : bfi.data[02], bfi.data[03]< 0x20? 0x2e : bfi.data[03], bfi.data[04]< 0x20? 0x2e : bfi.data[04], bfi.data[05]< 0x20? 0x2e : bfi.data[05], bfi.data[06]< 0x20? 0x2e : bfi.data[06], bfi.data[07]< 0x20? 0x2e : bfi.data[07], 
			bfi.data[ 8], bfi.data[ 9], bfi.data[10], bfi.data[11], bfi.data[12], bfi.data[13], bfi.data[14], bfi.data[15], 
			bfi.data[ 8] < 0x20? 0x2e : bfi.data[ 8], bfi.data[ 9]< 0x20? 0x2e : bfi.data[ 9], bfi.data[10]< 0x20? 0x2e : bfi.data[10], bfi.data[11]< 0x20? 0x2e : bfi.data[11], bfi.data[12]< 0x20? 0x2e : bfi.data[12], bfi.data[13]< 0x20? 0x2e : bfi.data[13], bfi.data[14]< 0x20? 0x2e : bfi.data[14], bfi.data[15]< 0x20? 0x2e : bfi.data[15], 
			bfi.data[16], bfi.data[17], bfi.data[18], bfi.data[19], bfi.data[20], bfi.data[21], bfi.data[22], bfi.data[23], 
			bfi.data[16] < 0x20? 0x2e : bfi.data[16], bfi.data[17]< 0x20? 0x2e : bfi.data[17], bfi.data[18]< 0x20? 0x2e : bfi.data[18], bfi.data[19]< 0x20? 0x2e : bfi.data[19], bfi.data[20]< 0x20? 0x2e : bfi.data[20], bfi.data[21]< 0x20? 0x2e : bfi.data[21], bfi.data[22]< 0x20? 0x2e : bfi.data[22], bfi.data[23]< 0x20? 0x2e : bfi.data[23], 
			bfi.data[24], bfi.data[25], bfi.data[26], bfi.data[27], bfi.data[28], bfi.data[29], bfi.data[30], bfi.data[31],
			bfi.data[24] < 0x20? 0x2e : bfi.data[24], bfi.data[25]< 0x20? 0x2e : bfi.data[25], bfi.data[26]< 0x20? 0x2e : bfi.data[26], bfi.data[27]< 0x20? 0x2e : bfi.data[27], bfi.data[28]< 0x20? 0x2e : bfi.data[28], bfi.data[29]< 0x20? 0x2e : bfi.data[29], bfi.data[30]< 0x20? 0x2e : bfi.data[30], bfi.data[31]< 0x20? 0x2e : bfi.data[31],
			bfi.openBracketStack,
			bmap, 
			bfi.prog
		);
	}

	public BFInterpreter (String prog) {
		this.prog = prog;
		pairBrackets();
		init();
		execute();
	}


	public void init () {
		data_index = 0; 
		prog_index = 0; 
		data = new int[32_768];
	}

	public void pairBrackets () {
		Stack<Integer> open = new Stack<>(); 
		for (int i = 0; i < prog.length(); i++)
			if (prog.charAt(i) == '[')
				open.push(i);
			else if (prog.charAt(i) == ']')
				bracketPair.put(open.pop(), i);

		if (open.size() != 0)
			throw new IllegalStateException("Brackets unpaired!");
	}

	public void execute () {
		System.out.print("\u001b[1;36m");
		try {
			for (prog_index = 0; prog_index < prog.length(); prog_index++) {
				printDebug(this);
				switch (prog.charAt(prog_index)) {
					case '+':
						data[data_index] = (data[data_index] == 255)? 0 : data[data_index] + 1; 			
						break;
					case '-':
						data[data_index] = (data[data_index] == 0)? 255 : data[data_index] - 1; 			
						break;

					case '<': 
						data_index = (data.length + data_index - 1) % data.length; 
						break; 	
					case '>': 
						data_index = (data.length + data_index + 1) % data.length; 
						break; 	
				
					case '.': 
						System.out.printf("%c", (data[data_index]));
						break; 

					case ',': 
						data[data_index] = (int)(in.nextLine().charAt(0));
						break;

					case '[': 
						if (data[data_index] != 0)	
							openBracketStack.push(prog_index);
						else 
							prog_index = bracketPair.get(prog_index); 
						break; 
					case ']': 
							prog_index = openBracketStack.pop()-1; 
						break; 
					default:
						break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			printDebug (this);
		}
		System.out.print("\u001b[0m");
	}

	private static class Stack<T> {
		HashMap<Integer, T> data = new HashMap<>();

		public void push (T data) {
			this.data.put (this.data.size(), data); 
		}

		public int size() {
			return data.size();
		}

		public T pop () {
			return this.data.remove(this.data.size()-1);
		}

		public T read () {
			T tmp = pop();
			push (tmp); 
			return tmp;
		}

		public String toString () {
			if (size() == 0) return "[]";
			var str = "";
			for (int i = 0; i < size(); i++)
				str += ", " + data.get(i); 
			return String.format ("[%s ]", str.substring(1)); 
		}
	}

}