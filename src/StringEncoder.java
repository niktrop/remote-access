import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 07.03.13
 * Time: 19:06
 */
public class StringEncoder extends SimpleChannelHandler{

  /**
   * Encodes String to a ChannelBuffer.
   * First 4 bytes represent length of the rest of the byte array.
   */
  @Override
  public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    String message = (String) e.getMessage();
    byte[] messageToBytes = message.getBytes(StandardCharsets.UTF_8);
    byte[] lengthAsBytes = ByteBuffer.allocate(4).putInt(messageToBytes.length).array();
    ChannelBuffer buf = ChannelBuffers.wrappedBuffer(lengthAsBytes, messageToBytes);

    Channels.write(ctx, e.getFuture(), buf);
  }
}
