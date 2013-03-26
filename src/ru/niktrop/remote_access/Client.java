package ru.niktrop.remote_access;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import ru.niktrop.remote_access.commands.GetFSImages;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;
import ru.niktrop.remote_access.gui.ClientGUI;
import ru.niktrop.remote_access.handlers.*;

import javax.swing.*;
import java.io.IOException;
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
 * Date: 12.03.13
 * Time: 9:25
 */
public class Client {
  private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Client.class.getName());

  private static int commandPort = 12345;
  private static int filePort = 12346;

  private static String host = "localhost";
  private static Path[] dirs = {Paths.get("C:\\\\", "TestClient")};
  private static final int MAX_DEPTH = 2;

  public static void main(String[] args) throws IOException, InterruptedException {

    final Controller controller = Controllers.getClientController();
    WatchService watchService = controller.getWatchService();

    for (Path dir : dirs) {
      if (!Files.isDirectory(dir)) {
        continue;
      }
      FSImage fsi = FSImages.getFromDirectory(dir, MAX_DEPTH, watchService);
      controller.addFSImage(fsi);
      LOG.info(fsi.getRootAlias() + " added to client's controller");
    }


    ChannelFuture commandFuture = getCommandChannelFuture(controller);

    commandFuture.sync();
    Channel commandChannel = commandFuture.getChannel();

    controller.getCommandManager().setChannel(commandChannel);
    LOG.info("channel is ready");

    CommandManager commandManager = controller.getCommandManager();
    commandManager.sendCommand(new GetFSImages());

    FileSystemWatcher fsWatcher = new FileSystemWatcher(controller);
    FSChangeHandler fsHandler = new FSChangeHandler(fsWatcher, controller);
    fsWatcher.runWatcher();
    fsHandler.runHandler();

    ChannelFuture fileFuture = getFileChannelFuture(controller);

    fileFuture.sync();
    controller.getFileTransferManager().setChannel(fileFuture.getChannel());

    final ClientGUI clientGUI = ClientGUI.instance();
    controller.getNotificationManager().setParentFrame(clientGUI);

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        clientGUI.init(controller);

      }
    });
  }

  private static ChannelFuture getFileChannelFuture(final Controller controller) {
    // Configure the file transfer client.
    ClientBootstrap fileBootstrap = new ClientBootstrap(
            new NioClientSocketChannelFactory(
                    Executors.newFixedThreadPool(1),
                    Executors.newFixedThreadPool(2)));

    // Set up the pipeline factory.
    fileBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        //pipeline.addLast("logger",  new Logger(Level.INFO));
        pipeline.addLast("file receiver", new FileReceiver(controller));

        return pipeline;
      }
    });

    // Bind and start to accept incoming connections.
    return fileBootstrap.connect(new InetSocketAddress(host, filePort));
  }

  private static ChannelFuture getCommandChannelFuture(final Controller controller) {
    ChannelFactory factory = new NioClientSocketChannelFactory(
            Executors.newFixedThreadPool(1),
            Executors.newFixedThreadPool(2));

    ClientBootstrap bootstrap = new ClientBootstrap(factory);

    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        pipeline.addLast("string decoder", new StringDecoder());
        pipeline.addLast("string encoder", new StringEncoder());
        pipeline.addLast("command decoder", new CommandDecoder());
        pipeline.addLast("command encoder", new CommandEncoder());
        pipeline.addLast("logger", new LoggerHandler(Level.INFO));
        pipeline.addLast("executor", new CommandExecutor(controller));

        return pipeline;
      }
    });

    return bootstrap.connect(new InetSocketAddress(host, commandPort));
  }
}
