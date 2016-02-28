package me.qinchao;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Created by SULVTO on 16-2-28.
 */
public class Main {
    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private final static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(100, 100, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1000));
    //
//    private final static int[] ports = {80, 8080, 3128, 8081, 9080, 1080, 21, 23, 443, 69, 22, 25, 110, 7001, 9090, 3389, 1521, 1158, 2100, 1433};
    private final static int[] ports = {80, 8080, 3128, 8081, 9080, 1080};


    public static void main(String[] args) {
        LOGGER.debug("LOGGER");

        // 100.0.0.1   -->   126.255.255.255

        // 100.4.75.172
        String startIp = "100.0.0.1";

        Arrays.stream(ports).forEach(value -> {
            addToThreadPool(() -> run(startIp, value));
        });

        while (!threadPool.isTerminated()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
    }

    public static void addToThreadPool(Runnable runnable) {
        int activeCount = threadPool.getActiveCount();
        System.out.println("threadPool activeCount ::" + activeCount);
        while (threadPool.getActiveCount() > 1000) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        threadPool.execute(runnable);
    }


    public static void run(String ip, int port) {
        Runnable runnable = () -> {
            HttpHost httpHost = new HttpHost(ip, port);
            boolean b = httpRequest(3000, httpHost);
            if (b) {
                System.out.println("success (" + new Date().toLocaleString() + ") --> " + ip + ":" + port);
                try {
                    Files.append(ip + ":" + port + "\n", new File("/home/qinchao/development/workspace/java/ProxyScanner/ip"), Charsets.UTF_8);
                } catch (IOException e) {
                    System.out.println("append file error (" + new Date().toLocaleString() + ") ::" + e.getLocalizedMessage());
                }
            } else {
                System.out.println("error (" + new Date().toLocaleString() + ") --> " + ip + ":" + port);
            }
        };
        addToThreadPool(runnable);

        String[] ipSplit = ip.split("\\.");

        int ipSplit_0 = Integer.parseInt(ipSplit[0]);
        int ipSplit_1 = Integer.parseInt(ipSplit[1]);
        int ipSplit_2 = Integer.parseInt(ipSplit[2]);
        int ipSplit_3 = Integer.parseInt(ipSplit[3]);
        if (ipSplit_3 < 255) {
            String nextIP = ipSplit_0 + "." + ipSplit_1 + "." + ipSplit_2 + "." + (ipSplit_3 + 1);
            addToThreadPool(() -> run(nextIP, port));
        } else if (ipSplit_2 < 255) {
            String nextIP = ipSplit_0 + "." + ipSplit_1 + "." + (ipSplit_2 + 1) + ".1";
            addToThreadPool(() -> run(nextIP, port));
        } else if (ipSplit_1 < 255) {
            String nextIP = ipSplit_0 + "." + (ipSplit_1 + 1) + ".1.1";
            addToThreadPool(() -> run(nextIP, port));
        } else if (ipSplit_0 < 126) {
            String nextIP = (ipSplit_0 + 1) + ".1.1.1";
            addToThreadPool(() -> run(nextIP, port));
        }
    }

    public static boolean httpRequest(HttpHost httpHost) {
        return httpRequest(3000, httpHost);
    }

    public static boolean httpRequest(int timeout, HttpHost httpHost) {
        RequestConfig config = RequestConfig.custom()
                .setProxy(httpHost)
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .build();
        return httpRequest(config);
    }

    public static boolean httpRequest(RequestConfig config) {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet("http://baidu.com");

        httpGet.setConfig(config);
        try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {

            String entity = EntityUtils.toString(httpResponse.getEntity());


            StatusLine statusLine = httpResponse.getStatusLine();
            if (statusLine.getStatusCode() == 200 || statusLine.getStatusCode() == 302) {
                return true;
            } else {
                System.out.println(config.getProxy().toHostString() + " -->  statusLine ::" + statusLine.getStatusCode());
            }
        } catch (IOException e) {
        }
        return false;
    }
}
