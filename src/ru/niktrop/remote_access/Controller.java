package ru.niktrop.remote_access;

import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImageCollection;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 04.03.13
 * Time: 18:34
 */
public class Controller {
  private static final Logger LOG = Logger.getLogger(Controller.class.getName());

  static enum ControllerType {
    CLIENT,
    SERVER;
  }

  private int maxDepth = 2;

  public final FSImageCollection fsImages = new FSImageCollection();
  private final ControllerType type;
  private final List<ControllerListener> listeners = new LinkedList<>();

  private final WatchService watchService;
  private final NotificationManager notificationManager;
  private final CommandManager commandManager;
  private final FileTransferManager fileTransferManager;

  {
    notificationManager = new NotificationManager();
    commandManager = new CommandManager(this);
    fileTransferManager = new FileTransferManager(this);
  }


  Controller(ControllerType type) throws IOException {
    watchService = FileSystems.getDefault().newWatchService();
    this.type = type;
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

  public PseudoFile getDefaultDirectory(List<FSImage> fsImages) {
    if (fsImages.isEmpty()) {
      return null;
  //TODO defaultDirectory when fsImages is empty?
    }
    FSImage fsi = fsImages.get(0);
    return new PseudoFile(fsi, new PseudoPath());
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
