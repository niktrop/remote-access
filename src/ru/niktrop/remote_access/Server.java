package ru.niktrop.remote_access;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import ru.niktrop.remote_access.commands.Notification;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;
import ru.niktrop.remote_access.handlers.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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

  private static int commandPort;
  private static int filePort;
  private static String host;
  private static Map<Path,String> directoriesAndAliases = new HashMap<>();
  private static Controller controller;
  private static String propFileName = "server.properties";
  private static final int MAX_DEPTH = 2;

  public static void main(String[] args){

    loadProperties(propFileName);

    setupController();

    createFSImages();

    startFileSystemWatching();

    Channel commandChannel = setupCommandChannel();
    Channel fileTransferChannel = setupFileTransferChannel();

    if (commandChannel == null || fileTransferChannel == null) {
      System.exit(1);
    }

    String message = "Remote access server is running.";
    controller.getNotificationManager().show(Notification.plain(message));
    LOG.log(Level.INFO, message);
  }

  private static void setupController() {
    try {
      controller = Controllers.getServerController();
      controller.setMaxDepth(MAX_DEPTH);
    } catch (IOException e) {
      String message = "Couldn't initialize WatchService and create controller.";
      LOG.log(Level.WARNING, message, e.getCause());
      Notification warning = Notification.warning(message);
      controller.getNotificationManager().show(warning);
      System.exit(1);
    }
  }

  private static void createFSImages() {
    WatchService watchService = controller.getWatchService();

    for (Path dir : directoriesAndAliases.keySet()) {
      if (!Files.isDirectory(dir)) {
        continue;
      }
      FSImage fsi = null;
      try {
        fsi = FSImages.getFromDirectory(dir, MAX_DEPTH, watchService);
        fsi.setRootAlias(directoriesAndAliases.get(dir));
        controller.addFSImage(fsi);
        LOG.info(fsi.getRootAlias() + " added to server's controller");
      } catch (IOException e) {
        String message = String.format("Couldn't build image of %s", dir.toString());
        LOG.log(Level.INFO, message, e.getCause());
        Notification warning = Notification.warning(message);
        controller.getNotificationManager().show(warning);
      }
    }
  }

  private static void startFileSystemWatching() {
    controller.getFileSystemWatcher().runWatcher();
    controller.getFsChangeHandler().runHandler();
  }

  private static Channel setupCommandChannel() {
    // Configure command server.
    final ServerBootstrap bootstrap = new ServerBootstrap(
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
        pipeline.addLast("logger",  new LoggerHandler(Level.FINE));
        pipeline.addLast("executor", new CommandExecutor(controller));

        return pipeline;
      }
    });

    // Bind and start to accept incoming connections.
    // ChannelSaver handler will save first connected channel to FileTransferManager
    Channel commandChannel = null;
    try {
      commandChannel = bootstrap.bind(new InetSocketAddress(host, commandPort));
    } catch (Exception e) {
      String message = String.format("Couldn't bind to %s : %s. \r\n " +
              "Probably port is already in use.", host, commandPort);
      LOG.log(Level.INFO, message, e.getCause());
      Notification warning = Notification.warning(message);
      controller.getNotificationManager().show(warning);
    }

    return commandChannel;
  }

  private static Channel setupFileTransferChannel() {
    final ServerBootstrap fileBootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(
                    Executors.newFixedThreadPool(1),
                    Executors.newFixedThreadPool(2)));

    // Set up the pipeline factory.
    fileBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        pipeline.addLast("channel saver", new ChannelSaver(controller.getFileTransferManager()));
        pipeline.addLast("file receiver", new FileReceiver(controller));

        return pipeline;
      }
    });

    // Bind and start to accept incoming connections.
    // ChannelSaver handler will save first connected channel to FileTransferManager
    Channel fileTransferChannel = null;
    try {
      fileTransferChannel = fileBootstrap.bind(new InetSocketAddress(host, filePort));
    } catch (Exception e) {
      String message = String.format("Couldn't bind to %s : %s. \r\n " +
              "Probably port is already in use.", host, filePort);
      LOG.log(Level.INFO, message, e.getCause());
      Notification warning = Notification.warning(message);
      controller.getNotificationManager().show(warning);
    }

    return fileTransferChannel;
  }

  private static void loadProperties(String filename) {
    Properties prop = new Properties();
    String path = "./" + filename;
    try (FileInputStream file = new FileInputStream(path)) {
      prop.load(file);

      filePort = Integer.parseInt(prop.getProperty("file_port"));
      commandPort = Integer.parseInt(prop.getProperty("command_port"));
      host = prop.getProperty("host");

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
