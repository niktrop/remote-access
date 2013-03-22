package ru.niktrop.remote_access.commands;

import org.apache.commons.io.FileUtils;
import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;
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

  DeleteFile() {
    this(null, null, null);
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
    CommandManager cm = controller.getCommandManager();

    if ( !fsi.isLocal()) {
      cm.sendCommand(this, cm.getChannel());
      return Collections.emptyList();
    }

    Notification start = Notification.operationStarted("Deleting " + path.toString(), operationUuid);

    if  (fsi.isLocal()) {
      cm.executeCommand(start);
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

      cm.executeCommand(response);
    }

    return Collections.emptyList();
  }

  @Override
  public String getStringRepresentation() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';

    builder.append(fsiUuid);
    builder.append(groupSeparator);

    builder.append(operationUuid);
    builder.append(groupSeparator);

    builder.append(path.serializeToString());

    return builder.toString();
  }

  @Override
  public SerializableCommand fromString(String representation) {
    String groupSeparator = "\u001E";

    StringTokenizer st = new StringTokenizer(representation, groupSeparator, false);
    String fsiUuid = st.nextToken();

    String operationUuid = st.nextToken();

    String pathAsString = st.nextToken();
    PseudoPath path = PseudoPath.deserialize(pathAsString);

    return new DeleteFile(fsiUuid, path, operationUuid);
  }
}