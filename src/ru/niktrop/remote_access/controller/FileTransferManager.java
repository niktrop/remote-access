package ru.niktrop.remote_access.controller;

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
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 20.03.13
 * Time: 22:14
 */

/**
 * This class is responsible for sending files from server to client or vice versa.
 * It owns special channel for this purpose.
 * */
public class FileTransferManager implements ChannelManager{
  private static final Logger LOG = Logger.getLogger(FileTransferManager.class.getName());

  private Channel channel;
  private final CommandManager commandManager;

  private Map<String, Path> targets = new ConcurrentHashMap<>();
  private Map<String, Path> sources = new ConcurrentHashMap<>();
  private FileChannel targetFileChannel;
  
  private final ExecutorService fileSender = Executors.newSingleThreadExecutor();

  private final BlockingQueue<String> waitingUuids = new LinkedBlockingQueue<>();

  public FileTransferManager(Controller controller) {
    this.commandManager = controller.getCommandManager();
  }

  public void setTargetFileChannel(FileChannel targetFileChannel) {
    this.targetFileChannel = targetFileChannel;
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
    fileSender.submit(new SendFileTask(operationUuid));
  }

  @Override
  public Channel getChannel() {
    return channel;
  }

  @Override
  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  private class SendFileTask implements Runnable {
    private final int BUFFER_SIZE = 4096;
    private final ChannelBuffer buffer = ChannelBuffers.buffer(BUFFER_SIZE);
    private long offset = 0;
    private long fileLength;
    
    private final String operationUuid;

    private SendFileTask(String operationUuid) {
      this.operationUuid = operationUuid;
    }

    @Override
    public void run() {
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
        throw new IOException("Sending chunk of the file failed.");
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
      removeSource(operationUuid);
    }

    private void sendingFileFailed(Throwable cause, String operationUUID) {
      String message = String.format("Sending file failed: \r\n %s", cause.toString());
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
  }
}
