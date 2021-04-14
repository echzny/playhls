package com.echzny.playhls;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RadioProxySelector extends ProxySelector {
  private final ProxySelector defaultSelector;
  @Setter private String radikoToken = "";

  public RadioProxySelector(ProxySelector defaultSelector) {
    this.defaultSelector = defaultSelector;
    CompletableFuture.runAsync(() -> {
      try {
        DefaultHttpProxyServer.bootstrap()
            .withPort(Config.PROXY_PORT)
            .withFiltersSource(new HttpFiltersSourceAdapter() {
              public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                return new HttpFiltersAdapter(originalRequest) {
                  @Override
                  public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                    String uri = null;
                    if (httpObject instanceof HttpRequest) {
                      uri = ((HttpRequest) httpObject).getUri();
                    }
                    if (httpObject instanceof HttpMessage) {
                      HttpHeaders headers = ((HttpMessage)httpObject).headers();
                      headers.set("Accept", "*/*");
                      headers.set("Range", "bytes=0-");
                      if (Objects.nonNull(uri) && uri.endsWith("playlist.m3u8")) {
                        headers.set("Connection", "close");
                      } else {
                        headers.set("Connection", "keep-alive");
                      }
                      headers.add("Icy-MetaData", "1");
                      headers.add("X-Radiko-AuthToken", radikoToken);
                    }

                    return super.clientToProxyRequest(httpObject);
                  }

                  @Override
                  public HttpObject serverToProxyResponse(HttpObject httpObject) {
                    log.info(httpObject.toString());
                    return httpObject;
                  }
                };
              }
            })
            .start();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  public RadioProxySelector() {
    this(ProxySelector.getDefault());
  }

  public List<Proxy> select(URI uri) {
    if (Config.HOST_NAME.equals(uri.getHost())) {
      val proxy = new Proxy(Proxy.Type.HTTP,
          new InetSocketAddress(InetAddress.getLoopbackAddress(), Config.PROXY_PORT));
      return Arrays.asList(proxy);
    } else {
      return defaultSelector.select(uri);
    }
  }

  public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
    ioe.printStackTrace();
  }
}
