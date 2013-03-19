package ru.niktrop.remote_access.commands;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import ru.niktrop.remote_access.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 18.03.13
 * Time: 15:01
 */
public class CommandManager {
  private static final Logger LOG = Logger.getLogger(CommandManager.class.getName());
  private static HashMap<Controller, CommandManager> commandManagers = new HashMap<>();
  private final Controller controller;

  private CommandManager(Controller controller) {
    this.controller = controller;
  }

  public static CommandManager instance(Controller controller) {
    CommandManager commandManager = commandManagers.get(controller);
    if (commandManagers.get(controller) == null) {
      CommandManager newCommandManager = new CommandManager(controller);
      return newCommandManager;
    }
    else return commandManager;
  }

  public void sendCommand(SerializableCommand command, Channel channel) {
    if (channel == null) {
      LOG.warning("Attempt to send command while channel is null.");
      return;
    }

    try {
      channel.write(command);
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Sending command problem.", e.getCause());
    }
  }

  public List<SerializableCommand> executeCommand(SerializableCommand command) {
    List<SerializableCommand> response = command.execute(controller);
    LOG.info("Command executed: " + command.getClass().getSimpleName());
    controller.fireControllerChange();
    return response;
  }

  public void sendResponseBack(List<SerializableCommand> response, ChannelHandlerContext ctx) {
    for (SerializableCommand command : response) {
      Channels.write(ctx.getChannel(), command);
    }
  }
}
