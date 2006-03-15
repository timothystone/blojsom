/**
 * Copyright (c) 2003-2006, David A. Czarnecki
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *     following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *     following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of "David A. Czarnecki" and "blojsom" nor the names of its contributors may be used to
 *     endorse or promote products derived from this software without specific prior written permission.
 * Products derived from this software may not be called "blojsom", nor may "blojsom" appear in their name,
 *     without prior written permission of David A. Czarnecki.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.blojsom.extension.xmlrpc.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.blojsom.BlojsomException;
import org.blojsom.plugin.pingback.event.PingbackResponseSubmissionEvent;
import org.blojsom.plugin.pingback.event.PingbackAddedEvent;
import org.blojsom.plugin.pingback.PingbackPlugin;
import org.blojsom.blog.*;
import org.blojsom.fetcher.BlojsomFetcherException;
import org.blojsom.util.BlojsomUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pingback handler provides support for the <a href="http://www.hixie.ch/specs/pingback/pingback">Pingback 1.0</a>
 * specification.
 *
 * @author David Czarnecki
 * @version $Id: PingbackHandler.java,v 1.9 2006-03-15 02:28:10 czarneckid Exp $
 * @since blojsom 2.23
 */
public class PingbackHandler extends AbstractBlojsomAPIHandler {

    private static final Log _logger = LogFactory.getLog(PingbackHandler.class);

    private static final String TITLE_PATTERN = "<title>(.*)</title>";

    protected static final String API_NAME = "pingback";

    protected static final int PINGBACK_GENERIC_FAULT_CODE = 0;
    protected static final int PINGBACK_SOURCE_URI_NON_EXISTENT_CODE = 16;
    protected static final int PINGBACK_NO_LINK_TO_TARGET_URI_CODE = 17;
    protected static final int PINGBACK_TARGET_URI_NON_EXISTENT_CODE = 32;
    protected static final int PINGBACK_TARGET_URI_NOT_ENABLED_CODE = 33;
    protected static final int PINGBACK_ALREADY_REGISTERED_CODE = 48;
    protected static final int PINGBACK_ACCESS_DENIED_CODE = 49;
    protected static final int PINGBACK_UPSTREAM_SERVER_ERROR_CODE = 50;

    /**
     * Construct a new Pingback handler
     */
    public PingbackHandler() {
    }

    /**
     * Attach a blog instance to the API Handler so that it can interact with the blog
     *
     * @param blogUser an instance of BlogUser
     * @throws org.blojsom.BlojsomException If there is an error setting the blog user instance or properties for the handler
     * @see org.blojsom.blog.BlogUser
     */
    public void setBlogUser(BlogUser blogUser) throws BlojsomException {
        _blogUser = blogUser;
        _blog = _blogUser.getBlog();
        _blogEntryExtension = _blog.getBlogProperty(BLOG_XMLRPC_ENTRY_EXTENSION_IP);
        if (BlojsomUtils.checkNullOrBlank(_blogEntryExtension)) {
            _blogEntryExtension = DEFAULT_BLOG_XMLRPC_ENTRY_EXTENSION;
        }
    }

    /**
     * Gets the name of API Handler. Used to bind to XML-RPC
     *
     * @return The API Name (ie: pingback)
     */
    public String getName() {
        return API_NAME;
    }

    /**
     * Try to find the &lt;title&gt;&lt/title&gt; tags from the source text
     *
     * @param source Source URI text
     * @return Title of text or <code>null</code> if title tags are not found
     */
    protected String getTitleFromSource(String source) {
        String title = null;
        Pattern titlePattern = Pattern.compile(TITLE_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL | Pattern.UNICODE_CASE);
        Matcher titleMatcher = titlePattern.matcher(source);
        if (titleMatcher.find()) {
            title = titleMatcher.group(1);
        }

        return title;
    }

    /**
     * Try to extract an excerpt from the source text. Currently looks ahead 200 and ahead 200 characters from
     * the location of the targetURI within the source.
     *
     * @param source Source URI text
     * @param targetURI Target URI from which to start the excerpt
     * @return Excerpt of text or <code>null</code> if we cannot find the targetURI
     */
    protected String getExcerptFromSource(String source, String targetURI) {
        String excerpt = null;

        int startOfTarget = source.indexOf(targetURI);
        if (startOfTarget != -1) {
            int startOfExcerpt = startOfTarget - 200;
            if (startOfExcerpt < 0) {
                startOfExcerpt = 0;
            }

            int endOfExcerpt = startOfTarget + 200;
            if (endOfExcerpt > source.length()) {
                endOfExcerpt = source.length();
            }

            excerpt = source.substring(startOfExcerpt, endOfExcerpt);
            excerpt = BlojsomUtils.stripHTML(excerpt);
            int firstSpace = excerpt.indexOf(' ') + 1;
            int lastSpace = excerpt.lastIndexOf(' ');
            if (-1 == lastSpace || lastSpace < firstSpace)
                lastSpace = excerpt.length();
            excerpt = excerpt.substring(firstSpace, lastSpace);
        }

        return excerpt;
    }

