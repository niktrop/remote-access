package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.Controller;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 06.03.13
 * Time: 21:45
 */
public interface SerializableCommand {

  /**
   * Returns response actions for sending back.
   */
  List<SerializableCommand> execute(Controller controller);
  String getStringRepresentation();
  SerializableCommand fromString(String string);
}
