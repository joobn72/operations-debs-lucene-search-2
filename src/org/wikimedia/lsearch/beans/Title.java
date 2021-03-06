package org.wikimedia.lsearch.beans;

import org.wikimedia.lsearch.config.IndexId;
import org.wikimedia.lsearch.util.Localization;

/** Wiki article title */
public class Title implements java.io.Serializable {
    public int namespace;

    public String title;

    public Title() {
   	 namespace = 0;
   	 title = "";
    }

    public Title(int namespace,String title) {
   	 this.namespace = namespace;
   	 this.title = title;
    }
    
    public Title(String key){
   	 int col = key.indexOf(':');
   	 if(col == -1)
   		 throw new RuntimeException("Wrong key format in Title constructor");
   	 this.namespace = Integer.parseInt(key.substring(0,col));
   	 this.title = key.substring(col+1);
    }
    
    public String getKey(){
   	 return namespace+":"+title;
    }
    
    /** Get title from ns:title */
    public static String titleFromKey(String key){
   	 return key.substring(key.indexOf(':')+1);
    }
    /** Get [ns, title] from ns:title */ 
    public static String[] partsFromKey(String key){
   	 int in = key.indexOf(':');
   	 return new String[] { key.substring(0,in), key.substring(in+1) };
    }

    @Override
	public String toString() {
		return namespace+":"+title;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + namespace;
		result = PRIME * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Title other = (Title) obj;
		if (namespace != other.namespace)
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}

	/**
     * Gets the namespace value for this Title.
     * 
     * @return namespace
     */
    public int getNamespace() {
        return namespace;
    }


    /**
     * Sets the namespace value for this Title.
     * 
     * @param namespace
     */
    public void setNamespace(int namespace) {
        this.namespace = namespace;
    }


    /**
     * Gets the title value for this Title.
     * 
     * @return title
     */
    public java.lang.String getTitle() {
        return title;
    }


    /**
     * Sets the title value for this Title.
     * 
     * @param title
     */
    public void setTitle(java.lang.String title) {
        this.title = title;
    }
    
    /**
     * Get string representation of namespace
     * 
     * @return
     */
    public String getNamespaceAsString(){
   	 return Integer.toString(namespace);
    }

    /**
     * Make Wikipedia:Contents from 4:Contents using localisation. This function could be a
     * synchronization bottleneck and should not be used in highly concurrent environment. 
     * 
     * @param key
     * @return
     */
    public static String textualFromKey(String key, IndexId iid) {
   	 int sc = key.indexOf(':');
   	 iid = iid.getDB();
   	 String nsName = Localization.getNamespaceText(iid.getLangCode(),Integer.parseInt(key.substring(0,sc)),iid.getDBname());
   	 if(nsName == null)
   		 throw new RuntimeException("Cannot get namespace name for key="+key+", on iid="+iid);
   	 if(nsName.equals("")) // main ns
   		 return key.substring(sc+1);
   	 else
   		 return nsName + key.substring(sc);
    }
    
    /** 
     * Extract 0 (as int) from 0:Some title
     * 
     * @param key
     * @return
     */
    public static int namespaceAsInt(String key){
   	 try{
   		 return Integer.parseInt(key.substring(0,key.indexOf(':')));
   	 } catch(Exception e){
   		 throw new RuntimeException("Cannot parse namespace form key="+key,e);
   	 }
    }

}
