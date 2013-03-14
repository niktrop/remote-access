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
public class CommandDecoderHandler extends SimpleChannelHandler {
  private static final Logger LOG = Logger.getLogger(StringDecoder.class.getName());

  /**
   * Produces String representation of written SerializableCommand object.
   * Beginning of the String encodes type of the object.
   */
  @Override
  public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    SerializableCommand command = (SerializableCommand) e.getMessage();
    String serialized = Commands.serializeToString(command);

    Channels.write(ctx, e.getFuture(), serialized);
  }

  /**
   * Transforms String message to a SerializableCommand instance.
   *
   */
  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    String serialized = (String) e.getMessage();

    SerializableCommand command = Commands.getFromString(serialized);
    Channels.write(ctx, e.getFuture(), command);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    LOG.log(Level.WARNING, null, e.getCause());
    e.getChannel().close();
  }

}
