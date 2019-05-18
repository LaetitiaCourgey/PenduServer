import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/***************************************************************************************
 * Written by: Simon Cicek * Last changed: 2012-04-13 *
 ***************************************************************************************/

// Handles all communication with a client
public class ClientHandler implements Runnable {
	Message msg;
	Socket client;
	ObjectInputStream in;
	ObjectOutputStream out;
	int score, allowedAttempts = 0;
	Main game;
	String word, guessedLetters;

	ClientHandler(Socket s, Main game) throws Exception {
		System.out.println("ClientHandler");
		try {
			client = s;
			in = new ObjectInputStream(s.getInputStream());
			out = new ObjectOutputStream(client.getOutputStream());
			this.game = game;
			this.score = 0;
		} catch (Exception ex) {
		}

	}

//	// Selects a new word from the list of words and sets allowed number of guesses
//	private void newWord() throws Exception {
//		File f = new File("words.txt");
//		BufferedReader reader = new BufferedReader(new FileReader(f));
//		for (int i = 0; i < new Random().nextInt((int) (f.length() - 1)); i++)
//			reader.readLine();
//
//		word = reader.readLine();
//		guessedLetters = new String(new char[word.length()]).replace('\0', '-');
//		allowedAttempts = word.length() * 2;
//		reader.close();
//		System.out.println(word);
//	}

	// Adds a valid letter to the current view of the word
	private void addValidLetter(String l) {
		System.out.println("addValidLetter");
		// Make sure it's a single letter
		if (l.length() != 1)
			return;
		char c = l.charAt(0);
		// Array used to mark where in the word the letter appears
		boolean[] letters = new boolean[game.getWord().length()];

		// Mark where the letter appears
		for (int i = 0; i < game.getWord().length(); i++)
			if (game.getWord().charAt(i) == c)
				letters[i] = true;

		// Insert the letter into the current view of the word
		char[] chars = game.getGuessedLetters().toCharArray();
		for (int i = 0; i < letters.length; i++)
			if (letters[i] == true)
				chars[i] = c;
		game.setGuessedLetters(new String(chars));
	}

	// Sends the current message and flushes the stream
	private void sendMessage() {
		System.out.println("sendMessage");
		try {
			out.writeObject(msg);
			out.flush();
		} catch (Exception e) {
		}
	}

	private void sendMessage2(Message sms) {
		System.out.println("sendMessage2");
		try {
			out.writeObject(sms);
			out.flush();
		} catch (Exception e) {
		}
	}

	private void sendMessage3() {
		System.out.println("sendMessage3");
		Message sms;
		if (msg.flag == Message.LOSE) {
			sendMessage();
			for (ClientHandler ch : game.getClients()) {
				if (!ch.client.equals(client)) {
					sms = new Message(Message.LOSE, ch.score, 0, null, null);
					ch.sendMessage2(sms);
				}
			}
			game.setStarted(false);
		} else if (msg.flag == Message.WIN) {
			sendMessage();
			for (ClientHandler ch : game.getClients()) {
				if (!ch.client.equals(client)) {
					sms = new Message(Message.WIN, ch.score, 0, game.getWord(), null);
					ch.sendMessage2(sms);
				}
			}
			game.setStarted(false);
		}

	}

	private void sendMessageAll() {
		System.out.println("sendMessageAll");
		for (ClientHandler ch : game.getClients()) {
			ch.sendMessage2(msg);
		}
	}

	// Sends a new message indicating that a new game has been started
	private void sendNewGame() {
		System.out.println("sendNewGame");
		msg = new Message(Message.NEW_GAME, score, game.getAllowedAttempts(), null, game.getGuessedLetters());
		sendMessage();

		if (Main.PRINT_INFO)
			System.out.println(
					"Client: " + client.getInetAddress().toString().replaceFirst("/", "") + "  started a new game!");
	}

	// Sends a new message indicating that the client has won
	private void sendCongrats() {
		System.out.println("sendCongrats");
		++score;
		msg = new Message(Message.WIN, score, 0, game.getWord(), null);
		// sendMessage();
		sendMessage3();

		if (Main.PRINT_INFO)
			System.out.println(
					"Client: " + client.getInetAddress().toString().replaceFirst("/", "") + "  won! Score: " + score);
	}

	// Sends a new message indicating that the client has guessed a single letter
	// right
	private void sendRightGuess() {
		System.out.println("sendRightGuess");
		msg = new Message(Message.RIGHT_GUESS, 0, game.getAllowedAttempts(), null, game.getGuessedLetters());
		// sendMessage();
		sendMessageAll();

		if (Main.PRINT_INFO)
			System.out.println("Client: " + client.getInetAddress().toString().replaceFirst("/", "")
					+ "  guessed right!  " + game.getGuessedLetters() + "   " + game.getAllowedAttempts());
	}

	// Sends a new message indicating that the client has guessed a single letter
	// wrong
	// or that the client has lost depending on the value fo the number of allowed
	// guesses
	private void sendWrongGuess() {
		System.out.println("sendWrongGuess");
		game.setAllowedAttempts(game.getAllowedAttempts() - 1);
		if (game.getAllowedAttempts() > 0) {
			msg = new Message(Message.WRONG_GUESS, 0, game.getAllowedAttempts(), null, game.getGuessedLetters());
			// sendMessage();
			sendMessageAll();

			if (Main.PRINT_INFO)
				System.out.println("Client: " + client.getInetAddress().toString().replaceFirst("/", "")
						+ "  guessed wrong!  " + game.getGuessedLetters() + "   " + game.getAllowedAttempts());
		} else {
			--score;
			msg = new Message(Message.LOSE, score, 0, null, null);
			// sendMessage();
			sendMessage3();

			if (Main.PRINT_INFO)
				System.out.println("Client: " + client.getInetAddress().toString().replaceFirst("/", "")
						+ " lost! Score: " + score);
		}
	}

	@Override
	public void run() {
		System.out.println("run");
		// Runs as long as a connection to the client is maintained
		while (!client.isClosed()) {
			if (in == null) // Something went wrong when getting the input stream so it is pointless to go
							// on
				break;
			try {
				Message message = (Message) in.readObject();
				if (message != null) // Nothing was sent from the client
				{
					if (message.flag == Message.NEW_GAME) // Client wants to start a new game
					{
						System.out.println("flag new game, " + game.isStarted());
						if (!game.isStarted()) {
							game.newWord();
						}
						sendNewGame();
						continue;

					} else if (message.flag == Message.CLOSE_CONNECTION) // Client terminated the connection
					{
						in.close();
						out.close();
						client.close();
						continue;
					}

					if (message.clientGuess.length() == 1) // Client sent a single letter
					{
						if (game.getWord().contains(message.clientGuess)) // The letter is found in the word
						{
							addValidLetter(message.clientGuess);
							if (!game.getGuessedLetters().contains("-")) // The client has guessed the entire word
								sendCongrats();
							else
								sendRightGuess();
						} else // The client guessed wrong
							sendWrongGuess();
					} else // The client guesses a word
					{
						if (game.getWord().equals(message.clientGuess))
							sendCongrats();
						else
							sendWrongGuess();
					}
				} else
					Thread.yield(); // The thread gives up its timeslice if the client does not send anything
			} catch (Exception ex) {
			}
		}
		if (Main.PRINT_INFO)
			System.out.println(
					"Client: " + client.getInetAddress().toString().replaceFirst("/", "") + "  has disconnected.");
	}
}
