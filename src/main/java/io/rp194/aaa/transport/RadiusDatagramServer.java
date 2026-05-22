package io.rp194.aaa.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.rp194.aaa.radius.RadiusPacket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RadiusDatagramServer {
  private static final Logger LOGGER = Logger.getLogger(RadiusDatagramServer.class.getName());
  private final int port;
  private final WorkerPool workers;
  private final OverloadPolicy overloadPolicy;
  private final RadiusPacketCodec codec;
  private final RadiusRequestRouter router;
  private final TransportMetrics metrics;
  private EventLoopGroup group;
  private Channel channel;

  public RadiusDatagramServer(int port, WorkerPool workers, OverloadPolicy overloadPolicy, RadiusPacketCodec codec, RadiusRequestRouter router, TransportMetrics metrics) {
    this.port = port;
    this.workers = workers;
    this.overloadPolicy = overloadPolicy;
    this.codec = codec;
    this.router = router;
    this.metrics = metrics;
  }

  public ChannelFuture start() {
    group = new NioEventLoopGroup();
    Bootstrap b = new Bootstrap();
    b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, false)
        .handler(new ChannelInitializer<DatagramChannel>() {
          @Override protected void initChannel(DatagramChannel ch) { ch.pipeline().addLast(new Handler()); }
        });
    ChannelFuture bind = b.bind(port);
    channel = bind.channel();
    return bind;
  }

  public void stop() {
    if (channel != null) channel.close();
    if (group != null) group.shutdownGracefully();
    workers.shutdown();
  }

  public int localPort() {
    if (channel == null) {
      return port;
    }
    return ((InetSocketAddress) channel.localAddress()).getPort();
  }

  private final class Handler extends ChannelInboundHandlerAdapter {
    @Override public void channelRead(ChannelHandlerContext ctx, Object msg) {
      DatagramPacket packet = (DatagramPacket) msg;
      byte[] bytes = new byte[packet.content().readableBytes()];
      packet.content().readBytes(bytes);
      InetSocketAddress remote = packet.sender();
      long started = System.nanoTime();
      metrics.recordQueueDepth(workers.queueDepth(), workers.queueCapacity());
      boolean accepted = workers.submit(() -> {
        try {
          RadiusPacketCodec.Decoded decoded = codec.decode(bytes);
          Optional<RadiusPacket> response = router.route(decoded);
          response.ifPresent(r -> ctx.writeAndFlush(new DatagramPacket(
              Unpooled.copiedBuffer((r.getCode()+":"+r.getIdentifier()).getBytes(StandardCharsets.UTF_8)), remote)));
          metrics.recordProcessed((System.nanoTime() - started) / 1000);
        } catch (Exception ex) {
          LOGGER.log(Level.WARNING, "Failed to decode radius packet", ex);
          metrics.recordDropped();
        }
      });
      if (!accepted) {
        metrics.recordDropped();
        if (overloadPolicy == OverloadPolicy.REJECT_RESPONSE) {
          ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("OVERLOADED".getBytes(StandardCharsets.UTF_8)), remote));
        }
      }
    }
  }
}
