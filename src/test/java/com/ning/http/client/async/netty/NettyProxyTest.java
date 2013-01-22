/*
 * Copyright (c) 2010-2012 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.ning.http.client.async.netty;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.testng.annotations.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.ProxyServer.Protocol;
import com.ning.http.client.Response;
import com.ning.http.client.async.ProviderUtil;
import com.ning.http.client.async.ProxyTest;

public class NettyProxyTest extends ProxyTest {
    @Override
    public AsyncHttpClient getAsyncHttpClient(AsyncHttpClientConfig config) {
        return ProviderUtil.nettyProvider(config);
    }


    @Test(groups = {"standalone", "default_provider"})
    public void testRequestLevelSocksProxy() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        ProxyServer proxy = new ProxyServer(Protocol.HTTP, "127.0.0.1", 1080, null, null, true);
        AsyncHttpClientConfig cfg = new AsyncHttpClientConfig.Builder().setProxyServer(proxy).build();
        AsyncHttpClient client = getAsyncHttpClient(cfg);
        String target = "http://127.0.0.1:1234/";


        Future<Response> f = client
                .prepareGet(target)
//                .setProxyServer(proxy)
                .execute();
        Response resp = f.get(300, TimeUnit.SECONDS);
        assertNotNull(resp);
        assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
        assertEquals(resp.getHeader("target"), "/");
        client.close();
    }

}



