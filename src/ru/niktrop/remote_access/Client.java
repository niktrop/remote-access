package ru.niktrop.remote_access;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import ru.niktrop.remote_access.commands.GetFSImages;
import ru.niktrop.remote_access.commands.Notification;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;
import ru.niktrop.remote_access.gui.ClientGUI;
import ru.niktrop.remote_access.handlers.*;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;


/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 12.03.13
 * Time: 9:25
 */
public class Client {
  private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Client.class.getName());

  private static int commandPort;
  private static int filePort;
  private static String host;

  private static Map<Path,String> directoriesAndAliases = new HashMap<>();
  private static String propFileName = "client.properties";
  private static Controller controller;
  private static final int MAX_DEPTH = 2;

  private static int waitConnection;

  public static void main(String[] args) {

    loadProperties(propFileName);

    setupController();

    createFSImages();

    startFileSystemWatching();

    setupCommandChannel();

    setupFileTransferChannel();

    final ClientGUI clientGUI = ClientGUI.instance();
    controller.getNotificationManager().setParentFrame(clientGUI);

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        clientGUI.init(controller);

        //Query to the server for its FSImages
        controller.getCommandManager().sendCommand(new GetFSImages());
      }
    });
  }

  private static void setupController() {
    try {
      controller = Controllers.getClientController();
      controller.setMaxDepth(MAX_DEPTH);
    } catch (IOException e) {
      String message = "Couldn't initialize WatchService and create controller.";
      LOG.log(Level.WARNING, message, e.getCause());
      Notification warning = Notification.warning(message);
      controller.getNotificationManager().show(warning);
      System.exit(1);
    }
  }

  private static void startFileSystemWatching() {
    FileSystemWatcher fsWatcher = new FileSystemWatcher(controller);
    FSChangeHandler fsHandler = new FSChangeHandler(fsWatcher, controller);
    fsWatcher.runWatcher();
    fsHandler.runHandler();
  }

  private static void createFSImages() {
    WatchService watchService = controller.getWatchService();

    Iterable<Path> dirs = null;
    if (directoriesAndAliases.isEmpty()) {
      dirs = FileSystems.getDefault().getRootDirectories();
    } else {
      dirs = directoriesAndAliases.keySet();
    }
    for (Path dir : dirs) {
      if (!Files.isDirectory(dir)) {
        String message = String.format("Not a directory: %s", dir.toString());
        LOG.log(Level.WARNING, message);
        Notification warning = Notification.warning(message);
        controller.getNotificationManager().show(warning);
        continue;
      }
      FSImage fsi = null;

      try {
        fsi = FSImages.getFromDirectory(dir, controller.getMaxDepth(), watchService);
        String alias = directoriesAndAliases.get(dir);
        if (alias == null) {
          alias = dir.toString();
        }
        fsi.setRootAlias(alias);
        controller.addFSImage(fsi);
        LOG.info(fsi.getRootAlias() + " added to client's controller");
      } catch (IOException e) {
        String message = String.format("Couldn't build image of %s", dir.toString());
        LOG.log(Level.INFO, message, e.getCause());
        Notification warning = Notification.warning(message);
        controller.getNotificationManager().show(warning);
      }
    }
  }

  private static void setupFileTransferChannel() {
    // Configure the file transfer client.
    final ClientBootstrap fileBootstrap = new ClientBootstrap(
            new NioClientSocketChannelFactory(
                    Executors.newFixedThreadPool(1),
                    Executors.newFixedThreadPool(2)));

    // Set up the pipeline factory.
    fileBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        //pipeline.addLast("logger",  new Logger(Level.PLAIN));
        pipeline.addLast("reconnector", new Reconnector(fileBootstrap, controller.getFileTransferManager()));
        pipeline.addLast("file receiver", new FileReceiver(controller));

        return pipeline;
      }
    });

    ChannelFuture fileFuture = fileBootstrap.connect(new InetSocketAddress(host, filePort));
    waitAndSetupChannel(fileFuture, controller.getFileTransferManager());
  }

  private static void setupCommandChannel() {
    ChannelFactory factory = new NioClientSocketChannelFactory(
            Executors.newFixedThreadPool(1),
            Executors.newFixedThreadPool(2));

    final ClientBootstrap bootstrap = new ClientBootstrap(factory);

    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        pipeline.addLast("reconnector", new Reconnector(bootstrap, controller.getCommandManager()));
        pipeline.addLast("string decoder", new StringDecoder());
        pipeline.addLast("string encoder", new StringEncoder());
        pipeline.addLast("command decoder", new CommandDecoder());
        pipeline.addLast("command encoder", new CommandEncoder());
        pipeline.addLast("logger", new LoggerHandler(Level.FINE));
        pipeline.addLast("executor", new CommandExecutor(controller));

        return pipeline;
      }
    });

    ChannelFuture commandFuture = bootstrap.connect(new InetSocketAddress(host, commandPort));
    waitAndSetupChannel(commandFuture, controller.getCommandManager());

  }

  private static void waitAndSetupChannel(ChannelFuture future, ChannelManager manager) {
    try {
      future.await(waitConnection, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      String message = "Connection was interrupted";
      LOG.log(Level.WARNING, message, e.getCause());
      Notification warning = Notification.warning(message);
      controller.getNotificationManager().show(warning);
      System.exit(1);
    }
    Channel channel = future.getChannel();
    if (channel.isConnected()) {
      manager.setChannel(channel);
      SocketAddress address = channel.getRemoteAddress();
      String message = String.format("Successfull connection to %s", address.toString());
      LOG.log(Level.INFO, message);
    } else {
      String message = "Time to connect is out";
      LOG.log(Level.WARNING, message);
      Notification warning = Notification.warning(message);
      controller.getNotificationManager().show(warning);
      System.exit(1);
    }
  }

  private static void loadProperties(String filename) {
    Properties prop = new Properties();
    String path = "./" + filename;
    try (FileInputStream file = new FileInputStream(path)) {

      prop.load(file);

      filePort = Integer.parseInt(prop.getProperty("file_port"));
      commandPort = Integer.parseInt(prop.getProperty("command_port"));
      host = prop.getProperty("host");
      waitConnection = Integer.parseInt(prop.getProperty("wait_connection"));

      Set<String> propertyNames = prop.stringPropertyNames();
      for (String name : propertyNames) {
        if (name.startsWith("alias_")) {
          String alias = name.substring("alias_".length());
          directoriesAndAliases.put(Paths.get(prop.getProperty(name)), alias);
        }
      }

    } catch (IOException ex) {
      String message = "Could not read property file";
      LOG.log(Level.WARNING, message, ex);
      Notification warning = Notification.warning(message);
      controller.getNotificationManager().show(warning);
      System.exit(1);
    }
  }
}
