package org.wikimedia.lsearch.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import org.mediawiki.importer.DumpWriter;
import org.mediawiki.importer.Page;
import org.mediawiki.importer.Revision;
import org.mediawiki.importer.Siteinfo;
import org.wikimedia.lsearch.beans.Article;
import org.wikimedia.lsearch.beans.Redirect;
import org.wikimedia.lsearch.beans.Title;
import org.wikimedia.lsearch.config.Configuration;
import org.wikimedia.lsearch.config.GlobalConfiguration;
import org.wikimedia.lsearch.config.IndexId;
import org.wikimedia.lsearch.ranks.Links;
import org.wikimedia.lsearch.related.RelatedTitle;
import org.wikimedia.lsearch.storage.RelatedStorage;
import org.wikimedia.lsearch.util.Localization;

public class DumpImporter implements DumpWriter {
	static Logger log = Logger.getLogger(DumpImporter.class);
	Page page;
	Revision revision;
	SimpleIndexWriter indexWriter = null, highlightWriter = null, titleWriter = null;
	int count = 0, limit;
	Links links;
	String langCode;
	RelatedStorage related;
	boolean makeIndex, makeHighlight, makeTitle;
	GlobalConfiguration global;
	IndexId iid;

	public DumpImporter(String dbname, int limit, Boolean optimize, Integer mergeFactor, 
			Integer maxBufDocs, boolean newIndex, Links links, String langCode,
			boolean makeIndex, boolean makeHighlight, boolean makeTitle, boolean newTitlesIndex){
		Configuration.open(); // make sure configuration is loaded
		global = GlobalConfiguration.getInstance();
		iid = IndexId.get(dbname);
		if(makeIndex)
			indexWriter = new SimpleIndexWriter(iid, optimize, mergeFactor, maxBufDocs, newIndex, iid);
		if(makeHighlight)
			highlightWriter = new SimpleIndexWriter(iid.getHighlight(), optimize, mergeFactor, maxBufDocs, newIndex, iid);
		if(makeTitle && iid.hasTitlesIndex())
			titleWriter = new SimpleIndexWriter(iid.getTitlesIndex(), optimize, mergeFactor, maxBufDocs, newTitlesIndex, iid);
		this.limit = limit;
		this.links = links;
		this.makeIndex = makeIndex;
		this.makeHighlight = makeHighlight;
		this.makeTitle = makeTitle;
		this.langCode = langCode;
		this.related = new RelatedStorage(iid);
		
		if(!related.canRead())
			related = null; // add only if available
	}
	public void writeRevision(Revision revision) throws IOException {
		this.revision = revision;		
	}
	public void writeStartPage(Page page) throws IOException {
		this.page = page;
	}
	public void writeEndPage() throws IOException {
		String key = page.Title.Namespace+":"+page.Title.Text;
		// defaults:
		int references = 0;
		boolean isRedirect = false;
		int redirectTargetNamespace = -1;
		ArrayList<Redirect> redirects = new ArrayList<Redirect>();
		Hashtable<String,Integer> anchors = new Hashtable<String,Integer>();
		ArrayList<RelatedTitle> rel = new ArrayList<RelatedTitle>();
		Date date = new Date(revision.Timestamp.getTimeInMillis());
		
		references = links.getNumInLinks(key);
		String redirectTo = links.getRedirectTarget(key);
		isRedirect = redirectTo != null;
		redirectTargetNamespace = isRedirect? links.getRedirectTargetNamespace(key) : -1;
		anchors.putAll(links.getAnchorMap(key,references));
		
		// make list of redirects
		redirects = new ArrayList<Redirect>();
		for(String rk : links.getRedirectsTo(key)){
			String[] parts = Title.partsFromKey(rk);
			int redirectRef = links.getNumInLinks(rk);
			redirects.add(new Redirect(Integer.parseInt(parts[0]),parts[1],redirectRef));
			Links.mergeAnchorMaps(anchors,links.getAnchorMap(rk,redirectRef));
		}
		// related
		if(makeIndex && related != null)
			rel = related.getRelated(key);
		// make article
		Article article = new Article(page.Id,page.Title.Namespace,
				page.Title.Text,revision.Text,redirectTo,references,
				redirectTargetNamespace,0,redirects,rel,anchors,date,
				processLiquidThreadInfo(page.DiscussionThreadingInfo));
		// index
		if(indexWriter != null)
			indexWriter.addArticle(article);
		if(highlightWriter != null)
			highlightWriter.addArticleHighlight(article);
		if(titleWriter != null)
			titleWriter.addArticleTitle(article);
		
		count++;
		if(limit >= 0 && count > limit)
			throw new IOException("stopped");
	}	
	
	/** Process LQT properties, convert titles into correct format */
	public static Hashtable<String, String> processLiquidThreadInfo(Hashtable<String,Object> info){
		Enumeration<String> e = info.keys();
		Hashtable<String,String> res = new Hashtable<String,String>();
		while (e.hasMoreElements()) {
			String key = (String)e.nextElement();
			Object rawvalue = info.get(key);
			String value = rawvalue.toString();
			if(rawvalue instanceof org.mediawiki.importer.Title){
				// put titles into <ns>:<title> format (where ns is integer)
				org.mediawiki.importer.Title t = (org.mediawiki.importer.Title) rawvalue;
				value = t.Namespace+":"+t.Text;
			}
			res.put(key, value);
		}
		
		return res;	
	}
	
	public void close() throws IOException {
		// nop		
	}
	public void writeEndWiki() throws IOException {
		// nop
	}
	public void writeSiteinfo(Siteinfo info) throws IOException {
		Iterator<Entry<Integer, String>> it = info.Namespaces.orderedEntries();
		while(it.hasNext()){
			Entry<Integer,String> pair = it.next();
			Localization.addCustomMapping(pair.getValue(),pair.getKey(),iid.getDBname());
		}
	}	
	public void writeStartWiki() throws IOException {
		// nop		
	}
	
	public void closeIndex() throws IOException {
		if(indexWriter != null)
			indexWriter.close();
		if(highlightWriter != null)
			highlightWriter.close();
		if(titleWriter != null)
			titleWriter.close();
	}
	

}
