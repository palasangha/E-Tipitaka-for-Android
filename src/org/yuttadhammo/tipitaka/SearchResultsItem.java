package org.yuttadhammo.tipitaka;

public class SearchResultsItem {
	private String lang;
	private String keywords;
	private String pages;
	private String suts;
	private String sCate;
	private String content;
	private String pClicked;
	private String sClicked;
	private String saved;
	private String marked;	

	public SearchResultsItem(String _lang, String _keywords, String _pages, String _suts, String _sCate) {
		lang = _lang;
		keywords = _keywords;
		pages = _pages;
		suts = _suts;
		sCate = _sCate;
		content = "";
		pClicked = "";
		sClicked = "";
		saved = "";
		marked = "";
	}
	
	public String getLanguage() {
		return lang;
	}
	
	public String getKeywords() {
		return keywords;
	}
	
	public String getPages() {
		return pages;
	}
	
	public String getSuts() {
		return suts;
	}
	
	public String getSelectedCategories() {
		return sCate;
	}
	
	public String getContent() {
		return content;
	}

	public String getPrimaryClicked() {
		return pClicked;
	}
	
	public String getSecondaryClicked() {
		return sClicked;
	}
	
	public String getSaved() {
		return saved;
	}
	
	public String getMarked() {
		return marked;
	}
	
	public void setLangauge(String _lang) {
		lang = _lang;
	}
	
	public void setKeywords(String _keywords) {
		keywords = _keywords;
	}
	
	public void setPages(String _pages) {
		pages = _pages;
	}
	
	public void setSuts(String _suts) {
		suts = _suts;
	}
	
	public void setSelectedCategories(String _sCate) {
		sCate = _sCate;
	}
	
	public void setContent(String _content) {
		content = _content;
	}
	
	public void setPrimaryClicked(String _pClicked) {
		pClicked = _pClicked;
	}
	
	public void setSecondaryClicked(String _sClicked) {
		sClicked = _sClicked;
	}
	
	public void setSaved(String _saved) {
		saved = _saved;
	}
	
	public void setMarked(String _marked) {
		marked = _marked;
	}
}