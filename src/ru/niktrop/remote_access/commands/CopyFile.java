package ru.niktrop.remote_access.commands;

import org.apache.commons.io.FileUtils;
import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.FileTransferManager;
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
 * Date: 21.03.13
 * Time: 11:49
 */
public class CopyFile implements SerializableCommand {
  private static final Logger LOG = Logger.getLogger(CopyFile.class.getName());

  private final String sourceFsiUuid;
  private final PseudoPath sourceFile;
  private final String targetFsiUuid;
  private final PseudoPath targetDirectory;

  private final String operationUuid;

  private CommandManager cm;
  private FileTransferManager ftm;

  public CopyFile(String sourceFsiUuid, PseudoPath sourceFile,
                  String targetFsiUuid, PseudoPath targetDirectory,
                  String operationUuid)
  {
    this.sourceFsiUuid = sourceFsiUuid;
    this.sourceFile = sourceFile;
    this.targetFsiUuid = targetFsiUuid;
    this.targetDirectory = targetDirectory;
    this.operationUuid = operationUuid;
  }

  //Only for deserialization.
  CopyFile() {
    this(null, null, null, null, null);
  }

  public CopyFile(PseudoFile sourceFile, PseudoFile targetDirectory) {
    this(sourceFile.getFsiUuid(), sourceFile.getPseudoPath(),
            targetDirectory.getFsiUuid(), targetDirectory.getPseudoPath(),
            UUID.randomUUID().toString());
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
            Notification.operationStarted("Copy started: " + sourceFile.toString(), operationUuid);
    cm.executeCommand(startNotification);

    Path sourceRelative = sourceFile.toPath();
    Path sourcePathToRoot = sourceFsi.getPathToRoot();
    Path source = sourcePathToRoot.resolve(sourceRelative);

    //if both files are local
    if (targetFsi != null && targetFsi.isLocal()) {
      Path targetDirRelative = targetDirectory.toPath();
      Path targetDir = targetFsi.getPathToRoot().resolve(targetDirRelative);

      localCopy(source, targetDir);

    } else {   //source is local, target is remote
      sendAllocateSpaceCommand(controller, source);
    }
  }

  private void localCopy(Path source, Path targetDir) {
    Notification response;
    try {
      FileUtils.copyFileToDirectory(source.toFile(), targetDir.toFile());
      response = Notification.operationFinished("Copy finished: " + sourceFile.toString(), operationUuid);
    } catch (IOException e) {
      e.printStackTrace();
      String message = String.format("Copy failed: \r\n %s", e.getMessage());
      LOG.log(Level.WARNING, message, e.getCause());
      response = Notification.operationFailed(message, operationUuid);
    }
    cm.executeCommand(response);
  }

  private void sendAllocateSpaceCommand(Controller controller, Path source) {

    //save data about this operation on the source side
    ftm.addSource(operationUuid, source);

    try {
      String sourceFileName = sourceFile.getFileName();
      long sourceSize = Files.size(source);
      PseudoPath targetFile = targetDirectory.resolve(sourceFileName);

      //send command for executing on the other side
      AllocateSpace allocateSpace = new AllocateSpace(targetFsiUuid, targetFile, sourceSize, operationUuid);
      cm.sendCommand(allocateSpace);

    } catch (IOException e) {
      String message = String.format("Sending AllocateSpace command failed for the file: %s \r\n %s",
              sourceFile.getFileName(), e.toString());
      LOG.log(Level.WARNING, message, e);
      cm.executeCommand(Notification.operationFailed(message, operationUuid));
      ftm.removeSource(operationUuid);
      return;
    }
  }

  @Override
  public String getStringRepresentation() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';

    builder.append(sourceFsiUuid);
    builder.append(groupSeparator);

    builder.append(sourceFile.serializeToString());
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

    return new CopyFile(sourceFsiUuid, sourceFile, targetFsiUuid, targetDirectory, operationUuid);
  }
}
