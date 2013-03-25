package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 23.03.13
 * Time: 14:35
 */

/**
 * Creates directory tree on the target side of CopyDirectory operation.
 * After execution sends back QueryDownloadContent command.
 * */
public class CreateDirectoryTree implements SerializableCommand{
  private static final Logger LOG = Logger.getLogger(CreateDirectoryTree.class.getName());

  private final String targetFsiUuid;
  private final PseudoPath targetDir;
  private final FSImage directoryTree;
  private final String operationUuid;

  private CommandManager cm;

  private List<PseudoFile> leafNodes = new LinkedList<>();

  public CreateDirectoryTree(String targetFsiUuid, PseudoPath targetDir, FSImage directoryTree, String operationUuid) {
    this.targetFsiUuid = targetFsiUuid;
    this.targetDir = targetDir;
    this.directoryTree = directoryTree;
    this.operationUuid = operationUuid;
  }

  //Only for deserialization.
  CreateDirectoryTree() {
    this(null, null, null, null);
  }

  @Override
  public void execute(Controller controller) {
    cm = controller.getCommandManager();

    FSImage targetFsi = controller.fsImages.get(targetFsiUuid);
    if ( !targetFsi.isLocal()) {
      String message = "No such FSImage on the target side";
      failed(new IllegalStateException(), message);
      return;
    }

    Path targetDirRelative = targetDir.toPath();
    Path realTarget = targetFsi.getPathToRoot().resolve(targetDirRelative);

    findLeafNodes(new PseudoFile(directoryTree, new PseudoPath()));

    for (PseudoFile dir : leafNodes) {
      Path fullPath = realTarget.resolve(dir.getPseudoPath().toPath());
      try {
        Files.createDirectories(fullPath);
      } catch (IOException e) {
        String message = "Failed to create directory structure";
        failed(e, message);
      }
    }

    QueryDownloadContent command = new QueryDownloadContent(targetFsiUuid, targetDir, operationUuid);
    cm.sendCommand(command);
  }

  @Override
  public String getStringRepresentation() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';

    builder.append(targetFsiUuid);
    builder.append(groupSeparator);

    builder.append(targetDir.serializeToString());
    builder.append(groupSeparator);

    builder.append(directoryTree.toXml());
    builder.append(groupSeparator);

    builder.append(operationUuid);

    return builder.toString();
  }

  @Override
  public SerializableCommand fromString(String representation) {
    String groupSeparator = "\u001E";

    StringTokenizer st = new StringTokenizer(representation, groupSeparator, false);

    String targetFsiUuid = st.nextToken();

    PseudoPath targetDir = PseudoPath.deserialize(st.nextToken());

    FSImage directoryTree = null;
    try {
      directoryTree = FSImages.getFromXml(st.nextToken());
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Deserialization of directory tree failed.", e);
    }

    String operationUuid = st.nextToken();

    return new CreateDirectoryTree(targetFsiUuid, targetDir, directoryTree, operationUuid);
  }

  private void findLeafNodes(PseudoFile root) {
    if (root.getContent().isEmpty()) {
      leafNodes.add(root);
    } else {
      for(PseudoFile pseudoFile : root.getContent()) {
        findLeafNodes(pseudoFile);
      }
    }
  }

  private void failed(Throwable cause, String message) {
    LOG.log(Level.WARNING, message, cause);

    Notification failed = Notification.operationFailed(message, operationUuid);
    cm.executeCommand(failed);
  }
}
