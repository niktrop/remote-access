package ru.niktrop.remote_access;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import ru.niktrop.remote_access.commands.Notification;
import ru.niktrop.remote_access.controller.CommandManager;
import ru.niktrop.remote_access.controller.Controller;
import ru.niktrop.remote_access.controller.FileTransferManager;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;
import ru.niktrop.remote_access.gui.ClientGUI;
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
 * Date: 12.03.13
 * Time: 9:25
 */

/**
 * Main class for the client side of the application.
 * */
public class Client {
  private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Client.class.getName());

  //Pause between reconnection attempts in ms
  private final int RECONNECTION_WAITING = 3000;

  private static int commandPort;
  private static int filePort;
  private static String host;

  private static Map<Path,String> directoriesAndAliases = new HashMap<>();
  private static String propFileName = "client.properties";
  private static Controller controller;
  private static int maxDepth = 2;

  private static CommandManager commandManager;

  public static void main(String[] args) {

    loadProperties(propFileName);

    setupController();

    createFSImages();

    startFileSystemWatching();

    setupCommandChannel();

    setupFileTransferChannel();

    runGUI();
  }

  private static void runGUI() {
    final ClientGUI clientGUI = ClientGUI.instance();
    controller.getNotificationManager().setParentFrame(clientGUI);

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        clientGUI.init(controller);
      }
    });
  }

  private static void setupController() {
    try {
      controller = new Controller(Controller.ControllerType.CLIENT);
      controller.setMaxDepth(maxDepth);
      commandManager = controller.getCommandManager();
    } catch (IOException e) {
      String message = "Couldn't initialize WatchService and create controller.";
      LOG.log(Level.WARNING, message, e.getCause());
      Notification warning = Notification.warning(message);
      commandManager.executeCommand(warning);
      System.exit(1);
    }
  }

  private static void startFileSystemWatching() {
    controller.getFileSystemWatcher().runWatcher();
    controller.getFsChangeHandler().runHandler();
  }

  private static void createFSImages() {
    String uuid = UUID.randomUUID().toString();
    commandManager.executeCommand(Notification.operationStarted("Creating image of file system...", uuid));

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
        commandManager.executeCommand(warning);
        continue;
      }
      FSImage fsi;

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
        commandManager.executeCommand(warning);
      }
    }

    commandManager.executeCommand(Notification.operationFinished("File system image created.", uuid));
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

        FileTransferManager manager = controller.getFileTransferManager();
        pipeline.addLast("logger", new LoggerHandler(Level.FINE, Level.INFO));
        pipeline.addLast("reconnector", new Reconnector(fileBootstrap, manager, controller));
        pipeline.addLast("file receiver", new FileReceiver(controller));

        return pipeline;
      }
    });

    InetSocketAddress remoteAddress = new InetSocketAddress(host, filePort);

    //notification and log
    String uuid = UUID.randomUUID().toString();
    String message = String.format("Trying to connect to %s", remoteAddress.toString());
    LOG.log(Level.INFO, message);
    commandManager.executeCommand(Notification.operationStarted(message, uuid));

    connect(fileBootstrap, remoteAddress, uuid);

  }

  private static void setupCommandChannel() {
    ChannelFactory factory = new NioClientSocketChannelFactory(
            Executors.newFixedThreadPool(1),
            Executors.newFixedThreadPool(2));

    final ClientBootstrap bootstrap = new ClientBootstrap(factory);

    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        pipeline.addLast("reconnector", new Reconnector(bootstrap, commandManager, controller));
        pipeline.addLast("string decoder", new StringDecoder());
        pipeline.addLast("string encoder", new StringEncoder());
        pipeline.addLast("command decoder", new CommandDecoder());
        pipeline.addLast("command encoder", new CommandEncoder());
        pipeline.addLast("logger", new LoggerHandler(Level.FINE, Level.INFO));
        pipeline.addLast("executor", new CommandExecutor(controller));

        return pipeline;
      }
    });

    InetSocketAddress remoteAddress = new InetSocketAddress(host, commandPort);

    //notification and log
    String uuid = UUID.randomUUID().toString();
    String message = String.format("Trying to connect to %s", remoteAddress.toString());
    LOG.log(Level.INFO, message);
    commandManager.executeCommand(Notification.operationStarted(message, uuid));

    connect(bootstrap, remoteAddress, uuid);

  }

  private static void connect(final ClientBootstrap bootstrap,
                              final InetSocketAddress remoteAddress,
                              final String uuid) {

    ChannelFuture future = bootstrap.connect(remoteAddress);
    future.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        Channel channel = future.getChannel();

        if (channel.isConnected()) {

          String message = String.format("Successful connection to %s", remoteAddress.toString());
          commandManager.executeCommand(Notification.operationFinished(message, uuid));

        } else {
          String message = String.format("Connection to %s failed: \r\n %s",
                  remoteAddress.toString(), future.getCause().getMessage());
          LOG.log(Level.INFO, message);
          commandManager.executeCommand(Notification.operationContinued(message, uuid));

          try {
            Thread.sleep(3000);
          } catch (InterruptedException e) {
            LOG.log(Level.INFO, "Pause between reconnection attempts was interrupted.", e.getCause());
          }

          message = String.format("Trying to connect to %s again...", remoteAddress.toString());
          commandManager.executeCommand(Notification.operationContinued(message, uuid));

          //trying to connect again
          connect(bootstrap, remoteAddress, uuid);
        }
      }
    });

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
      commandManager.executeCommand(warning);
      System.exit(1);
    }
  }
}
