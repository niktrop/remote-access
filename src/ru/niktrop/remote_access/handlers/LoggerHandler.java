package ru.niktrop.remote_access.handlers;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

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
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    Throwable cause = e.getCause();
    LOG.log(Level.WARNING, cause.toString(), cause);
    e.getChannel().close();
  }
}
