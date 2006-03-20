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
package org.blojsom.blog.database;

import org.blojsom.blog.*;
import org.blojsom.util.BlojsomUtils;

import java.io.Serializable;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * DatabaseEntry
 *
 * @author David Czarnecki
 * @since blojsom 3.0
 * @version $Id: DatabaseEntry.java,v 1.1 2006-03-20 21:31:13 czarneckid Exp $
 */
public class DatabaseEntry implements Entry, Serializable {

    protected Integer _id;
    protected Integer _blogCategoryId;
    protected String _blogId;

    protected String _title;
    protected String _link;
    protected String _description;
    protected Date _entryDate;
    protected Date _modifiedDate;
    protected List _comments;
    protected List _trackbacks;
    protected List _pingbacks;
    protected Category _category;
    protected Map _metaData;
    protected Integer _allowComments;
    protected Integer _allowTrackbacks;
    protected Integer _allowPingbacks;
    protected String _status;
    protected String _author;
    protected String _postSlug;

    /**
     *
     */
    public DatabaseEntry() {
    }

    /**
     *
     * @param id
     */
    public void setId(Integer id) {
        _id = id;
    }

    /**
     *
     * @return
     */
    public Integer getId() {
        return _id;
    }

    /**
     *
     * @return
     */
    public Integer getBlogCategoryId() {
        return _blogCategoryId;
    }

    /**
     *
     * @param blogCategoryId
     */
    public void setBlogCategoryId(Integer blogCategoryId) {
        _blogCategoryId = blogCategoryId;
    }

    /**
     *
     * @return
     */
    public String getBlogId() {
        return _blogId;
    }

    /**
     *
     * @param blogId
     */
    public void setBlogId(String blogId) {
        _blogId = blogId;
    }

    /**
     * Date of the blog entry
     * <p/>
     * This value is constructed from the lastModified value of the file
     *
     * @return Date of the blog entry
     */
    public Date getDate() {
        return _entryDate;
    }

    /**
     * Date of this blog entry
     *
     * @param entryDate Date of the blog entry
     */
    public void setDate(Date entryDate) {
        _entryDate = entryDate;
    }

    /**
     *
     * @return
     */
    public Date getModifiedDate() {
        return _modifiedDate;
    }

    /**
     *
     * @param modifiedDate
     */
    public void setModifiedDate(Date modifiedDate) {
        _modifiedDate = modifiedDate;
    }

    /**
     * Return an RFC 822 style date
     *
     * @return Date formatted in RFC 822 format
     */
    public String getRFC822Date() {
        return BlojsomUtils.getRFC822Date(_entryDate);
    }

    /**
     * Return an UTC style date
     *
     * @return Date formatted in UTC format
     */
    public String getUTCDate() {
        return BlojsomUtils.getUTCDate(_entryDate);
    }

    /**
     * Return an ISO 8601 style date
     * http://www.w3.org/TR/NOTE-datetime
     *
     * @return Date formatted in ISO 8601 format
     */
    public String getISO8601Date() {
        return BlojsomUtils.getISO8601Date(_entryDate);
    }

    /**
     * Return the blog entry date formatted with a specified date format
     *
     * @param format Date format
     * @return <code>null</code> if the entry date or format is null, otherwise returns the entry date formatted to the specified format. If the format is invalid, returns <tt>entryDate.toString()</tt>
     */
    public String getDateAsFormat(String format) {
        return getDateAsFormat(format, null);
    }

    /**
     * Return the blog entry date formatted with a specified date format
     *
     * @param format Date format
     * @param locale Locale for date formatting
     * @return <code>null</code> if the entry date or format is null, otherwise returns the entry date formatted to the specified format. If the format is invalid, returns <tt>entryDate.toString()</tt>
     */
    public String getDateAsFormat(String format, Locale locale) {
        if (_entryDate == null || format == null) {
            return null;
        }

        SimpleDateFormat sdf;
        try {
            if (locale == null) {
                sdf = new SimpleDateFormat(format);
            } else {
                sdf = new SimpleDateFormat(format, locale);
            }

            return sdf.format(_entryDate);
        } catch (IllegalArgumentException e) {
            return _entryDate.toString();
        }
    }

    /**
     * Title of the blog entry
     *
     * @return Blog title
     */
    public String getTitle() {
        return _title;
    }

