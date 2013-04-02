package ru.niktrop.remote_access.commands;

import org.apache.commons.io.FileUtils;
import ru.niktrop.remote_access.controller.CommandManager;
import ru.niktrop.remote_access.controller.Controller;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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

/**
 * Command for deleting file or directory.
 * */
public class DeleteFile implements SerializableCommand {
  private static final Logger LOG = Logger.getLogger(DeleteFile.class.getName());

  private final String fsiUuid;
  private final PseudoPath path;
  private final String operationUuid;

  //Only for deserialization.
  DeleteFile() {
    this(null, null, null);
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
  public void execute(Controller controller) {
    FSImage fsi = controller.fsImages.get(fsiUuid);
    CommandManager cm = controller.getCommandManager();

    if ( !fsi.isLocal()) {
      cm.sendCommand(this);
      return;
    }

    Notification start = Notification.operationStarted("Deleting " + path.toString(), operationUuid);

    if  (fsi.isLocal()) {
      cm.executeCommand(start);
      Path pathToRoot = fsi.getPathToRoot();
      Path fullPath = pathToRoot.resolve(path.toPath());
      File file = fullPath.toFile();
      Notification response;
      try {
        if (file.isDirectory()) {
          //FileUtils.forceDelete() sometimes just clean the directory
          //and throws exception when deleting empty directory after that.
          //Waiting some times after cleaning seems to work.
          FileUtils.cleanDirectory(file);
          try {
            Thread.sleep(100L);
          } catch (InterruptedException e) {
          }
          FileUtils.deleteDirectory(file);
        } else {
          FileUtils.forceDelete(file);
        }
        response = Notification.operationFinished("Deleted: " + path.toString(), operationUuid);
      } catch (IOException e) {
        String message = String.format("Deleting failed:\r\n%s", e.getMessage());
        LOG.log(Level.WARNING, message, e.getCause());
        response = Notification.operationFailed(message, operationUuid);
      }

      cm.executeCommand(response);
    }
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
