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
import java.util.*;
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

  public CopyFile(String sourceFsiUuid, PseudoPath sourceFile,
                  String targetFsiUuid, PseudoPath targetDirectory,
                  String operationUuid) {
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
  public List<SerializableCommand> execute(Controller controller) {
    CommandManager cm = controller.getCommandManager();

    FSImage sourceFsi = controller.fsImages.get(sourceFsiUuid);

    //begin execution on the side of the source file
    if ( !sourceFsi.isLocal()) {
      cm.sendCommand(this, cm.getChannel());
      return Collections.emptyList();
    }

    Notification startNotification =
            Notification.operationStarted("Copy started: " + sourceFile.toString(), operationUuid);
    cm.executeCommand(startNotification);


    Path sourcePathInFsi = sourceFile.toPath();
    Path sourceFsiPathToRoot = sourceFsi.getPathToRoot();
    Path source = sourceFsiPathToRoot.resolve(sourcePathInFsi);

    //if both files are local
    FSImage targetFsi = controller.fsImages.get(targetFsiUuid);
    if (targetFsi != null && targetFsi.isLocal()) {
      Path targetPathInFsi = targetDirectory.toPath();
      Path targetDir = targetFsi.getPathToRoot().resolve(targetPathInFsi);
      Notification response;
      try {
        FileUtils.copyFileToDirectory(source.toFile(), targetDir.toFile());
        response = Notification.operationFinished("Copy finished: " + sourceFile.toString(), operationUuid);
      } catch (IOException e) {
        String message = String.format("Copy failed: \r\n %s", e.toString());
        LOG.log(Level.WARNING, message, e.getCause());
        response = Notification.operationFailed(message, operationUuid);
      }
      cm.executeCommand(response);
    } else {

      //save memo about this operation on the source side
      FileTransferManager ftm = controller.getFileTransferManager();
      ftm.addSource(operationUuid, source);

      List<SerializableCommand> result = new ArrayList<>(1);
      try {
        String sourceFileName = sourceFile.getName(sourceFile.getNameCount() - 1);
        long sourceSize = Files.size(source);
        PseudoPath targetFullPath = targetDirectory.resolve(sourceFileName);

        //send command for executing on the other side
        AllocateSpace allocateSpace = new AllocateSpace(targetFsiUuid, targetFullPath, sourceSize, operationUuid);
        if (controller.isClient()) {
          cm.sendCommand(allocateSpace, cm.getChannel());
        } else {
          result.add(allocateSpace);
        }

      } catch (IOException e) {
        String message = String.format("Copy failed: \r\n %s", e.toString());
        LOG.log(Level.WARNING, message, e.getCause());
        cm.executeCommand(Notification.operationFailed(message, operationUuid));

        ftm.removeSource(operationUuid);
      }
      return result;
    }
    return Collections.emptyList();
  }

  @Override
  public String getStringRepresentation() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';
    char nul = '\u0000';

    builder.append(sourceFsiUuid);
    builder.append(groupSeparator);

    builder.append(sourceFile.serializeToString());
    builder.append(groupSeparator);

    builder.append(targetFsiUuid);
    builder.append(groupSeparator);

    builder.append(targetDirectory.serializeToString());
    builder.append(groupSeparator);

    builder.append(operationUuid);

    return builder.toString();  }

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
