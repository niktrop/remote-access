package ru.niktrop.remote_access.commands;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 11.03.13
 * Time: 14:30
 */
public class QueryReloadDirectory implements SerializableCommand {
  private static final Logger LOG = Logger.getLogger(QueryReloadDirectory.class.getName());

  private final String fsiUuid;
  private final PseudoPath dir;

  private QueryReloadDirectory(String fsiUuid, PseudoPath dir) {
    this.fsiUuid = fsiUuid;
    this.dir = dir;
  }

  public QueryReloadDirectory(PseudoFile pseudoFile) {
    if (pseudoFile.getType() != FileType.DIR.getName()) {
      throw new IllegalArgumentException("Attempt to create QueryReloadDirectory not on a directory");
    }
    this.dir = pseudoFile.getPseudoPath();
    this.fsiUuid = pseudoFile.getFsiUuid();
  }

  @Override
  public void execute(Controller controller, ChannelHandlerContext ctx) {
    if (ctx != null) {
      executeOnServer(controller, ctx);
    }
    else {
      executeOnClient(controller);
    }
  }

  @Override
  public String getStringRepresentation() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';
    char unitSeparator = '\u001F';

    builder.append(fsiUuid);
    builder.append(groupSeparator);
    for (int i = 0; i < dir.getNameCount(); i++) {
      builder.append(dir.getName(i));
      builder.append(unitSeparator);
    }
    return builder.toString();
  }

  @Override
  public SerializableCommand fromString(String serialized) {
    String groupSeparator = "\u001E";
    String unitSeparator = "\u001F";

    StringTokenizer st = new StringTokenizer(serialized, groupSeparator, false);
    String fsiUuid = st.nextToken();

    String pathAsString = st.nextToken();
    st = new StringTokenizer(pathAsString, unitSeparator, false);
    PseudoPath path = new PseudoPath();
    while (st.hasMoreTokens()) {
      path = path.resolve(st.nextToken());
    }
    return new QueryReloadDirectory(fsiUuid, path);
  }

  private List<FSChange> getApplicableFSChanges(Controller controller) {
    int maxDepth = controller.getMaxDepth();
    FSImage fsi = controller.getFSImage(fsiUuid);
    int depth = new PseudoFile(fsi, dir).getDepth();

    //reload only directories with small depth
    if (depth >= maxDepth) {
      return Collections.emptyList();
    }

    Path pathToRoot1 = fsi.getPathToRoot();
    Path fullPath = pathToRoot1.resolve(dir.toPath());
    
    WatchService watcher = controller.getWatcher();
    ChangeType type = ChangeType.CREATE_DIR;

    List<FSChange> result = new ArrayList<>();

    try {
      FSImage newFsi = FSImages.getFromDirectory(fullPath, maxDepth, watcher);
      String xml = newFsi.toXml();
      for (FSImage fsImage : controller.getFSImages()) {
        if (fsImage.containsPath(fullPath)) {
          Path pathToRoot = fsImage.getPathToRoot();
          PseudoPath pseudoPath = new PseudoPath(pathToRoot.relativize(fullPath));
          FSChange fsChange = new FSChange(type, fsImage.getUuid(), pseudoPath, xml);
          result.add(fsChange);
        }
      }
    } catch (IOException e) {
      LOG.log(Level.WARNING, null, e.getCause());
    }

    return result;
  }

  private void executeOnServer(Controller controller, ChannelHandlerContext ctx) {
    Channel channel = ctx.getChannel();
    for (FSChange fsChange : getApplicableFSChanges(controller)) {
      Channels.write(channel, fsChange);
    }
  }

  private void executeOnClient(Controller controller) {
    for (FSChange fsChange : getApplicableFSChanges(controller)) {
      fsChange.execute(controller, null);
    }
  }

}
