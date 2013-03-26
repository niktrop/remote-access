package ru.niktrop.remote_access;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import ru.niktrop.remote_access.commands.Notification;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 20.03.13
 * Time: 22:14
 */
public class FileTransferManager implements ChannelManager{
  private static final Logger LOG = Logger.getLogger(FileTransferManager.class.getName());

  private Channel channel;
  private final CommandManager commandManager;

  private Map<String, Path> targets = new ConcurrentHashMap<>();
  private Map<String, Path> sources = new ConcurrentHashMap<>();

  private FileChannel targetFileChannel;

  public FileChannel getTargetFileChannel() {
    return targetFileChannel;
  }

  public void setTargetFileChannel(FileChannel targetFileChannel) {
    this.targetFileChannel = targetFileChannel;
  }

  //Responsible for sending files one at a time
  private final Thread fileSender = new FileSender();

  private final BlockingQueue<String> waitingUuids = new LinkedBlockingQueue<>();

  public FileTransferManager(Controller controller) {
    this.commandManager = controller.getCommandManager();
    fileSender.start();
  }

  public Path getTarget(String uuid) {
    return targets.get(uuid);
  }

  public void addTarget(String uuid, Path path) {
    targets.put(uuid, path);
  }

  public void removeTarget(String uuid) {
    targets.remove(uuid);
  }

  public Path getSource(String uuid) {
    return sources.get(uuid);
  }

  public void addSource(String uuid, Path path) {
    sources.put(uuid, path);
  }

  public void removeSource(String uuid) {
    sources.remove(uuid);
  }

  public void sendFile(String operationUuid) {
    waitingUuids.offer(operationUuid);
  }

  public void sendingFileFailed(Throwable cause, String operationUUID) {
    String message = String.format("Copy failed: \r\n %s", cause.toString());
    LOG.log(Level.WARNING, message, cause);


    commandManager.executeCommand(Notification.operationFailed(message, operationUUID));

    try {
      targetFileChannel.close();
    } catch (IOException e) {
      String message_2 = String.format("Couldn't close target file channel",
              targetFileChannel.toString());
      LOG.log(Level.WARNING, message_2, e.getCause());
      commandManager.executeCommand(Notification.warning(message_2));
    }

    removeSource(operationUUID);
    removeTarget(operationUUID);
    return;
  }

  @Override
  public Channel getChannel() {
    return channel;
  }

  @Override
  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  private class FileSender extends Thread {
    private final int BUFFER_SIZE = 4096;
    private final ChannelBuffer buffer = ChannelBuffers.buffer(BUFFER_SIZE);
    private long offset = 0;
    private String operationUuid;
    private long fileLength;

    @Override
    public void run() {
      while (true) {
        try {
          operationUuid = waitingUuids.take();
        } catch (InterruptedException e) {
          LOG.log(Level.WARNING, "File sender thread waiting was interrupted: ", e);
        }
        doSending();
      }
    }

    private void doSending() {
      Path path = getSource(operationUuid);

      try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)){
        ChannelFuture future = sendHeader(fileChannel);

        while( hasSomethingToSend() ) {
          future = sendChunk(fileChannel, future);
        }

        future.syncUninterruptibly();
        afterLastChunk();

      } catch (IOException e) {
        sendingFileFailed(e, operationUuid);
      }
    }

    private ChannelFuture sendHeader(FileChannel fileChannel) throws IOException {
      UUID uuid = UUID.fromString(operationUuid);

      //2 longs for uuid, 1 long for file length
      ChannelBuffer header = ChannelBuffers.buffer(24);

      fileLength = fileChannel.size();

      header.writeLong(uuid.getMostSignificantBits());
      header.writeLong(uuid.getLeastSignificantBits());
      header.writeLong(fileLength);

      return channel.write(header);
    }

    private ChannelFuture sendChunk(FileChannel fileChannel, ChannelFuture previous) throws IOException {
      previous.syncUninterruptibly();
      if (!previous.isSuccess()) {
        sendingFileFailed(previous.getCause(), operationUuid);
        return null;
      }
      buffer.clear();
      buffer.writeBytes(fileChannel, (int) Math.min(fileLength - offset, buffer.writableBytes()));
      offset += buffer.writerIndex();
      return previous.getChannel().write(buffer);
    }

    private boolean hasSomethingToSend() {
      return offset < fileLength;
    }

    private void afterLastChunk() throws IOException {
      offset = 0;
      buffer.clear();

      String message = String.format("Copy finished: %s", getSource(operationUuid).toString());
      Notification finished = Notification.operationFinished(message, operationUuid);
      commandManager.executeCommand(finished);
      removeSource(operationUuid);
    }
  }
}
