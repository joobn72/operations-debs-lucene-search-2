package org.wikimedia.lsearch.oai;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.wikimedia.lsearch.config.IndexId;
import org.wikimedia.lsearch.index.IndexUpdateRecord;

/**
 * OAI Client. Contacts OAI repo and returns a list of index
 * update records.   
 * 
 * @author rainman
 *
 */
public class OAIHarvester {
	static Logger log = Logger.getLogger(OAIHarvester.class);
	protected String urlbase;
	protected OAIParser parser;
	protected IndexUpdatesCollector collector;
	protected IndexId iid;
	protected String resumptionToken, responseDate;
	protected String host;
	/** number of retries before giving up, useful when there are broken servers in the cluster */
	protected int retries = 5;
	
	// for debugging
	// save contents of input stream to memory stream and dump to file
	private static final boolean DBG = false;
	private static int fnum = 1;
	public static InputStream toMem( InputStream is ) throws IOException {
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    // open new dump file
	    File dir = new File( "/var/tmp" );
	    String
		pfx = String.format( "oai_%d_", fnum++ ),
		sfx = ".xml";
	    File dumpfile = File.createTempFile( pfx, sfx, dir );
	    FileOutputStream fos = new FileOutputStream( dumpfile );

	    // read bytes
	    int c;
	    while ( -1 != (c = is.read()) ) {
		os.write( c ); fos.write( c );
	    }
	    fos.close();
	    ByteArrayInputStream bis = new ByteArrayInputStream( os.toByteArray() );
	    return bis;
	}  // toMem

	public OAIHarvester(IndexId iid, String url, Authenticator auth) throws MalformedURLException{
		this.urlbase = url;
		this.iid = iid;
		URL base = new URL(url);
		this.host = base.getHost();
		log.info(iid+" using base url: "+url);
		Authenticator.setDefault(auth); 
	}
	
	/** Invoke ListRecords from a certain timestamp, fetching at least records..  */
	public ArrayList<IndexUpdateRecord> getRecords(String from, int atLeast) throws IOException {
		ArrayList<IndexUpdateRecord> ret = new ArrayList<IndexUpdateRecord>();
		read(new URL(urlbase+"&verb=ListRecords&metadataPrefix=mediawiki&from="+from));
		ret.addAll(collector.getRecords());
		if(ret.size() < atLeast && hasMore())
			ret.addAll( getMoreRecords(atLeast - ret.size()) );
		
		return ret;
	}		
	
	/** Get single record */
	public ArrayList<IndexUpdateRecord> getRecord(String key) throws IOException {
		// sample key: oai:localhost:wikilucene:25139
		String id = "oai:"+host+":"+iid.getDBname()+":"+key;
		read(new URL(urlbase+"&verb=GetRecord&metadataPrefix=mediawiki&identifier="+id));
		return collector.getRecords();
	}
	
	protected void read(URL url) throws IOException {
		log.info("Reading records from "+url);
		// try reading from url a number of times before giving up
		for(int tryNum = 1; tryNum <= this.retries; tryNum++){
			try{
				collector = new IndexUpdatesCollector(iid);
				URLConnection urlConn = url.openConnection();
				// set some timeouts
				urlConn.setReadTimeout(60 * 1000); // 60 seconds
				urlConn.setConnectTimeout(60 * 1000); // 60 seconds
				InputStream in;
				if ( ! DBG ) {
				    in = new BufferedInputStream( urlConn.getInputStream() );
				} else {
				    in = toMem( urlConn.getInputStream() );
				}

				parser = new OAIParser(in,collector);
				parser.parse();
				resumptionToken = parser.getResumptionToken();
				responseDate = parser.getResponseDate();
				in.close();
				log.trace( String.format( "resumptionToken = %s, responseDate = %s", resumptionToken, responseDate ) );
				break;
			} catch(IOException e){				
				if(tryNum == this.retries)
					throw e;
				else
					log.warn("Error reading from url (will retry): "+url);
			}
		}
	}

	/** Invoke ListRecords using the last resumption token, get atLeast num of records */
	public ArrayList<IndexUpdateRecord> getMoreRecords(int atLeast){
		ArrayList<IndexUpdateRecord> ret = new ArrayList<IndexUpdateRecord>();
		try{			
			do{
				URL url = new URL( urlbase + "&verb=ListRecords&metadataPrefix=mediawiki&resumptionToken=" + getSequence() );
				read( url );
				ret.addAll(collector.getRecords());
			} while(hasMore() && ret.size() < atLeast);
		} catch(IOException e){
			log.warn("I/O exception listing records: "+e.getMessage(),e);
			return null;
		}
		return ret;
	}
	
	/** Invoke ListRecords using the last resumption token, get atLeast num of records */
	public ArrayList<IndexUpdateRecord> getRecordsSeq( String seq, int atLeast ) {
		ArrayList<IndexUpdateRecord> ret = new ArrayList<IndexUpdateRecord>();
		try{			
			do{
				read( new URL( urlbase + "&verb=ListRecords&metadataPrefix=mediawiki&resumptionToken=" + seq ) );
				ret.addAll( collector.getRecords() );
			} while( hasMore() && ret.size() < atLeast );
		} catch( IOException e ){
			log.warn( "I/O exception listing records: " + e.getMessage(), e );
			return null;
		}
		return ret;
	}
	
	public boolean hasMore(){
		return resumptionToken.endsWith(":");
	}
	
	public String getSequence(){
		return resumptionToken.endsWith(":")
			? resumptionToken.substring( 0, resumptionToken.length() - 1 )
			: resumptionToken;
	}
	
	public String getResponseDate(){
		return responseDate;
	}

}
