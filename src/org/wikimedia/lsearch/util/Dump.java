package org.wikimedia.lsearch.util;

import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;
import java.lang.reflect.Method;

public class Dump {
	private static void indent( int level, PrintWriter out ) {
		for ( int i = 0; i < level; ++i ) {
		       out.format( "    " );
		}
	}  // indent

        public static void dumpIterable( String name, Iterable a, int level, PrintWriter out ) {
		if ( null == a ) return;

		indent( level, out );
		final int size = (a instanceof ArrayList) ? ((ArrayList)a).size()
			: (a instanceof Set) ? ((Set)a).size()
			: -1;

		if ( 0 == size ) {
			out.format( "%s = []%n", name );
			return;
		}
		out.format( "%s = [ %d%n", name, size );

		final int nLevel = 1 + level;
		Iterator iter = a.iterator();
		for ( int i = 0; iter.hasNext(); ++i ) {
			indent( nLevel, out );
			out.format( "%d: %s%n", i, iter.next().toString() );
		}
		indent( level, out );
		out.println( "]" );
	}  // dumpIterable

        public static void dumpMap( String name, Map h, int level, PrintWriter out ) {
		if ( null == h ) return;

		indent( level, out );
		int size = h.size();
		if ( 0 == size ) {
			out.format( "%s = {}%n", name );
			return;
		}
		out.format( "%s = { %d%n", name, size );

		final int nLevel = 1 + level;
		ArrayList keys = new ArrayList( h.keySet() );
		if ( keys.get( 0 ) instanceof Pattern ) {
			Collections.sort( keys, new Comparator() {
					public int compare( Object o1, Object o2 ) {
						return o1.toString().compareTo( o2.toString() );
					} } );
		} else {
			Collections.sort( keys );
		}
		for ( Object key : keys ) {
			String skey = key.toString();
			Object val = h.get( key );
			if ( val instanceof String || val instanceof Boolean || val instanceof Integer ||
			     val instanceof Float || val instanceof Double ) {    // primitive types
				indent( nLevel, out );
				out.format( "%s = %s%n", skey, val );
				continue;
			}
			if ( val instanceof Map ) {
				dumpMap( skey, (Map)val, nLevel, out );
				continue;
			}
			if ( val instanceof Iterable ) {
				dumpIterable( skey, (Iterable)val, nLevel, out );
				continue;
			}

			// convert to string if possible
			Method m = null;
			try { m = val.getClass().getMethod( "toString" ); } catch (Exception e) {}
			if ( null != m ) {
				indent( nLevel, out );
				out.format( "%s = %s%n", skey, val );
				continue;
			}

			// just print class name
			out.format( "<%s>%n", val.getClass().getName() );
		}
		indent( level, out );
		out.println( "}" );
	}  // dumpMap

}
