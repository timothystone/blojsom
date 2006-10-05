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
package org.blojsom.upgrade;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.blojsom.blog.Blog;
import org.blojsom.blog.*;
import org.blojsom.blog.database.*;
import org.blojsom.fetcher.Fetcher;
import org.blojsom.fetcher.FetcherException;
import org.blojsom.plugin.common.ResponseConstants;
import org.blojsom.util.BlojsomConstants;
import org.blojsom.util.BlojsomMetaDataConstants;
import org.blojsom2.BlojsomException;
import org.blojsom2.blog.*;
import org.blojsom2.blog.Pingback;
import org.blojsom2.blog.Trackback;
import org.blojsom2.fetcher.BlojsomFetcher;
import org.blojsom2.fetcher.BlojsomFetcherException;
import org.blojsom2.util.BlojsomProperties;
import org.blojsom2.util.BlojsomUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.InvalidPropertyException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Utility class to migrate from blojsom 2 to blojsom 3
 *
 * @author David Czarnecki
 * @since blojsom 3
 * @version $Id: Blojsom2ToBlojsom3Utility.java,v 1.12 2006-10-05 21:24:33 czarneckid Exp $
 */
public class Blojsom2ToBlojsom3Utility {

    private static Log _logger = LogFactory.getLog(Blojsom2ToBlojsom3Utility.class);

    private ServletConfig _servletConfig;
    private Fetcher _fetcher;

    private BlojsomFetcher _blojsom2Fetcher;
    private BlojsomConfiguration _blojsomConfiguration;

    private String _blojsom2Path;
    private String _blojsom3Path;

    /**
     * Construct a new instance of the blojsom 2 to blojsom 3 utility
     */
    public Blojsom2ToBlojsom3Utility() {
    }

    /**
     * Set the path to the blojsom 2 installation directory
     *
     * @param blojsom2Path blojsom 2 installation directory
     */
    public void setBlojsom2Path(String blojsom2Path) {
        _blojsom2Path = blojsom2Path;
    }

    /**
     * Set the path to the blojsom 3 installation directory
     *
     * @param blojsom3Path blojsom 3 installation directory
     */
    public void setBlojsom3Path(String blojsom3Path) {
        _blojsom3Path = blojsom3Path;
    }

    /**
     * Set the {@link Fetcher}
     *
     * @param fetcher {@link Fetcher}
     */
    public void setFetcher(Fetcher fetcher) {
        _fetcher = fetcher;
    }

    /**
     * Set the {@link ServletConfig}
     *
     * @param servletConfig {@link ServletConfig}
     */
    public void setServletConfig(ServletConfig servletConfig) {
        _servletConfig = servletConfig;
    }

