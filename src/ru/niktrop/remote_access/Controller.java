package ru.niktrop.remote_access;

import nu.xom.ParsingException;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import ru.niktrop.remote_access.commands.ChangeType;
import ru.niktrop.remote_access.commands.FSChange;
import ru.niktrop.remote_access.commands.SerializableCommand;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;
import ru.niktrop.remote_access.handlers.StringDecoder;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static ru.niktrop.remote_access.file_system_model.FSImages.*;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 04.03.13
 * Time: 18:34
 */
public class Controller {
  private static final Logger LOG = Logger.getLogger(StringDecoder.class.getName());

  static enum ControllerType {
    CLIENT,
    SERVER;
  }

  private List<ControllerListener> listeners = new LinkedList<>();

  private final ControllerType type;
  private final Map<String, FSImage> fsImageMap = new HashMap<>();

  private final BlockingQueue<FSChange> fsChangeQueue = new LinkedBlockingQueue<>();
  private final WatchService watcher;
  private int maxDepth = 2;
  private Channel channel;

  Controller(ControllerType type) throws IOException {
    watcher = FileSystems.getDefault().newWatchService();
    this.type = type;
  }

  public void sendCommand(SerializableCommand command) {
    if (channel == null) {
      LOG.warning("Attempt to send command while channel is null.");
      return;
    }
    //channel.write(command, new InetSocketAddress("localhost", 11111));
    channel.write(command);
  }

  public List<SerializableCommand> executeCommand(SerializableCommand command) {
    List<SerializableCommand> response = command.execute(this);
    LOG.info("Command executed: " + command.getClass().getSimpleName());
    fireControllerChange();
    return response;
  }

  public void sendResponseBack(List<SerializableCommand> response, ChannelHandlerContext ctx) {
    for (SerializableCommand command : response) {
      Channels.write(ctx.getChannel(), command);
    }
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

  public Channel getChannel() {
    return channel;
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }
  /**
   * Returns list of local FSImages, sorted by root alias.
   * */
  public List<FSImage> getLocalFSImages() {
    List<FSImage> result = new ArrayList<>();
    for (FSImage fsImage : getFSImages()) {
      if (fsImage.isLocal()) {
        result.add(fsImage);
      }
    }
    Collections.sort(result, byAlias);
    return result;
  }

  /**
   * Returns list of remote FSImages, sorted by root alias.
   * */
  public List<FSImage> getRemoteFSImages() {
    List<FSImage> result = new ArrayList<>();
    for (FSImage fsImage : getFSImages()) {
      if (!fsImage.isLocal()) {
        result.add(fsImage);
      }
    }
    Collections.sort(result, byAlias);
    return result;
  }

  public PseudoFile getDefaultDirectory() {
    List<FSImage> fsImages = getLocalFSImages();
    fsImages.addAll(getRemoteFSImages());
    if (fsImages.isEmpty()) {
      String xmlEmpty = "<directory alias=\"empty\" />";
      try {
        FSImage empty = getFromXml(xmlEmpty);
        return new PseudoFile(empty, new PseudoPath());
      } catch (ParsingException e) {
        LOG.warning("Couldn't create empty FSImage from xml");
        return null;
      } catch (IOException e) {
        LOG.warning("Couldn't create empty FSImage from xml");
        return null;
      }
    }
    FSImage fsi = fsImages.get(0);
    return new PseudoFile(fsi, new PseudoPath());
  }

  public void addListener(ControllerListener listener) {
    listeners.add(listener);
  }

  public void addFSImage(FSImage fsi) {
    fsImageMap.put(fsi.getUuid(), fsi);
    fireControllerChange();
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
   * Builds FSChange objects from WatchService and
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

  public int getMaxDepth() {
    return maxDepth;
  }

  public void setMaxDepth(int maxDepth) {
    this.maxDepth = maxDepth;
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
              FSImage addedFsi = getFromDirectory(fullPath, getMaxDepth(), watcher);
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

  private void fireControllerChange() {
    for (ControllerListener listener : listeners)
      listener.controllerChanged();
  }
}
