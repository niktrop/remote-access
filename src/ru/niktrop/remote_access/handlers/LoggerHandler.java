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

  //Logging level of messageReceived and writeRequested events.
  private Level levelMessages;
  //Logging level of different connection related events.
  private Level levelConnections;


  public LoggerHandler(Level levelMessages, Level levelConnections) {
    this.levelMessages = levelMessages;
    this.levelConnections = levelConnections;
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    String typeOfMessage = e.getMessage().getClass().getSimpleName();
    LOG.log(levelMessages, "Received: " + typeOfMessage);
    ctx.sendUpstream(e);
  }

  @Override
  public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    String typeOfMessage = e.getMessage().getClass().getSimpleName();
    LOG.log(levelMessages, "Sent: " + typeOfMessage);
    ctx.sendDownstream(e);
  }

  @Override
  public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    LOG.log(levelConnections, "Open: " + ctx.getChannel().toString());
    super.channelOpen(ctx, e);
  }

  @Override
  public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    LOG.log(levelConnections, "Bound: " + ctx.getChannel().toString());
    super.channelBound(ctx, e);
  }

  @Override
  public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    LOG.log(levelConnections, "Connected: " + ctx.getChannel().toString());
    super.channelConnected(ctx, e);

  }

  @Override
  public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    LOG.log(levelConnections, "Closed: " + ctx.getChannel().toString());
    super.channelClosed(ctx, e);

  }

  @Override
  public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    LOG.log(levelConnections, "Disconnected: " + ctx.getChannel().toString());
    super.channelDisconnected(ctx, e);

  }

  @Override
  public void channelUnbound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    LOG.log(levelConnections, "Unbound: " + ctx.getChannel().toString());
    super.channelUnbound(ctx, e);

  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    Throwable cause = e.getCause();
    LOG.log(Level.WARNING, cause.toString(), cause);
    e.getChannel().close();
  }
}
