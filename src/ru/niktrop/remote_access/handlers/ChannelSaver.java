package ru.niktrop.remote_access.handlers;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import ru.niktrop.remote_access.ChannelManager;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 19.03.13
 * Time: 9:01
 */
public class ChannelSaver extends SimpleChannelHandler {
  private ChannelManager manager;

  public ChannelSaver(ChannelManager manager) {
    this.manager = manager;
  }

  @Override
  public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    manager.setChannel(ctx.getChannel());
  }
}