    /**
     * Set the title of the blog entry
     *
     * @param title Title for the blog entry
     */
    public void setTitle(String title) {
        _title = title;
    }

    /**
     * Title for the entry where the &lt;, &gt;, and &amp; characters are escaped
     *
     * @return Escaped entry title
     */
    public String getEscapedTitle() {
        return BlojsomUtils.escapeString(_title);
    }

    /**
     * Description of the blog entry
     *
     * @return Blog entry description
     */
    public String getDescription() {
        return _description;
    }

    /**
     * Escaped description of the blog entry
     * This method would be used for generating RSS feeds where the &lt;, &gt;, and &amp; characters are escaped
     *
     * @return Blog entry description where &amp;, &lt;, and &gt; have been escaped
     */
    public String getEscapedDescription() {
        return BlojsomUtils.escapeString(_description);
    }

    /**
     * Set the description for the blog entry
     *
     * @param description Description for the blog entry
     */
    public void setDescription(String description) {
        _description = description;
    }

    /**
     * Category for the blog entry. This corresponds to the category directory name.
     *
     * @return Blog entry category
     */
    public String getCategory() {
        return _category.getName();
    }

    /**
     * Return the category name encoded.
     *
     * @return Category name encoded as UTF-8
     */
    public String getEncodedCategory() {
        return _category.getEncodedName();
    }

    /**
     * Determines whether or not this blog entry supports comments.
     *
     * @return <code>true</code> if the blog entry supports comments, <code>false</code> otherwise
     */
    public Integer getAllowComments() {
        return _allowComments;
    }

    /**
     * Get the comments
     *
     * @return List of comments
     */
    public List getComments() {
        if (_comments == null) {
            return new ArrayList();
        }

        return _comments;
    }

    /**
     * Set the comments for this blog entry. The comments must be an <code>List</code>
     * of {@link org.blojsom.blog.Comment}. This method will not writeback or change the comments on disk.
     *
     * @param comments Comments for this entry
     */
    public void setComments(List comments) {
        _comments = comments;
    }

    /**
     * Get the comments as an array of {@link org.blojsom.blog.Comment} objects
     *
     * @return BlogComment[] array
     */
    public Comment[] getCommentsAsArray() {
        if (_comments == null) {
            return new Comment[0];
        } else {
            return (Comment[]) _comments.toArray(new Comment[_comments.size()]);
        }
    }

    /**
     * Get the number of comments for this entry
     *
     * @return 0 if comments is <code>null</code>, or the number of comments otherwise, which could be 0
     */
    public int getNumComments() {
        if (_comments == null) {
            return 0;
        } else {
            return _comments.size();
        }
    }

    /**
     * Determines whether or not this blog entry supports trackbacks.
     *
     * @return <code>true</code> if the blog entry supports trackbacks, <code>false</code> otherwise
     */
    public Integer getAllowTrackbacks() {
        return _allowTrackbacks;
    }

    /**
     * Get the trackbacks
     *
     * @return List of trackbacks
     */
    public List getTrackbacks() {
        if (_trackbacks == null) {
            return new ArrayList();
        }

        return _trackbacks;
    }

    /**
     * Set the trackbacks for this blog entry. The trackbacks must be an <code>List</code>
     * of {@link org.blojsom.blog.Trackback}. This method will not writeback or change the trackbacks to disk.
     *
     * @param trackbacks Trackbacks for this entry
     */
    public void setTrackbacks(List trackbacks) {
        _trackbacks = trackbacks;
    }

    /**
     * Get the trackbacks as an array of Trackback objects
     *
     * @return Trackback[] array
     */
    public Trackback[] getTrackbacksAsArray() {
        if (_trackbacks == null) {
            return new Trackback[0];
        } else {
            return (Trackback[]) _trackbacks.toArray(new Trackback[_trackbacks.size()]);
        }
    }

    /**
     * Get the number of trackbacks for this entry
     *
     * @return 0 if trackbacks is <code>null</code>, or the number of trackbacks otherwise, which could be 0
     */
    public int getNumTrackbacks() {
        if (_trackbacks == null) {
            return 0;
        } else {
            return _trackbacks.size();
        }
    }

