import java.util.StringTokenizer;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 07.03.13
 * Time: 23:56
 */
public class Commands {

  /**
   *
   * */
  public static SerializableCommand getFromString(String serialized)
          throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    String groupSeparator = "\u001E";
    StringTokenizer st = new StringTokenizer(serialized, groupSeparator, false);

    String className = st.nextToken();
    String stringRepresentation = serialized.substring(className.length());

    SerializableCommand instance = (SerializableCommand) Class.forName(className).newInstance();
    return instance.fromString(stringRepresentation);
  }

  /**
   * Builds string, which begins with full class name of FSChange.
   * The rest of the string contains all information for command execution.
   */
  public static String serializeToString(SerializableCommand command) {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';
    char unitSeparator = '\u001F';

    String className = command.getClass().getCanonicalName();

    builder.append(className);
    builder.append(groupSeparator);

    builder.append(command.getStringRepresentation());

    return builder.toString();
  }


}
