package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.*;
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

  //For deserialization
  ReloadDirectory() {
    fsiUuid = null;
    dir = null;
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
  public List<SerializableCommand> execute(Controller controller) {

    if (controller.isClient())
      return executeOnClient(controller);

    else
      return executeOnServer(controller);

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
    return new ReloadDirectory(fsiUuid, path);
  }

  private List<FSChange> getApplicableFSChanges(Controller controller) {
    int maxDepth = controller.getMaxDepth();
    FSImage fsi = controller.getFSImage(fsiUuid);
    if (fsi == null)
      return Collections.emptyList();

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
      for (FSImage fsImage : controller.getLocalFSImages()) {
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

  private List<SerializableCommand> executeOnServer(Controller controller) {
    List<SerializableCommand> response = new LinkedList<>();
    for (FSChange fsChange : getApplicableFSChanges(controller)) {
      controller.executeCommand(fsChange);
      response.add(fsChange);
    }
    return response;
  }

  private List<SerializableCommand> executeOnClient(Controller controller) {
    for (FSChange fsChange : getApplicableFSChanges(controller)) {
      controller.executeCommand(fsChange);
    }
    return Collections.emptyList();
  }

}
