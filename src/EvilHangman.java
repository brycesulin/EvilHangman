import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This program plays a game of Hangman using the “EvilHangman” algorithm
 *
 * @author Bryce Sulin
 * @version March 12, 2018
 */
public class EvilHangman {

    // Main method that runs the Hangman game
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        int numOfLetter = 0;
        int numOfGuess = 0;
        int runningTotal = 0;
        List<String> dictionary = new ArrayList<>();
        char guess;

        try (Stream<String> stream = Files.lines(Paths.get("google-10000-english.txt"))) {
            dictionary = stream
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Enter the length of word to choose: ");
        numOfLetter = getIntegerInput(scan, 2, 40);

        System.out.println("Enter the number of guesses allowed: ");
        numOfGuess = getIntegerInput(scan, 1, 40);

        Hangman game = new Hangman(numOfLetter, numOfGuess, dictionary);

        System.out.println("Do you want a running total of words? 1(yes), 0(no). ");
        runningTotal = getIntegerInput(scan, 0, 1);
        System.out.println();

        if (runningTotal == 1) {
            System.out.println("Possible words = " + game.wordListSize());
        }

        System.out.println("Guesses remaining: " + game.getNumberOfGuess());

        System.out.println("Guessed Letters = ");
        System.out.println(game.printGuessedCharacter());

        System.out.println("Current State = ");
        System.out.println(game.showCurrentState());

        // Each loop prompts for a user's guess and present the results
        // Loop continues until the game ends
        while (!game.gameOver()) {
            System.out.println("Please enter your guess: ");
            guess = getLetterInput(scan);
            if (game.play(guess)) {
                System.out.println();
            } else {
                System.out.println();
            }

            if (runningTotal == 1 && game.getNumberOfGuess() >= 1) {
                System.out.println("Possible words = " + game.wordListSize());
            }

            if (game.getNumberOfGuess() >= 1) {
                System.out.println("Guesses remaining: " + game.getNumberOfGuess());

                System.out.println("Guessed Letters = ");
                System.out.println(game.printGuessedCharacter());

                System.out.println("Current State = ");
                System.out.println(game.showCurrentState());
            }
        }

        if (game.playerWins()) {
            System.out.println("You won!");
        } else {
            System.out.print("You lose! ");
        }

        // Return the final answer if the game has ended
        System.out.print("The word was: ");
        System.out.println(game.getFinalAnswer());

        scan.close();
    }

    /**
     * Method that gets the user's integer input.
     *
     * @param scan Scanner to read user input
     * @param min  Minimum integer value
     * @param max  Maximum integer value
     * @return user's valid integer input
     */
    private static int getIntegerInput(Scanner scan, int min, int max) {
        int input = 0;
        while (true) {
            if (scan.hasNextInt()) {
                input = scan.nextInt();
                if (input > max) {
                    System.out.println("Too many. Try again: ");
                } else if (input < min) {
                    System.out.println("Too few. Try again: ");
                } else {
                    return input;
                }
            } else {
                System.out.println("Input must be an integer. Try again: ");
                scan.next();
            }
        }
    }

    /**
     * Method that gets the user's letter input.
     *
     * @param scan Scanner we pass in to read user input
     * @return user's valid input as a letter
     */
    private static char getLetterInput(Scanner scan) {
        while (true) {
            if (scan.hasNext()) {
                String temp = scan.next();
                if (temp.length() == 1 && Character.isLetter(temp.charAt(0))) {
                    return temp.charAt(0);
                }
            }
            System.out.println("Input is invalid. Please enter one letter: ");
        }
    }

    // This is the game implementation of Hangman
    public static class Hangman {
        private int numOfLetter = 0;
        private int numOfGuess = 0;
        private ArrayList<String> wordList;
        private String output;
        private String finalAnswer;
        private ArrayList<Character> guessed = new ArrayList<>();

        /**
         * Constructor sets up the number of letters and guesses, and takes in the dictionary
         * First, set the word guess status as "[_, _, ... _, _]"
         * Then, set the game's own word list as words of the given length in the dictionary
         *
         * @param numOfLetter is the target word length
         * @param numOfGuess  is the number of guesses allowed for the user
         * @param dictionary  includes all valid words for the game
         */
        public Hangman(int numOfLetter, int numOfGuess, List<String> dictionary) {
            this.numOfLetter = numOfLetter;
            this.numOfGuess = numOfGuess;
            wordList = new ArrayList<>();

            // Set initial output: "_" for all possible letter positions
            char[] outputArray = new char[numOfLetter];
            for (int i = 0; i < numOfLetter; i++) {
                outputArray[i] = '_';
            }

            output = new String(outputArray);

            // Add all words in dictionary with correct length to our game's word list
            wordList.addAll(dictionary.stream().filter(word -> word.length() == numOfLetter).collect(Collectors.toList()));
        }

