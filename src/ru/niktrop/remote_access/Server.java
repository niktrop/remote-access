package ru.niktrop.remote_access;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import ru.niktrop.remote_access.commands.Notification;
import ru.niktrop.remote_access.controller.Controller;
import ru.niktrop.remote_access.controller.NotificationManager;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;
import ru.niktrop.remote_access.gui.ServerGUI;
import ru.niktrop.remote_access.handlers.*;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 15.03.13
 * Time: 9:46
 */

/**
 * Main class for the server side of the application.
 * */
public class Server {
  private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Server.class.getName());
  private static final long TIME_BEFORE_EXIT_ON_ERROR = 10000L;

  private static int commandPort;
  private static int filePort;
  private static String host;
  private static Map<Path,String> directoriesAndAliases = new HashMap<>();
  private static Controller controller;
  private static String propFileName = "server.properties";
  private static int maxDepth = 2;

  /*
  Need to use NotificationManager.show() on the server side instead of
  CommandManager.executeCommand(), because the last show messages only on client.
  */
  private static NotificationManager notificationManager;

  public static void main(String[] args){

    loadProperties(propFileName);

    setupController();

    createFSImages();

    startFileSystemWatching();

    setupCommandChannel();
    setupFileTransferChannel();

    runGUI();
  }

  private static void runGUI() {
    final ServerGUI serverGUI = ServerGUI.instance();

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        serverGUI.init();
      }
    });
  }


  private static void setupController() {
    try {
      controller = new Controller(Controller.ControllerType.SERVER);
      controller.setMaxDepth(maxDepth);
      notificationManager = controller.getNotificationManager();
    } catch (IOException e) {
      String message = "Couldn't initialize WatchService and create controller.";
      LOG.log(Level.WARNING, message, e.getCause());
      Notification warning = Notification.warning(message);
      notificationManager.show(warning);

      try {
        Thread.sleep(TIME_BEFORE_EXIT_ON_ERROR);
      } catch (InterruptedException ie) {
      } finally {
        System.exit(1);
      }
    }
  }

  private static void createFSImages() {
    String uuid = UUID.randomUUID().toString();
    notificationManager.show(Notification.operationStarted("Creating image of file system...", uuid));

    WatchService watchService = controller.getWatchService();

    Iterable<Path> dirs;
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
        notificationManager.show(warning);
        continue;
      }
      FSImage fsi;
      try {
        fsi = FSImages.getFromDirectory(dir, maxDepth, watchService);
        String alias = directoriesAndAliases.get(dir);
        if (alias == null) {
          alias = dir.toString();
        }
        fsi.setRootAlias(alias);
        controller.addFSImage(fsi);
        LOG.info(fsi.getRootAlias() + " added to server's controller");
      } catch (IOException e) {
        String message = String.format("Couldn't build image of %s", dir.toString());
        LOG.log(Level.INFO, message, e.getCause());
        Notification warning = Notification.warning(message);
        notificationManager.show(warning);
      }
    }

    notificationManager.show(Notification.operationFinished("File system image created.", uuid));
  }

  private static void startFileSystemWatching() {
    controller.getFileSystemWatcher().runWatcher();
    controller.getFsChangeHandler().runHandler();
  }

  private static void setupCommandChannel() {
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
        pipeline.addLast("logger",  new LoggerHandler(Level.FINE, Level.INFO));
        pipeline.addLast("executor", new CommandExecutor(controller));

        return pipeline;
      }
    });

    // Bind and start to accept incoming connections.
    // ChannelSaver handler will save first connected channel to the CommandManager
    try {
      bootstrap.bind(new InetSocketAddress(host, commandPort));
    } catch (Exception e) {
      String message = String.format("Couldn't bind to %s : %s. \r\n " +
              "Probably port is already in use.", host, commandPort);
      LOG.log(Level.INFO, message, e.getCause());
      Notification warning = Notification.warning(message);
      notificationManager.show(warning);

      try {
        Thread.sleep(TIME_BEFORE_EXIT_ON_ERROR);
      } catch (InterruptedException ie) {
      } finally {
        System.exit(1);
      }
    }

  }

  private static void setupFileTransferChannel() {
    final ServerBootstrap fileBootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(
                    Executors.newFixedThreadPool(1),
                    Executors.newFixedThreadPool(2)));

    // Set up the pipeline factory.
    fileBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        pipeline.addLast("logger", new LoggerHandler(Level.FINE, Level.INFO));
        pipeline.addLast("channel saver", new ChannelSaver(controller.getFileTransferManager()));
        pipeline.addLast("file receiver", new FileReceiver(controller));

        return pipeline;
      }
    });

    // Bind and start to accept incoming connections.
    // ChannelSaver handler will save first connected channel to the FileTransferManager
    try {
      fileBootstrap.bind(new InetSocketAddress(host, filePort));
    } catch (Exception e) {
      String message = String.format("Couldn't bind to %s : %s. \r\n " +
              "Probably port is already in use.", host, filePort);
      LOG.log(Level.INFO, message, e.getCause());
      Notification warning = Notification.warning(message);
      notificationManager.show(warning);

      try {
        Thread.sleep(TIME_BEFORE_EXIT_ON_ERROR);
      } catch (InterruptedException ie) {
      } finally {
        System.exit(1);
      }
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
      maxDepth = Integer.parseInt(prop.getProperty("max_depth"));

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
      notificationManager.show(warning);

      try {
        Thread.sleep(TIME_BEFORE_EXIT_ON_ERROR);
      } catch (InterruptedException ie) {
      } finally {
        System.exit(1);
      }
    }
  }
}
