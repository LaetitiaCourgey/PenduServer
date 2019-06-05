import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * @author Tuan Nam Davaux, Laetitia Courgey and Samuel Cohen
 * @since 2019-05-26
 *        <p>
 *        <b>Handles all communication with a client</b>
 *        </p>
 */
public class ClientHandler implements Runnable {

	Message msg;
	Socket client;
	ObjectInputStream in;
	ObjectOutputStream out;
	int score = 0;
	Main game;
	String word, guessedLetters;
	private String name;
	String scores = "";

	ClientHandler(Socket s, Main game) throws Exception {
		try {
			client = s;
			in = new ObjectInputStream(s.getInputStream());
			out = new ObjectOutputStream(client.getOutputStream());
			this.game = game;
			this.score = 0;
			this.name = "Player " + (1 + (int) (Math.random() * ((100 - 1) + 1)));
		} catch (Exception ex) {
		}

	}

	/**
	 * Adds a valid letter to the current view of the word
	 */
	private void addValidLetter(String l) {
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

	/**
	 * Sends the current message to this.client and flushes the stream
	 */
	private void sendMessage() {
		try {
			out.writeObject(msg);
			out.flush();
		} catch (Exception e) {
		}
	}

	private void sendMessage2(Message sms) {
		try {
			out.writeObject(sms);
			out.flush();
		} catch (Exception e) {
		}
	}

	/**
	 * Notifies this.client who has lost or won with his new score and then notifies
	 * others
	 */
	private void sendMessage3() {

		Message sms;
		if (msg.flag == Message.LOSE) {
			sendMessage();
			for (ClientHandler ch : game.getClients()) {
				if (!ch.client.equals(client)) {
					sms = new Message(Message.LOSE, ch.score, 0, null, null, name, scores);
					ch.sendMessage2(sms);
				}
			}
			game.setStarted(false);
		} else if (msg.flag == Message.WIN) {
			sendMessage();
			for (ClientHandler ch : game.getClients()) {
				if (!ch.client.equals(client)) {
					sms = new Message(Message.WIN, ch.score, 0, game.getWord(), null, name, scores);
					ch.sendMessage2(sms);
				}
			}
			game.setStarted(false);
		}

	}

	/**
	 * Sends message to all clients
	 */
	private void sendMessageAll() {

		for (ClientHandler ch : game.getClients()) {
			ch.sendMessage2(msg);
		}
	}

	/**
	 * Sends a new message indicating that a new game has been started
	 * 
	 * @param pseudo
	 */
	private void sendNewGame(String pseudo) {
		name = pseudo;
		msg = new Message(Message.NEW_GAME, score, game.getAllowedAttempts(), null, game.getGuessedLetters(), name,
				game.getLetters());
		sendMessage();

		if (Main.PRINT_INFO)
			System.out.println("Client: " + client.getInetAddress().toString().replaceFirst("/", "") + " (" + name
					+ ")  started a new game!");
	}

	/**
	 * Sends a new message indicating that the client has won
	 */
	private void sendCongrats() {

		++score;
		scores = "";
		for (ClientHandler ch : game.getClients()) {
			scores += ch.name + ": " + ch.score + " points \n";
		}
		msg = new Message(Message.WIN, score, 0, game.getWord(), null, name, scores);
		// sendMessage();
		sendMessage3();

		if (Main.PRINT_INFO)
			System.out.println(name + " won! Score: " + score);
	}

	/**
	 * Sends a new message indicating that the client has guessed a single letter
	 * right
	 */
	private void sendRightGuess() {

		msg = new Message(Message.RIGHT_GUESS, 0, game.getAllowedAttempts(), null, game.getGuessedLetters(), null,
				game.getLetters());
		// sendMessage();
		sendMessageAll();

		if (Main.PRINT_INFO)
			System.out.println(name + "  guessed right!  " + game.getGuessedLetters() + "  attempts left : "
					+ game.getAllowedAttempts());
	}

	/**
	 * Sends a new message indicating that the client has guessed a single letter
	 * wrong or that the client has lost depending on the value fo the number of
	 * allowed guesses
	 */
	private void sendWrongGuess(String l) {

		game.setAllowedAttempts(game.getAllowedAttempts() - 1);
		if (game.getAllowedAttempts() > 0) {
			msg = new Message(Message.WRONG_GUESS, 0, game.getAllowedAttempts(), l, game.getGuessedLetters(), null,
					game.getLetters());
			// sendMessage();
			sendMessageAll();

			if (Main.PRINT_INFO)
				System.out.println(name + "  guessed wrong!  " + game.getGuessedLetters() + " attempts left : "
						+ game.getAllowedAttempts());
		} else {
			--score;
			scores = "";
			for (ClientHandler ch : game.getClients()) {
				scores += ch.name + ": " + ch.score + " points \n";
			}
			msg = new Message(Message.LOSE, score, 0, null, null, name, scores);
			sendMessage3();

			if (Main.PRINT_INFO)
				System.out.println(name + " lost! Score: " + score);
		}
	}

	@Override
	public void run() {

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
						if (!game.isStarted()) {
							game.newWord();
						}
						sendNewGame(message.name);
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
						if (game.getWord().contains((message.clientGuess).toLowerCase())) // The letter is found in the
																							// word
						{
							addValidLetter(message.clientGuess.toLowerCase());
							if (!game.getGuessedLetters().contains("-")) // The client has guessed the entire word
								sendCongrats();
							else
								sendRightGuess();
						} else {// The client guessed wrong
							ArrayList<String> l = game.getLetters();
							if (!l.contains(message.clientGuess.toLowerCase())) {
								l.add(message.clientGuess.toLowerCase());
							}
							game.setLetters(l);
							sendWrongGuess(message.clientGuess.toLowerCase());
						}
					} else // The client guesses a word
					{
						if (game.getWord().equals(message.clientGuess.toLowerCase()))
							sendCongrats();
						else
							sendWrongGuess(message.clientGuess.toLowerCase());
					}
				} else
					Thread.yield(); // The thread gives up its timeslice if the client does not send anything
			} catch (Exception ex) {
			}
		}
		if (Main.PRINT_INFO)
			game.getClients().remove(this);
		System.out.println(name + " has disconnected.");
	}
}
