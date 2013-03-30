package ru.niktrop.remote_access;

import org.jboss.netty.channel.Channel;
import ru.niktrop.remote_access.commands.SerializableCommand;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
  private final BlockingQueue<SerializableCommand> commandsToExecute = new LinkedBlockingQueue<>();
  private final BlockingQueue<SerializableCommand> commandsToSend = new LinkedBlockingQueue<>();
  private Channel channel;

  private final Thread commandExecutor = new CommandExecutor("CommandExecutor");
  private final Thread commandSender = new CommandSender("CommandSender");

  public CommandManager(Controller controller) {
    this.controller = controller;
    commandExecutor.start();
    commandSender.start();
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
    commandsToSend.offer(command);
  }

  public void executeCommand(SerializableCommand command){
    commandsToExecute.offer(command);
  }

  private class CommandExecutor extends Thread {

    CommandExecutor(String name) {
      super(name);
    }

    @Override
    public void run() {
      while(true) {
        //get command from the queue
        SerializableCommand command = null;
        try {
          command = commandsToExecute.take();
        } catch (InterruptedException e) {
          LOG.log(Level.WARNING, "CommandExecutor thread waiting was interrupted: ", e);
          continue;
        }

        //execute command
        try {
          command.execute(controller);
          //LOG.info("Command executed: " + command.getClass().getSimpleName());
          controller.fireControllerChange();
        } catch (Exception e) {
          String message = String.format("%s occured when executing %s", e.toString(), command.getStringRepresentation());
          LOG.log(Level.WARNING, message, e.getCause());
          e.printStackTrace();
        }
      }
    }
  }

  private class CommandSender extends Thread {
    CommandSender(String name) {
      super(name);
    }
    @Override
    public void run() {
      SerializableCommand command = null;
      while(true) {

        //get command from the queue, if previous was sent
        if (command == null) {
          try {
            command = commandsToSend.take();
          } catch (InterruptedException e) {
            LOG.log(Level.WARNING, "CommandSender waiting was interrupted: ", e);
            continue;
          }
        } else {

          //check channel
          if (channel == null || !channel.isConnected()) {
            LOG.warning("Attempt to send command while channel is not connected.");
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
            continue;
          }

          //clear command
          command = null;
        }
      }
    }
  }

}
