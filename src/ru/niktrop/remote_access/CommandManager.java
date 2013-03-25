package ru.niktrop.remote_access;

import org.jboss.netty.channel.Channel;
import ru.niktrop.remote_access.commands.SerializableCommand;

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

  public void sendCommand(SerializableCommand command) {
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

  public void executeCommand(SerializableCommand command){
    try {
      command.execute(controller);
      LOG.info("Command executed: " + command.getClass().getSimpleName());
      controller.fireControllerChange();
    } catch (Exception e) {
      String message = String.format("%s occured when executing %s", e.toString(), command.getClass().getSimpleName());
      LOG.log(Level.WARNING, message, e.getCause());
    }
  }

}
