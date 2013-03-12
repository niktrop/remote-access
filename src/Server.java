import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 07.03.13
 * Time: 12:50
 */
public class Server {
  private static int port = 11111;
  private static String host = "localhost";
  private static Iterable<Path> dirs = FileSystems.getDefault().getRootDirectories();
  private static final int MAX_DEPTH = 2;

  public static void main(String[] args) throws Exception {
    WatchService watcther = FileSystems.getDefault().newWatchService();
    List<FSImage> fsImages = new ArrayList<>();
    for (Path dir : dirs) {
      if (!Files.isDirectory(dir)) {
        continue;
      }
      FSImage fsi = FSImages.getFromDirectory(dir, MAX_DEPTH, watcther);
      fsImages.add(fsi);
      System.out.println(dir.toString());
    }
    final Controller controller = new Controller(fsImages, watcther);

    // Configure the server.
    ServerBootstrap bootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(),
                    Executors.newCachedThreadPool()));

    // Set up the pipeline factory.
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("decoder", new StringDecoder());
        pipeline.addLast("encoder", new StringEncoder());
        pipeline.addLast("commandHandler", new CommandHandler(controller));

        return pipeline;
      }
    });

    // Bind and start to accept incoming connections.
    bootstrap.bind(new InetSocketAddress(host, port));
    System.out.println("Listening...");
  }
}
