/**
 * Copyright (c) 2003, David A. Czarnecki
 * All rights reserved.
 *
 * Portions Copyright (c) 2003 by Mark Lussier
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of the "David A. Czarnecki" and "blojsom" nor the names of
 * its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Products derived from this software may not be called "blojsom",
 * nor may "blojsom" appear in their name, without prior written permission of
 * David A. Czarnecki.
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
package org.ignition.blojsom.extension.echoapi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.Base64;
import org.ignition.blojsom.blog.Blog;
import org.ignition.blojsom.blog.BlogCategory;
import org.ignition.blojsom.blog.BlogEntry;
import org.ignition.blojsom.blog.BlojsomConfigurationException;
import org.ignition.blojsom.fetcher.BlojsomFetcher;
import org.ignition.blojsom.fetcher.BlojsomFetcherException;
import org.ignition.blojsom.util.BlojsomConstants;
import org.ignition.blojsom.util.BlojsomUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * EchoAPIServlet
 *
 * Implementation of J.C. Gregorio's EchoAPI
 * <a href="http://bitworking.org/rfc/draft-gregorio-03.html">http://bitworking.org/rfc/draft-gregorio-03.html</a>
 *
 * @author Mark Lussier
 * @version $Id: EchoAPIServlet.java,v 1.5 2003-07-15 01:08:34 intabulas Exp $
 */
public class EchoAPIServlet extends HttpServlet implements BlojsomConstants, EchoConstants {

    private static final String FETCHER_PERMALINK = "FETCHER_PERMALINK";
    private static final String FETCHER_FLAVOR = "FETCHER_FLAVOR";
    private static final String FETCHER_NUM_POSTS_INTEGER = "FETCHER_NUM_POSTS_INTEGER";
    private static final String FETCHER_CATEGORY = "FETCHER_CATEGORY";


    private static final String BLOG_CONFIGURATION_IP = "blog-configuration";
    private static final String DEFAULT_BLOJSOM_CONFIGURATION = "/WEB-INF/blojsom.properties";


    private static final String HEADER_LOCATION = "Location";
    private static final String HEADER_AUTHORIZATION = "Authorization";


    private static final String CONTENTTYPE_ECHO = "application/not-echo+xml";

    private Log _logger = LogFactory.getLog(EchoAPIServlet.class);

    protected Blog _blog = null;

    private BlojsomFetcher _fetcher;

    /**
     * Public Constructor
     */
    public EchoAPIServlet() {
    }

    /**
     * Configure the authorization table blog (user id's and and passwords)
     *
     * @param servletConfig Servlet configuration information
     */
    private void configureAuthorization(ServletConfig servletConfig) {
        Map authorization = new HashMap();

        String authConfiguration = servletConfig.getInitParameter(BLOG_AUTHORIZATION_IP);
        Properties authProperties = new Properties();
        InputStream is = servletConfig.getServletContext().getResourceAsStream(authConfiguration);
        try {
            authProperties.load(is);
            is.close();
            Iterator authIterator = authProperties.keySet().iterator();
            while (authIterator.hasNext()) {
                String userid = (String) authIterator.next();
                String password = authProperties.getProperty(userid);
                authorization.put(userid, password);
            }

            if (!_blog.setAuthorization(authorization)) {
                _logger.error("Authorization table could not be assigned");
            }

        } catch (IOException e) {
            _logger.error(e);
        }
    }

    /**
     * Load blojsom configuration information
     *
     * @param context Servlet context
     * @param filename blojsom configuration file to be loaded
     */
    public void processBlojsomCongfiguration(ServletContext context, String filename) {
        Properties configuration = new Properties();
        InputStream cis = context.getResourceAsStream(filename);

        try {
            configuration.load(cis);
            cis.close();
            _blog = new Blog(configuration);
        } catch (IOException e) {
            _logger.error(e);
        } catch (BlojsomConfigurationException e) {
            _logger.error(e);
        }
    }


    /**
     * Configure the {@link org.ignition.blojsom.fetcher.BlojsomFetcher} that will be used to fetch categories and
     * entries
     *
     * @param servletConfig Servlet configuration information
     * @throws ServletException If the {@link org.ignition.blojsom.fetcher.BlojsomFetcher} class could not be loaded and/or initialized
     */
    private void configureFetcher(ServletConfig servletConfig) throws ServletException {
        String fetcherClassName = _blog.getBlogFetcher();
        if ((fetcherClassName == null) || "".equals(fetcherClassName)) {
            fetcherClassName = BLOG_DEFAULT_FETCHER;
        }

        try {
            Class fetcherClass = Class.forName(fetcherClassName);
            _fetcher = (BlojsomFetcher) fetcherClass.newInstance();
            _fetcher.init(servletConfig, _blog);
            _logger.info("Added blojsom fetcher: " + fetcherClassName);
        } catch (ClassNotFoundException e) {
            _logger.error(e);
            throw new ServletException(e);
        } catch (InstantiationException e) {
            _logger.error(e);
            throw new ServletException(e);
        } catch (IllegalAccessException e) {
            _logger.error(e);
            throw new ServletException(e);
        } catch (BlojsomFetcherException e) {
            _logger.error(e);
            throw new ServletException(e);
        }
    }


    /**
     * Initialize the blojsom EchoAPI servlet
     *
     * @param servletConfig Servlet configuration information
     * @throws javax.servlet.ServletException If there is an error initializing the servlet
     */
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        String cfgfile = servletConfig.getInitParameter(BLOG_CONFIGURATION_IP);

