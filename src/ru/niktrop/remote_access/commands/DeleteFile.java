package ru.niktrop.remote_access.commands;

import org.apache.commons.io.FileUtils;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 18.03.13
 * Time: 9:48
 */
public class DeleteFile implements SerializableCommand {
  private static final Logger LOG = Logger.getLogger(DeleteFile.class.getName());

  private final String fsiUuid;
  private final PseudoPath path;
  private final String operationUuid;

  public DeleteFile() {
    fsiUuid = null;
    path = null;
    operationUuid = null;
  }

  public DeleteFile(String fsiUuid, PseudoPath path) {
    this.fsiUuid = fsiUuid;
    this.path = path;
    operationUuid = UUID.randomUUID().toString();
  }

  public DeleteFile(String fsiUuid, PseudoPath path, String operationUuid) {
    this.fsiUuid = fsiUuid;
    this.path = path;
    this.operationUuid = operationUuid;
  }

  public DeleteFile(PseudoFile pseudoFile) {
    this.path = pseudoFile.getPseudoPath();
    this.fsiUuid = pseudoFile.getFsiUuid();
    operationUuid = UUID.randomUUID().toString();
  }

  @Override
  public List<SerializableCommand> execute(Controller controller) {
    FSImage fsi = controller.fsImages.get(fsiUuid);
    CommandManager cm = CommandManager.instance(controller);
    Notification start = Notification.operationStarted("Deleting " + path.toString(), operationUuid);

    if (controller.isClient()) {
      cm.executeCommand(start);

      if ( !fsi.isLocal()) {
        cm.sendCommand(this, controller.getChannel());
        return Collections.emptyList();
      }
    }

    if  (fsi.isLocal()) {
      Path pathToRoot = fsi.getPathToRoot();
      Path fullPath = pathToRoot.resolve(path.toPath());
      Notification response;
      try {
        FileUtils.forceDelete(fullPath.toFile());
        response = Notification.operationFinished("Deleted: " + path.toString(), operationUuid);
      } catch (IOException e) {
        String message = "Deleting failed: " + path.toString();
        LOG.log(Level.WARNING, message, e.getCause());
        response = Notification.operationFailed(message, operationUuid);
      }

      if (controller.isClient()) {
        cm.executeCommand(response);
        return Collections.emptyList();
      } else {
        List<SerializableCommand> result = new ArrayList<>(1);
        result.add(response);
        return result;
      }
    }

    return Collections.emptyList();
  }

  @Override
  public String getStringRepresentation() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';
    char unitSeparator = '\u001F';

    builder.append(fsiUuid);
    builder.append(groupSeparator);

    builder.append(operationUuid);
    builder.append(groupSeparator);

    for (int i = 0; i < path.getNameCount(); i++) {
      builder.append(path.getName(i));
      builder.append(unitSeparator);
    }
    return builder.toString();
  }

  @Override
  public SerializableCommand fromString(String representation) {
    String groupSeparator = "\u001E";
    String unitSeparator = "\u001F";

    StringTokenizer st = new StringTokenizer(representation, groupSeparator, false);
    String fsiUuid = st.nextToken();

    String operationUuid = st.nextToken();

    String pathAsString = st.nextToken();
    st = new StringTokenizer(pathAsString, unitSeparator, false);
    PseudoPath path = new PseudoPath();
    while (st.hasMoreTokens()) {
      path = path.resolve(st.nextToken());
    }
    return new DeleteFile(fsiUuid, path, operationUuid);
  }
}
