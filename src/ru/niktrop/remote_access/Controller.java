package ru.niktrop.remote_access;

import org.jboss.netty.channel.Channel;
import ru.niktrop.remote_access.commands.ChangeType;
import ru.niktrop.remote_access.commands.FSChange;
import ru.niktrop.remote_access.commands.SerializableCommand;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 04.03.13
 * Time: 18:34
 */
public class Controller {

  static enum ControllerType {
    CLIENT,
    SERVER;
  }

  private final ControllerType type;
  private static final Logger LOG = Logger.getLogger(StringDecoder.class.getName());
  private final Map<String, FSImage> fsImageMap = new HashMap<>();

  private final BlockingQueue<FSChange> fsChangeQueue = new LinkedBlockingQueue<>();
  private final WatchService watcher;
  private int maxDepth = 2;
  private Channel channel;
  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  Controller(ControllerType type) throws IOException {
    watcher = FileSystems.getDefault().newWatchService();
    this.type = type;
  }

  public void sendCommand(SerializableCommand command) {
    if (channel == null) {
      LOG.warning("Attempt to send command while channel is null.");
      return;
    }
    channel.write(command);
  }

  public void executeCommand(SerializableCommand command) {
    command.execute(this, null);
  }

  public WatchService getWatcher() {
    return watcher;
  }

  public FSImage getFSImage(String fsiUuid) {
    return fsImageMap.get(fsiUuid);
  }

  public Iterable<FSImage> getFSImages() {
    return fsImageMap.values();
  }

  public void addFSImage(FSImage fsi) {
    fsImageMap.put(fsi.getUuid(), fsi);
  }

  public void listenAndHandleFileChanges() {
    Thread listener = new Thread() {
      @Override
      public void run() {
        while(true) {
          enqueueChanges(fsChangeQueue);
        }
      }
    };

    Thread handler = new Thread() {
      @Override
      public void run() {
        while(true) {
          try {
            FSChange fsChange = fsChangeQueue.take();
            executeCommand(fsChange);
            if (type == ControllerType.SERVER) {
              sendCommand(fsChange);
            }
          } catch (InterruptedException e) {
            LOG.warning("Waiting file system change was interrupted.");
          }
        }
      }
    };

    listener.setDaemon(true);
    handler.setDaemon(true);

    listener.start();
    handler.start();
  }

  /**
   * Builds ru.niktrop.remote_access.commands.FSChange objects from WatchService and
   * adds them to the internal queue of the Controller.
   * Should be invoked from separate thread in an infinite loop.
   */
  public void enqueueChanges(Queue<FSChange> fsChangeQueue) {
    while(true) {
      WatchKey key = watcher.poll();
      if (key == null)
        break;

      Path dir = (Path)key.watchable();
      for (WatchEvent<?> event: key.pollEvents()) {
        WatchEvent.Kind<?> kind = event.kind();

        if (kind == OVERFLOW) {
          LOG.warning("File system change caused StandardWatchEventKinds.OVERFLOW");
          continue;
        }

        WatchEvent<Path> ev = (WatchEvent<Path>)event;

        List<FSChange> applicableFSChanges =
                getApplicableFSChanges(fsImageMap.values(), dir, ev);
        for (FSChange fsChange : applicableFSChanges) {
          fsChangeQueue.offer(fsChange);
        }
        boolean valid = key.reset();
        if (!valid) {
          break;
        }
      }
    }
  }

  //Filters all ru.niktrop.remote_access.file_system_model.FSImages that store changing directory.
  //If new directory is added, register it in WatchService.
  private List<FSChange> getApplicableFSChanges(Collection<FSImage> fsImages, Path dir, WatchEvent<Path> event) {
    List<FSChange> result = new ArrayList<>();
    Path filename = event.context();
    Path fullPath = dir.resolve(filename);
    ChangeType type = ChangeType.getChangeType(event, fullPath);
    for (FSImage fsi : fsImages) {

      //search all fsImages that store content of changing directory
      Path pathToRoot = fsi.getPathToRoot();
      if (pathToRoot == null)
        continue;
      if (dir.startsWith(pathToRoot)) {
        PseudoPath pseudoDir = new PseudoPath(pathToRoot.relativize(dir));
        PseudoPath relFullPath = new PseudoPath(pathToRoot.relativize(fullPath));
        if (fsi.contains(pseudoDir)) {
          try {
            String xmlFsi = null;
            if (type == ChangeType.CREATE_DIR) {
              FSImage addedFsi = FSImages.getFromDirectory(fullPath, getMaxDepth(), watcher);
              xmlFsi = addedFsi.toXml();
            }
            FSChange fsChange = new FSChange(type, fsi.getUuid(), relFullPath, xmlFsi);
            result.add(fsChange);

          } catch (IOException e) {
            LOG.log(Level.WARNING, null, e.getCause());
          }
        }
      }

    }
    return result;
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  public void setMaxDepth(int maxDepth) {
    this.maxDepth = maxDepth;
  }
}