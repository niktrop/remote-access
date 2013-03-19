package ru.niktrop.remote_access.handlers;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import ru.niktrop.remote_access.Controller;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 19.03.13
 * Time: 9:01
 */
public class ChannelSaver extends SimpleChannelHandler {
  private final Controller controller;

  public ChannelSaver(Controller controller) {
    this.controller = controller;
  }

  @Override
  public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    controller.setChannel(ctx.getChannel());
  }
}
