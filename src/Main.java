import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author Tuan Nam Davaux, Laetitia Courgey and Samuel Cohen
 * @since 2019-05-26
 *        <p>
 *        <b>Server Launcher</b>
 *        </p>
 */
public class Main {
	private static int maxClients = 10;
	private static int port = 81;
	private static Executor executor;
	public static final boolean PRINT_INFO = true;
	private int allowedAttempts = 0;
	private String word, guessedLetters;
	private ArrayList<String> letters = new ArrayList<String>();
	private ArrayList<ClientHandler> clients = new ArrayList<ClientHandler>();
	private boolean started = false;

	public ArrayList<String> getLetters() {
		return letters;
	}

	public void setLetters(ArrayList<String> letters) {
		this.letters = letters;
	}

	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}

	public int getAllowedAttempts() {
		return allowedAttempts;
	}

	public void setAllowedAttempts(int allowedAttempts) {
		this.allowedAttempts = allowedAttempts;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getGuessedLetters() {
		return guessedLetters;
	}

	public void setGuessedLetters(String guessedLetters) {
		this.guessedLetters = guessedLetters;
	}

	public ArrayList<ClientHandler> getClients() {
		return clients;
	}

	public void setClients(ArrayList<ClientHandler> clients) {
		this.clients = clients;
	}

	/**
	 * Selects a new word from the list of words and sets allowed number of guesses
	 * 
	 * @throws Exception
	 */
	public void newWord() throws Exception {

		File f = new File("words.txt");
		BufferedReader reader = new BufferedReader(new FileReader(f));
		int r = new Random().nextInt(835);
		for (int i = 0; i < r; i++)
			reader.readLine();

		word = reader.readLine().toLowerCase();

		guessedLetters = new String(new char[word.length()]).replace('\0', '-');
		allowedAttempts = 8;
		letters = new ArrayList<String>();
		reader.close();
		System.out.println("The new word to guess is : " + word);
		System.out.println("guessedLetters : " + guessedLetters + ", allowedAttempts : " + allowedAttempts);
		started = true;
	}

	public void callClientHandler(String[] args) {

		try {
			ServerSocket socket;

			// Get input from command line
			if (args.length == 1)
				port = Integer.parseInt(args[0]);
			else if (args.length == 2) {
				port = Integer.parseInt(args[0]);
				maxClients = Integer.parseInt(args[1]);
			}

			// Create a new server socket and a pool of threads
			socket = new ServerSocket(port);
			executor = Executors.newFixedThreadPool(maxClients);

			if (PRINT_INFO)
				System.out.println("Listening on port: " + port + ".\nAccepting: " + maxClients + " clients.");

			// Keep the server alive
			while (true) {
				try {
					// Accept a new client and handle it (if the number of current clients <
					// maxClients)
					Socket client = socket.accept();
					if (PRINT_INFO)
						System.out.println(
								"New connection from: " + client.getInetAddress().toString().replaceFirst("/", ""));
					ClientHandler ch = new ClientHandler(client, this);
					clients.add(ch);
					executor.execute(ch);
				} catch (Exception ex) {
				}
			}
		} catch (Exception e) {
		}
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Server Launched");
		Main m = new Main();
		m.callClientHandler(args);
	}

}