        if (cfgfile == null || cfgfile.equals("")) {
            _logger.info("blojsom configuration not specified, using " + DEFAULT_BLOJSOM_CONFIGURATION);
            cfgfile = DEFAULT_BLOJSOM_CONFIGURATION;
        }

        processBlojsomCongfiguration(servletConfig.getServletContext(), cfgfile);
        processBlojsomCongfiguration(servletConfig.getServletContext(), cfgfile);
        configureAuthorization(servletConfig);
        configureFetcher(servletConfig);

        _logger.info("EchoAPI initialized, home is [" + _blog.getBlogHome() + "]");

    }


    /**
     * Decode the HTTP Authorization Header
     * @param httpServletRequest
     * @return
     */
    private String extractAuthorization(HttpServletRequest httpServletRequest) {
        String result = null;
        String auth = httpServletRequest.getHeader(HEADER_AUTHORIZATION);
        if ((auth != null) && !"".equals(auth)) {
            result = new String(Base64.decode(auth.getBytes()));
        }
        return result;
    }


    /**
     *
     * @param httpServletRequest Request
     * @return
     */
    public boolean isAuthorized(HttpServletRequest httpServletRequest) {
        boolean result = false;
        String realmAuth = extractAuthorization(httpServletRequest);
        if (realmAuth != null && !"".equals(realmAuth)) {
            _logger.info("Processing Authorization for [" + realmAuth + "]");
            int pos = realmAuth.indexOf(":");
            if (pos > 0) {
                String username = realmAuth.substring(0, pos);
                String password = realmAuth.substring(pos + 1);
                result = _blog.checkAuthorization(username, password);
            }

        }

        return result;

    }


    /**
     *
     * @param httpServletRequest Request
     * @param httpServletResponse Response
     * @throws ServletException If there is an error processing the request
     * @throws IOException If there is an error during I/O
     */
    protected void doDelete(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

        _logger.info("EchoAPI Delete Called =====[ SUPPORTED ]=====");
        _logger.info("       Path: " + httpServletRequest.getPathInfo());

        if (isAuthorized(httpServletRequest)) {

        } else {
            httpServletResponse.setContentType("text/html");
            httpServletResponse.setHeader("WWW-Authenticate", "basic realm=\"/blojsom\"");
            httpServletResponse.setStatus(401);
        }

    }


    /**
     *
     * @param httpServletRequest Request
     * @param httpServletResponse Response
     * @throws ServletException If there is an error processing the request
     * @throws IOException If there is an error during I/O
     */
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {


        _logger.info("EchoAPI GET Called =====[ SUPPORTED ]=====");
        _logger.info("       Path: " + httpServletRequest.getPathInfo());

        // NOTE: Assumes that the getPathInfo() returns only category data

        String permalink = BlojsomUtils.getRequestValue(PERMALINK_PARAM, httpServletRequest);
        String category = BlojsomUtils.normalize(httpServletRequest.getPathInfo());

        Map fetchMap = new HashMap();
        BlogCategory blogCategory = _fetcher.newBlogCategory();
        blogCategory.setCategory(category);
        blogCategory.setCategoryURL(_blog.getBlogURL() + category);
        fetchMap.put(FETCHER_CATEGORY, blogCategory);
        fetchMap.put(FETCHER_PERMALINK, permalink);
        try {
            BlogEntry[] _entries = _fetcher.fetchEntries(fetchMap);

            if (_entries != null && _entries.length > 0) {
                BlogEntry entry = _entries[0];
                // EchoEntry converts a BlogEntry to an Echo Entry XML stream..
                // VERY messy right now and it will be refactored to be bidi
                EchoEntry echo = new EchoEntry(_blog, entry);
                String content = echo.getAsString();
                httpServletResponse.setContentType(CONTENTTYPE_ECHO);
                httpServletResponse.setStatus(200);
                httpServletResponse.setContentLength(content.length());
                OutputStreamWriter osw = new OutputStreamWriter(httpServletResponse.getOutputStream(), "UTF-8");
                osw.write(content);
                osw.flush();

            }
        } catch (BlojsomFetcherException e) {
            _logger.error(e);
            httpServletResponse.setStatus(404);
        }


    }

    /**
     * Handle HTTP POST
     *
     * @param httpServletRequest Request
     * @param httpServletResponse Response
     * @throws ServletException If there is an error processing the request
     * @throws IOException If there is an error during I/O
     */
    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

        _logger.info("EchoAPI POST Called =====[ SUPPORTED ]=====");
        _logger.info("       Path: " + httpServletRequest.getPathInfo());

        if (isAuthorized(httpServletRequest)) {

        }


    }

    /**
     * Handle HTTP PUT
     * @param httpServletRequest Request
     * @param httpServletResponse Response
     * @throws ServletException If there is an error processing the request
     * @throws IOException If there is an error during I/O
     */
    protected void doPut(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

        _logger.info("EchoAPI PUT Called =====[ SUPPORTED ]=====");
        _logger.info("       Path: " + httpServletRequest.getPathInfo());

        if (isAuthorized(httpServletRequest)) {

        }


    }


    /**
     * Called when removing the servlet from the servlet container
     */
    public void destroy() {
        try {
            _fetcher.destroy();
        } catch (BlojsomFetcherException e) {
            _logger.error(e);
        }

    }

}

