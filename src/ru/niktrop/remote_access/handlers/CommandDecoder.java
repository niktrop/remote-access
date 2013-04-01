package ru.niktrop.remote_access.handlers;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import ru.niktrop.remote_access.commands.Commands;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 07.03.13
 * Time: 19:04
 */
public class CommandDecoder extends OneToOneDecoder {
  /**
   * Transforms String message to a SerializableCommand instance.
   */
  @Override
  protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
    String serialized = (String) msg;

    return Commands.getFromString(serialized);
  }
}
