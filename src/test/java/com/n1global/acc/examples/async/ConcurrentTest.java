package com.n1global.acc.examples.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import com.n1global.acc.CouchDb;
import com.n1global.acc.CouchDbConfig;
import com.n1global.acc.json.CouchDbDocument;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class ConcurrentTest {
    private static final int CONCURRENT_CONNECTIONS_COUNT = 15000;

    public static void shouldWorkWithSockets() throws IOException {
        long t = System.currentTimeMillis();

        byte[] httpRequest = "POST /test1/ HTTP/1.1\nContent-Length: 2\nContent-Type: application/json\n\n{}".getBytes("UTF-8");

        SocketChannel[] chArray = new SocketChannel[CONCURRENT_CONNECTIONS_COUNT];

        for (int i = 0; i < CONCURRENT_CONNECTIONS_COUNT; i++) {
            SocketChannel ch = SocketChannel.open();

            ch.configureBlocking(false);

            ch.connect(new InetSocketAddress("127.0.0.1", 5984));

            chArray[i] = ch;
        }

        for (int i = 0; i < CONCURRENT_CONNECTIONS_COUNT; i++) {
            chArray[i].finishConnect();
        }

        System.out.println("All sockets are connected...");

        ByteBuffer[] bufArray = new ByteBuffer[CONCURRENT_CONNECTIONS_COUNT];

        for (int i = 0; i < CONCURRENT_CONNECTIONS_COUNT; i++) {
            ByteBuffer buf = ByteBuffer.allocate(httpRequest.length);

            buf.put(httpRequest);

            buf.flip();

            bufArray[i] = buf;
        }

        System.out.println("All sockets are configured...");

        boolean complete = true;

        do {
            complete = true;

            for (int i = 0; i < CONCURRENT_CONNECTIONS_COUNT; i++) {
                ByteBuffer buf = bufArray[i];

                if (buf.hasRemaining()) {
                    chArray[i].write(buf);

                    complete = false;
                }
            }
        } while (!complete);

        System.out.println("Write complete...");

        for (int i = 0; i < CONCURRENT_CONNECTIONS_COUNT; i++) {
            chArray[i].close();
        }

        System.out.println("Closed. Time: " + (System.currentTimeMillis() - t));
    }

    public static void shouldWorkWithClient() throws Exception {
        AsyncHttpClient httpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setRequestTimeoutInMs(-1).build());

        CouchDb db = new CouchDb(new CouchDbConfig.Builder().setUser("root")
                                                            .setPassword("root")
                                                            .setDbName("test1")
                                                            .setHttpClient(httpClient)
                                                            .build());

        List<Future<CouchDbDocument>> futures = new ArrayList<>(CONCURRENT_CONNECTIONS_COUNT);

        long t = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_CONNECTIONS_COUNT; i++) {
            futures.add(db.async().saveOrUpdate(new CouchDbDocument()));
        }

        System.out.println("t1: " + (System.currentTimeMillis() - t));

        for (Future<CouchDbDocument> f : futures) {
            f.get();
        }

        System.out.println("t2: " + (System.currentTimeMillis() - t));
    }

    private static void loggerOff() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        context.getLogger("com").setLevel(Level.ERROR);
    }

    public static void main(String[] args) throws Exception {
        loggerOff();

//        shouldWorkWithSockets();

        shouldWorkWithClient();
    }
}
