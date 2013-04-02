package ru.niktrop.remote_access.handlers;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import ru.niktrop.remote_access.commands.SerializableCommand;
import ru.niktrop.remote_access.controller.CommandManager;
import ru.niktrop.remote_access.controller.Controller;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 14.03.13
 * Time: 17:10
 */

/**
 * Executes received command.
 * */
public class CommandExecutor extends SimpleChannelUpstreamHandler {

  private final CommandManager commandManager;

  public CommandExecutor(Controller controller) {
    this.commandManager = controller.getCommandManager();
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    SerializableCommand command = (SerializableCommand) e.getMessage();

    commandManager.executeCommand(command);
  }

}
