package ru.niktrop.remote_access.handlers;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import ru.niktrop.remote_access.controller.ChannelManager;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 19.03.13
 * Time: 9:01
 */

/**
 * Saves channel to server controller when a client connect to server.
 * Does nothing, if server already have connected channel.
 * */
public class ChannelSaver extends SimpleChannelHandler {
  private ChannelManager manager;

  public ChannelSaver(ChannelManager manager) {
    this.manager = manager;
  }

  @Override
  public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    Channel oldChannel = manager.getChannel();
    if (oldChannel == null || !oldChannel.isConnected()) {
      manager.setChannel(ctx.getChannel());
    }
  }
}
