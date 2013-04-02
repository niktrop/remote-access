package ru.niktrop.remote_access.controller;

import org.jboss.netty.channel.Channel;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 21.03.13
 * Time: 18:39
 */

/**
 * Common interface of CommandManager and FileTransferManager.
 * */
public interface ChannelManager {
  Channel getChannel();
  void setChannel(Channel channel);
}
