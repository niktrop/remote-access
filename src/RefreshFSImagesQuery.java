import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 11.03.13
 * Time: 11:17
 */
public class RefreshFSImagesQuery implements SerializableCommand {

  /**
   * Results in sending all local FSImages to the other side of the channel.
   * Should be sent from client to server when connected first time.
   * */
  @Override
  public void execute(Controller controller, ChannelHandlerContext ctx) {
    Map<String,FSImage> fsImageMap = controller.getFsImageMap();
    Channel channel = ctx.getChannel();
    for (FSImage fsi : fsImageMap.values()) {
      String uuid = fsi.getUuid();
      String xml = fsi.toXml();
      FSChange fsChange = new FSChange(ChangeType.NEW_IMAGE, uuid, null, xml);
      Channels.write(channel, fsChange);
    }
  }

  @Override
  public String getStringRepresentation() {
    return "";
  }

  @Override
  public SerializableCommand fromString(String string) {
    return new RefreshFSImagesQuery();
  }
}
