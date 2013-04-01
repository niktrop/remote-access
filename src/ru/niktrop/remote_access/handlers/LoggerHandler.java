package ru.niktrop.remote_access.handlers;

import org.jboss.netty.channel.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 14.03.13
 * Time: 17:03
 */
public class LoggerHandler extends SimpleChannelHandler {
  private static final Logger LOG = Logger.getLogger(LoggerHandler.class.getName());
  private Level level;

  public LoggerHandler(Level level) {
    this.level = level;
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    String typeOfMessage = e.getMessage().getClass().getSimpleName();
    LOG.log(level, "Received: " + typeOfMessage);
    ctx.sendUpstream(e);
  }

  @Override
  public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    String typeOfMessage = e.getMessage().getClass().getSimpleName();
    LOG.log(level, "Sent: " + typeOfMessage);
    ctx.sendDownstream(e);
  }

  @Override
  public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    LOG.log(Level.INFO, "Open: " + ctx.getChannel().toString());
    super.channelOpen(ctx, e);
  }

  @Override
  public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    LOG.log(Level.INFO, "Bound: " + ctx.getChannel().toString());
    super.channelBound(ctx, e);
  }

  @Override
  public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    LOG.log(Level.INFO, "Connected: " + ctx.getChannel().toString());
    super.channelConnected(ctx, e);

  }

  @Override
  public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    LOG.log(Level.INFO, "Closed: " + ctx.getChannel().toString());
    super.channelClosed(ctx, e);

  }

  @Override
  public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    LOG.log(Level.INFO, "Disconnected: " + ctx.getChannel().toString());
    super.channelDisconnected(ctx, e);

  }

  @Override
  public void channelUnbound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    LOG.log(Level.INFO, "Unbound: " + ctx.getChannel().toString());
    super.channelUnbound(ctx, e);

  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    Throwable cause = e.getCause();
    LOG.log(Level.WARNING, cause.toString(), cause);
    e.getChannel().close();
  }
}
