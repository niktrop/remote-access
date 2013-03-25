package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.Controller;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 06.03.13
 * Time: 21:45
 */

/**
 * All implementations should have parameterless constructor for deserialization.
 * */
public interface SerializableCommand {

  /**
   * Returns response actions for sending back.
   */
  void execute(Controller controller);

  /**
   * Should build string, which begins with full class name of the SerializedCommand.
   * The rest of the string contains all information for command execution.
   * Two parts of the string separated by the character '\u001E' ("group separator").
   */
  String getStringRepresentation();

  /**
   * Should return the instance of the SerializedCommand.
   * Argument should match part of the string representation after separator (without class name).
   */
  SerializableCommand fromString(String string);

}
