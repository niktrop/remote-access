package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 20.03.13
 * Time: 9:17
 */
public class RenameFile implements SerializableCommand {
  private static final Logger LOG = Logger.getLogger(RenameFile.class.getName());

  private final String fsiUuid;
  private final PseudoPath path;
  private final String newName;
  private final String operationUuid;

  private RenameFile(String fsiUuid, PseudoPath path, String newName, String operationUuid) {

    this.fsiUuid = fsiUuid;
    this.path = path;
    this.newName = newName;
    this.operationUuid = operationUuid;
  }

  public RenameFile(PseudoFile pseudoFile, String newName) {
    this(pseudoFile.getFsiUuid(), pseudoFile.getPseudoPath(), newName, UUID.randomUUID().toString());
  }

  //Only for deserialization.
  public RenameFile() {
    this(null, null, null, null);
  }

  @Override
  public void execute(Controller controller){
    if (fsiUuid == null || fsiUuid.equals(""))
      throw new IllegalStateException("FSImage uuid should be non-empty string");
    if (newName == null || newName.equals(""))
      throw new IllegalStateException("New name should be non-empty string");
    if (operationUuid == null || operationUuid.equals(""))
      throw new IllegalStateException("Operation uuid should be non-empty string");

    FSImage fsi = controller.fsImages.get(fsiUuid);
    CommandManager cm = controller.getCommandManager();

    if ( !fsi.isLocal()) {
      cm.sendCommand(this);
      return;
    }

    Path pathToRoot = fsi.getPathToRoot();
    Path fullPath = pathToRoot.resolve(path.toPath());
    try {
      Files.move(fullPath, fullPath.resolveSibling(newName));
    } catch (IOException e) {
      String message = "Renaming failed: " + path.toString();
      LOG.log(Level.WARNING, message, e.getCause());
      cm.executeCommand(Notification.warning(message));
    }
  }

  @Override
  public String getStringRepresentation() {

    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';

    builder.append(fsiUuid);
    builder.append(groupSeparator);

    builder.append(newName);
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
    String newName = st.nextToken();
    String operationUuid = st.nextToken();

    String pathAsString = st.nextToken();
    PseudoPath path = PseudoPath.deserialize(pathAsString);

    return new RenameFile(fsiUuid, path, newName, operationUuid);
  }
}
