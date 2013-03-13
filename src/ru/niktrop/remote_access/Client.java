package ru.niktrop.remote_access;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 12.03.13
 * Time: 9:25
 */
public class Client {

  private static int port = 11111;
  private static String host = "localhost";
  private static Iterable<Path> dirs = FileSystems.getDefault().getRootDirectories();
  private static final int MAX_DEPTH = 2;

  public static void main(String[] args) throws IOException, InterruptedException {

    final Controller controller = Controllers.getClientController();
    WatchService watcher = controller.getWatcher();

    for (Path dir : dirs) {
      if (!Files.isDirectory(dir)) {
        continue;
      }
      FSImage fsi = FSImages.getFromDirectory(dir, MAX_DEPTH, watcher);
      controller.addFSImage(fsi);
      System.out.println(dir.toString());
    }


    ChannelFactory factory = new NioClientSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool());

    ClientBootstrap bootstrap = new ClientBootstrap(factory);

    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("decoder", new StringDecoder());
        pipeline.addLast("encoder", new StringEncoder());
        pipeline.addLast("commandHandler", new CommandHandler(controller));

        return pipeline;
      }
    });

    ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
    future.sync();
    System.out.println("Connection established");
  }
}
