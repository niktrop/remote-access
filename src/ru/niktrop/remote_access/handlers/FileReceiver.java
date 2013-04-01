package ru.niktrop.remote_access.handlers;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.FileTransferManager;
import ru.niktrop.remote_access.commands.Notification;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 30.03.13
 * Time: 15:03
 */

/**
 * Responsible for receiving files, that are sent by FileTransferManager.
 * */
public class FileReceiver extends ReplayingDecoder<FileReceiver.DecodingState> {
  private static final Logger LOG = Logger.getLogger(FileReceiver.class.getName());

  private final FileTransferManager ftm;
  private final CommandManager cm;
  private Path target;
  private String operationUuid;
  private long remainingFileLength;
  private FileChannel currentFileChannel;

  //Buffer for minimize write to file operations for large files
  private ChannelBuffer tempBuf = ChannelBuffers.buffer(65536);

  public FileReceiver(Controller controller) {
    super(DecodingState.OPERATION_UUID);
    ftm = controller.getFileTransferManager();
    cm = controller.getCommandManager();
  }

  @Override
  protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, DecodingState state)
          throws Exception {

    switch (state) {
      case OPERATION_UUID:
        //receive operation uuid and setup FileChannel
        operationUuid = new UUID(buffer.readLong(), buffer.readLong()).toString();
        target = ftm.getTarget(operationUuid);
        currentFileChannel = FileChannel.open(target, StandardOpenOption.WRITE);
        ftm.setTargetFileChannel(currentFileChannel);
        checkpoint(DecodingState.FILE_LENGTH);
      case FILE_LENGTH:
        remainingFileLength = buffer.readLong();
        checkpoint(DecodingState.DATA);
      case DATA:
        int toRead;
        while (remainingFileLength > 0) {
          //attempts to write to temp buffer
          toRead = (int) Math.min(remainingFileLength, tempBuf.capacity());
          tempBuf = buffer.readBytes(toRead);
          checkpoint();
          //write to file when writing to temp buffer succeeded
          tempBuf.readBytes(currentFileChannel, toRead);
          remainingFileLength -= toRead;
          tempBuf.clear();
        }

        if (remainingFileLength == 0) {
          checkpoint(DecodingState.OPERATION_UUID);
          fileReceived();
          return null;
        }

      default:
        throw new Error("Shouldn't reach here.");
    }
  }

  private void fileReceived() {
    try {
      currentFileChannel.close();
    } catch (IOException e) {
      String message = String.format("Couldn't close file ", target.toString());
      LOG.log(Level.WARNING, message, e.getCause());
    }

    String message = String.format("Copy finished: %s", target.getFileName().toString());
    Notification finished = Notification.operationFinished(message, operationUuid);
    cm.executeCommand(finished);

    ftm.removeTarget(operationUuid);
  }

  public enum DecodingState {
    OPERATION_UUID,
    FILE_LENGTH,
    DATA
  }
}

