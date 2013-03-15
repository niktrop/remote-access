package ru.niktrop.remote_access.handlers;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 14.03.13
 * Time: 17:03
 */
public class Logger extends SimpleChannelHandler {
  private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Logger.class.getName());

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    String typeOfMessage = e.getMessage().getClass().getSimpleName();
    LOG.info("Received: " + typeOfMessage);
  }

  @Override
  public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    String typeOfMessage = e.getMessage().getClass().getSimpleName();
    LOG.info("Sent: " + typeOfMessage);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    LOG.log(Level.WARNING, null, e.getCause());
    e.getChannel().close();
  }
}
