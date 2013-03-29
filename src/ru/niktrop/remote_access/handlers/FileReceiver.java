package ru.niktrop.remote_access.handlers;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.FileTransferManager;
import ru.niktrop.remote_access.commands.Notification;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 20.03.13
 * Time: 21:56
 */
public class FileReceiver extends SimpleChannelUpstreamHandler{
  private static final Logger LOG = Logger.getLogger(FileReceiver.class.getName());

  private final FileTransferManager ftm;
  private final CommandManager cm;
  private String operationUuid;
  private long remainingFileLength;
  private FileChannel currentFileChannel;

  public FileReceiver(Controller controller) {
    ftm = controller.getFileTransferManager();
    cm = controller.getCommandManager();
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    ChannelBuffer buf = (ChannelBuffer) e.getMessage();

    //receiving new file
    if (remainingFileLength == 0) {

      if (buf.readableBytes() < 24) {
        return;
      }

      operationUuid = new UUID(buf.readLong(), buf.readLong()).toString();
      remainingFileLength = buf.readLong();
      Path target = ftm.getTarget(operationUuid);
      currentFileChannel = FileChannel.open(target, StandardOpenOption.WRITE);
      ftm.setTargetFileChannel(currentFileChannel);
      return;
    }

    if (remainingFileLength > 0) {
      int readable = buf.readableBytes();
      int toRead = (int) Math.min(remainingFileLength, readable);
      buf.readBytes(currentFileChannel, toRead);
      remainingFileLength -= toRead;
    }

    if (remainingFileLength == 0) {
      currentFileChannel.close();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    //clean handler state
    remainingFileLength = 0;
    currentFileChannel.close();
    //delete info about this operation from FileTransferManager
    Path targetPath = ftm.getTarget(operationUuid);
    ftm.removeTarget(operationUuid);

    String message = String.format("Receiving file failed: %s \r\n %s", operationUuid, e.getCause());
    LOG.log(Level.WARNING, message, e.getCause());
    cm.executeCommand(Notification.operationFailed(message, operationUuid));
    return;
  }
}
