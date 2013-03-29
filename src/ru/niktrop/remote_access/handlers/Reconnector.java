package ru.niktrop.remote_access.handlers;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import ru.niktrop.remote_access.ChannelManager;

import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 29.03.13
 * Time: 12:22
 */
public class Reconnector extends SimpleChannelHandler{
  private static final Logger LOG = Logger.getLogger(Reconnector.class.getName());

  private ClientBootstrap bootstrap;
  private ChannelManager manager;

  public Reconnector(ClientBootstrap bootstrap, ChannelManager manager) {
    this.bootstrap = bootstrap;
    this.manager = manager;
  }

  @Override
  public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    final SocketAddress remoteAddress = ctx.getChannel().getRemoteAddress();
    String message = String.format("Disconnected: %s", remoteAddress.toString());
    LOG.log(Level.INFO, message);

    reconnect(remoteAddress);
  }

  private void reconnect(final SocketAddress remoteAddress) {
    ChannelFuture future = bootstrap.connect(remoteAddress);
    future.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        Channel channel = future.getChannel();
        if (channel.isConnected()) {
          manager.setChannel(channel);
          String message = String.format("Successful reconnection to %s", remoteAddress.toString());
          LOG.log(Level.INFO, message);
        } else {
          String message = String.format("Reconnection to %s failed", remoteAddress.toString());
          LOG.log(Level.INFO, message);
          reconnect(remoteAddress);
        }
      }
    });
  }
}
