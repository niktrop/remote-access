package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.CommandManager;
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
public class ReloadDirectory implements SerializableCommand {
  private static final Logger LOG = Logger.getLogger(ReloadDirectory.class.getName());

  private final String fsiUuid;
  private final PseudoPath dir;

  //Only for deserialization.
  ReloadDirectory() {
    this(null, null);
  }

  private ReloadDirectory(String fsiUuid, PseudoPath dir) {
    this.fsiUuid = fsiUuid;
    this.dir = dir;
  }

  public ReloadDirectory(PseudoFile pseudoFile) {
    if (pseudoFile.getType() != FileType.DIR.getName()) {
      throw new IllegalArgumentException("Attempt to create ReloadDirectory not on a directory");
    }
    this.dir = pseudoFile.getPseudoPath();
    this.fsiUuid = pseudoFile.getFsiUuid();
  }

  @Override
  public void execute(Controller controller) {

    CommandManager cm = controller.getCommandManager();
    for (FSChange fsChange : getApplicableFSChanges(controller)) {
      cm.executeCommand(fsChange);

      //if on server, send changes to the client too
      if ( !controller.isClient()) {
        cm.sendCommand(fsChange);
      }
    }

  }

  @Override
  public String getStringRepresentation() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';

    builder.append(fsiUuid);
    builder.append(groupSeparator);

    builder.append(dir.serializeToString());

    return builder.toString();
  }

  @Override
  public SerializableCommand fromString(String serialized) {
    String groupSeparator = "\u001E";

    StringTokenizer st = new StringTokenizer(serialized, groupSeparator, false);
    String fsiUuid = st.nextToken();

    String pathAsString = st.nextToken();
    PseudoPath path = PseudoPath.deserialize(pathAsString);

    return new ReloadDirectory(fsiUuid, path);
  }

  private List<FSChange> getApplicableFSChanges(Controller controller) {
    int maxDepth = controller.getMaxDepth();
    FSImage fsi = controller.fsImages.get(fsiUuid);
    if (fsi == null)
      return Collections.emptyList();

    int depth = new PseudoFile(fsi, dir).getDepth();

//    //reload only directories with small depth
//    if (depth >= 1 ) {
//      return Collections.emptyList();
//    }

    Path pathToRoot1 = fsi.getPathToRoot();
    Path fullPath = pathToRoot1.resolve(dir.toPath());

    WatchService watcher = controller.getWatchService();
    ChangeType type = ChangeType.CREATE_DIR;

    List<FSChange> result = new ArrayList<>();

    try {
      FSImage newFsi = FSImages.getFromDirectory(fullPath, maxDepth, watcher);
      String xml = newFsi.toXml();
      for (FSImage fsImage : controller.fsImages.getLocal()) {
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

}
