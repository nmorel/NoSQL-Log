package com.github.nmorel.couchlog;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.couchbase.client.CouchbaseClient;
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
     * @see <a href="http://www.couchbase.com/docs/couchbase-sdk-java-1.0/couchbase-sdk-java-summary-expiry.html">Expiry value</a>.
     *      0 by default so no expiration.
     */
    private int expiry = 0;

    /**
     * Client
     */
    private CouchbaseClient client;

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
            log.put( "level", eventObject.getLevel().toInt() );
            log.put( "message", eventObject.getFormattedMessage() );
            log.put( "timestamp", eventObject.getTimeStamp() );
            log.put( "logger", eventObject.getLoggerName() );
        }
        catch ( JSONException e )
        {
            e.printStackTrace();
        }

        client.add( UUID.randomUUID().toString(), expiry, log.toString() );
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
