package ru.niktrop.remote_access;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import ru.niktrop.remote_access.commands.SerializableCommand;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 14.03.13
 * Time: 17:10
 */
public class CommandExecutorHandler extends SimpleChannelUpstreamHandler {

  private final Controller controller;

  public CommandExecutorHandler(Controller controller) {
    this.controller = controller;
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    SerializableCommand command = (SerializableCommand) e.getMessage();
    controller.executeCommand(command);
  }

}
