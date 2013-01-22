package com.ning.http.client.providers.netty;

import static java.net.InetAddress.getByName;

import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class NettySocksHandler extends SimpleChannelUpstreamHandler {
    static final byte PROTO_VER5 = 5;
    static final byte CONNECT = 1;
    static final byte NO_AUTH = 0;
    static final byte IPV4 = 1;

    static final byte[] VERSION_AUTH = new byte[] { PROTO_VER5, 1, NO_AUTH };
    static final byte[] CON = new byte[] { PROTO_VER5, CONNECT, 0, IPV4 };

    private State state = State.INIT;
    private final NettyResponseFuture<?> mFuture;
    private final URI mUri;

    static enum State {
        INIT, CON_SENT
    }

    public NettySocksHandler(NettyResponseFuture<?> future, URI uri) {
        mFuture = future;
        mUri = uri;

    }

    public NettySocksHandler(NettyResponseFuture<?> future, String uri) throws URISyntaxException {
        mFuture = future;
        mUri = new URI(uri);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
        Channel channel = ctx.getChannel();
        if (state == State.INIT && buffer.readableBytes() == 2) {
            buffer.readByte();
            buffer.readByte();

            ChannelBuffer send = ChannelBuffers.buffer(10);
            send.writeBytes(CON);
            send.writeBytes(getByName(mUri.getHost()).getAddress());
            send.writeShort(getPort(mUri));
            ctx.getChannel().write(send);

            state = State.CON_SENT;
        } else if (state == State.CON_SENT && buffer.readableBytes() == 10) {
            byte[] data = new byte[10];
            buffer.readBytes(data);
            if (data[1] != 0) {
                mFuture.done(null);
                channel.close();
            } else {
                channel.write(mFuture.getRequest());
            }
        } else {
            ctx.getPipeline().remove(this);
            ctx.getPipeline().sendUpstream(e);
        }
    }


    public static int getPort(URI uri) {
        int port = uri.getPort();
        if (port == -1) {
            if ("https".equals(uri.getScheme()))
                port = 443;
            else
                port = 80;
        }
        return port;
    }



}