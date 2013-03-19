package ru.niktrop.remote_access;

import ru.niktrop.remote_access.commands.ChangeType;
import ru.niktrop.remote_access.commands.FSChange;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImageCollection;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

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
          enqueueChanges();
        }
      }
    };
    watcher.setDaemon(true);
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
      LOG.log(Level.WARNING, "Waiting for file system change was interrupted.", e.getCause());
    }
    return fsChange;
  }

  public WatchService getWatchService() {
    return watchService;
  }

  /**
   * Builds FSChange objects from WatchService events and
   * adds them to the internal queue of the FileSystemWatcher.
   * Should be invoked from a separate thread in an infinite loop.
   */
  public void enqueueChanges() {
    WatchKey key = null;
    key = watchService.poll();

    if (key == null)
      return;

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
        LOG.info("FSChange enqueued:" + fsChange.getChangeType().name() + " , " + fsChange.getPath());
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
              //Find depth of loading for created directory
              int depth = new PseudoFile(fsi, pseudoDir).getDepth() - 1;
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
}
