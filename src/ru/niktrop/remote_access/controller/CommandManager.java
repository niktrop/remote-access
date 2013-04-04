package ru.niktrop.remote_access.controller;

import org.jboss.netty.channel.Channel;
import ru.niktrop.remote_access.commands.SerializableCommand;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 18.03.13
 * Time: 15:01
 */

/**
 * Class, responsible for executing and sending SerializableCommands.
 * Does it in two different threads. Manage one of two channels used by the application.
 * */
public class CommandManager implements ChannelManager {
  private static final Logger LOG = Logger.getLogger(CommandManager.class.getName());
  private final Controller controller;

  private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor();
  private final ExecutorService commandSender = Executors.newSingleThreadExecutor();

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
    commandSender.submit(new SendTask(command));
  }

  public void executeCommand(SerializableCommand command){
    if (command != null) {
      commandExecutor.submit(new ExecuteTask(command));
    }
  }

  private class SendTask implements Runnable {
    private final SerializableCommand command;

    private SendTask(SerializableCommand command) {
      this.command = command;
    }

    @Override
    public void run() {
      //check channel and wait connection
      while (channel == null || !channel.isConnected()) {
        LOG.fine("Attempt to send command while channel is not connected.");
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          LOG.log(Level.WARNING, "Waiting for reconnection was interrupted.");
        }
        continue;
      }

      //send command
      try {
        channel.write(command);
      } catch (Exception e) {
        String message = String.format("Problem with sending to %s.", channel.getRemoteAddress());
        LOG.log(Level.WARNING, message, e.getCause());
      }
    }
  }

  private class ExecuteTask implements Runnable {
    private final SerializableCommand command;

    private ExecuteTask(SerializableCommand command) {
      this.command = command;
    }

    @Override
    public void run() {
      try {
        command.execute(controller);
        LOG.log(Level.FINE, "Command executed: " + command.getClass().getSimpleName());
        //controller.fireControllerChange();
      } catch (Exception e) {
        String message = String.format("%s occured when executing %s",
                e.toString(), command.getClass().getSimpleName());
        LOG.log(Level.WARNING, message, e.getCause());
      }
    }
  }

}
