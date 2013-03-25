package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 20.03.13
 * Time: 23:36
 */
public class CreateFile implements SerializableCommand {
  private static final Logger LOG = Logger.getLogger(CreateFile.class.getName());

  private final String fsiUuid;
  private final PseudoPath path;
  private final boolean isDirectory;

  //Only for deserialization.
  CreateFile() {
    this(null, null, false);
  }


  public CreateFile(String fsiUuid, PseudoPath path, boolean directory) {
    this.fsiUuid = fsiUuid;
    this.path = path;
    isDirectory = directory;
  }


  @Override
  public void execute(Controller controller) {
    FSImage fsi = controller.fsImages.get(fsiUuid);
    CommandManager cm = controller.getCommandManager();

    if ( !fsi.isLocal()) {
      cm.sendCommand(this);
      return;
    }

    if  (fsi.isLocal()) {
      Path pathToRoot = fsi.getPathToRoot();
      Path fullPath = pathToRoot.resolve(path.toPath());
      try {
        if (isDirectory) {
          Files.createDirectory(fullPath);
        } else {
          Files.createFile(fullPath);
        }
      } catch (IOException e) {
        String message = String.format("Creating failed: \r\n %s", e.toString());
        LOG.log(Level.WARNING, message, e.getCause());
        Notification warning = Notification.warning(message);
        cm.executeCommand(warning);
      }
    }
  }

  @Override
  public String getStringRepresentation() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';

    builder.append(fsiUuid);
    builder.append(groupSeparator);

    builder.append(isDirectory);
    builder.append(groupSeparator);

    builder.append(path.serializeToString());

    return builder.toString();
  }

  @Override
  public SerializableCommand fromString(String representation) {
    String groupSeparator = "\u001E";

    StringTokenizer st = new StringTokenizer(representation, groupSeparator, false);
    String fsiUuid = st.nextToken();
    boolean isDirectory = Boolean.parseBoolean(st.nextToken());

    String pathAsString = st.nextToken();

    PseudoPath path = PseudoPath.deserialize(pathAsString);

    return new CreateFile(fsiUuid, path, isDirectory);
  }
}
