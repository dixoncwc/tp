package seedu.financeit.parser;

import seedu.financeit.common.CommandPacket;
import seedu.financeit.common.Constants;
import seedu.financeit.common.exceptions.EmptyCommandStringException;
import seedu.financeit.ui.UiManager;
import seedu.financeit.utils.RegexMatcher;

import java.security.InvalidParameterException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.regex.Matcher;

public class InputParser {
    protected Matcher matcher;

    public InputParser() {
    }

    public String getSeparator(String input) {
        return String.valueOf(input.charAt(matcher.start() + 1));
    }

    /**
     * Example input: deadline do homework /by tomorrow /note skip page 70.
     * commandString: "deadline"
     * CommandPacket created:
     * {
     *  commandType: ADD_DEADLINE
     *  commandContent: "do homework"
     *  params: HashMap< String, String >
     *  {
     *   "by": "tomorrow"
     *   "note": "skip page 70"
     *  }
     * }
     */
    public CommandPacket parseInput(String input) {
        String commandContent = "";
        String commandString = "";
        String restOfCommand;
        CommandPacket packet;
        HashMap<String, String> params = new HashMap<>();
        String[] buffer = null;
        String separator = "";
        boolean paramsExist = false;

        //Split into [command, rest of input]
        //Check for existence of command title

        try {
            input = " " + input + " ";
            this.matcher = RegexMatcher.paramMatcher(input);
            separator = this.getSeparator(input);
            paramsExist = true;

        } catch (java.lang.IllegalStateException exception) {
            commandString = input;
            return new CommandPacket(commandString, params);
        }

        try {
            buffer = input.split(separator, 2);
            if (buffer[0].equals(" ")) {
                throw new EmptyCommandStringException();
            }
            //command , /a param
            buffer[1] = separator + buffer[1];
            commandString = buffer[0];
            String paramSubstring = buffer[1];
            params = new ParamsParser(paramSubstring).parseParams();
        } catch (EmptyCommandStringException exception) {
            UiManager.printWithStatusIcon(Constants.PrintType.SYS_MSG, exception.getMessage());
        }
        return new CommandPacket(commandString, params);
    }

    /**
     * Parses raw date and time input from the user and return a formatted string that can be parsed by DateTine class.
     * @param input Input string to parse
     * @return Formatted String in YYYY-MM-DDTHH:MM:SS
     */
    public static LocalDateTime parseRawDateTime(String input) {
        return parseRawDateTime(input, " ");
    }

    public static LocalDateTime parseRawDateTime(String input, String mode) throws InvalidParameterException,
        NullPointerException, DateTimeException {
        String dateTimeInput;
        int matchCount = 0;
        // If user uses a string token as a separator between two datetimes, say "to", remove from the string.
        String[] tokens = input.replaceAll("[\\s]+[\\D]+[\\s]+|,", " ").split("[\\s]+");

        String[] output = new String[2];
        String date = "";
        if (RegexMatcher.alphabetMatcher(input).find()) {
            throw new InvalidParameterException();
        }
        if (mode.equals("date")) {
            date = parseDateTime(tokens[0], "date");
            output[0] = date + "T" + Constants.PLACEHOLDER_TIME;
            output[1] = "\0";
        } else if (mode.equals("time")) {
            date = Constants.PLACEHOLDER_DATE;
            output[0] = date + "T" + parseDateTime(tokens[0], "time");
            output[1] = "\0";
        } else if (tokens.length == 2) {
            // Considers the case with date and time
            date = parseDateTime(tokens[0], "date");
            output[0] = date + "T" + parseDateTime(tokens[1], "time");
            output[1] = "\0";
        } else if (tokens.length == 3) {
            // Considers the case with date, start time and end time
            date = parseDateTime(tokens[0], "date");
            output[0] = date + "T" + parseDateTime(tokens[1], "time");
            output[1] = date + "T" + parseDateTime(tokens[2], "time");
        } else if (tokens.length == 4) {
            // Considers the case with start date, end date, start time and end time.
            date = parseDateTime(tokens[0], "date");
            output[0] = date + "T" + parseDateTime(tokens[1], "time");
            date = parseDateTime(tokens[2], "date");
            output[1] = date + "T" + parseDateTime(tokens[3], "time");
        } else {
            throw new InvalidParameterException();
        }

        // If two datetimes are to be given, then segregate them with "," in a single string output.
        dateTimeInput = String.join("", output).trim();
        return LocalDateTime.parse(dateTimeInput);
    }

    /**
     * Parse strings to produce date or time depending on mode specified in arguments.
     * @param input Input by user to set date or time
     * @param mode Mode set by user, can be "date" or "time" depends on input to be parsed
     * @return Formatted date or time for parsing with the DateTime class.
     */
    public static String parseDateTime(String input, final String mode) {
        final String partition = mode.equals("time") ? ":" :
                mode.equals("date") ? "-" : "?";
        String[] output = new String[3];
        if (input.matches("[0-9]{4}") && mode.equals("time")) {
            // If input time format is XXXX, replace with XX:XX:00.
            input = input.substring(0,2) + "," + input.substring(2) + ",00";
        } else if (input.matches("[0-9]{6}") && mode.equals("date")) {
            // If input date format is XXXXXX, replace with XX,XX,XX
            input = input.substring(0,2) + "," + input.substring(2, 4) + "," + input.substring(4);
        } else if (input.matches("[0-9]{8}") && mode.equals("date")) {
            // If input date format is XXXXXX, replace with XX,XX,XX
            input = input.substring(0,2) + "," + input.substring(2, 4) + "," + input.substring(6);
        }
        // Replaces on no-digit numbers with the same string "c"
        // such that they can be manipulated easily regardless of string.
        input = input.replaceAll("[\\D]+", "c");
        String[] tokens = input.split("c");
        for (int i = 0; i < output.length; i++) {
            if (i > tokens.length - 1) {
                // If subsequent pair of digits is undefined by simplified input, set to 0.
                output[i] = "01";
            } else if (tokens[i].length() < 2) {
                // If token is 1 or 0 digits long, fill the remaining space with 0 such that a 2 digit number is formed.
                output[i] = "0" + tokens[i];
            } else if (i == 0 && tokens[i].matches("[0-9]{2}") && mode.equals("date")) {
                // If year given is YY
                output[i] = "20" + tokens[i].substring(0,2);
            } else {
                output[i] = tokens[i];
            }
        }
        return String.join(partition, output);
    }
}