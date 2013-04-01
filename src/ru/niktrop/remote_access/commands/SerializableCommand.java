package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.Controller;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 06.03.13
 * Time: 21:45
 */

/**
 * All implementations should have parameterless constructor for the deserialization.
 * */
public interface SerializableCommand {

  /**
   * Does all the work. Controller encapsulates all resources, needed for execution.
   * */
  void execute(Controller controller);

  /**
   * Should build string, which contains all information for command execution.
   */
  String getStringRepresentation();

  /**
   * Should reconstruct the instance of the SerializedCommand from string representation.
   * */
  SerializableCommand fromString(String string);

}
