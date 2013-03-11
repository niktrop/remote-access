import org.jboss.netty.channel.ChannelHandlerContext;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 06.03.13
 * Time: 21:45
 */
public interface SerializableCommand {
  public void execute(Controller controller, ChannelHandlerContext ctx);
  String getStringRepresentation();
  SerializableCommand fromString(String string);
}
