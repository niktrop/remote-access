package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.controller.CommandManager;
import ru.niktrop.remote_access.controller.Controller;
import ru.niktrop.remote_access.controller.FileTransferManager;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 23.03.13
 * Time: 15:18
 */

/**
 * Creates and begin execution of all CopyFile commands, needed to CopyDirectory.
 * */
public class QueryDownloadContent implements SerializableCommand {
  private static final Logger LOG = Logger.getLogger(QueryDownloadContent.class.getName());

  private final String targetFsiUuid;
  private final PseudoPath targetDir;
  private final String operationUuid;

  private CommandManager cm;
  private FileTransferManager ftm;

  public QueryDownloadContent(String targetFsiUuid, PseudoPath targetDir, String operationUuid) {
    this.targetFsiUuid = targetFsiUuid;
    this.targetDir = targetDir;
    this.operationUuid = operationUuid;
  }

  //Only for deserialization.
  QueryDownloadContent() {
    this(null, null, null);
  }

  @Override
  public void execute(Controller controller) {
    cm = controller.getCommandManager();
    ftm = controller.getFileTransferManager();

    final Path directory = ftm.getSource(operationUuid);
    FSImage sourceFsi = controller.fsImages.findContainingFSImage(directory);
    Path sourceFsiPathToRoot = sourceFsi.getPathToRoot();
    final String sourceFsiUuid = sourceFsi.getUuid();
    final Path sourceRootToDirectory = sourceFsiPathToRoot.relativize(directory);
    final Path targetRootToTargetDir = targetDir.toPath();

    try {
      Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          //form source PseudoFile
          Path directoryToFile = directory.relativize(file);
          Path sourceRootToFile = sourceRootToDirectory.resolve(directoryToFile);
          PseudoPath sourceFile = new PseudoPath(sourceRootToFile);

          //form target directory
          Path directoryName = directory.getFileName();
          PseudoPath localTargetDirectory =
                  new PseudoPath(targetRootToTargetDir.resolve(directoryName).resolve(directoryToFile)).getParent();

          CopyFile command = new CopyFile(
                  sourceFsiUuid,
                  sourceFile,
                  targetFsiUuid,
                  localTargetDirectory,
                  UUID.randomUUID().toString());
          cm.executeCommand(command);

          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      String message = String.format("Some files may be not copied from \r\n %s \r\n to %s",
              sourceRootToDirectory.toString(), targetRootToTargetDir.toString());
      LOG.log(Level.WARNING, message, e);
      cm.executeCommand(Notification.warning(message));
    }
    String message = "Copy of directory structure finished";
    Notification finished = Notification.operationFinished(message, operationUuid);
    cm.executeCommand(finished);
  }

  @Override
  public String getStringRepresentation() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';

    builder.append(targetFsiUuid);
    builder.append(groupSeparator);

    builder.append(targetDir.serializeToString());
    builder.append(groupSeparator);

    builder.append(operationUuid);

    return builder.toString();
  }

  @Override
  public SerializableCommand fromString(String representation) {
    String groupSeparator = "\u001E";
    StringTokenizer st = new StringTokenizer(representation, groupSeparator, false);

    String targetFsiUuid = st.nextToken();

    PseudoPath path = PseudoPath.deserialize(st.nextToken());

    String operationUuid = st.nextToken();

    return new QueryDownloadContent(targetFsiUuid, path, operationUuid);
  }

}
