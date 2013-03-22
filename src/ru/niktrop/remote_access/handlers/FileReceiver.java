package ru.niktrop.remote_access.handlers;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.FileTransferManager;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 20.03.13
 * Time: 21:56
 */
public class FileReceiver extends SimpleChannelUpstreamHandler{

  private final FileTransferManager fileTransferManager;
  private String operationUuid;
  private long remainingFileLength;
  private FileChannel currentFileChannel;

  public FileReceiver(Controller controller) {
    fileTransferManager = controller.getFileTransferManager();
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
      Path target = fileTransferManager.getTarget(operationUuid);
      currentFileChannel = FileChannel.open(target, StandardOpenOption.WRITE);
      return;
    }

    if (remainingFileLength > 0) {
      int readable = buf.readableBytes();
      buf.readBytes(currentFileChannel, readable);
      remainingFileLength -= readable;
    }

    if (remainingFileLength == 0) {
      currentFileChannel.close();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    String uuid = operationUuid;
    //clean handler state
    operationUuid = null;
    remainingFileLength = 0;
    currentFileChannel.close();

    fileTransferManager.sendingFileFailed(e.getCause(), uuid);
  }
}
