package ru.niktrop.remote_access;

import ru.niktrop.remote_access.commands.ChangeType;
import ru.niktrop.remote_access.commands.FSChange;
import ru.niktrop.remote_access.file_system_model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static ru.niktrop.remote_access.file_system_model.FSImages.getFromDirectory;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 18.03.13
 * Time: 12:38
 */

//TODO Если удаляется корень FSImage?
public class FileSystemWatcher {
  private static final Logger LOG = Logger.getLogger(FileSystemWatcher.class.getName());

  private final WatchService watchService;
  private final FSImageCollection fsImages;
  private final BlockingQueue<FSChange> fsChangeQueue = new LinkedBlockingQueue<>() ;
  private int maxDepth;

  public FileSystemWatcher(Controller controller) {
    this.watchService = controller.getWatchService();
    this.fsImages = controller.fsImages;
    this.maxDepth = controller.getMaxDepth();
  }

  public void runWatcher() {
    Thread watcher = new Thread("File system watcher") {
      @Override
      public void run() {
        while(true) {
          enqueueChangesWaiting();
        }
      }
    };
    watcher.start();
  }

  public boolean hasFSChanges() {
    return !fsChangeQueue.isEmpty();
  }

  /**
   * Returns one FSChange object from the internal queue, blocks if queue is empty.
   * */
  public FSChange takeFSChange() {
    FSChange fsChange = null;
    try {
      fsChange = fsChangeQueue.take();
    } catch (InterruptedException e) {
      LOG.log(Level.WARNING, "Waiting for FSChange from the queue was interrupted.", e.getCause());
    }
    return fsChange;
  }

  public WatchService getWatchService() {
    return watchService;
  }

  /**
   * Builds FSChange objects from WatchService events and
   * adds them to the internal queue of the FileSystemWatcher.
   * Does not wait changes, if watch service does not have any.
   * Should be invoked from a separate thread in an infinite loop.
   */
  public void enqueueChangesIfAny() {
    WatchKey key = watchService.poll();

    if (key == null)
      return;

    enqueueChanges(key);
  }

  /**
   * Builds FSChange objects from WatchService events and
   * adds them to the internal queue of the FileSystemWatcher.
   * Wait changes, if watch service is empty.
   * Should be invoked from a separate thread in an infinite loop.
   */
  public void enqueueChangesWaiting() {
    WatchKey key = null;
    try {
      key = watchService.take();
    } catch (InterruptedException e) {
      LOG.log(Level.WARNING, "Waiting of file system changes was interrupted.");
    }

    if (key == null) {
      return;
    }

    enqueueChanges(key);
  }

  private void enqueueChanges(WatchKey key) {
    Path dir = (Path)key.watchable();
    List<WatchEvent<?>> events = key.pollEvents();
    for (WatchEvent<?> event : events) {
      WatchEvent.Kind<?> kind = event.kind();

      if (kind == OVERFLOW) {
        LOG.warning("File system change caused StandardWatchEventKinds.OVERFLOW");
        continue;
      }

      WatchEvent<Path> ev = (WatchEvent<Path>)event;

      List<FSChange> applicableFSChanges = getApplicableFSChanges(fsImages.getLocal(), dir, ev);

      for (FSChange fsChange : applicableFSChanges) {
        fsChangeQueue.offer(fsChange);
      }

      boolean valid = key.reset();
      if (!valid) {
        break;
      }
    }
  }

  //Filters all FSImages that store changing directory.
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
              FSImage addedFsi = getFromDirectory(fullPath, maxDepth, watchService);
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

  /**
   * Update contents of specific directory. Works only with local FSImages.
   * */
  public void forceUpdate(PseudoFile directory) {
    FSImage fsImage = directory.getFsImage();
    if ( !fsImage.isLocal()) {
      throw new IllegalArgumentException("FSImage of this Pseudofile should be local.");
    }

    Path fullPath = directory.toPath();
    FSImage newDirFSImage = null;
    try {
      newDirFSImage = FSImages.getFromDirectory(fullPath, maxDepth, watchService);
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Couldn't make FSImage from directory " + fullPath.toString());
    }

    FSChange fsChange = new FSChange(
            ChangeType.CREATE_DIR,
            fsImage.getUuid(),
            directory.getPseudoPath(),
            newDirFSImage.toXml());

    fsChangeQueue.offer(fsChange);
  }
}
