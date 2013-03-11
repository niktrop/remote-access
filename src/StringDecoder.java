import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 07.03.13
 * Time: 12:58
 */
public class StringDecoder extends FrameDecoder{
  private static final Logger LOG = Logger.getLogger(StringDecoder.class.getName());

  @Override
  protected String decode(ChannelHandlerContext ctx,
                          Channel channel,
                          ChannelBuffer buf) throws Exception {

    if (buf.readableBytes() < 4) {
      return null;
    }

    buf.markReaderIndex();
    int length = buf.readInt();

    if (buf.readableBytes() < length) {
      buf.resetReaderIndex();
      return null;
    }

    ChannelBuffer frame = buf.readBytes(length);
    return frame.toString(StandardCharsets.UTF_8);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    LOG.log(Level.WARNING, null, e.getCause());
    e.getChannel().close();
  }
}
