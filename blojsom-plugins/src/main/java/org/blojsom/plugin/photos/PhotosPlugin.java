/**
 * Copyright (c) 2005-2006, Timothy Stone
 * All rights reserved.
 *
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of the "Timothy Stone" nor the names of
 * its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
package org.blojsom.plugin.photos;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.blojsom.blog.Blog;
import org.blojsom.blog.Entry;
import org.blojsom.util.BlojsomUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletConfig;
import org.blojsom.plugin.Plugin;
import org.blojsom.plugin.PluginException;

/**
 * Photos Plugin
 * A simple photo gallery plugin
 *
 * @author Timothy Stone
 * @version 1.0
 */
public class PhotosPlugin implements Plugin {

    private Log _logger = LogFactory.getLog(PhotosPlugin.class);
    private ServletConfig _servletConfig;
    private Properties _blojsomProperties;

    /**
     * Default constructor.
     */
    public PhotosPlugin() {
    }

    /**
     * Initialize this plugin. This method only called when the plugin is instantiated.
     *
     * @throws PluginException If there is an error initializing the plugin
     */
    public void init() throws PluginException {
    }

    /**
     * Process the blog entries
     *
     * @param request  Request
     * @param response Response
     * @param blog                {@link Blog} instance
     * @param context             Context
     * @param entries             Blog entries retrieved for the particular request
     * @return Modified set of blog entries
     * @throws BlojsomPluginException If there is an error processing the blog entries
     */
    public Entry[] process(HttpServletRequest request,
                           HttpServletResponse response,
                           Blog blog,
                           Map context,
                           Entry[] entries) throws PluginException {

        ArrayList photos = null;
        String photoAlbum = "";
        String photoAlbumList = "";
        String photoAlbumThumbnail = "";
        String blogResourcePath = getBlogResourcePath(blog);

        for (int i = 0; i < entries.length; i++) {
            Entry entry = entries[i];
            photos = new ArrayList();
            photoAlbum = getPhotoAlbum(entry, photoAlbum);
            photoAlbumList = getPhotoAlbumList(entry, photoAlbumList);
            photoAlbumThumbnail = getPhotoAlbumThumbnail(entry, photoAlbumThumbnail);
            String photoAlbumRealPath = getPhotoAlbumRealPath(blogResourcePath, photoAlbum);

            if( !BlojsomUtils.checkNullOrBlank(photoAlbumRealPath) ) {
                try {

                    File file = new File( photoAlbumRealPath );

                    if( file.isDirectory() ) {

                        if( BlojsomUtils.checkNullOrBlank(photoAlbumList) ) {
                            String[] filesInList = BlojsomUtils.parseCommaList((String)entry.getMetaData().get("photo-album-list"));
                            for( int f = 0; f < filesInList.length; f++ ) {
                                if( (filesInList[f].toLowerCase().indexOf("thumb")) > 0 ) {
                                    continue;
                                }
                                photos.add(blogResourcePath + BlojsomUtils.addTrailingSlash(photoAlbum) + filesInList[f]);
                            }

                        } else {

                            File[] files = file.listFiles( BlojsomUtils.getExtensionsFilter( new String[] {".jpg",".gif",".png"} ) );
                            for( int f = 0; f < files.length; f++ ) {
                                if( (files[f].getName()).toLowerCase().indexOf("thumb") > 0 ) {
                                    continue;
                                }
                                photos.add(blogResourcePath + BlojsomUtils.addTrailingSlash(photoAlbum) + files[f].getName());
                            }
                        }
                        photoAlbumThumbnail = blogResourcePath + BlojsomUtils.addTrailingSlash(photoAlbum) + photoAlbumThumbnail;
                   }


                    if( file.isFile() ) {
                        photos.add( blogResourcePath + photoAlbum );
                        photoAlbumThumbnail = blogResourcePath + photoAlbumThumbnail;
                    }

                    if( photoAlbumThumbnail != null ) {
                        entry.getMetaData().put( "photo-album-thumbnail", photoAlbumThumbnail );
                        _logger.debug( "photoAlbumThumbnail is: " + photoAlbumThumbnail );
                    }

                    entry.getMetaData().put( "photo-album", photos );

                    _logger.debug( "Photo album loaded with " +
                            photos.size() + ((photos.size() == 1)? " photo.":" photos.") );

                } catch( NullPointerException npe ) {
                    _logger.error( "Error while trying to retrieve photo album");
                }

            } else {
                _logger.debug( "No photo album defined. Moving on...");
            }
        }

        return entries;
    }

    private String getBlogResourcePath(Blog blog) {
        String blogResourcePath = _blojsomProperties.getProperty("resources-directory") + blog.getBlogId();
        blogResourcePath = BlojsomUtils.addTrailingSlash(blogResourcePath);
        _logger.debug("blogResourcePath is: " + blogResourcePath);
        return blogResourcePath;
    }

    private String getPhotoAlbumRealPath(String blogResourcePath, String photoAlbum) {
        String photoAlbumRealPath = _servletConfig.getServletContext().getRealPath(blogResourcePath + photoAlbum);
        _logger.debug("photoAlbumRealPath is: " + photoAlbumRealPath);
        return photoAlbumRealPath;
    }

    private String getPhotoAlbumThumbnail(Entry entry, String photoAlbumThumbnail) {
        if (BlojsomUtils.checkMapForKey(entry.getMetaData(), "photo-album-thumbnail")) {
            photoAlbumThumbnail = ((String) entry.getMetaData().get("photo-album-thumbnail")).trim();
        } else {
            photoAlbumThumbnail = "";
        }
        _logger.debug("photoAlbumThumbnail is: " + photoAlbumThumbnail);
        return photoAlbumThumbnail;
    }

    private String getPhotoAlbumList(Entry entry, String photoAlbumList) {
        if (BlojsomUtils.checkMapForKey(entry.getMetaData(), "photo-album-list")) {
            photoAlbumList = ((String) entry.getMetaData().get("photo-album-list")).trim();
        } else {
            photoAlbumList = "";
        }
        _logger.debug("photoAlbumList is: " + photoAlbumList);
        return photoAlbumList;
    }

    private String getPhotoAlbum(Entry entry, String photoAlbum) {
        if (BlojsomUtils.checkMapForKey(entry.getMetaData(), "photo-album")) {
            photoAlbum = ((String) entry.getMetaData().get("photo-album")).trim();
        } else {
            photoAlbum = "";
        }
        _logger.debug("photoAlbum is: " + photoAlbum);
        return photoAlbum;
    }

    /**
     * Perform any cleanup for the plugin. Called after {@link #process}.
     *
     * @throws PluginException If there is an error performing cleanup for this plugin
     */
    public void cleanup() throws PluginException {
    }

    /**
     * Called when BlojsomServlet is taken out of service
     *
     * @throws PluginException If there is an error in finalizing this plugin
     */
    public void destroy() throws PluginException {
    }

    /**
     * @param servletContext the _servletContext to set
     */
    public void setServletConfig(ServletConfig servletConfig) {
        this._servletConfig = servletConfig;
    }

    /**
     * @param blojsomProperties the _blojsomProperties to set
     */
    public void setBlojsomProperties(Properties blojsomProperties) {
        this._blojsomProperties = blojsomProperties;
    }

}


