package ru.niktrop.remote_access;

import org.jboss.netty.channel.*;
import ru.niktrop.remote_access.commands.Commands;
import ru.niktrop.remote_access.commands.SerializableCommand;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 07.03.13
 * Time: 19:04
 */
public class CommandHandler extends SimpleChannelHandler {
  private static final Logger LOG = Logger.getLogger(StringDecoder.class.getName());

  private final Controller controller;

  public CommandHandler(Controller controller) {
    this.controller = controller;
  }

  /**
   * Produces String representation of written ru.niktrop.remote_access.commands.SerializableCommand object.
   * Beginning of the String encodes type of the object.
   */
  @Override
  public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    SerializableCommand command = (SerializableCommand) e.getMessage();
    String serialized = Commands.serializeToString(command);

    Channels.write(ctx, e.getFuture(), serialized);
  }

  /**
   * Transforms String message to a ru.niktrop.remote_access.commands.SerializableCommand instance.
   *
   */
  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    String serialized = (String) e.getMessage();

    Commands.getFromString(serialized).execute(controller, ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    LOG.log(Level.WARNING, null, e.getCause());
    e.getChannel().close();
  }

}