    /**
     * Configure the {@link BlojsomFetcher} that will be used to fetch categories and
     * entries
     *
     * @param servletConfig Servlet configuration information
     * @param blojsomConfiguration blojsom properties
     * @throws javax.servlet.ServletException If the {@link BlojsomFetcher} class could not be loaded and/or initialized
     */
    protected void configureFetcher(ServletConfig servletConfig, BlojsomConfiguration blojsomConfiguration) throws ServletException {
        String fetcherClassName = blojsomConfiguration.getFetcherClass();
        try {
            Class fetcherClass = Class.forName(fetcherClassName);
            _blojsom2Fetcher = (BlojsomFetcher) fetcherClass.newInstance();
            _blojsom2Fetcher.init(servletConfig, blojsomConfiguration);
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
     * Load the blojsom 2 configuration information
     */
    private void loadBlojsom2Configuration() {
        String blojsomPropertiesPath = _blojsom2Path + "/" + BlojsomConstants.DEFAULT_CONFIGURATION_BASE_DIRECTORY + "/blojsom.properties";
        BlojsomProperties _blojsomProperties;
        try {
            _blojsomProperties = new BlojsomProperties();
            _blojsomProperties.load(new FileInputStream(blojsomPropertiesPath));
        } catch (IOException e) {
            if (_logger.isErrorEnabled()) {
                _logger.error(e);
            }

            _blojsomProperties = null;
        }

        if (_blojsomProperties == null) {
            throw new FatalBeanException("Unable to load blojsom properties file: " + blojsomPropertiesPath);
        }

        try {
            _blojsomConfiguration = new BlojsomConfiguration(_servletConfig, org.blojsom.util.BlojsomUtils.propertiesToMap(_blojsomProperties));
        } catch (BlojsomConfigurationException e) {
            if (_logger.isErrorEnabled()) {
                _logger.error(e);
            }
        }

        if (_blojsomConfiguration == null) {
            throw new FatalBeanException("Unable to construct blojsom configuration object");
        }

        try {
            configureFetcher(_servletConfig, _blojsomConfiguration);
        } catch (ServletException e) {
            if (_logger.isErrorEnabled()) {
                _logger.error(e);
            }

            throw new FatalBeanException("Unable to construct blojsom 2 fetcher object", e);
        }
    }

    /**
     * Upgrade the blojsom 2 instance to blojsom 3
     */
    public void upgrade() {
        if (org.blojsom.util.BlojsomUtils.checkNullOrBlank(_blojsom2Path)) {
            throw new InvalidPropertyException(Blojsom2ToBlojsom3Utility.class, "blojsom2Path", "blojsom2Path property was null or blank");
        }

        if (org.blojsom.util.BlojsomUtils.checkNullOrBlank(_blojsom3Path)) {
            throw new InvalidPropertyException(Blojsom2ToBlojsom3Utility.class, "blojsom3Path", "blojsom3Path property was null or blank");
        }

        if (_logger.isDebugEnabled()) {
            _logger.debug("blojsom 2 path: " + _blojsom2Path);
            _logger.debug("blojsom 3 path: " + _blojsom3Path);
        }

        loadBlojsom2Configuration();

        // Migrate each blog
        String[] blojsom2IDs = _blojsomConfiguration.getBlojsomUsers();
        for (int i = 0; i < blojsom2IDs.length; i++) {
            String blojsom2ID = blojsom2IDs[i];
            Blog blog;

            // Try and load the blog in the blojsom 3 installation, otherwise, create a new blog
            try {
                blog = _fetcher.loadBlog(blojsom2ID);

                if (_logger.isDebugEnabled()) {
                    _logger.debug("Updating existing blog: " + blojsom2ID);
                }
            } catch (FetcherException e) {
                if (_logger.isDebugEnabled()) {
                    _logger.debug("Creating new blog: " + blojsom2ID);
                }

                blog = new DatabaseBlog();
                blog.setBlogId(blojsom2ID);
            }

            BlogUser blogUser;
            try {
                blogUser = _blojsomConfiguration.loadBlog(blojsom2ID);
            } catch (BlojsomException e) {
                if (_logger.isErrorEnabled()) {
                    _logger.error("Unable to load blojsom 2 blog ID: " + blojsom2ID);
                    _logger.error(e);
                }

                continue;
            }

            // Migrate the properties
            Map blojsom2BlogProperties = blogUser.getBlog().getBlogProperties();
            Properties blogProperties = org.blojsom2.util.BlojsomUtils.mapToProperties(blojsom2BlogProperties);
            // Remove unused blog properties in blojsom 3
            blogProperties.remove("blog-home");
            blogProperties.remove("blog-directory-depth");
            blogProperties.remove("blog-file-extensions");
            blogProperties.remove("blog-entry-meta-data-extension");
            blogProperties.remove("blog-properties-extensions");
            blogProperties.remove("blog-default-category-mapping");
            blogProperties.remove("blog-comments-directory");
            blogProperties.remove("blog-trackbacks-directory");
            blogProperties.remove("blog-pingbacks-directory");
            blogProperties.remove("blog-xmlrpc-entry-extension");
            blogProperties.remove("blog-directory-filter");
            blogProperties.remove("blog-blacklist-file");
            blog.setProperties(org.blojsom.util.BlojsomUtils.propertiesToMap(blogProperties));
            // Sanitize some properties
            String url = blog.getBlogURL();
            url = org.blojsom.util.BlojsomUtils.removeTrailingSlash(url);
            blog.setBlogURL(url);

            url = blog.getBlogBaseURL();
            url = org.blojsom.util.BlojsomUtils.removeTrailingSlash(url);
            blog.setBlogBaseURL(url);

            blog.setBlogBaseAdminURL(url);

            url = blog.getBlogAdminURL();
            url = org.blojsom.util.BlojsomUtils.removeTrailingSlash(url);
            blog.setBlogAdminURL(url);

            // Migrate the plugin chains
            Map blojsom2PluginChains = blogUser.getPluginChain();
            Map blojsom3PluginChains = new HashMap();
            Iterator pluginIterator = blojsom2PluginChains.keySet().iterator();
            while (pluginIterator.hasNext()) {
                String pluginChainForFlavor = (String) pluginIterator.next();
                String[] flavorForPluginChain = pluginChainForFlavor.split("\\.");
                if (flavorForPluginChain.length == 2) {
                    blojsom3PluginChains.put(flavorForPluginChain[0], blojsom2PluginChains.get(pluginChainForFlavor));
                } else {
                    blojsom3PluginChains.put("default", blojsom2PluginChains.get(pluginChainForFlavor));
                }
            }
            blog.setPlugins(blojsom3PluginChains);

            // Migrate the flavor and template mappings
            Map blojsom2BlogFlavors = blogUser.getFlavors();
            Map blojsom3FlavorToTemplate = new HashMap();
            Iterator flavorIterator = blojsom2BlogFlavors.keySet().iterator();
            while (flavorIterator.hasNext()) {
                String flavor = (String) flavorIterator.next();
                blojsom3FlavorToTemplate.put(flavor, blogUser.getFlavorToTemplate().get(flavor).toString() + ", " + blogUser.getFlavorToContentType().get(flavor).toString());
            }
            blog.setTemplates(blojsom3FlavorToTemplate);

            // Save the blojsom 3 blog
            try {
                _fetcher.saveBlog(blog);
                blog = _fetcher.loadBlog(blog.getBlogId());
            } catch (FetcherException e) {
                if (_logger.isErrorEnabled()) {
                    _logger.error(e);
                }

                continue;
            }

            // Migrate the users and permissions for each user
            String blogPermissionsPath = _blojsom2Path + "/" + BlojsomConstants.DEFAULT_CONFIGURATION_BASE_DIRECTORY + "/" + blogUser.getId() + "/permissions.properties";
            Map blojsom2PermissionsForBlog = new HashMap();
            try {
                InputStream is = new FileInputStream(blogPermissionsPath);
                BlojsomProperties permissions = new BlojsomProperties(true);
                permissions.load(is);
                is.close();

                blojsom2PermissionsForBlog = org.blojsom2.util.BlojsomUtils.propertiesToMap(permissions);

                if (_logger.isDebugEnabled()) {
                    _logger.debug("Loaded permissions for blojsom 2 blog: " + blogUser.getId());
                }
            } catch (IOException e) {
                if (_logger.isErrorEnabled()) {
                    _logger.error(e);
                }
            }

            Map authorizationMap = blogUser.getBlog().getAuthorization();
            Iterator blojsom2UserIterator = authorizationMap.keySet().iterator();
            while (blojsom2UserIterator.hasNext()) {
                String userID = (String) blojsom2UserIterator.next();
                if (_logger.isDebugEnabled()) {
                    _logger.debug("Migrating blojsom 2 user: " + userID);
                }

                User blojsom3User = new DatabaseUser();

                blojsom3User.setBlogId(blog.getId());
                blojsom3User.setUserLogin(userID);
                blojsom3User.setUserName(userID);
                blojsom3User.setUserRegistered(new Date());
                blojsom3User.setUserStatus("new");
                String[] parsedPasswordAndEmail = BlojsomUtils.parseLastComma((String) authorizationMap.get(userID));
                blojsom3User.setUserPassword(parsedPasswordAndEmail[0]);
                if (parsedPasswordAndEmail.length == 2) {
                    blojsom3User.setUserEmail(parsedPasswordAndEmail[1]);
                } else {
                    blojsom3User.setUserEmail(blogUser.getBlog().getBlogOwnerEmail());
                }

                Map blojsom3UserMetadata = new HashMap();

                if (blojsom2PermissionsForBlog.containsKey(userID)) {
                    Object permissionsForUser = blojsom2PermissionsForBlog.get(userID);

                    // Check where user has multiple permissions
                    if (permissionsForUser instanceof List) {
                        List permissions = (List) permissionsForUser;
                        for (int j = 0; j < permissions.size(); j++) {
                            String permission = (String) permissions.get(j);
                            String updatedPermission;

                            if ("*".equals(permission)) {
                                updatedPermission = "all_permissions_permission";
                            } else {
                                updatedPermission = permission.replaceAll("-", "_") + "_permission";
                            }

                            blojsom3UserMetadata.put(updatedPermission, "true");
                        }
                    // Check where user has only a single permission
                    } else {
                        if ("*".equals(permissionsForUser)) {
                            blojsom3UserMetadata.put("all_permissions_permission", "true");
                        } else {
                            blojsom3UserMetadata.put(permissionsForUser.toString().replaceAll("-", "_") + "_permission", "true");
                        }
                    }
                }

                blojsom3User.setMetaData(blojsom3UserMetadata);

                try {
                    _fetcher.saveUser(blog, blojsom3User);
                } catch (FetcherException e) {
                    if (_logger.isErrorEnabled()) {
                        _logger.error(e);
                    }
                }
            }

            // Migrate the categories
            Map blojsom2CategoriesMap = new HashMap();
            Map blojsom3CategoriesMap = new HashMap();
            try {
                BlogCategory[] blojsom2Categories = _blojsom2Fetcher.fetchCategories(null, blogUser);
                for (int j = 0; j < blojsom2Categories.length; j++) {
                    BlogCategory blojsom2Category = blojsom2Categories[j];
                    Category blojsom3Category = new DatabaseCategory();
                    blojsom3Category.setBlogId(blog.getId());
                    if (org.blojsom.util.BlojsomUtils.checkNullOrBlank(blojsom2Category.getDescription())) {
                        blojsom3Category.setDescription(blojsom2Category.getEncodedCategory().replaceAll("/", " "));
                    } else {
                        blojsom3Category.setDescription(blojsom2Category.getDescription());
                    }
                    blojsom3Category.setName(blojsom2Category.getEncodedCategory());
                    blojsom3Category.setParentCategoryId(null);
                    blojsom3Category.setMetaData(blojsom2Category.getMetaData());

                    try {
                        _fetcher.saveCategory(blog, blojsom3Category);
                        if (_logger.isDebugEnabled()) {
                            _logger.debug("Created blojsom 3 category: " + blojsom3Category.getName());
                        }

                        blojsom3Category = _fetcher.loadCategory(blog, blojsom3Category.getName());
                        blojsom2CategoriesMap.put(blojsom2Category.getEncodedCategory(), blojsom2Category);
                        blojsom3CategoriesMap.put(blojsom2Category.getEncodedCategory(), blojsom3Category);
                    } catch (FetcherException e) {
                        if (_logger.isErrorEnabled()) {
                            _logger.error(e);
                        }
                    }
                }
            } catch (BlojsomFetcherException e) {
                if (_logger.isErrorEnabled()) {
                    _logger.error(e);
                }

                continue;
            }

            // Migrate the entries
            Iterator blojsom2CategoriesIterator = blojsom2CategoriesMap.keySet().iterator();
            while (blojsom2CategoriesIterator.hasNext()) {
                String categoryName = (String) blojsom2CategoriesIterator.next();
                BlogCategory blogCategory = (BlogCategory) blojsom2CategoriesMap.get(categoryName);

                Map fetchParameters = new HashMap();
                fetchParameters.put(BlojsomFetcher.FETCHER_CATEGORY, blogCategory);
                fetchParameters.put(BlojsomFetcher.FETCHER_NUM_POSTS_INTEGER, new Integer(-1));
                try {
                    BlogEntry[] entries = _blojsom2Fetcher.fetchEntries(fetchParameters, blogUser);

                    if (_logger.isDebugEnabled()) {
                        _logger.debug("Migrating " + entries.length + " entries from blojsom 2 category: " + blogCategory.getEncodedCategory());
                    }

                    for (int j = 0; j < entries.length; j++) {
                        BlogEntry entry = entries[j];
                        Entry blojsom3Entry = new DatabaseEntry();
                        blojsom3Entry.setBlogId(blog.getId());

                        Category category = (Category) blojsom3CategoriesMap.get(blogCategory.getEncodedCategory());
                        blojsom3Entry.setBlogCategoryId(category.getId());
                        if (entry.getMetaData().get("blog-entry-author") != null) {
                            blojsom3Entry.setAuthor(entry.getMetaData().get("blog-entry-author").toString());
                        }

                        if (entry.supportsComments()) {
                            blojsom3Entry.setAllowComments(new Integer(1));
                        } else {
                            blojsom3Entry.setAllowComments(new Integer(0));
                        }

                        blojsom3Entry.setDate(entry.getDate());
                        blojsom3Entry.setDescription(entry.getDescription());
                        blojsom3Entry.setMetaData(entry.getMetaData());
                        blojsom3Entry.setModifiedDate(entry.getDate());

                        if (entry.supportsPingbacks()) {
                            blojsom3Entry.setAllowPingbacks(new Integer(1));
                        } else {
                            blojsom3Entry.setAllowPingbacks(new Integer(0));
                        }

                        blojsom3Entry.setPostSlug(entry.getPermalink());
                        blojsom3Entry.setStatus(BlojsomMetaDataConstants.PUBLISHED_STATUS);
                        blojsom3Entry.setTitle(entry.getTitle());

                        if (entry.supportsTrackbacks()) {
                            blojsom3Entry.setAllowTrackbacks(new Integer(1));
                        } else {
                            blojsom3Entry.setAllowTrackbacks(new Integer(0));
                        }

                        try {
                            _fetcher.saveEntry(blog, blojsom3Entry);
                        } catch (FetcherException e) {
                            if (_logger.isErrorEnabled()) {
                                _logger.error(e);
                            }
                        }

                        // Migrate the comments
                        BlogComment[] comments = entry.getCommentsAsArray();
                        for (int k = 0; k < comments.length; k++) {
                            BlogComment comment = comments[k];
                            Comment blojsom3Comment = new DatabaseComment();

                            blojsom3Comment.setAuthor(comment.getAuthor());
                            blojsom3Comment.setAuthorEmail(comment.getAuthorEmail());
                            blojsom3Comment.setAuthorURL(comment.getAuthorURL());
                            blojsom3Comment.setBlogEntryId(blojsom3Entry.getId());
                            blojsom3Comment.setBlogId(blog.getId());
                            blojsom3Comment.setComment(comment.getComment());
                            blojsom3Comment.setCommentDate(comment.getCommentDate());
                            blojsom3Comment.setIp((String) comment.getMetaData().get("BLOJSOM_COMMENT_PLUGIN_METADATA_IP"));
                            blojsom3Comment.setMetaData(comment.getMetaData());
                            blojsom3Comment.setStatus(ResponseConstants.APPROVED_STATUS);

                            try {
                                _fetcher.saveComment(blog, blojsom3Comment);
                            } catch (FetcherException e) {
                                if (_logger.isErrorEnabled()) {
                                    _logger.error(e);
                                }
                            }
                        }

                        // Migrate the trackbacks
                        Trackback[] trackbacks = entry.getTrackbacksAsArray();
                        for (int k = 0; k < trackbacks.length; k++) {
                            Trackback trackback = trackbacks[k];
                            DatabaseTrackback blojsom3Trackback = new DatabaseTrackback();

                            blojsom3Trackback.setBlogName(trackback.getBlogName());
                            blojsom3Trackback.setExcerpt(trackback.getExcerpt());
                            blojsom3Trackback.setTitle(trackback.getTitle());
                            blojsom3Trackback.setUrl(trackback.getUrl());
                            blojsom3Trackback.setTrackbackDate(trackback.getTrackbackDate());
                            blojsom3Trackback.setBlogEntryId(blojsom3Entry.getId());
                            blojsom3Trackback.setBlogId(blog.getId());
                            blojsom3Trackback.setIp((String) trackback.getMetaData().get("BLOJSOM_TRACKBACK_PLUGIN_METADATA_IP"));
                            blojsom3Trackback.setMetaData(trackback.getMetaData());
                            blojsom3Trackback.setStatus(ResponseConstants.APPROVED_STATUS);

                            try {
                                _fetcher.saveTrackback(blog, blojsom3Trackback);
                            } catch (FetcherException e) {
                                if (_logger.isErrorEnabled()) {
                                    _logger.error(e);
                                }
                            }
                        }

                        // Migrate the pingbacks
                        Pingback[] pingbacks = entry.getPingbacksAsArray();
                        for (int k = 0; k < pingbacks.length; k++) {
                            Pingback pingback = pingbacks[k];
                            DatabasePingback blojsom3Pingback = new DatabasePingback();

                            blojsom3Pingback.setBlogName(pingback.getBlogName());
                            blojsom3Pingback.setExcerpt(pingback.getExcerpt());
                            blojsom3Pingback.setTitle(pingback.getTitle());
                            blojsom3Pingback.setUrl(pingback.getUrl());
                            blojsom3Pingback.setTrackbackDate(pingback.getTrackbackDate());
                            blojsom3Pingback.setBlogEntryId(blojsom3Entry.getId());
                            blojsom3Pingback.setBlogId(blog.getId());
                            blojsom3Pingback.setIp((String) pingback.getMetaData().get("BLOJSOM_PINGBACK_PLUGIN_METADATA_IP"));
                            blojsom3Pingback.setMetaData(pingback.getMetaData());
                            blojsom3Pingback.setStatus(ResponseConstants.APPROVED_STATUS);
                            blojsom3Pingback.setSourceURI(pingback.getUrl());
                            blojsom3Pingback.setTargetURI(pingback.getTitle());

                            try {
                                _fetcher.savePingback(blog, blojsom3Pingback);
                            } catch (FetcherException e) {
                                if (_logger.isErrorEnabled()) {
                                    _logger.error(e);
                                }
                            }
                        }
                    }
                } catch (BlojsomFetcherException e) {
                    if (_logger.isErrorEnabled()) {
                        _logger.error(e);
                    }
                }
            }

            // Migrate the resources and templates
            File blojsom2BlogResourcesPath = new File(_blojsom2Path + "/resources/" + blojsom2ID + "/");
            File blojsom3BlogResourcesPath = new File(_blojsom3Path + "/resources/" + blojsom2ID + "/");
            try {
                FileUtils.copyDirectory(blojsom2BlogResourcesPath, blojsom3BlogResourcesPath);
                if (_logger.isDebugEnabled()) {
                    _logger.debug("Copied blojsom 2 blog resources from: " + blojsom2BlogResourcesPath.toString() + " to: " +
                        blojsom3BlogResourcesPath.toString());
                }
            } catch (IOException e) {
                if (_logger.isErrorEnabled()) {
                    _logger.error(e);
                }
            }

            File blojsom2BlogPath = new File(_blojsom2Path + BlojsomConstants.DEFAULT_CONFIGURATION_BASE_DIRECTORY + "/" + blojsom2ID + "/");
            File blojsom3BlogPath = new File(_blojsom3Path + BlojsomConstants.DEFAULT_CONFIGURATION_BASE_DIRECTORY + "/blogs/" + blojsom2ID + "/");
            try {
                FileUtils.copyDirectory(blojsom2BlogPath, blojsom3BlogPath);
                if (_logger.isDebugEnabled()) {
                    _logger.debug("Copied blojsom 2 blog data from: " + blojsom2BlogPath.toString() + " to: " +
                        blojsom3BlogPath.toString());
                }
            } catch (IOException e) {
                if (_logger.isErrorEnabled()) {
                    _logger.error(e);
                }
            }
        }

        if (_logger.isDebugEnabled()) {
            _logger.debug("Finished upgrading blojsom 2 instance to blojsom 3!");
        }
    }
}
