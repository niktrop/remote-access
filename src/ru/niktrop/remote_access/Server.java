package ru.niktrop.remote_access;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
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
import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 15.03.13
 * Time: 9:46
 */
public class Server {
  private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Server.class.getName());

  private static int commandPort = 12345;
  private static int filePort = 12346;
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
    Channel commandChannel = getCommandChannel(controller);
    controller.getCommandManager().setChannel(commandChannel);

    CommandManager commandManager = controller.getCommandManager();
    commandManager.sendCommand(new GetFSImages());

    FileSystemWatcher fsWatcher = new FileSystemWatcher(controller);
    FSChangeHandler fsHandler = new FSChangeHandler(fsWatcher, controller);
    fsWatcher.runWatcher();
    fsHandler.runHandler();

    if (commandChannel.isBound())
      LOG.info("Listening...");

    // Configure the file transfer server.
    Channel filechannel = getFileTransferChannel(controller);
    controller.getFileTransferManager().setChannel(filechannel);
  }

  private static Channel getCommandChannel(final Controller controller) {
    // Configure command server.
    ServerBootstrap bootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(
                    Executors.newFixedThreadPool(1),
                    Executors.newFixedThreadPool(2)));

    // Set up the pipeline factory.
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        pipeline.addLast("channel saver", new ChannelSaver(controller.getCommandManager()));
        pipeline.addLast("string decoder", new StringDecoder());
        pipeline.addLast("string encoder", new StringEncoder());
        pipeline.addLast("command decoder", new CommandDecoder());
        pipeline.addLast("command encoder", new CommandEncoder());
        pipeline.addLast("logger",  new Logger(Level.INFO));
        pipeline.addLast("executor", new CommandExecutor(controller));

        return pipeline;
      }
    });

    // Bind and start to accept incoming connections.
    return bootstrap.bind(new InetSocketAddress(host, commandPort));
  }

  private static Channel getFileTransferChannel(final Controller controller) {
    ServerBootstrap fileBootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(
                    Executors.newFixedThreadPool(1),
                    Executors.newFixedThreadPool(2)));

    // Set up the pipeline factory.
    fileBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        //pipeline.addLast("logger",  new Logger(Level.INFO));
        pipeline.addLast("channel saver", new ChannelSaver(controller.getFileTransferManager()));
        pipeline.addLast("file receiver", new FileReceiver(controller));

        return pipeline;
      }
    });

    // Bind and start to accept incoming connections.
    return fileBootstrap.bind(new InetSocketAddress(host, filePort));
  }
}
