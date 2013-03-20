package ru.niktrop.remote_access.commands;

import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 07.03.13
 * Time: 23:56
 */
public class Commands {
  private static final Logger LOG = Logger.getLogger(Commands.class.getName());


  public static SerializableCommand getFromString(String serialized)
          throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    String groupSeparator = "\u001E";
    StringTokenizer st = new StringTokenizer(serialized, groupSeparator, false);

    String className = st.nextToken();
    String stringRepresentation = serialized.substring(className.length());

    Class<?> commandClass = null;
    try {
      commandClass = Class.forName(className);
    } catch (ClassNotFoundException e) {
      LOG.log(Level.WARNING, "Could not find command class with such name: " + className, e.getCause());
    }

    SerializableCommand instance = null;
    try {
      instance = (SerializableCommand) commandClass.newInstance();
    } catch (InstantiationException e) {
      LOG.log(Level.WARNING, "Error in creating instance of serializable command object.", e.getCause());
    } catch (IllegalAccessException e) {
      LOG.log(Level.WARNING, "Class " + className + " must have constructor without parameters.", e.getCause());
    }
    return instance.fromString(stringRepresentation);
  }


  public static String serializeToString(SerializableCommand command) {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';

    String className = command.getClass().getCanonicalName();

    builder.append(className);
    builder.append(groupSeparator);

    builder.append(command.getStringRepresentation());

    return builder.toString();
  }


}