    /**
     * Notifies the server that a link has been added to sourceURI, pointing to targetURI.
     *
     * @param sourceURI The absolute URI of the post on the source page containing the link to the target site.
     * @param targetURI The absolute URI of the target of the link, as given on the source page.
     * @return
     */
    public String ping(String sourceURI, String targetURI) throws XmlRpcException {
        _logger.debug("Pingback from: " + sourceURI + " to: " + targetURI);

        if (BlojsomUtils.checkNullOrBlank(sourceURI)) {
            _logger.error("Pingback must include a source URI");

            throw new XmlRpcException(PINGBACK_SOURCE_URI_NON_EXISTENT_CODE, "Pingback must include a source URI");
        }

        // Fetch sourceURI to make sure there is a link to the targetURI
        StringBuffer sourcePage;
        try {
            URL source = new URL(sourceURI);
            HttpURLConnection sourceConnection = (HttpURLConnection) source.openConnection();
            sourceConnection.setRequestMethod("GET");
            sourceConnection.connect();
            BufferedReader sourceReader = new BufferedReader(new InputStreamReader(sourceConnection.getInputStream(), UTF8));
            String line;
            sourcePage = new StringBuffer();

            while ((line = sourceReader.readLine()) != null) {
                sourcePage.append(line);
                sourcePage.append(LINE_SEPARATOR);
            }
        } catch (IOException e) {
            _logger.error(e);

            throw new XmlRpcException(PINGBACK_GENERIC_FAULT_CODE, "Unable to retrieve source URI");
        }

        // Check that the sourceURI contains a link to the targetURI
        if (sourcePage.indexOf(targetURI) == -1) {
            _logger.error("Target URI not found in Source URI");

            throw new XmlRpcException(PINGBACK_NO_LINK_TO_TARGET_URI_CODE, "Target URI not found in source URI");
        }

        // Check targetURI exists and is a valid entry
        try {
            URL target = new URL(targetURI);
            HttpURLConnection httpURLConnection = (HttpURLConnection) target.openConnection();
            httpURLConnection.setRequestMethod("HEAD");
            httpURLConnection.connect();

            if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                _logger.error("Target URI does not exist");

                throw new XmlRpcException(PINGBACK_TARGET_URI_NON_EXISTENT_CODE, "Target URI does not exist");
            }
        } catch (IOException e) {
            _logger.error(e);

            throw new XmlRpcException(PINGBACK_GENERIC_FAULT_CODE, "Unable to retrieve target URI");
        }

        String pingbackID = BlojsomUtils.digestString(sourceURI + ":" + targetURI);

        BlogCategory blogCategory = getBlogCategory(_blogUser, _httpServletRequest);
        String permalink = BlojsomUtils.getRequestValue(PERMALINK_PARAM, _httpServletRequest);

        // Check the resource is pingback-enabled
        try {
            BlogEntry blogEntry = BlojsomUtils.fetchEntry(_fetcher, _blogUser, blogCategory.getCategory(), permalink);
            if (_blog.getBlogPingbacksEnabled().booleanValue() && blogEntry.supportsPingbacks()) {
                Map pingbackMetaData = new HashMap();
                pingbackMetaData.put(PingbackPlugin.BLOJSOM_PINGBACK_PLUGIN_METADATA_IP, _httpServletRequest.getRemoteAddr());

                // Record pingback
                Pingback pingback = _fetcher.newPingback();
                pingback.setBlogEntry(blogEntry);

                _configuration.getEventBroadcaster().processEvent(new PingbackResponseSubmissionEvent(this, new Date(), _blogUser, _httpServletRequest, _httpServletResponse, getTitleFromSource(sourcePage.toString()), getTitleFromSource(sourcePage.toString()), sourceURI, getExcerptFromSource(sourcePage.toString(), targetURI), blogEntry, pingbackMetaData));

                // Check to see if the trackback should be destroyed (not saved) automatically
                if (!pingbackMetaData.containsKey(PingbackPlugin.BLOJSOM_PLUGIN_PINGBACK_METADATA_DESTROY)) {

                    Integer status = addPingback(new HashMap(), blogCategory.getCategory(), permalink, blogEntry.getTitle(), getExcerptFromSource(sourcePage.toString(), targetURI),
                            sourceURI, getTitleFromSource(sourcePage.toString()), _blog.getBlogFileExtensions(), _blog.getBlogHome(),
                            _blog.getBlogPingbacksDirectory(), UTF8, pingbackMetaData, pingback, pingbackID);

                    if (status.intValue() != 0) {
                        throw new XmlRpcException(status.intValue(), "Unknown exception occurred");
                    } else {
                        _configuration.getEventBroadcaster().broadcastEvent(new PingbackAddedEvent(this, new Date(), pingback, _blogUser));
                    }
                } else {
                    _logger.info("Pingback meta-data contained destroy key. Pingback was not saved");

                    throw new XmlRpcException(PINGBACK_ACCESS_DENIED_CODE, "Pingback meta-data contained destroy key. Pingback was not saved.");
                }
            } else {
                _logger.debug("Target URI does not support pingbacks");

                throw new XmlRpcException(PINGBACK_TARGET_URI_NOT_ENABLED_CODE, "Target URI does not support pingbacks");
            }
        } catch (BlojsomFetcherException e) {
            _logger.error(e);

            throw new XmlRpcException(PINGBACK_TARGET_URI_NON_EXISTENT_CODE, "Target URI does not exist");
        }

