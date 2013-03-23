package com.github.nmorel.couchlog;

import java.util.Random;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogGenerator
{
    private static final Logger logger = LoggerFactory.getLogger( LogGenerator.class );

    public static void main( String[] args )
    {
        Random rnd = new Random();

        long start = System.currentTimeMillis();

        for ( int i = 0; i < 5000; i++ )
        {
            int level = rnd.nextInt( 5 );
            String message = "Message #" + padStart( Integer.toString( i ), 4, '0' );
            switch ( level )
            {
                case 0:
                    logger.trace( message );
                    break;
                case 1:
                    logger.debug( message );
                    break;
                case 2:
                    logger.info( message );
                    break;
                case 3:
                    logger.warn( message );
                    break;
                case 4:
                    logger.error( message );
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
    private static String padStart(String string, int minLength, char padChar) {
        if (string.length() >= minLength) {
            return string;
        }
        StringBuilder sb = new StringBuilder(minLength);
        for (int i = string.length(); i < minLength; i++) {
            sb.append(padChar);
        }
        sb.append(string);
        return sb.toString();
    }
}