        /**
         * Each play takes in one guess and implements the word families using a HashMap
         * Each key to the HashMap is a string
         * Words that contain the guessed letter will have that guessed letter shown,
         * and we use '_' for unknown letters
         *
         * @param guess is the user's guessed letter
         * @return whether the user got the guessed letter right or wrong
         */
        public boolean play(char guess) {
            String temp = output;
            boolean guessRight = false;
            HashMap<String, ArrayList<String>> wordChoices = new HashMap<>();

            if (gameOver()) {
                System.out.println("Game Over!");
                return playerWins();
            }

            // Parse all words in word list to create keys for the hash map
            for (String word : wordList) {
                char[] key = new char[numOfLetter];

                for (int index = 0; index < numOfLetter; index++) {
                    if (word.charAt(index) == guess) {
                        key[index] = guess;
                    } else {
                        key[index] = output.charAt(index);
                    }
                }

                String keyString = new String(key);

                addWordChoice(keyString, word, wordChoices);
            }

            // Implementing the last guess situation
            if (numOfGuess == 1) {
                // If output is present in HashMap, then there are words available without the guessed letter
                if (wordChoices.keySet().contains(output)) {

                    // Then simply do the update and return false
                    wordList = new ArrayList<>(wordChoices.get(output));
                    numOfGuess--;
                    finalAnswer = wordChoices.get(output).get(0);
                    guessed.add(guess);
                    return false;
                }
            }

            for (String keyString : wordChoices.keySet()) {
                if (!wordChoices.keySet().contains(temp)) {
                    temp = keyString;
                }

                // Go through all word families to find the one with the most possible words
                if (wordChoices.get(keyString).size() > wordChoices.get(temp).size()) {
                    temp = keyString;
                }
            }

            // Handle cases where the word list is empty (in case we change the dictionary)
            if (wordChoices.keySet().contains(temp)) {

                // Shallow copy of the max value in word choices
                wordList = new ArrayList<>(wordChoices.get(temp));
                guessRight = !temp.equals(output);

                if (!guessRight) numOfGuess--;

                output = temp;
                finalAnswer = wordChoices.get(output).get(0);
                guessed.add(guess);
                return guessRight;

            } else {

                // When word list is empty, return false
                wordList = new ArrayList<>();
                guessed.add(guess);
                return false;
            }
        }

        /**
         * Getter for number of guesses left
         *
         * @return number of guesses left
         */
        public int getNumberOfGuess() {
            return numOfGuess;
        }

        /**
         * Check if game is over. Game is over when:
         * There is no more guess left or no word in word list. Or the player already won
         *
         * @return true if game is over. false otherwise.
         */
        public boolean gameOver() {
            return numOfGuess == 0 || !output.contains("_") || wordList.isEmpty();
        }

        /**
         * Method to print out the current status of guesses.
         * Result string will look like "[_, _, a, _, _]"
         *
         * @return The formatted result string that shows what letters the user has guessed right and the unknown letters
         */
        public String showCurrentState() {
            char[] formattedOutput = output.toCharArray();
            return Arrays.toString(formattedOutput);
        }

        /**
         * Method to append a word to the hash map.
         * If the key does not exist in the map yet, create an ArrayList and append the word
         * Otherwise simply append the word to the correct ArrayList
         *
         * @param key           key string of the pair - used to find the target ArrayList in the given HashMap
         * @param word          value of the pair - to be appended to the value (ArrayList) in given HashMap
         * @param wordChoiceMap HashMap of string keys and ArrayList values
         */
        private void addWordChoice(String key, String word, HashMap<String, ArrayList<String>> wordChoiceMap) {
            if (wordChoiceMap.get(key) == null) {
                wordChoiceMap.put(key, new ArrayList<>());
            }
            wordChoiceMap.get(key).add(word);
        }

        /**
         * Return how many words are in the current family, as requested
         *
         * @return how many words are in the current family
         */
        public int wordListSize() {
            return wordList.size();
        }

        /**
         * Check if the player has won.
         *
         * @return true if the player has won. If the game is not over yet, return false.
         */
        public boolean playerWins() {
            if (!gameOver()) {
                System.out.println("Game not over yet.");
                return false;
            } else {
                return !output.contains("_");
            }
        }

        /**
         * We used guessed() to store all guessed letters.
         * This method helps print those stored letters in a well formatted way.
         *
         * @return A string that shows all guessed letters.
         */
        public String printGuessedCharacter() {
            Collections.sort(guessed);
            return guessed.toString();
        }

        /**
         * Only when game is over, this method shows the "secretly pre-selected" word.
         * Otherwise, return null
         *
         * @return the chosen word if game is over. Otherwise return null.
         */
        public String getFinalAnswer() {
            if (gameOver()) {
                return finalAnswer;
            } else {
                return null;
            }
        }
    }
}