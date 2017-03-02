import java.util.HashMap;

/**
 * Provides various methods for parsing command line arguments given to the main program
 */
public class ArgumentParser {

  //map containing all of the arguments and their values (initally empty)
  private HashMap<String, String> argumentsValues = new HashMap<>();

  /**
   * Creates an argument parser, parsing the given arguments into a Map
   *
   * @param args The arguments given to the Main method
   */
  public ArgumentParser(String[] args) {
    argumentsValues = new HashMap<>();

    //populate the Hashmap with the given arguments and their values
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("-")) {
        String key = arg;
        String value;
        if (i + 1 == args.length || args[i + 1].startsWith("-")) {
          value = null;
        } else {
          value = args[i + 1];
        }
        argumentsValues.put(key, value);
      }
    }
  }

  /**
   * Returns the value associated with the given argument or null if it does not exist
   *
   * @param argument The argument whose value will be retrieved
   * @return The value mapped to the given argument
   */
  public String getValueForArgument(String argument) {
    return argumentsValues.get(argument);
  }

  /**
   * Returns whether the given argument exists or not
   *
   * @param argument The argument to find
   * @return whether the given argument exists or not
   */
  public boolean containsArgument(String argument) {
    return argumentsValues.containsKey(argument);
  }
}
