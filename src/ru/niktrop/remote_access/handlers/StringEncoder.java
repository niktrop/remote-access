package ru.niktrop.remote_access.handlers;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 07.03.13
 * Time: 19:06
 */
public class StringEncoder extends OneToOneEncoder{

  /**
   * Encodes String to a ChannelBuffer.
   * First 4 bytes represent length of the rest of the byte array.
   */
  @Override
  protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
    String message = (String) msg;
    byte[] messageToBytes = message.getBytes(StandardCharsets.UTF_8);
    byte[] lengthAsBytes = ByteBuffer.allocate(4).putInt(messageToBytes.length).array();
    ChannelBuffer buf = ChannelBuffers.wrappedBuffer(lengthAsBytes, messageToBytes);

    return buf;
  }



}
