package com.github.nmorel.nosqllog;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.couchbase.client.CouchbaseClient;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Simple appender that sends logging event to a Couchbase bucket.
 * You need to explicitly stop the appender at the end of the application lifecycle.
 * <pre>((LoggerContext) LoggerFactory.getILoggerFactory()).stop();</pre>
 */
public class CouchbaseAppender
    extends UnsynchronizedAppenderBase<ILoggingEvent>
{
    /**
     * the URI list of one or more servers from the cluster
     */
    private List<URI> uriList = new ArrayList<URI>();
    /**
     * the bucket name in the cluster you wish to use
     */
    private String bucket;
    /**
     * the username for the bucket
     */
    private String user;
    /**
     * the password for the bucket
     */
    private String pwd;
    /**
     * @see <a href="http://www.couchbase.com/docs/couchbase-sdk-java-1.0/couchbase-sdk-java-summary-expiry
     *      .html">Expiry value</a>.
     *      0 by default so no expiration.
     */
    private int expiry = 0;
    /**
     * Client
     */
    private CouchbaseClient client;
    /**
     * If more than one log occurs in the same millisecond, we need something to order them.
     */
    private AtomicInteger counter = new AtomicInteger( 0 );

    @Override
    public void start()
    {
        try
        {
            System.out.println( "Initializing Couchbase client" + uriList );
            client = new CouchbaseClient( uriList, bucket, user, pwd );
            System.out.println( "Couchbase client initialized" );
        }
        catch ( IOException e )
        {
            System.err.println( "IOException connecting to Couchbase: " + e.getMessage() );
            System.exit( 1 );
        }
        super.start();
    }

    @Override
    public void stop()
    {
        System.out.println( "Shutting down Couchbase client" );

        // flushing any pending logging event
        client.flush();

        // shutting down the client
        client.shutdown();

        System.out.println( "Couchbase client shutted down" );
        super.stop();
    }

    @Override
    protected void append( ILoggingEvent eventObject )
    {
        JSONObject log = new JSONObject();
        try
        {
            log.put( "index", getIndex() );
            log.put( "timestamp", eventObject.getTimeStamp() );
            log.put( "level", eventObject.getLevel().toInt() );
            log.put( "message", eventObject.getFormattedMessage() );
            log.put( "logger", eventObject.getLoggerName() );
            log.put( "thread", eventObject.getThreadName() );

            // Throwable
            if ( null != eventObject.getThrowableProxy() )
            {
                JSONArray array = new JSONArray();
                IThrowableProxy proxy = eventObject.getThrowableProxy();
                array.put( convertThrowable( proxy ) );
                while ( null != proxy.getCause() )
                {
                    proxy = proxy.getCause();
                    array.put( convertThrowable( proxy ) );
                }
                log.put( "throwable", array );
            }

            // Caller Data
            if ( eventObject.hasCallerData() )
            {
                // we only store the first one
                StackTraceElement ste = eventObject.getCallerData()[0];
                JSONObject steJson = new JSONObject();
                steJson.put( "class", ste.getClassName() );
                steJson.put( "method", ste.getMethodName() );
                steJson.put( "line", ste.getLineNumber() );
                log.append( "caller", steJson );
            }

            // MDC
            for ( Map.Entry<String, String> entry : eventObject.getMDCPropertyMap().entrySet() )
            {
                log.put( entry.getKey(), entry.getValue() );
            }
        }
        catch ( JSONException e )
        {
            e.printStackTrace();
        }

        client.add( UUID.randomUUID().toString(), expiry, log.toString() );
    }

    private int getIndex()
    {
        for (; ; )
        {
            int current = counter.get();
            // if we reach the max value, we start over to 0. We shouldn't have Integer.MAX_VALUE logs in the same
            // millisecond
            int next = current == Integer.MAX_VALUE ? 0 : current + 1;
            if ( counter.compareAndSet( current, next ) )
            {
                return current;
            }
        }
    }

    private JSONObject convertThrowable( IThrowableProxy throwableProxy ) throws JSONException
    {
        JSONObject throwable = new JSONObject();
        throwable.put( "name", throwableProxy.getClassName() );
        throwable.put( "message", throwableProxy.getMessage() );

        if ( null != throwableProxy.getStackTraceElementProxyArray() && throwableProxy.getStackTraceElementProxyArray
            ().length > 0 )
        {
            JSONArray array = new JSONArray();
            for ( StackTraceElementProxy ste : throwableProxy.getStackTraceElementProxyArray() )
            {
                array.put( ste.getSTEAsString() );
            }
            throwable.put( "stackTrace", array );
        }

        return throwable;
    }

    public void addUri( String uri )
    {
        this.uriList.add( URI.create( uri ) );
    }

    public void setBucket( String bucket )
    {
        this.bucket = bucket;
    }

    public void setUser( String user )
    {
        this.user = user;
    }

    public void setPwd( String pwd )
    {
        this.pwd = pwd;
    }

    public void setExpiry( int expiry )
    {
        this.expiry = expiry;
    }
}
