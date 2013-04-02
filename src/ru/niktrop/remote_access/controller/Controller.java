package ru.niktrop.remote_access.controller;

import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImageCollection;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 04.03.13
 * Time: 18:34
 */

/**
 * Used to store and pass different resources to different components of the application.
 * Can notify registered ControllerListeners about changes.
 * */
public class Controller {

  public static enum ControllerType {
    CLIENT,
    SERVER;
  }

  private final ControllerType type;

  public final FSImageCollection fsImages = new FSImageCollection();

  private int maxDepth = 2;
  private final WatchService watchService;
  private final FileSystemWatcher fileSystemWatcher;
  private final FSChangeHandler fsChangeHandler;

  private final NotificationManager notificationManager;
  private final CommandManager commandManager;
  private final FileTransferManager fileTransferManager;

  private final List<ControllerListener> listeners = new ArrayList<>();

  public Controller(ControllerType type) throws IOException {
    this.type = type;
    watchService = FileSystems.getDefault().newWatchService();
    fileSystemWatcher = new FileSystemWatcher(this);
    fsChangeHandler = new FSChangeHandler(fileSystemWatcher, this);
    notificationManager = new NotificationManager();
    commandManager = new CommandManager(this);
    fileTransferManager = new FileTransferManager(this);
  }
  public boolean isClient() {
    return type.equals(ControllerType.CLIENT);
  }

  public FileTransferManager getFileTransferManager() {
    return fileTransferManager;
  }

  public WatchService getWatchService() {
    return watchService;
  }

  public NotificationManager getNotificationManager() {
    return notificationManager;
  }

  public CommandManager getCommandManager() {
    return commandManager;
  }

  public FileSystemWatcher getFileSystemWatcher() {
    return fileSystemWatcher;
  }

  public FSChangeHandler getFsChangeHandler() {
    return fsChangeHandler;
  }

  public void addListener(ControllerListener listener) {
    listeners.add(listener);
  }

  public void addFSImage(FSImage fsi) {
    fsImages.addFSImage(fsi);
    fireControllerChange();
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  public void setMaxDepth(int maxDepth) {
    this.maxDepth = maxDepth;
  }

  public void fireControllerChange() {
    for (ControllerListener listener : listeners)
      listener.controllerChanged();
  }
}
