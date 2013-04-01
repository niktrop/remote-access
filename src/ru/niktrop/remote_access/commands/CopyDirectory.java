package ru.niktrop.remote_access.commands;

import org.apache.commons.io.FileUtils;
import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.FileTransferManager;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.io.IOException;
import java.nio.file.Path;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 23.03.13
 * Time: 14:16
 */
public class CopyDirectory implements SerializableCommand {
  private static final Logger LOG = Logger.getLogger(CopyDirectory.class.getName());

  private final String sourceFsiUuid;
  private final PseudoPath source;
  private final String targetFsiUuid;
  private final PseudoPath targetDirectory;

  private final String operationUuid;

  private CommandManager cm;
  private FileTransferManager ftm;

  public CopyDirectory(String sourceFsiUuid, PseudoPath source,
                       String targetFsiUuid, PseudoPath targetDirectory,
                       String operationUuid)
  {
    this.sourceFsiUuid = sourceFsiUuid;
    this.source = source;
    this.targetFsiUuid = targetFsiUuid;
    this.targetDirectory = targetDirectory;
    this.operationUuid = operationUuid;
  }

  public CopyDirectory(PseudoFile sourceFile, PseudoFile targetDirectory) {
    this(sourceFile.getFsiUuid(), sourceFile.getPseudoPath(),
            targetDirectory.getFsiUuid(), targetDirectory.getPseudoPath(),
            UUID.randomUUID().toString());
  }

  //Only for deserialization.
  CopyDirectory() {
    this(null, null, null, null, null);
  }

  @Override
  public void execute(Controller controller) {
    cm = controller.getCommandManager();
    ftm = controller.getFileTransferManager();

    FSImage sourceFsi = controller.fsImages.get(sourceFsiUuid);
    FSImage targetFsi = controller.fsImages.get(targetFsiUuid);

    //begin execution on the side of the source file
    if ( !sourceFsi.isLocal()) {
      cm.sendCommand(this);
      return;
    }

    Notification startNotification =
            Notification.operationStarted("Copy started: " + source.toString(), operationUuid);
    cm.executeCommand(startNotification);

    Path sourceRelative = source.toPath();
    Path sourcePathToRoot = sourceFsi.getPathToRoot();
    Path realSource = sourcePathToRoot.resolve(sourceRelative);

    //if both files are local
    if (targetFsi != null && targetFsi.isLocal()) {
      Path targetDirRelative = targetDirectory.toPath();
      Path targetDir = targetFsi.getPathToRoot().resolve(targetDirRelative);

      localCopyToDirectory(realSource, targetDir);

      //FileSystemWatcher may have no time to register target directory
      //before copy of subelements will be made, so we so we force update it.
      PseudoFile targetPseudoFile = new PseudoFile(targetFsi, targetDirectory.resolve(source.getFileName()));
      controller.getFileSystemWatcher().forceUpdate(targetPseudoFile);

    } else {   //source is local, target is remote

      //save data about this operation on the source side
      ftm.addSource(operationUuid, realSource);

      sendCreateDirectoryTree(controller, realSource);
    }
  }

  private void sendCreateDirectoryTree(Controller controller, Path realSource) {
    try {
      FSImage directoryTree = FSImages.getDirectoryStructure(realSource);
      //send command for executing on the other side
      CreateDirectoryTree command =
              new CreateDirectoryTree(targetFsiUuid, targetDirectory, directoryTree, operationUuid);
      cm.sendCommand(command);

    } catch (IOException e) {
      String message = String.format("Sending CreateDirectoryTree command failed: %s \r\n %s",
              realSource.toString(), e.toString());
      LOG.log(Level.WARNING, message, e);
      cm.executeCommand(Notification.operationFailed(message, operationUuid));
      return;
    }
  }

  @Override
  public String getStringRepresentation() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';

    builder.append(sourceFsiUuid);
    builder.append(groupSeparator);

    builder.append(source.serializeToString());
    builder.append(groupSeparator);

    builder.append(targetFsiUuid);
    builder.append(groupSeparator);

    builder.append(targetDirectory.serializeToString());
    builder.append(groupSeparator);

    builder.append(operationUuid);

    return builder.toString();
  }

  @Override
  public SerializableCommand fromString(String representation) {
    String groupSeparator = "\u001E";

    StringTokenizer st = new StringTokenizer(representation, groupSeparator, false);

    String sourceFsiUuid = st.nextToken();

    PseudoPath sourceFile = PseudoPath.deserialize(st.nextToken());

    String targetFsiUuid = st.nextToken();

    PseudoPath targetDirectory = PseudoPath.deserialize(st.nextToken());

    String operationUuid = st.nextToken();

    return new CopyDirectory(sourceFsiUuid, sourceFile, targetFsiUuid, targetDirectory, operationUuid);
  }

  private void localCopyToDirectory(Path source, Path targetDir) {
    Notification response;
    try {
      FileUtils.copyDirectoryToDirectory(source.toFile(), targetDir.toFile());
      response = Notification.operationFinished("Copy finished: " + source.toString(), operationUuid);
    } catch (IOException e) {
      String message = String.format("Copy failed: \r\n %s", e.toString());
      LOG.log(Level.WARNING, message, e.getCause());
      response = Notification.operationFailed(message, operationUuid);
    }
    cm.executeCommand(response);
  }
}
