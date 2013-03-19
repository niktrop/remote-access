package ru.niktrop.remote_access.handlers;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.commands.CommandManager;
import ru.niktrop.remote_access.commands.SerializableCommand;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 14.03.13
 * Time: 17:10
 */
public class CommandExecutor extends SimpleChannelUpstreamHandler {

  private final CommandManager commandManager;

  public CommandExecutor(Controller controller) {
    this.commandManager = CommandManager.instance(controller);
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    SerializableCommand command = (SerializableCommand) e.getMessage();

    List<SerializableCommand> response = commandManager.executeCommand(command);
    commandManager.sendResponseBack(response, ctx);
  }

}
