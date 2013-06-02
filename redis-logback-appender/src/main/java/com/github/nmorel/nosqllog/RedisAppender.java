package com.github.nmorel.nosqllog;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import redis.clients.jedis.Jedis;

import java.util.Map;

/** Simple appender that sends logging event to a Redis DB. */
public class RedisAppender
        extends UnsynchronizedAppenderBase<ILoggingEvent>
{
    private static final String INCR_KEY_FORMAT = "%s.next.key";

    private static final String LOG_KEY_FORMAT = "%s.log#";

    /** Host of the Redis DB */
    private String host;

    /** Port of the Redis DB */
    private Integer port;

    /** Name that will be used as a key for Redis */
    private String name;

    /** Expiration time in seconds */
    private Integer expiry;

    /** Client */
    private Jedis client;

    private String incrKey;

    private String logKey;

    @Override
    public void start()
    {
        if( null == port )
        {
            System.out.println("Initializing Redis client for host '" + host + "'");
            client = new Jedis(host);
        }
        else
        {
            System.out.println("Initializing Redis client for host '" + host + "' and port '" + port + "'");
            client = new Jedis(host, port);
        }
        client.connect();
        System.out.println("Redis client initialized");

        incrKey = String.format(INCR_KEY_FORMAT, name);
        logKey = String.format(LOG_KEY_FORMAT, name);

        super.start();
    }

    @Override
    public void stop()
    {
        System.out.println("Stopping Redis client");

        // disconnect the client
        client.disconnect();

        System.out.println("Redis client stopped");
        super.stop();
    }

    @Override
    protected void append( ILoggingEvent eventObject )
    {
        long index = client.incr(incrKey);

        JsonObject log = new JsonObject();
        log.addProperty("index", index);
        log.addProperty("timestamp", eventObject.getTimeStamp());
        log.addProperty("level", eventObject.getLevel().toInt());
        log.addProperty("message", eventObject.getFormattedMessage());
        log.addProperty("logger", eventObject.getLoggerName());
        log.addProperty("thread", eventObject.getThreadName());

        // Throwable
        if( null != eventObject.getThrowableProxy() )
        {
            JsonArray array = new JsonArray();
            IThrowableProxy proxy = eventObject.getThrowableProxy();
            array.add(convertThrowable(proxy));
            while( null != proxy.getCause() )
            {
                proxy = proxy.getCause();
                array.add(convertThrowable(proxy));
            }
            log.add("throwable", array);
        }

        // Caller Data
        if( eventObject.hasCallerData() )
        {
            // we only store the first one
            StackTraceElement ste = eventObject.getCallerData()[0];
            JsonObject steJson = new JsonObject();
            steJson.addProperty("class", ste.getClassName());
            steJson.addProperty("method", ste.getMethodName());
            steJson.addProperty("line", ste.getLineNumber());
            log.add("caller", steJson);
        }

        // MDC
        for( Map.Entry<String, String> entry : eventObject.getMDCPropertyMap().entrySet() )
        {
            log.addProperty(entry.getKey(), entry.getValue());
        }

        if( null == expiry )
        {
            client.set(logKey + index, log.toString());
        }
        else
        {
            client.setex(logKey + index, expiry, log.toString());
        }
    }

    private JsonObject convertThrowable( IThrowableProxy throwableProxy )
    {
        JsonObject throwable = new JsonObject();
        throwable.addProperty("name", throwableProxy.getClassName());
        throwable.addProperty("message", throwableProxy.getMessage());

        if( null != throwableProxy.getStackTraceElementProxyArray() && throwableProxy.getStackTraceElementProxyArray
                ().length > 0 )
        {
            JsonArray array = new JsonArray();
            for( StackTraceElementProxy ste : throwableProxy.getStackTraceElementProxyArray() )
            {
                array.add(new JsonPrimitive(ste.getSTEAsString()));
            }
            throwable.add("stackTrace", array);
        }

        return throwable;
    }

    public void setHost( String host )
    {
        this.host = host;
    }

    public void setPort( Integer port )
    {
        this.port = port;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setExpiry( Integer expiry )
    {
        this.expiry = expiry;
    }
}
