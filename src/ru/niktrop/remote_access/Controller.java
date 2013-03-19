package ru.niktrop.remote_access;

import org.jboss.netty.channel.Channel;
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
  public final FSImageCollection fsImages = new FSImageCollection();

  private final ControllerType type;
  private final List<ControllerListener> listeners = new LinkedList<>();
  private final WatchService watchService;

  private int maxDepth = 2;
  private Channel channel;

  Controller(ControllerType type) throws IOException {
    watchService = FileSystems.getDefault().newWatchService();
    this.type = type;
  }

  public boolean isClient() {
    return type.equals(ControllerType.CLIENT);
  }

  public WatchService getWatchService() {
    return watchService;
  }

//  public void sendCommand(SerializableCommand command) {
//    if (channel == null) {
//      LOG.warning("Attempt to send command while channel is null.");
//      return;
//    }
//    //channel.write(command, new InetSocketAddress("localhost", 11111));
//    channel.write(command);
//  }
//
//  public List<SerializableCommand> executeCommand(SerializableCommand command) {
//    List<SerializableCommand> response = command.execute(this);
//    LOG.info("Command executed: " + command.getClass().getSimpleName());
//    fireControllerChange();
//    return response;
//  }
//
//  public void sendResponseBack(List<SerializableCommand> response, ChannelHandlerContext ctx) {
//    for (SerializableCommand command : response) {
//      Channels.write(ctx.getChannel(), command);
//    }
//  }

  public Channel getChannel() {
    return channel;
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public PseudoFile getDefaultDirectory() {
    List<FSImage> fsImages = this.fsImages.getLocal();
    fsImages.addAll(this.fsImages.getRemote());
    if (fsImages.isEmpty()) {
      return null;
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