    /**
     * Get the {@link org.blojsom.blog.Category} object for this blog entry
     *
     * @return {@link org.blojsom.blog.Category} object
     */
    public Category getBlogCategory() {
        return _category;
    }

    /**
     * Set the {@link org.blojsom.blog.Category} object for this blog entry
     *
     * @param blogCategory New {@link org.blojsom.blog.Category} object
     */
    public void setBlogCategory(Category blogCategory) {
        _category = blogCategory;
    }

    /**
     * Return meta data for this blog entry. This method may return <code>null</code>.
     *
     * @return Meta data
     */
    public Map getMetaData() {
        if (_metaData == null) {
            return new HashMap();
        }

        return _metaData;
    }

    /**
     * Set the meta-data associated with this blog entry
     *
     * @param metaData Meta-data
     */
    public void setMetaData(Map metaData) {
        _metaData = metaData;
    }

    /**
     * Determines whether or not this blog entry supports pingbacks.
     *
     * @return <code>true</code> if the blog entry supports pingbacks, <code>false</code> otherwise
     */
    public Integer getAllowPingbacks() {
        return _allowPingbacks;
    }

    /**
     * Get the pingbacks for this entry
     *
     * @return List of {@link org.blojsom.blog.Pingback}
     */
    public List getPingbacks() {
        if (_pingbacks == null) {
            return new ArrayList();
        }

        return _pingbacks;
    }

    /**
     * Set the pingbacks for this blog entry. The pingbacks must be a <code>List</code>
     * of {@link org.blojsom.blog.Pingback}. This method will not writeback or change the pingbacks to disk.
     *
     * @param pingbacks {@link org.blojsom.blog.Pingback}s for this entry
     */
    public void setPingbacks(List pingbacks) {
        _pingbacks = pingbacks;
    }

    /**
     * Get the pingbacks as an array of {@link org.blojsom.blog.Pingback}s objects
     *
     * @return {@link org.blojsom.blog.Pingback}[] array
     */
    public Pingback[] getPingbacksAsArray() {
        if (_pingbacks == null) {
            return new Pingback[0];
        } else {
            return (Pingback[]) _pingbacks.toArray(new Pingback[_pingbacks.size()]);
        }
    }

    /**
     * Get the number of pingbacks for this entry
     *
     * @return 0 if pingbacks is <code>null</code>, or the number of pingbacks otherwise, which could be 0
     */
    public int getNumPingbacks() {
        if (_pingbacks == null) {
            return 0;
        } else {
            return _pingbacks.size();
        }
    }

    /**
     *
     * @param allowComments
     */
    public void setAllowComments(Integer allowComments) {
        _allowComments = allowComments;
    }

    /**
     *
     * @param allowTrackbacks
     */
    public void setAllowTrackbacks(Integer allowTrackbacks) {
        _allowTrackbacks = allowTrackbacks;
    }

    /**
     *
     * @param allowPingbacks
     */
    public void setAllowPingbacks(Integer allowPingbacks) {
        _allowPingbacks = allowPingbacks;
    }

    /**
     *
     * @return
     */
    public String getStatus() {
        return _status;
    }

    /**
     *
     * @param status
     */
    public void setStatus(String status) {
        _status = status;
    }

    /**
     *
     * @return
     */
    public String getAuthor() {
        return _author;
    }

    /**
     *
     * @param author
     */
    public void setAuthor(String author) {
        _author = author;
    }

    /**
     *
     * @return
     */
    public Boolean allowsComments() {
        if (_allowComments == null) {
            return Boolean.TRUE;
        }

        return Boolean.valueOf((_allowComments.intValue() == 1));
    }

    /**
     *
     * @return
     */
    public Boolean allowsTrackbacks() {
        if (_allowTrackbacks == null) {
            return Boolean.TRUE;
        }

        return Boolean.valueOf((_allowTrackbacks.intValue() == 1));
    }

    /**
     *
     * @return
     */
    public Boolean allowsPingbacks() {
        if (_allowPingbacks == null) {
            return Boolean.TRUE;
        }

        return Boolean.valueOf((_allowPingbacks.intValue() == 1));
    }

    /**
     *
     * @return
     */
    public String getPostSlug() {
        return _postSlug;
    }

    /**
     *
     * @param postSlug
     */
    public void setPostSlug(String postSlug) {
        _postSlug = postSlug;
    }
}