        // Update notification
        return "Registered pingback from: " + sourceURI + " to: " + targetURI;
    }

    /**
     * Add a pingback for a given blog ID
     *
     * @param context Context
     * @param category Category
     * @param permalink Permalink
     * @param title Pingback title
     * @param excerpt Pingback excerpt
     * @param url Pingback URL
     * @param blogName Pingback blog name
     * @param blogFileExtensions File extensions
     * @param blogHome Blog home
     * @param blogPingbackDirectory Pingbacks directory
     * @param blogFileEncoding Blog file encoding
     * @param pingbackMetaData Pingback meta-data
     * @param pingback {@link Pingback}
     * @param id ID to use for pingback
     * @return <code>0</code> if the pingback was registered, otherwise a fault code is returned
     */
    protected Integer addPingback(Map context, String category, String permalink, String title,
                                  String excerpt, String url, String blogName,
                                  String[] blogFileExtensions, String blogHome,
                                  String blogPingbackDirectory, String blogFileEncoding, Map pingbackMetaData,
                                  Pingback pingback, String id) throws XmlRpcException {
        title = BlojsomUtils.escapeStringSimple(title);
        title = BlojsomUtils.stripLineTerminators(title, " ");
        pingback.setTitle(title);

        excerpt = BlojsomUtils.escapeStringSimple(excerpt);
        excerpt = BlojsomUtils.stripLineTerminators(excerpt, " ");
        pingback.setExcerpt(excerpt);

        url = BlojsomUtils.escapeStringSimple(url);
        url = BlojsomUtils.stripLineTerminators(url, " ");
        pingback.setUrl(url);

        blogName = BlojsomUtils.escapeStringSimple(blogName);
        blogName = BlojsomUtils.stripLineTerminators(blogName, " ");
        pingback.setBlogName(blogName);

        pingback.setTrackbackDateLong(new Date().getTime());
        pingback.setMetaData(pingbackMetaData);
        pingback.setId(id);

        try {
            pingback.save(_blogUser);
        } catch (BlojsomException e) {
            if (e.getCause() instanceof XmlRpcException) {
                throw (XmlRpcException) e.getCause();
            }
        }

        return new Integer(0);
    }

    /**
     * Determine the blog category based on the request
     *
     * @param httpServletRequest Request
     * @return {@link org.blojsom.blog.BlogCategory} of the requested category
     */
    protected BlogCategory getBlogCategory(BlogUser user, HttpServletRequest httpServletRequest) {
        Blog blog = user.getBlog();

        // Determine the user requested category
        String requestedCategory;
        String userFromPath = BlojsomUtils.getUserFromPath(httpServletRequest.getPathInfo());
        if (userFromPath == null) {
            requestedCategory = httpServletRequest.getPathInfo();
        } else {
            _logger.debug("User: " + user.getId());
            _logger.debug("Path: " + userFromPath);
            if (userFromPath.equals(user.getId())) {
                requestedCategory = BlojsomUtils.getCategoryFromPath(httpServletRequest.getPathInfo());
            } else {
                requestedCategory = httpServletRequest.getPathInfo();
            }
        }

        requestedCategory = BlojsomUtils.normalize(requestedCategory);
        _logger.debug("blojsom path info: " + requestedCategory);

        String categoryParameter = httpServletRequest.getParameter(CATEGORY_PARAM);
        if (!(categoryParameter == null) && !("".equals(categoryParameter))) {
            categoryParameter = BlojsomUtils.normalize(categoryParameter);
            _logger.debug("Category parameter override: " + categoryParameter);
            requestedCategory = categoryParameter;
        }

        if (requestedCategory == null) {
            requestedCategory = "/";
        } else if (!requestedCategory.endsWith("/")) {
            requestedCategory += "/";
        }

        requestedCategory = BlojsomUtils.urlDecode(requestedCategory);
        _logger.debug("User requested category: " + requestedCategory);
        BlogCategory category = _fetcher.newBlogCategory();
        category.setCategory(requestedCategory);
        category.setCategoryURL(blog.getBlogURL() + BlojsomUtils.removeInitialSlash(requestedCategory));

        try {
            category.load(user);
        } catch (BlojsomException e) {
            _logger.error(e);
        }

        return category;
    }
}