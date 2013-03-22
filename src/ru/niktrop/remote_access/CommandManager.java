package ru.niktrop.remote_access;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import ru.niktrop.remote_access.commands.SerializableCommand;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 18.03.13
 * Time: 15:01
 */
public class CommandManager implements ChannelManager {
  private static final Logger LOG = Logger.getLogger(CommandManager.class.getName());
  private final Controller controller;
  private Channel channel;

  public CommandManager(Controller controller) {
    this.controller = controller;
  }

  @Override
  public Channel getChannel() {
    return channel;
  }

  @Override
  public void setChannel(Channel channel) {
    this.channel = channel;
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

  public List<SerializableCommand> executeCommand(SerializableCommand command){
    List<SerializableCommand> response = Collections.emptyList();
    try {
      response = command.execute(controller);
      LOG.info("Command executed: " + command.getClass().getSimpleName());
      controller.fireControllerChange();
    } catch (Exception e) {
      String message = String.format("%s occured when executing %s", e.toString(), command.getClass().getSimpleName());
      LOG.log(Level.WARNING, message, e.getCause());
    }
    return response;
  }

  public void sendResponseBack(List<SerializableCommand> response, ChannelHandlerContext ctx) {
    for (SerializableCommand command : response) {
      Channels.write(ctx.getChannel(), command);
    }
  }
}
