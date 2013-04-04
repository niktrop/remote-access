package ru.niktrop.remote_access.handlers;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import ru.niktrop.remote_access.commands.GetFSImages;
import ru.niktrop.remote_access.commands.Notification;
import ru.niktrop.remote_access.controller.ChannelManager;
import ru.niktrop.remote_access.controller.CommandManager;
import ru.niktrop.remote_access.controller.Controller;

import java.net.SocketAddress;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 29.03.13
 * Time: 12:22
 */

/**
 * Tries to reconnect channel on the client side if something went wrong.
 * */
public class Reconnector extends SimpleChannelUpstreamHandler{
  private static final Logger LOG = Logger.getLogger(Reconnector.class.getName());

  //Pause between reconnection attempts in ms
  private final int RECONNECTION_WAITING = 3000;

  private final ClientBootstrap bootstrap;
  private final ChannelManager manager;
  private final Controller controller;
  private final CommandManager commandManager;
  private String uuid;

  public Reconnector(ClientBootstrap bootstrap, ChannelManager manager, Controller controller) {
    this.bootstrap = bootstrap;
    this.manager = manager;
    this.controller = controller;
    this.commandManager = controller.getCommandManager();
  }

  @Override
  public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

    manager.setChannel(ctx.getChannel());

    //Query to the server for refreshing its FSImages
    if (manager instanceof CommandManager) {
      commandManager.sendCommand(new GetFSImages());
    }

    ctx.sendUpstream(e);
  }

  @Override
  public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    final SocketAddress remoteAddress = ctx.getChannel().getRemoteAddress();
    String message = String.format("Disconnected: %s \r\n Trying to reconnect...", remoteAddress.toString());
    LOG.log(Level.INFO, message);
    uuid = UUID.randomUUID().toString();
    commandManager.executeCommand(Notification.operationStarted(message, uuid));

    //clear remote FSImages on disconnection
    controller.fsImages.clearRemoteFSImages();
    controller.fireControllerChange();

    reconnect(remoteAddress);

    ctx.sendUpstream(e);
  }

  private void reconnect(final SocketAddress remoteAddress) {
    ChannelFuture future = bootstrap.connect(remoteAddress);
    future.addListener(new ChannelFutureListener() {

      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        Channel channel = future.getChannel();
        if (channel.isConnected()) {

          String message = String.format("Successful reconnection to %s", remoteAddress.toString());
          commandManager.executeCommand(Notification.operationFinished(message, uuid));

        } else {
          String message = String.format("Reconnection to %s failed: \r\n %s",
                  remoteAddress.toString(), future.getCause().getMessage());
          LOG.log(Level.INFO, message);
          commandManager.executeCommand(Notification.operationContinued(message, uuid));

          try {
            Thread.sleep(RECONNECTION_WAITING);
          } catch (InterruptedException e) {
            LOG.log(Level.INFO, "Pause between reconnection attempts was interrupted.", e.getCause());
          } finally {
            message = String.format("Trying to reconnect to %s", remoteAddress.toString());
            commandManager.executeCommand(Notification.operationContinued(message, uuid));

            reconnect(remoteAddress);
          }
        }
      }
    });
  }
}
