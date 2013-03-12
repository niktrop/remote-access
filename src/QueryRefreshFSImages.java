import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 11.03.13
 * Time: 11:17
 */
public class QueryRefreshFSImages implements SerializableCommand {

  /**
   * Results in sending all local FSImages to the other side of the channel.
   * Should be sent from client to server when connected first time.
   * */
  @Override
  public void execute(Controller controller, ChannelHandlerContext ctx) {
    Iterable<FSImage> fsImages = controller.getFSImages();
    Channel channel = ctx.getChannel();
    for (FSImage fsi : fsImages) {
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
    return new QueryRefreshFSImages();
  }
}
