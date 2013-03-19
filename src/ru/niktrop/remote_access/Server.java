package ru.niktrop.remote_access;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import ru.niktrop.remote_access.commands.CommandManager;
import ru.niktrop.remote_access.commands.GetFSImages;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;
import ru.niktrop.remote_access.handlers.*;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.util.concurrent.Executors;

import static org.jboss.netty.logging.InternalLogLevel.INFO;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 15.03.13
 * Time: 9:46
 */
public class Server {
  private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Server.class.getName());

  private static int port = 12345;
  private static String host = "localhost";
  private static Path[] dirs = {Paths.get("C:\\\\", "Test")};
  private static final int MAX_DEPTH = 2;

  public static void main(String[] args) throws Exception {

    final Controller controller = Controllers.getServerController();
    WatchService watchService = controller.getWatchService();

    for (Path dir : dirs) {
      if (!Files.isDirectory(dir)) {
        continue;
      }
      FSImage fsi = FSImages.getFromDirectory(dir, MAX_DEPTH, watchService);
      controller.addFSImage(fsi);
      LOG.info(fsi.getRootAlias() + " added to server's controller");
    }

    // Configure the server.
    ServerBootstrap bootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(),
                    Executors.newCachedThreadPool()));

    // Set up the pipeline factory.
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        pipeline.addLast("logger", new LoggingHandler(INFO));
        pipeline.addLast("channel saver", new ChannelSaver(controller));
        pipeline.addLast("string decoder", new StringDecoder());
        pipeline.addLast("string encoder", new StringEncoder());
        pipeline.addLast("command decoder", new CommandDecoder());
        pipeline.addLast("command encoder", new CommandEncoder());
        pipeline.addLast("executor", new CommandExecutor(controller));

        return pipeline;
      }
    });

    // Bind and start to accept incoming connections.
    Channel channel = bootstrap.bind(new InetSocketAddress(host, port));
    controller.setChannel(channel);

    CommandManager commandManager = CommandManager.instance(controller);
    commandManager.sendCommand(new GetFSImages(), channel);

    FileSystemWatcher fsWatcher = new FileSystemWatcher(controller);
    FSChangeHandler fsHandler = new FSChangeHandler(fsWatcher, controller);
    fsWatcher.runWatcher();
    fsHandler.runHandler();

    if (channel.isBound())
      LOG.info("Listening...");
  }
}
