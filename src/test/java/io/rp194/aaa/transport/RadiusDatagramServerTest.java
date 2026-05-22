package io.rp194.aaa.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.rp194.aaa.accounting.AccountingService;
import io.rp194.aaa.accounting.AsyncLedgerWriter;
import io.rp194.aaa.accounting.InMemoryAccountingLedgerStore;
import io.rp194.aaa.device.InMemoryDeviceProfileRepository;
import io.rp194.aaa.profile.InMemoryUserProfileStore;
import io.rp194.aaa.profile.UserProfile;
import io.rp194.aaa.server.RadiusAccessHandler;
import io.rp194.aaa.session.InMemorySessionStore;
import io.rp194.aaa.vendor.DefaultVendorMapperRegistry;
import io.rp194.aaa.vendor.GenericVendorMapper;
import io.rp194.aaa.vendor.InMemoryTemplateRepository;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RadiusDatagramServerTest {
  @Test
  void dropsMalformedPayloadsAndKeepsServerResponsive() throws Exception {
    TransportMetrics metrics = new TransportMetrics();
    AsyncLedgerWriter ledgerWriter = new AsyncLedgerWriter(new InMemoryAccountingLedgerStore(), 1);
    RadiusDatagramServer server = server(metrics, ledgerWriter, OverloadPolicy.DROP, new WorkerPool(1, 2, OverloadPolicy.DROP, false));
    server.start().syncUninterruptibly();

    DatagramClient client = new DatagramClient();
    try {
      client.send(server.localPort(), new byte[] {1, 2, 3});
      String response = client.sendAndAwait(server.localPort(), validAccessPacket(9), 2);

      assertEquals("ACCESS_ACCEPT:9", response);
      assertTrue(awaitMetric(metrics::droppedCount, 1));
    } finally {
      client.close();
      server.stop();
      ledgerWriter.close();
    }
  }

  @Test
  void respondsWhenOverloaded() throws Exception {
    TransportMetrics metrics = new TransportMetrics();
    AsyncLedgerWriter ledgerWriter = new AsyncLedgerWriter(new InMemoryAccountingLedgerStore(), 1);
    WorkerPool pool = new WorkerPool(1, 1, OverloadPolicy.REJECT_RESPONSE, false);
    CountDownLatch blocker = new CountDownLatch(1);
    pool.submit(() -> await(blocker));
    pool.submit(() -> await(blocker));

    RadiusDatagramServer server = server(metrics, ledgerWriter, OverloadPolicy.REJECT_RESPONSE, pool);
    server.start().syncUninterruptibly();

    DatagramClient client = new DatagramClient();
    try {
      String response = client.sendAndAwait(server.localPort(), validAccessPacket(11), 2);
      assertEquals("OVERLOADED", response);
      assertTrue(awaitMetric(metrics::droppedCount, 1));
    } finally {
      blocker.countDown();
      client.close();
      server.stop();
      ledgerWriter.close();
    }
  }

  @Test
  void sustainsBurstLoadAndDropsWhenSaturated() throws Exception {
    TransportMetrics metrics = new TransportMetrics();
    AsyncLedgerWriter ledgerWriter = new AsyncLedgerWriter(new InMemoryAccountingLedgerStore(), 1);
    RadiusDatagramServer server = server(metrics, ledgerWriter, OverloadPolicy.DROP, new WorkerPool(2, 64, OverloadPolicy.DROP, false));
    server.start().syncUninterruptibly();

    List<DatagramClient> clients = List.of(new DatagramClient(), new DatagramClient());
    try {
      int packets = 20000;
      long start = System.nanoTime();
      for (int i = 0; i < packets; i++) {
        DatagramClient client = clients.get(i % clients.size());
        client.sendQueued(server.localPort(), accountingPacket(i));
      }
      for (DatagramClient client : clients) {
        client.flush();
      }
      long elapsedNanos = System.nanoTime() - start;
      double rate = packets / (elapsedNanos / 1_000_000_000.0);

      Thread.sleep(200);
      String response = sendWithRetries(clients.get(0), server.localPort(), validAccessPacket(15), 3);
      assertEquals("ACCESS_ACCEPT:15", response);
      assertTrue(rate >= 10000.0);
      assertTrue(awaitMetric(metrics::processedCount, 1));
    } finally {
      for (DatagramClient client : clients) {
        client.close();
      }
      server.stop();
      ledgerWriter.close();
    }
  }

  private static RadiusDatagramServer server(TransportMetrics metrics,
                                             AsyncLedgerWriter ledgerWriter,
                                             OverloadPolicy policy,
                                             WorkerPool pool) {
    InMemoryUserProfileStore profiles = new InMemoryUserProfileStore();
    profiles.upsert(new UserProfile("t1", "u1", "pw", "10M/10M", "1.1.1.1", 0, 1, 1000, 1000, "default"));
    RadiusAccessHandler accessHandler = new RadiusAccessHandler(
        new InMemoryDeviceProfileRepository(),
        profiles,
        new DefaultVendorMapperRegistry(new GenericVendorMapper(), new GenericVendorMapper(), new GenericVendorMapper(), new InMemoryTemplateRepository()),
        new InMemorySessionStore(),
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    AccountingService accountingService = new AccountingService(new InMemorySessionStore(), ledgerWriter, Clock.systemUTC());
    RadiusRequestRouter router = new RadiusRequestRouter(accessHandler, accountingService);
    return new RadiusDatagramServer(0, pool, policy, new RadiusPacketCodec(), router, metrics);
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static boolean awaitMetric(java.util.function.LongSupplier supplier, long minimum) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
    while (System.nanoTime() < deadline) {
      if (supplier.getAsLong() >= minimum) {
        return true;
      }
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return supplier.getAsLong() >= minimum;
  }

  private static String sendWithRetries(DatagramClient client, int port, byte[] payload, int attempts) throws Exception {
    for (int i = 0; i < attempts; i++) {
      String response = client.sendAndAwait(port, payload, 2);
      if (response != null) {
        return response;
      }
    }
    return null;
  }

  private static byte[] validAccessPacket(int identifier) {
    return radiusPacket(1, identifier, new Attribute[] {
        attr(1, "u1"),
        attr(4, "192.0.2.10"),
        attr(44, "sess-1"),
        attr(250, "t1")
    });
  }

  private static byte[] accountingPacket(int identifier) {
    return radiusPacket(4, identifier, new Attribute[] {
        attr(1, "u1"),
        attr(4, "192.0.2.10"),
        attr(44, "acct-" + identifier),
        attr(42, "1"),
        attr(43, "1"),
        attr(250, "t1")
    });
  }

  private static byte[] radiusPacket(int code, int identifier, Attribute[] attrs) {
    int len = 20;
    for (Attribute attr : attrs) {
      len += 2 + attr.value().length;
    }
    ByteBuffer buffer = ByteBuffer.allocate(len).order(ByteOrder.BIG_ENDIAN);
    buffer.put((byte) code).put((byte) identifier).putShort((short) len).put(new byte[16]);
    for (Attribute attr : attrs) {
      buffer.put((byte) attr.type()).put((byte) (2 + attr.value().length)).put(attr.value());
    }
    return buffer.array();
  }

  private static Attribute attr(int type, String value) {
    return new Attribute(type, value.getBytes(StandardCharsets.UTF_8));
  }

  private record Attribute(int type, byte[] value) {}

  private static final class DatagramClient implements AutoCloseable {
    private final EventLoopGroup group = new NioEventLoopGroup(1);
    private final BlockingQueue<String> responses = new ArrayBlockingQueue<>(16);
    private final Channel channel;

    private DatagramClient() throws Exception {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(group)
          .channel(NioDatagramChannel.class)
          .handler(new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel ch) {
              ch.pipeline().addLast(new ResponseHandler());
            }
          });
      ChannelFuture bind = bootstrap.bind(0).sync();
      channel = bind.channel();
    }

    private void send(int port, byte[] payload) {
      channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(payload), new InetSocketAddress("127.0.0.1", port)));
    }

    private void sendQueued(int port, byte[] payload) {
      channel.write(new DatagramPacket(Unpooled.copiedBuffer(payload), new InetSocketAddress("127.0.0.1", port)));
    }

    private void flush() {
      channel.flush();
    }

    private String sendAndAwait(int port, byte[] payload, int timeoutSeconds) throws Exception {
      send(port, payload);
      return responses.poll(timeoutSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
      channel.close().syncUninterruptibly();
      group.shutdownGracefully().syncUninterruptibly();
    }

    private final class ResponseHandler extends SimpleChannelInboundHandler<DatagramPacket> {
      @Override
      protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
        responses.offer(msg.content().toString(StandardCharsets.UTF_8));
      }
    }
  }
}
