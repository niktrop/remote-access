package ru.niktrop.remote_access.commands;

import org.apache.commons.io.FileUtils;
import ru.niktrop.remote_access.controller.CommandManager;
import ru.niktrop.remote_access.controller.Controller;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.io.File;
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
  RenameFile() {
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
    File file = fullPath.toFile();
    Path newFullPath = fullPath.resolveSibling(newName);
    File newFile = newFullPath.toFile();
    try {
      if (Files.isDirectory(fullPath)) {
        renameDirectory(file, newFile);

        //FileSystemWatcher may have no time to register target directory
        //before copy of sub-elements will be made, so we force update it.
        PseudoPath newPseudoPath = new PseudoPath(pathToRoot.relativize(newFullPath));
        PseudoFile newPseudoFile = new PseudoFile(fsi, newPseudoPath);
        controller.getFileSystemWatcher().forceUpdate(newPseudoFile);
      } else {
        FileUtils.moveFile(file, newFile);
      }
    } catch (IOException e) {
      String message = String.format("Renaming failed:\r\n%s", e.getMessage());
      LOG.log(Level.WARNING, message, e.getCause());
      cm.executeCommand(Notification.warning(message));
    }
  }

  //Implemented in essence as in FileUtils.moveDirectory(), but added some extra time
  //between cleaning and deleting directory, if File.renameTo doesn't work.
  private void renameDirectory(File srcDir, File destDir) throws IOException {
    boolean rename = srcDir.renameTo(destDir);
    if (!rename) {
      FileUtils.copyDirectory(srcDir, destDir);
      FileUtils.cleanDirectory(srcDir);
      try {
        Thread.sleep(100L);
      } catch (InterruptedException e) {
        String message = String.format("Pause between cleaning and deleting directory %s was interrupted.",
                srcDir.toString());
        LOG.log(Level.FINE, message);
      }
      FileUtils.deleteDirectory(srcDir);
      if (srcDir.exists()) {
        throw new IOException("Failed to delete original directory '" + srcDir +
                "' after copy to '" + destDir + "'");
      }
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
