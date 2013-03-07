import nu.xom.ParsingException;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 04.03.13
 * Time: 18:34
 */
public class Controller {
  private final Map<String, FSImage> fsImageMap;
  private final WatchService watcher;
  private final int MAX_DEPTH = 2;

  public Controller() throws IOException {
    this.fsImageMap = new HashMap<>();
    this.watcher = FileSystems.getDefault().newWatchService();
  }

  public Controller(Iterable<FSImage> fsImages, WatchService watcher) {
    this.fsImageMap = new HashMap<>();
    for(FSImage fsi : fsImages) {
      addFSImage(fsi);
    }
    this.watcher = watcher;
  }

  public WatchService getWatcher() {
    return watcher;
  }

  public void addFSImage(FSImage fsi) {
    fsImageMap.put(fsi.getUuid(), fsi);
  }

  /**
   * Builds FSChange objects from WatchService and
   * adds them to the internal queue of the Controller.
   * Should be invoked from separate thread in an infinite loop.
   */
  public void enqueueChanges(BlockingQueue<FSChange> fsChangeQueue) {
    while(true) {
      WatchKey key = watcher.poll();
      if (key == null)
        break;

      Path dir = (Path)key.watchable();
      for (WatchEvent<?> event: key.pollEvents()) {
        WatchEvent.Kind<?> kind = event.kind();

        if (kind == OVERFLOW) {
          continue;
        }

        WatchEvent<Path> ev = (WatchEvent<Path>)event;

        List<FSChange> applicableFSChanges =
                getApplicableFSChanges(fsImageMap.values(), dir, ev);
        for (FSChange fsChange : applicableFSChanges) {
          fsChangeQueue.offer(fsChange);
        }
        //TODO Send these changes to another side of application
        boolean valid = key.reset();
        if (!valid) {
          break;
        }
      }
    }
  }

  /**
   * Applies all changes from the internal queue if it is not empty, or waits.
   */
  public void applyChange(FSChange fsChange) {
    if (fsChange == null)
      return;

    FSImage fsi = fsImageMap.get(fsChange.getFsiUuid());
    PseudoPath pseudoPath = fsChange.getPath();
    switch (fsChange.getType()) {
      case CREATE_DIR:
        FSImage createdDirFsi = null;
        try {
          createdDirFsi = FSImages.getFromXml(fsChange.getXmlFSImage());
        } catch (ParsingException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
        fsi.addToDirectory(pseudoPath.getParent(), createdDirFsi);
        break;
      case CREATE_FILE:
        try {
          fsi.addFile(pseudoPath);
        } catch (Exception e) {
          e.printStackTrace();
        }
        break;
      case DELETE:
        fsi.deletePath(pseudoPath);
        break;
      default:
        break;
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
              FSImage addedFsi = FSImages.getFromDirectory(fullPath, MAX_DEPTH, watcher);
              xmlFsi = addedFsi.toXml();
            }
            FSChange fsChange = new FSChange(type, fsi.getUuid(), relFullPath, xmlFsi);
            result.add(fsChange);

          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }

    }
    return result;
  }




}
