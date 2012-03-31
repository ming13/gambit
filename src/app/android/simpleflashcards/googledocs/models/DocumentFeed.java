package app.android.simpleflashcards.googledocs.models;


import java.util.ArrayList;
import java.util.List;

import app.android.simpleflashcards.googledocs.DocumentsListUrl;

import com.google.api.client.util.Key;


public class DocumentFeed
{
	private static final String RESUMABLE_CREATE_MEDIA_SCHEME = "http://schemas.google.com/g/2005#resumable-create-media";
	private static final String POST_SCHEME = "http://schemas.google.com/g/2005#post";

	@Key("link")
	private LinksList links;

	@Key("entry")
	private List<DocumentEntry> entries;

	public DocumentFeed() {
		links = new LinksList();
		entries = new ArrayList<DocumentEntry>();
	}

	public List<DocumentEntry> getEntries() {
		return entries;
	}

	public DocumentsListUrl getResumableCreateMediaUrl() {
		String resumableCreateMediaHref = links.findFirstWithRel(RESUMABLE_CREATE_MEDIA_SCHEME)
			.getHref();
		return new DocumentsListUrl(resumableCreateMediaHref);
	}

	public DocumentsListUrl getPostUrl() {
		String postHref = links.findFirstWithRel(POST_SCHEME).getHref();
		return new DocumentsListUrl(postHref);
	}
}