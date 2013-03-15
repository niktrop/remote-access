package ru.niktrop.remote_access.handlers;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import ru.niktrop.remote_access.commands.Commands;
import ru.niktrop.remote_access.commands.SerializableCommand;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 15.03.13
 * Time: 9:14
 */
public class CommandEncoder extends OneToOneEncoder {

  /**
   * Transforms String message to a SerializableCommand instance.
   */
  @Override
  protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
    SerializableCommand command = (SerializableCommand) msg;
    return Commands.serializeToString(command);
  }
}
