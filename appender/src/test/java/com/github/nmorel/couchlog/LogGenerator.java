package com.github.nmorel.couchlog;

import java.math.BigDecimal;
import java.util.Random;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class LogGenerator
{
    private static final Logger logger = LoggerFactory.getLogger( LogGenerator.class );

    public static void main( String[] args )
    {
        Random rnd = new Random();

        long start = System.currentTimeMillis();

        try
        {
            new BigDecimal( (String) null );
        }
        catch ( Exception e )
        {
            logger.error( "Error while creating a BigDecimal", e );
        }

        MDC.put( "user", "Nicolas" );

        for ( int i = 0; i < 50; i++ )
        {
            int level = rnd.nextInt( 5 );
            String message = "Message #{}";
            Object param = padStart( Integer.toString( i ), 4, '0' );
            switch ( level )
            {
                case 0:
                    logger.trace( message, param );
                    break;
                case 1:
                    logger.debug( message, param );
                    break;
                case 2:
                    logger.info( message, param );
                    break;
                case 3:
                    logger.warn( message, param );
                    break;
                case 4:
                    logger.error( message, param );
                    break;
                default:
                    break;
            }
        }

        long end = System.currentTimeMillis();

        System.out.println( "Time taken : " + (end - start) + "ms" );

        ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
    }

    /**
     * Copy of the guava Strings.padStart. Don't want to add dependencies to guava just for one method used in test
     */
    private static String padStart( String string, int minLength, char padChar )
    {
        if ( string.length() >= minLength )
        {
            return string;
        }
        StringBuilder sb = new StringBuilder( minLength );
        for ( int i = string.length(); i < minLength; i++ )
        {
            sb.append( padChar );
        }
        sb.append( string );
        return sb.toString();
    }
}
