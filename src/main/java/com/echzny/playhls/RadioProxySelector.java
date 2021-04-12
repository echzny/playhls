package com.echzny.playhls;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RadioProxySelector extends ProxySelector {
  private final ProxySelector defaultSelector;
  @Setter private String radikoToken = "";

  public RadioProxySelector(ProxySelector defaultSelector) {
    this.defaultSelector = defaultSelector;
    CompletableFuture.runAsync(() -> {
      try {
        String host = "f-radiko.smartstream.ne.jp";
        int remoteport = 80;
        int localport = 3000;
        // Printing a start-up message
        System.out.println("Starting proxy for " + host + ":" + remoteport
            + " on port " + localport);
        // And start running the server
        runServer(host, remoteport, localport); // never returns
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  public RadioProxySelector() {
    this(ProxySelector.getDefault());
  }

  public List<Proxy> select(URI uri) {
    if ("f-radiko.smartstream.ne.jp".equals(uri.getHost())) {
      val proxy = new Proxy(Proxy.Type.HTTP,
          new InetSocketAddress(InetAddress.getLoopbackAddress(), 3000));
      return Arrays.asList(proxy);
    } else {
      return defaultSelector.select(uri);
    }
  }

  public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
    ioe.printStackTrace();
  }

  /**
   * It will run a single-threaded proxy server on
   * the provided local port.
   */
  public void runServer(String host, int remoteport, int localport)
      throws IOException {
    // Creating a ServerSocket to listen for connections with
    ServerSocket s = new ServerSocket(localport);
    final byte[] request = new byte[1024];
    byte[] reply = new byte[4096];
    while (true) {
      Socket client = null, server = null;
      try {
        // It will wait for a connection on the local port
        client = s.accept();
        final InputStream streamFromClient = client.getInputStream();
        final OutputStream streamToClient = client.getOutputStream();

        // Create a connection to the real server.
        // If we cannot connect to the server, send an error to the
        // client, disconnect, and continue waiting for connections.
        try {
          SSLSocketFactory factory =
              (SSLSocketFactory)SSLSocketFactory.getDefault();
          server = factory.createSocket(host, remoteport);
        } catch (IOException e) {
          PrintWriter out = new PrintWriter(streamToClient);
          out.print("Proxy server cannot connect to " + host + ":"
              + remoteport + ":\n" + e + "\n");
          out.flush();
          client.close();
          continue;
        }

        // Get server streams.
        final InputStream streamFromServer = server.getInputStream();
        final OutputStream streamToServer = server.getOutputStream();

        // a thread to read the client's requests and pass them
        // to the server. A separate thread for asynchronous.
        Thread t = new Thread() {
          public void run() {
            int bytesRead;
            try {
              while ((bytesRead = streamFromClient.read(request)) != -1) {
                // headers.put("X-Radiko-AuthToken", client.getAuths().getToken());
                val token = "\r\nX-Radiko-AuthToken: " + radikoToken + "\r\n";
                val req = new String(request).replaceAll("\r\n\r\n", token) + "\r\n";

                streamToServer.write(req.getBytes(), 0, bytesRead + token.length());
                streamToServer.flush();
                log.info("request: " + req);
                log.info("request.length: " + req.length());
              }
            } catch (IOException e) {
              log.error(e.getMessage(), e);
            }

            // the client closed the connection to us, so close our
            // connection to the server.
            try {
              streamToServer.close();
            } catch (IOException e) {
              log.error(e.getMessage(), e);
            }
          }
        };

        // Start the client-to-server request thread running
        t.start();
        // Read the server's responses
        // and pass them back to the client.
        int bytesRead;
        try {
          while ((bytesRead = streamFromServer.read(reply)) != -1) {
            streamToClient.write(reply, 0, bytesRead);
            streamToClient.flush();
          }
        } catch (IOException e) {
          log.error(e.getMessage(), e);
        }
        // The server closed its connection to us, so we close our
        // connection to our client.
        streamToClient.close();
      } catch (IOException e) {
        System.err.println(e);
      } finally {
        try {
          if (server != null)
            server.close();
          if (client != null)
            client.close();
        } catch (IOException e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }
}
