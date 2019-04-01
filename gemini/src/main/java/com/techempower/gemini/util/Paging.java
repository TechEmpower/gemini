/*******************************************************************************
 * Copyright (c) 2018, TechEmpower, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name TechEmpower, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TECHEMPOWER, INC. BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.techempower.gemini.util;

import java.util.*;

import com.techempower.gemini.*;
import com.techempower.helper.*;

/**
 * Paging controls to be rendered as HTML.  To a user on page 10 of 20, 
 * they might look like this:
 * 
 * <pre><u>Previous</u> <u>1</u> <u>2</u> ... <u>8</u> <u>9</u> <b>10</b> <u>11</u> <u>12</u> ... <u>19</u> <u>20</u> <u>Next</u></pre>
 * 
 * <p>This class is intended to be a replacement for 
 * <tt>com.techempower.gemini.PagedList</tt>.</p>
 * 
 * <p>There are basic methods available to render the controls as 
 * <tt>&lt;span&gt;</tt>, a <tt>&lt;p&gt;</tt>, a <tt>&lt;ul&gt;</tt>, 
 * or a <tt>&lt;table&gt;</tt> element.  These methods are intended to be 
 * satisfactory for the majority of applications:</p>
 * 
 * <ul>
 * <li><tt>renderAsSpan(Context, String)</tt></li>
 * <li><tt>renderAsParagraph(Context, String)</tt></li>
 * <li><tt>renderAsList(Context, String)</tt></li>
 * <li><tt>renderAsTable(Context, String)</tt></li>
 * </ul>
 * 
 * <p>In those methods, the second parameter is a CSS class name to
 * be applied to the container element.</p>
 * 
 * <p>The following is a code sample from a handler in an imaginary 
 * application that makes use of the <tt>Paging</tt> class.</p>
 * 
 * <pre>
 * // this method handles the "search" command
 * protected boolean handleSearch(Context context, String command)
 * {
 *   String queryString = context.getRequestValue("q");
 *   int itemsPerPage = context.getIntRequestValue("r");
 *   int currentPage = context.getIntRequestValue("p");
 * 
 *   //
 *   // We have some imaginary "SearchResultList" class which is basically 
 *   // a list of results for the current page, except it also knows how many search
 *   // results there are in total (a "totalHits" method).
 *   //
 *   SearchResultList results = theSearcher.search(queryString, currentPage, itemsPerPage);
 * 
 *   //
 *   // This is the form of the URL that each page link would have, minus
 *   // the parameter that tells it what page to go to.
 *   //
 *   String baseURL = context.getCmdURL(
 *      "search", 
 *      new String[] { "q", "r" },
 *      new String[] { queryString, Integer.toString(resultsPerPage) });
 *      
 *   //
 *   // We give it the name of the request parameter for the current page,
 *   // "p", along with the base URL so that it can build links to other 
 *   // pages.
 *   //
 *   Paging paging = new Paging(baseURL, "p", results.totalHits(), currentPage, itemsPerPage);
 *   
 *   context.putDelivery("paging", paging);
 *   context.putDelivery("results", results);
 *   return context.includeJSP("search.jsp");
 * }
 * </pre>
 * 
 * <p>Then, on search.jsp, there might be something like this:</p>
 * 
 * <pre>
 * &lt;% Paging paging = (Paging)context.getDelivery("paging"); %&gt;
 * &lt;% SearchResultList results = (SearchResultList)context.getDelivery("results"); %&gt;
 * 
 * ...
 * 
 * &lt;% for (SearchResult r : results) { %&gt;
 * &lt;%= r.render(context) %&gt;
 * &lt;% } %&gt;
 * 
 * &lt;%= paging.renderAsParagraph(context) %&gt;
 * </pre>
 * 
 * <p>There are two basic ways to create an instance of this class.
 * One is to give it a base URL and a parameter name in the constructor.
 * In that case, URLs for the page links will be constructed in this
 * fashion:</p>
 * 
 * <pre>baseURL + "&amp;" + paramName + "=" + currentPage</pre>
 * 
 * <p>If that kind of URL construction is not valid for your application
 * (as may be the case if you're doing URL rewriting), you will need to 
 * create a class that generates the URLs by implementing the 
 * <tt>Paging.URLGenerator</tt> interface defined here.  Then you would pass 
 * an instance of that class into the constructor rather than a base URL and
 * parameter name.</p>
 * 
 * <p>Instances of this class may be used to filter a collection of items, 
 * removing all items that would not appear on the current page.  This 
 * approach is intended to be used when you're paging objects that live in
 * memory.  Note that if you're using Lucene, you should probably not have 
 * to use this method.  If you do, it probably means that you're iterating 
 * over the entire collection of hits.  Instead, you should only be looking 
 * at the ones that would appear on the current page.</p>
 * 
 * <p>You may also use this class to render individual links to pages by 
 * using the <tt>renderPage(Context, int)</tt> method.  See the other 
 * <tt>renderPage</tt> methods for more advanced options.</p>
 */
public class Paging
{
  /**
   * Generates URLs for use by a paging control.  The URLs it generates
   * should <i>not</i> be escaped for HTML.
   */
  public interface URLGenerator
  {
    /**
     * Returns a URL for the specified page in the given context. 
     * This URL should <i>not</i> be escaped for HTML.
     * 
     * @param context the current context
     * @param page the page for which a URL will be returned
     * @return a URL to the specified page in the given context
     */
    String getURL(Context context, int page);
  }
  
  /**
   * The default number of items per page.
   */
  public static final int DEFAULT_ITEMS_PER_PAGE = 10;
  
  /**
   * The default number of page links to show before the current
   * page in the middle group of page links.
   */
  public static final int DEFAULT_PAGES_BEFORE_CURRENT = 2;

  /**
   * The default number of page links to show after the current
   * page in the middle group of page links.
   */
  public static final int DEFAULT_PAGES_AFTER_CURRENT = 2;
  
  /**
   * The default number of pages to be shown at the "beginning",
   * that is, before the pages surrounding the current page.  This
   * will include the first page and may be succeeded by an ellipsis.  
   */
  public static final int DEFAULT_PAGES_AT_BEGINNING = 2;

  /**
   * The default number of pages to be shown at the "end",
   * that is, after the pages surrounding the current page.  This
   * will include the last page and may be preceded by an ellipsis.  
   */
  public static final int DEFAULT_PAGES_AT_END = 2;

  /**
   * The default HTML to use for the label of the link to the previous page.
   */
  public static final String DEFAULT_PREVIOUS_PAGE_HTML = "Previous";
  
  /**
   * The default HTML to use for the label of the link to the next page.
   */
  public static final String DEFAULT_NEXT_PAGE_HTML = "Next";
  
  /**
   * The default HTML to use for the ellipsis in paging controls with block-
   * level page items.  This ellipsis separates the pages surrounding the 
   * current page from the beginning and end groups.
   */
  public static final String DEFAULT_BLOCK_ELLIPSIS_HTML = "...";
  
  /**
   * The default HTML to use for the ellipsis in paging controls with inline-
   * level page items.  This ellipsis separates the pages surrounding the 
   * current page from the beginning and end groups.
   */
  public static final String DEFAULT_INLINE_ELLIPSIS_HTML = " ... ";
  
  /**
   * An array that contains all the tokens available for formatting a
   * page item.  
   * 
   * <pre>
   * 0: "$N" (the page number)
   * 1: "$L" (the HTML label of the page)
   * 2: "$U" (the URL of the page)
   * </pre>
   */
  private static final String[] TOKENS = { "$N", "$L", "$U" };
  
  /**
   * The default format used for all non-current pages.
   */
  public static final String DEFAULT_PAGE_FORMAT = "<a title=\"Go to page $N\" href=\"$U\">$L</a>";
  
  /**
   * The default format used for the the current page.
   */
  public static final String DEFAULT_CURRENT_PAGE_FORMAT = "<b>$L</b>";
  
  private URLGenerator urlGenerator;
  
  private String baseURL;
  private String pageParameterName;
  private String pageSizeParameterName;

  private int currentPage;
  private int itemCount;
  private int itemsPerPage;
  private int itemsPerPageMinimum = 1;  // Default minimum items per page.
  private int itemsPerPageMaximum = Integer.MAX_VALUE; // Default maximum items per page.
  private int pagesBeforeCurrent;
  private int pagesAfterCurrent;
  private int pagesAtBeginning;
  private int pagesAtEnd;
  
  private int pageCount;
  private int firstDisplayedPage;
  private int lastDisplayedPage;
  private int firstDisplayedResult;
  private int lastDisplayedResult;
  private boolean previousPageDisplayed;
  private boolean nextPageDisplayed;
  private int lastPageAtBeginning;
  private int firstPageAtEnd;
  private boolean firstEllipsisDisplayed;
  private boolean lastEllipsisDisplayed;

  /**
   * Creates a new paging control using simple URL construction.
   * Uses default values for the parameters not specified here.
   * 
   * @param baseURL the base URL to use when constructing the page
   *    links
   * @param pageParameterName the name of the request parameter 
   *    that corresponds with the current page number
   * @param itemCount the number of items that are to be paged
   */
  public Paging(String baseURL, String pageParameterName, 
      int itemCount)
  {
    this(baseURL, pageParameterName, itemCount, 1);
  }

  /**
   * Creates a new paging control that uses simple URL construction.
   * Uses default values for the parameters not specified here.
   * 
   * @param baseURL the base URL to use when constructing the page
   *    links
   * @param pageParameterName the name of the request parameter 
   *    that corresponds with the current page number
   * @param itemCount the number of items that are to be paged
   * @param currentPage the current, visible page
   */
  public Paging(String baseURL, String pageParameterName, 
      int itemCount, int currentPage)
  {
    this(baseURL, pageParameterName, itemCount, currentPage, 
        DEFAULT_ITEMS_PER_PAGE);
  }

  /**
   * Creates a new paging control that uses simple URL construction.
   * Uses default values for the parameters not specified here.
   * 
   * @param baseURL the base URL to use when constructing the page
   *    links
   * @param pageParameterName the name of the request parameter 
   *    that corresponds with the current page number
   * @param itemCount the number of items that are to be paged
   * @param currentPage the current, visible page
   * @param itemsPerPage the maximum number of items displayed on
   *    each page
   */
  public Paging(String baseURL, String pageParameterName, 
      int itemCount, int currentPage, int itemsPerPage)
  {
    this(baseURL, pageParameterName, itemCount, currentPage, 
        itemsPerPage, DEFAULT_PAGES_BEFORE_CURRENT, 
        DEFAULT_PAGES_AFTER_CURRENT);
  }

  /**
   * Creates a new paging control that uses simple URL construction.
   * Uses default values for the parameters not specified here.
   * 
   * @param baseURL the base URL to use when constructing the page
   *    links
   * @param pageParameterName the name of the request parameter 
   *    that corresponds with the current page number
   * @param itemCount the number of items that are to be paged
   * @param currentPage the current, visible page
   * @param itemsPerPage the maximum number of items displayed on
   *    each page
   * @param pagesBeforeCurrent the maximum number of pages to show
   *    before the current page in the middle group of page links
   * @param pagesAfterCurrent the maximum number of pages to show
   *    after the current page in the middle group of page links
   */
  public Paging(String baseURL, String pageParameterName, 
      int itemCount, int currentPage, int itemsPerPage,
      int pagesBeforeCurrent, int pagesAfterCurrent)
  {
    this(baseURL, pageParameterName, itemCount, currentPage, 
        itemsPerPage, pagesBeforeCurrent, pagesAfterCurrent,
        DEFAULT_PAGES_AT_BEGINNING, 
        DEFAULT_PAGES_AT_END);
  }

  /**
   * Creates a new paging control that uses simple URL construction.
   * 
   * @param baseURL the base URL to use when constructing the page
   *    links
   * @param pageParameterName the name of the request parameter 
   *    that corresponds with the current page number
   * @param itemCount the number of items that are to be paged
   * @param currentPage the current, visible page
   * @param itemsPerPage the maximum number of items displayed on
   *    each page
   * @param pagesBeforeCurrent the maximum number of pages to show
   *    before the current page in the middle group of page links
   * @param pagesAfterCurrent the maximum number of pages to show
   *    after the current page in the middle group of page links
   * @param pagesAtBeginning the maximum number of pages shown 
   *    at the beginning, that is, including the first page, and 
   *    possibly before an ellipsis
   * @param pagesAtEnd the maximum number of pages shown 
   *    at the end, that is, including the last page, and 
   *    possibly after an ellipsis
   */
  public Paging(String baseURL, String pageParameterName, 
      int itemCount, int currentPage, int itemsPerPage,
      int pagesBeforeCurrent, int pagesAfterCurrent,
      int pagesAtBeginning, int pagesAtEnd)
  {
    this.baseURL = baseURL;
    this.pageParameterName = pageParameterName;
    this.itemCount = itemCount;
    this.currentPage = currentPage;
    setItemsPerPage(itemsPerPage);
    this.pagesBeforeCurrent = pagesBeforeCurrent;
    this.pagesAfterCurrent = pagesAfterCurrent;
    this.pagesAtBeginning = pagesAtBeginning;
    this.pagesAtEnd = pagesAtEnd;
    recalculate();
  }

  /**
   * Creates a new paging control based on a <tt>URLGenerator</tt>.
   * Uses default values for the parameters not specified here.
   * 
   * @param urlGenerator the object that will generate the URLs in 
   *    the href tags of each of the page links
   * @param itemCount the number of items that are to be paged
   */
  public Paging(URLGenerator urlGenerator, int itemCount)
  {
    this(urlGenerator, itemCount, 1);
  }

  /**
   * Creates a new paging control based on a <tt>URLGenerator</tt>.
   * Uses default values for the parameters not specified here.
   * 
   * @param urlGenerator the object that will generate the URLs in 
   *    the href tags of each of the page links
   * @param itemCount the number of items that are to be paged
   * @param currentPage the current, visible page
   */
  public Paging(URLGenerator urlGenerator, int itemCount, 
      int currentPage)
  {
    this(urlGenerator, itemCount, currentPage, DEFAULT_ITEMS_PER_PAGE);
  }

  /**
   * Creates a new paging control based on a <tt>URLGenerator</tt>.
   * Uses default values for the parameters not specified here.
   * 
   * @param urlGenerator the object that will generate the URLs in 
   *    the href tags of each of the page links
   * @param itemCount the number of items that are to be paged
   * @param currentPage the current, visible page
   * @param itemsPerPage the maximum number of items displayed on
   *    each page
   */
  public Paging(URLGenerator urlGenerator, int itemCount, 
      int currentPage, int itemsPerPage)
  {
    this(urlGenerator, itemCount, currentPage, itemsPerPage, 
        DEFAULT_PAGES_BEFORE_CURRENT, DEFAULT_PAGES_AFTER_CURRENT);
  }

  /**
   * Creates a new paging control based on a <tt>URLGenerator</tt>.
   * Uses default values for the parameters not specified here.
   * 
   * @param urlGenerator the object that will generate the URLs in 
   *    the href tags of each of the page links
   * @param itemCount the number of items that are to be paged
   * @param currentPage the current, visible page
   * @param itemsPerPage the maximum number of items displayed on
   *    each page
   * @param pagesBeforeCurrent the maximum number of pages to show
   *    before the current page in the middle group of page links
   * @param pagesAfterCurrent the maximum number of pages to show
   *    after the current page in the middle group of page links
   */
  public Paging(URLGenerator urlGenerator, int itemCount, 
      int currentPage, int itemsPerPage, int pagesBeforeCurrent, 
      int pagesAfterCurrent)
  {
    this(urlGenerator, itemCount, currentPage, itemsPerPage, 
        pagesBeforeCurrent, pagesAfterCurrent,
        DEFAULT_PAGES_AT_BEGINNING, 
        DEFAULT_PAGES_AT_END);
  }
  
  /**
   * Creates a new paging control based on a <tt>URLGenerator</tt>.
   * 
   * @param urlGenerator the object that will generate the URLs in 
   *    the href tags of each of the page links
   * @param itemCount the number of items that are to be paged
   * @param currentPage the current, visible page
   * @param itemsPerPage the maximum number of items displayed on
   *    each page
   * @param pagesBeforeCurrent the maximum number of pages to show
   *    before the current page in the middle group of page links
   * @param pagesAfterCurrent the maximum number of pages to show
   *    after the current page in the middle group of page links
   * @param pagesAtBeginning the maximum number of pages shown 
   *    at the beginning, that is, including the first page, and 
   *    possibly before an ellipsis
   * @param pagesAtEnd the maximum number of pages shown 
   *    at the end, that is, including the last page, and 
   *    possibly after an ellipsis
   */
  public Paging(URLGenerator urlGenerator, int itemCount, 
      int currentPage, int itemsPerPage, int pagesBeforeCurrent, 
      int pagesAfterCurrent, int pagesAtBeginning, int pagesAtEnd)
  {
    this.urlGenerator = urlGenerator;
    this.currentPage = currentPage;
    this.itemCount = itemCount;
    setItemsPerPage(itemsPerPage);
    this.pagesBeforeCurrent = pagesBeforeCurrent;
    this.pagesAfterCurrent = pagesAfterCurrent;
    this.pagesAtBeginning = pagesAtBeginning;
    this.pagesAtEnd = pagesAtEnd;
    recalculate();
  }
  
  /**
   * Recalculates the private members that depend on other 
   * private members, such as which page is the first one 
   * to be displayed, and whether the ellipses are displayed.
   * 
   * <p>This method should get called whenever one of the variables
   * that it uses is changed.</p>
   */
  private void recalculate()
  {
    this.pageCount = this.itemCount / this.itemsPerPage;

    if (this.itemCount % this.itemsPerPage > 0)
    {
      this.pageCount++;
    }
    
    this.firstDisplayedPage = this.currentPage - this.pagesBeforeCurrent;

    if (this.firstDisplayedPage < 1)
    {
      this.firstDisplayedPage = 1;
    }

    this.lastDisplayedPage = this.currentPage + this.pagesAfterCurrent;

    if (this.lastDisplayedPage > this.pageCount)
    {
      this.lastDisplayedPage = this.pageCount;
    }
    
    this.firstDisplayedResult = 1 + (this.currentPage - 1) * this.itemsPerPage;
    
    this.lastDisplayedResult = this.currentPage * this.itemsPerPage > this.itemCount ? 
        this.itemCount : this.currentPage * this.itemsPerPage;

    this.previousPageDisplayed = this.currentPage > 1;
    
    this.nextPageDisplayed = this.currentPage < this.pageCount;
    
    this.lastPageAtBeginning = Math.min(1 + this.pagesAtBeginning, this.firstDisplayedPage) - 1;
    
    this.firstPageAtEnd = Math.max(this.lastDisplayedPage + 1, this.pageCount - this.pagesAtEnd + 1);
    
    this.firstEllipsisDisplayed = this.pagesAtBeginning > 0 && this.firstDisplayedPage > this.pagesAtBeginning + 1;
    
    this.lastEllipsisDisplayed = this.pagesAtEnd > 0 && this.lastDisplayedPage < this.pageCount - this.pagesAtEnd;
  }
  
  /**
   * Sets the current page and page size from request parameters.
   * 
   * @param context The Context from which to read the parameters.
   */
  public void readParameters(Context context)
  {
    int pageNumber = context.query().getInt(getPageParameterName(), 1);
    int pageSize = context.query().getInt(getPageSizeName(), getItemsPerPage());
    
    setCurrentPage(pageNumber);
    setItemsPerPage(pageSize);
  }
  
  /**
   * Renders the paging as a <tt>&lt;span&gt;</tt> element.
   * Each page item is a direct descendant of the 
   * <tt>&lt;span&gt;</tt>, and each is separated by a non-
   * breaking space.
   * 
   * @param context the current context
   * @return the paging as a <tt>&lt;span&gt;</tt> element
   */
  public String renderAsSpan(Context context)
  {
    return renderAsSpan(context, null);
  }

  /**
   * Renders the paging as a <tt>&lt;span&gt;</tt> element.
   * Each page item is a direct descendant of the 
   * <tt>&lt;span&gt;</tt>, and each is separated by a non-
   * breaking space.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;span&gt;</tt> element
   * @return the paging as a <tt>&lt;span&gt;</tt> element
   */
  public String renderAsSpan(Context context, String cssClassName)
  {
    return renderAsSpan(context, cssClassName, null);
  }

  /**
   * Renders the paging as a <tt>&lt;span&gt;</tt> element.
   * Each page item is a direct descendant of the 
   * <tt>&lt;span&gt;</tt>.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;span&gt;</tt> element
   * @param innerPrefixHTML a string to insert before the first page item
   * @return the paging as a <tt>&lt;span&gt;</tt> element
   */
  public String renderAsSpan(Context context, String cssClassName, 
      String innerPrefixHTML)
  {
    return renderAsSpan(context, cssClassName, innerPrefixHTML, 
        null, " ");
  }

  /**
   * Renders the paging as a <tt>&lt;span&gt;</tt> element.
   * Each page item is a direct descendant of the 
   * <tt>&lt;span&gt;</tt>, and each is separated by the 
   * specified separator.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;span&gt;</tt> element
   * @param innerPrefixHTML a string to insert before the first page item
   * @param innerSuffixHTML a string to insert after the last page item
   * @param itemSeparatorHTML the separator to use between each page item
   * @return the paging as a <tt>&lt;span&gt;</tt> element
   */
  public String renderAsSpan(Context context, String cssClassName, 
      String innerPrefixHTML, String innerSuffixHTML, String itemSeparatorHTML)
  {
    return renderAsSpan(context, cssClassName, innerPrefixHTML, 
        innerSuffixHTML, itemSeparatorHTML, DEFAULT_PREVIOUS_PAGE_HTML, 
        DEFAULT_NEXT_PAGE_HTML, DEFAULT_INLINE_ELLIPSIS_HTML);
  }

  /**
   * Renders the paging as a <tt>&lt;span&gt;</tt> element.
   * Each page item is a direct descendant of the 
   * <tt>&lt;span&gt;</tt>, and each is separated by the 
   * specified separator.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;span&gt;</tt> element
   * @param innerPrefixHTML a string to insert before the first page item
   * @param innerSuffixHTML a string to insert after the last page item
   * @param itemSeparatorHTML the separator to use between each page item
   * @param previousPageHTML the HTML that is used as the label for the 
   *    button that links to the previous page
   * @param nextPageHTML the HTML that is used as the label for the 
   *    button that links to the next page.
   * @param ellipsisHTML the HTML that is used as the ellipsis between
   *    the middle group of page links (those surrounding the 
   *    current page) and the end groups (containing the first 
   *    or last pages)
   * @return the paging as a <tt>&lt;span&gt;</tt> element
   */
  public String renderAsSpan(Context context, String cssClassName, 
      String innerPrefixHTML, String innerSuffixHTML, String itemSeparatorHTML,
      String previousPageHTML, String nextPageHTML, String ellipsisHTML)
  {
    return render(context, 
        StringHelper.isEmpty(cssClassName) ? "<span>" : "<span class=\"" + cssClassName + "\">", 
        "</span>", null, null, innerPrefixHTML, innerSuffixHTML, itemSeparatorHTML,
        previousPageHTML, nextPageHTML, ellipsisHTML);
  }
  
  /**
   * Renders the paging as a <tt>&lt;p&gt;</tt> element.
   * Each page item is a direct descendant of the 
   * <tt>&lt;p&gt;</tt>, and each is separated by a non-
   * breaking space.
   * 
   * @param context the current context
   * @return the paging as a <tt>&lt;p&gt;</tt> element
   */
  public String renderAsParagraph(Context context)
  {
    return renderAsParagraph(context, null);
  }

  /**
   * Renders the paging as a <tt>&lt;p&gt;</tt> element.
   * Each page item is a direct descendant of the 
   * <tt>&lt;p&gt;</tt>, and each is separated by a non-
   * breaking space.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;p&gt;</tt> element
   * @return the paging as a <tt>&lt;p&gt;</tt> element
   */
  public String renderAsParagraph(Context context, String cssClassName)
  {
    return renderAsParagraph(context, cssClassName, null);
  }

  /**
   * Renders the paging as a <tt>&lt;p&gt;</tt> element.
   * Each page item is a direct descendant of the 
   * <tt>&lt;p&gt;</tt>.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;p&gt;</tt> element
   * @param innerPrefixHTML a string to insert before the first page item
   * @return the paging as a <tt>&lt;p&gt;</tt> element
   */
  public String renderAsParagraph(Context context, String cssClassName, 
      String innerPrefixHTML)
  {
    return renderAsParagraph(context, cssClassName, innerPrefixHTML, 
        null, " ");
  }

  /**
   * Renders the paging as a <tt>&lt;p&gt;</tt> element.
   * Each page item is a direct descendant of the 
   * <tt>&lt;p&gt;</tt>, and each is separated by the 
   * specified separator.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;p&gt;</tt> element
   * @param innerPrefixHTML a string to insert before the first page item
   * @param innerSuffixHTML a string to insert after the last page item
   * @param itemSeparatorHTML the separator to use between each page item
   * @return the paging as a <tt>&lt;p&gt;</tt> element
   */
  public String renderAsParagraph(Context context, String cssClassName, 
      String innerPrefixHTML, String innerSuffixHTML, String itemSeparatorHTML)
  {
    return renderAsParagraph(context, cssClassName, innerPrefixHTML, 
        innerSuffixHTML, itemSeparatorHTML, DEFAULT_PREVIOUS_PAGE_HTML, 
        DEFAULT_NEXT_PAGE_HTML, DEFAULT_INLINE_ELLIPSIS_HTML);
  }

  /**
   * Renders the paging as a <tt>&lt;p&gt;</tt> element.
   * Each page item is a direct descendant of the 
   * <tt>&lt;p&gt;</tt>, and each is separated by the 
   * specified separator.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;p&gt;</tt> element
   * @param innerPrefixHTML a string to insert before the first page item
   * @param innerSuffixHTML a string to insert after the last page item
   * @param itemSeparatorHTML the separator to use between each page item
   * @param previousPageHTML the HTML that is used as the label for the 
   *    button that links to the previous page
   * @param nextPageHTML the HTML that is used as the label for the 
   *    button that links to the next page.
   * @param ellipsisHTML the HTML that is used as the ellipsis between
   *    the middle group of page links (those surrounding the 
   *    current page) and the end groups (containing the first 
   *    or last pages)
   * @return the paging as a <tt>&lt;p&gt;</tt> element
   */
  public String renderAsParagraph(Context context, String cssClassName, 
      String innerPrefixHTML, String innerSuffixHTML, String itemSeparatorHTML,
      String previousPageHTML, String nextPageHTML, String ellipsisHTML)
  {
    return render(context, 
        StringHelper.isEmpty(cssClassName) ? "<p>" : "<p class=\"" + cssClassName + "\">", 
        "</p>", null, null, innerPrefixHTML, innerSuffixHTML, itemSeparatorHTML,
        previousPageHTML, nextPageHTML, ellipsisHTML);
  }
  
  /**
   * Renders the paging as a <tt>&lt;ul&gt;</tt> element.
   * Each page item is contained inside a <tt>&lt;li&gt;</tt>
   * element.
   * 
   * @param context the current context
   * @return the paging as a <tt>&lt;ul&gt;</tt> element
   */
  public String renderAsList(Context context)
  {
    return renderAsList(context, null);
  }

  /**
   * Renders the paging as a <tt>&lt;ul&gt;</tt> element.
   * Each page item is contained inside a <tt>&lt;li&gt;</tt>
   * element.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;ul&gt;</tt> element
   * @return the paging as a <tt>&lt;ul&gt;</tt> element
   */
  public String renderAsList(Context context, String cssClassName)
  {
    return renderAsList(context, cssClassName, null);
  }
  
  /**
   * Renders the paging as a <tt>&lt;ul&gt;</tt> element.
   * Each page item is contained inside a <tt>&lt;li&gt;</tt>
   * element, as is the prefix.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;ul&gt;</tt> element
   * @param innerPrefixHTML a string to insert before the first page item
   * @return the paging as a <tt>&lt;ul&gt;</tt> element
   */
  public String renderAsList(Context context, String cssClassName,
      String innerPrefixHTML)
  {
    return renderAsList(context, cssClassName, innerPrefixHTML, null);
  }

  /**
   * Renders the paging as a <tt>&lt;ul&gt;</tt> element.
   * Each page item is contained inside a <tt>&lt;li&gt;</tt>
   * element, as are the prefix and suffix.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;ul&gt;</tt> element
   * @param innerPrefixHTML a string to insert before the first page item
   * @param innerSuffixHTML a string to insert after the last page item
   * @return the paging as a <tt>&lt;ul&gt;</tt> element
   */
  public String renderAsList(Context context, String cssClassName,
      String innerPrefixHTML, String innerSuffixHTML)
  {
    return renderAsList(context, cssClassName, null, null,
        DEFAULT_PREVIOUS_PAGE_HTML, DEFAULT_NEXT_PAGE_HTML, 
        DEFAULT_BLOCK_ELLIPSIS_HTML);
  }

  /**
   * Renders the paging as a <tt>&lt;ul&gt;</tt> element.
   * Each page item is contained inside a <tt>&lt;li&gt;</tt>
   * element, as are the prefix and suffix.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;ul&gt;</tt> element
   * @param innerPrefixHTML a string to insert before the first page item
   * @param innerSuffixHTML a string to insert after the last page item
   * @param previousPageHTML the HTML that is used as the label for the 
   *    button that links to the previous page
   * @param nextPageHTML the HTML that is used as the label for the 
   *    button that links to the next page.
   * @param ellipsisHTML the HTML that is used as the ellipsis between
   *    the middle group of page links (those surrounding the 
   *    current page) and the end groups (containing the first 
   *    or last pages)
   * @return the paging as a <tt>&lt;ul&gt;</tt> element
   */
  public String renderAsList(Context context, String cssClassName,
      String innerPrefixHTML, String innerSuffixHTML,
      String previousPageHTML, String nextPageHTML, String ellipsisHTML)
  {
    return render(context, 
        StringHelper.isEmpty(cssClassName) ? "<ul>" : "<ul class=\"" + cssClassName + "\">", 
        "</ul>", "<li>", "</li>", innerPrefixHTML, innerSuffixHTML,  null,
        previousPageHTML, nextPageHTML, ellipsisHTML);
  }
  
  /**
   * Renders the paging as a <tt>&lt;table&gt;</tt> element.
   * Each page item is contained inside a table cell.
   * 
   * @param context the current context
   * @return the paging as a <tt>&lt;table&gt;</tt> element
   */
  public String renderAsTable(Context context)
  {
    return renderAsTable(context, null);
  }

  /**
   * Renders the paging as a <tt>&lt;table&gt;</tt> element.
   * Each page item is contained inside a <tt>&lt;td&gt;</tt>
   * element.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;table&gt;</tt> element
   * @return the paging as a <tt>&lt;table&gt;</tt> element
   */
  public String renderAsTable(Context context, String cssClassName)
  {
    return renderAsTable(context, cssClassName, null);
  }

  /**
   * Renders the paging as a <tt>&lt;table&gt;</tt> element.
   * Each page item is contained inside a <tt>&lt;td&gt;</tt>
   * element, as is the prefix.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;table&gt;</tt> element
   * @param innerPrefixHTML a string to insert before the first page item
   * @return the paging as a <tt>&lt;table&gt;</tt> element
   */
  public String renderAsTable(Context context, String cssClassName, 
      String innerPrefixHTML)
  {
    return renderAsTable(context, cssClassName, innerPrefixHTML, null);
  }

  /**
   * Renders the paging as a <tt>&lt;table&gt;</tt> element.
   * Each page item is contained inside a <tt>&lt;td&gt;</tt>
   * element, as are the prefix and suffix.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;table&gt;</tt> element
   * @param innerPrefixHTML a string to insert before the first page item
   * @param innerSuffixHTML a string to insert after the last page item
   * @return the paging as a <tt>&lt;table&gt;</tt> element
   */
  public String renderAsTable(Context context, String cssClassName, 
      String innerPrefixHTML, String innerSuffixHTML)
  {
    return renderAsTable(context, cssClassName, innerPrefixHTML, 
        innerSuffixHTML, DEFAULT_PREVIOUS_PAGE_HTML, 
        DEFAULT_NEXT_PAGE_HTML, DEFAULT_BLOCK_ELLIPSIS_HTML);
  }

  /**
   * Renders the paging as a <tt>&lt;table&gt;</tt> element.
   * Each page item is contained inside a <tt>&lt;td&gt;</tt>
   * element, as are the prefix and suffix.
   * 
   * @param context the current context
   * @param cssClassName the css class name to apply to the 
   *    rendered <tt>&lt;table&gt;</tt> element
   * @param innerPrefixHTML a string to insert before the first page item
   * @param innerSuffixHTML a string to insert after the last page item
   * @param previousPageHTML the HTML that is used as the label for the 
   *    button that links to the previous page
   * @param nextPageHTML the HTML that is used as the label for the 
   *    button that links to the next page.
   * @param ellipsisHTML the HTML that is used as the ellipsis between
   *    the middle group of page links (those surrounding the 
   *    current page) and the end groups (containing the first 
   *    or last pages)
   * @return the paging as a <tt>&lt;table&gt;</tt> element
   */
  public String renderAsTable(Context context, String cssClassName, 
      String innerPrefixHTML, String innerSuffixHTML,
      String previousPageHTML, String nextPageHTML, String ellipsisHTML)
  {
    return render(context, 
        StringHelper.isEmpty(cssClassName) ? "<table cellspacing=\"0\"><tbody><tr>" : "<table cellspacing=\"0\" class=\"" + cssClassName + "\"><tbody><tr>", 
        "</tr></tbody></table>", "<td>", "</td>", innerPrefixHTML, innerSuffixHTML, null,
        previousPageHTML, nextPageHTML, ellipsisHTML);
  }
  
  /**
   * <i>(Advanced)</i> Renders the paging control in the specified style.  
   * The default formats for the page items are used.
   * 
   * @param context the current context
   * @param outerPrefixHTML a string to insert at the start of the 
   *    rendered controls
   * @param outerSuffixHTML a string to insert at the end of the rendered
   *    controls
   * @param itemPrefixHTML a string to insert before each page item,
   *    including the ellipses
   * @param itemSuffixHTML a string to insert after each page item,
   *    including the ellipses
   * @param innerPrefixHTML a string to insert before the first page item.
   *    That will be rendered between the specified <tt>itemPrefixHTML</tt>
   *    and <tt>itemSuffixHTML</tt>
   * @param innerSuffixHTML a string to insert after the last page item.
   *    That will be rendered between the specified <tt>itemPrefixHTML</tt>
   *    and <tt>itemSuffixHTML</tt>
   * @param itemSeparatorHTML the separator to use between each page item
   * @param previousPageHTML the HTML that is used as the label for the 
   *    button that links to the previous page
   * @param nextPageHTML the HTML that is used as the label for the 
   *    button that links to the next page.
   * @param ellipsisHTML the HTML that is used as the ellipsis between
   *    the middle group of page links (those surrounding the 
   *    current page) and the end groups (containing the first 
   *    or last pages)
   * @return an HTML string representing the paging control
   */
  public String render(Context context,  
      String outerPrefixHTML, String outerSuffixHTML, 
      String itemPrefixHTML, String itemSuffixHTML,
      String innerPrefixHTML, String innerSuffixHTML, 
      String itemSeparatorHTML, String previousPageHTML,
      String nextPageHTML, String ellipsisHTML)
  {
    return render(context, outerPrefixHTML, outerSuffixHTML, 
        itemPrefixHTML, itemSuffixHTML, innerPrefixHTML, 
        innerSuffixHTML, itemSeparatorHTML, previousPageHTML, 
        nextPageHTML, ellipsisHTML, 2, "", 
        DEFAULT_PAGE_FORMAT, DEFAULT_CURRENT_PAGE_FORMAT);
  }
  
  /**
   * <i>(Advanced)</i> Renders the paging control in the specified style.  
   * 
   * @param context the current context
   * @param outerPrefixHTML a string to insert at the start of the 
   *    rendered controls
   * @param outerSuffixHTML a string to insert at the end of the rendered
   *    controls
   * @param itemPrefixHTML a string to insert before each page item,
   *    including the ellipses
   * @param itemSuffixHTML a string to insert after each page item,
   *    including the ellipses
   * @param innerPrefixHTML a string to insert before the first page item.
   *    This will be rendered between the specified <tt>itemPrefixHTML</tt>
   *    and <tt>itemSuffixHTML</tt>
   * @param innerSuffixHTML a string to insert after the last page item.
   *    This will be rendered between the specified <tt>itemPrefixHTML</tt>
   *    and <tt>itemSuffixHTML</tt>
   * @param itemSeparatorHTML the separator to use between each page
   * @param previousPageHTML the HTML that is used as the label for the 
   *    button that links to the previous page
   * @param nextPageHTML the HTML that is used as the label for the 
   *    button that links to the next page.
   * @param ellipsisHTML the HTML that is used as the ellipsis between
   *    the middle group of page links (those surrounding the 
   *    current page) and the end groups (containing the first 
   *    or last pages)
   * @param minPages the minimum number of pages this paging control
   *    must have in order to render normally.  If it has less than 
   *    that number of pages, then <tt>defaultHTML</tt> will be 
   *    returned.  It is very likely that only a value of 0, 1, or 2
   *    makes sense here.
   * @param defaultHTML the HTML to display in place of the paging
   *    controls when they do not have the minimum number of pages
   *    specified by <tt>minPages</tt>
   * @param pageFormat the format to use for all non-current page 
   *    items (see <tt>Paging.DEFAULT_PAGE_FORMAT</tt> for an example)
   * @param currentPageFormat the format to use for the current page 
   *    item (see <tt>Paging.DEFAULT_CURRENT_PAGE_FORMAT</tt> for an example)
   * @return an HTML string representing the paging control
   */
  public String render(Context context,  
      String outerPrefixHTML, String outerSuffixHTML, 
      String itemPrefixHTML, String itemSuffixHTML,
      String innerPrefixHTML, String innerSuffixHTML, 
      String itemSeparatorHTML, String previousPageHTML,
      String nextPageHTML, String ellipsisHTML,
      int minPages, String defaultHTML, 
      String pageFormat, String currentPageFormat)
  {
    if (getPageCount() < minPages)
    {
      return defaultHTML;
    }
    
    boolean useItemPrefix = StringHelper.isNonEmpty(itemPrefixHTML);
    boolean useItemSuffix = StringHelper.isNonEmpty(itemSuffixHTML);
    boolean useItemSeparator = StringHelper.isNonEmpty(itemSeparatorHTML);
    
    StringBuilder sb = new StringBuilder();
    
    if (StringHelper.isNonEmpty(outerPrefixHTML))
    {
      sb.append(outerPrefixHTML);
    }
    
    if (StringHelper.isNonEmpty(innerPrefixHTML))
    {
      if (useItemPrefix)
      {
        sb.append(itemPrefixHTML);
      }
      
      sb.append(innerPrefixHTML);
      
      if (useItemSuffix)
      {
        sb.append(itemSuffixHTML);
      }
    }
    
    if (isPreviousPageDisplayed() && StringHelper.isNonEmpty(previousPageHTML))
    {
      if (useItemPrefix)
      {
        sb.append(itemPrefixHTML);
      }
      
      sb.append(renderPage(context, getCurrentPage() - 1, 
          previousPageHTML, pageFormat));
      
      if (useItemSuffix)
      {
        sb.append(itemSuffixHTML);
      }
      
      if (useItemSeparator)
      {
        sb.append(itemSeparatorHTML);
      }
    }
    
    for (int i = 1; i <= getLastPageAtBeginning(); i++)
    {
      if (useItemPrefix)
      {
        sb.append(itemPrefixHTML);
      }
      
      sb.append(renderPage(context, i, Integer.toString(i), pageFormat));
      
      if (useItemSuffix)
      {
        sb.append(itemSuffixHTML);
      }
      
      if (useItemSeparator 
          && (i < getLastPageAtBeginning() || !isFirstEllipsisDisplayed()))
      {
        sb.append(itemSeparatorHTML);
      }
    }
    
    if (isFirstEllipsisDisplayed() && StringHelper.isNonEmpty(ellipsisHTML))
    {
      if (useItemPrefix)
      {
        sb.append(itemPrefixHTML);
      }
      
      sb.append(ellipsisHTML);
      
      if (useItemSuffix)
      {
        sb.append(itemSuffixHTML);
      }
    }
    
    for (int i = getFirstDisplayedPage(); i <= getLastDisplayedPage(); i++)
    {
      if (useItemPrefix)
      {
        sb.append(itemPrefixHTML);
      }
      
      sb.append(renderPage(context, i, Integer.toString(i), 
          (getCurrentPage() == i ? currentPageFormat : pageFormat)));
      
      if (useItemSuffix)
      {
        sb.append(itemSuffixHTML);
      }
      
      if (useItemSeparator
          && (i < getLastDisplayedPage()
              || !isLastEllipsisDisplayed() && isNextPageDisplayed() && StringHelper.isNonEmpty(nextPageHTML)))
      {
        sb.append(itemSeparatorHTML);
      }
    }
    
    if (isLastEllipsisDisplayed() && StringHelper.isNonEmpty(ellipsisHTML))
    {
      if (useItemPrefix)
      {
        sb.append(itemPrefixHTML);
      }
      
      sb.append(ellipsisHTML);
      
      if (useItemSuffix)
      {
        sb.append(itemSuffixHTML);
      }
    }
    
    for (int i = getFirstPageAtEnd(); i <= getPageCount(); i++)
    {
      if (useItemSeparator
          && i == getLastDisplayedPage() + 1)
      {
        sb.append(itemSeparatorHTML);
      }
      
      if (useItemPrefix)
      {
        sb.append(itemPrefixHTML);
      }

      sb.append(renderPage(context, i, Integer.toString(i), pageFormat));
      
      if (useItemSuffix)
      {
        sb.append(itemSuffixHTML);
      }
      
      if (useItemSeparator
          && (i < getPageCount()
              || isNextPageDisplayed() && StringHelper.isNonEmpty(nextPageHTML)))
      {
        sb.append(itemSeparatorHTML);
      }
    }
    
    if (isNextPageDisplayed() && StringHelper.isNonEmpty(nextPageHTML))
    {
      if (useItemPrefix)
      {
        sb.append(itemPrefixHTML);
      }
      
      sb.append(renderPage(context, getCurrentPage() + 1, 
          nextPageHTML, pageFormat));
      
      if (useItemSuffix)
      {
        sb.append(itemSuffixHTML);
      }
    }
    
    if (StringHelper.isNonEmpty(innerSuffixHTML))
    {
      if (useItemPrefix)
      {
        sb.append(itemPrefixHTML);
      }
      
      sb.append(innerSuffixHTML);
      
      if (useItemSuffix)
      {
        sb.append(itemSuffixHTML);
      }
    }

    if (StringHelper.isNonEmpty(outerSuffixHTML))
    {
      sb.append(outerSuffixHTML);
    }
    
    return sb.toString();
  }
  
  /**
   * Returns the URL for the specified page.  If a <tt>URLGenerator</tt>
   * was provided during construction, this method simply makes a call
   * to the <tt>getURL(Context, int)</tt> method of the generator.  
   * Otherwise, simple URL construction is used with a base URL and page
   * parameter name.
   * 
   * @param context the current context
   * @param page the page number for which to get the URL
   * @return the URL for the specified page number
   */
  public String getURL(Context context, int page)
  {
    if (getURLGenerator() == null)
    {
      String ourBaseURL = getBaseURL();
      
      StringBuilder sb = new StringBuilder();
      sb.append(ourBaseURL);
      if (ourBaseURL.contains("?"))
      {
        sb.append("&");
      }
      else
      {
        sb.append("?");
      }
      sb.append(getPageParameterName());
      sb.append("=");
      sb.append(page);
      return sb.toString();
    }

    return getURLGenerator().getURL(context, page);
  }

  /**
   * Renders the specified page item as HTML using the default format
   * and the page number for the label 
   * 
   * @see Paging#DEFAULT_PAGE_FORMAT
   * @param context the current context
   * @param page the number of the page item to render
   * @return an HTML string representing the specified page item
   */
  public String renderPage(Context context, int page)
  {
    return renderPage(context, page, Integer.toString(page));
  }

  /**
   * Renders the specified page item as HTML using the default format.
   * 
   * @see Paging#DEFAULT_PAGE_FORMAT
   * @param context the current context
   * @param page the number of the page item to render
   * @param labelHTML an HTML string that represents the label for 
   *    the page item
   * @return an HTML string representing the specified page item
   */
  public String renderPage(Context context, int page, String labelHTML)
  {
    return renderPage(context, page, labelHTML, DEFAULT_PAGE_FORMAT);
  }
  
  /**
   * Renders the specified page item as HTML.
   * 
   * @see Paging#TOKENS for the tokens that may be used in the format string
   * @param context the current context
   * @param page the number of the page item to render
   * @param labelHTML an HTML string that represents the label for 
   *    the page item
   * @param format the format string to use
   * @return an HTML string representing the specified page item
   */
  public String renderPage(Context context, int page, String labelHTML, String format)
  {
    return StringHelper.replaceSubstrings(format, TOKENS, new String[] { 
        Integer.toString(page), labelHTML, NetworkHelper.render(getURL(context, page)) });
  }
  
  /**
   * Provided a List of the same length as the item count, extract the
   * portion that matches the current page.  For example, for an item
   * count of 35, a page size of 10, and a current page of 2, this method
   * would return a new List including items from positions 10 through 19.
   *   <p>
   * This is similar to the filter method except that it does not iterate
   * through the entire List or modify the source List. 
   */
  public <E extends Object> List<E> extractCurrentPage(List<E> items)
  {
    ArrayList<E> toReturn = new ArrayList<>(getItemsPerPage());
    
    // Only proceed if the sizes match.
    if (items.size() == getItemCount())
    {
      int startIndex = getFirstDisplayedResult() - 1;
      int endIndex = getLastDisplayedResult();
      
      for (int i = startIndex; i < endIndex; i++)
      {
        toReturn.add(items.get(i));
      }
    }
    
    return toReturn;
  }

  /**
   * Returns the url generator of this paging control. This is used to 
   * generate the href values of each of the links.  If this is <tt>null</tt>,
   * we know that URLs should be built out manually.
   * 
   * @return the url generator of this paging control
   */
  protected URLGenerator getURLGenerator()
  {
    return this.urlGenerator;
  }

  /**
   * Returns the base URL used for constructing the page links.
   * 
   * @return the base URL used for constructing the page links
   */
  protected String getBaseURL()
  {
    return this.baseURL;
  }

  /**
   * Returns the name of the request parameter that corresponds
   * to the current page number.
   * 
   * @return the name of the request parameter that corresponds
   *    to the current page number
   */
  protected String getPageParameterName()
  {
    return this.pageParameterName;
  }

  /**
   * Returns the name of the request parameter that corresponds
   * to the page size.
   * 
   * @return the name of the request parameter that corresponds
   *    to the page size
   */
  protected String getPageSizeName()
  {
    return this.pageSizeParameterName;
  }

  /**
   * Sets the name of the request parameter that corresponds
   * to the page size.
   * 
   * @param pageSizeParameterName the name of the request parameter that
   *    corresponds to the page size
   */
  protected void setPageSizeName(String pageSizeParameterName)
  {
    this.pageSizeParameterName = pageSizeParameterName;
  }

  /**
   * Sets the items per page boundaries (minimum and maximum).  This is
   * primarily useful for constraining user-provided page sizes.
   */
  public void setItemsPerPageBoundaries(int minimum, int maximum)
  {
    this.itemsPerPageMinimum = minimum;
    this.itemsPerPageMaximum = maximum;
  }
  
  /**
   * Returns the current page of this paging control.
   * 
   * @return the current page of this paging control
   */
  public int getCurrentPage()
  {
    return this.currentPage;
  }

  /**
   * Sets the current page of this paging control.  
   * 
   * @param currentPage what will become the new current page of this
   *    paging control.
   */
  public void setCurrentPage(int currentPage)
  {
    // Enforce bounds.
    this.currentPage = NumberHelper.boundInteger(currentPage, 1, getPageCount());
    recalculate();
  }

  /**
   * Returns the number of items that are paged by this paging control.
   * 
   * @return the number of items that are paged by this paging control
   */
  public int getItemCount()
  {
    return this.itemCount;
  }

  /**
   * Sets the number of items that are paged by this paging control.
   * 
   * @param itemCount the number of items that this paging control 
   *    pages
   */
  public void setItemCount(int itemCount)
  {
    this.itemCount = itemCount;
    recalculate();
  }
  
  /**
   * Returns the maximum number of page links shown before the 
   * current page in the middle group of page links.
   * 
   * @return the maximum number of page links shown before the 
   *    current page
   */
  public int getPagesBeforeCurrent()
  {
    return this.pagesBeforeCurrent;
  }

  /**
   * Sets the maximum number of page links shown before the 
   * current page in the middle group of page links.
   * 
   * @param pagesBeforeCurrent the maximum number of page links to be
   *    shown before the current page
   */
  public void setPagesBeforeCurrent(int pagesBeforeCurrent)
  {
    this.pagesBeforeCurrent = pagesBeforeCurrent;
    recalculate();
  }
  
  /**
   * Returns the maximum number of page links shown after the 
   * current page in the middle group of page links.
   * 
   * @return the maximum number of page links shown after the 
   *    current page
   */
  public int getPagesAfterCurrent()
  {
    return this.pagesAfterCurrent;
  }

  /**
   * Sets the maximum number of page links shown after the 
   * current page in the middle group of page links.
   * 
   * @param pagesAfterCurrent the maximum number of page links to be
   *    shown after the current page
   */
  public void setPagesAfterCurrent(int pagesAfterCurrent)
  {
    this.pagesAfterCurrent = pagesAfterCurrent;
    recalculate();
  }
  
  /**
   * Returns the maximum number of pages shown at the the "beginning" of the 
   * list of pages, that is, before the pages surrounding the current page. 
   * This will include the first page and may be succeeded by an ellipsis.  
   * 
   * @return the maximum number of pages shown at the beginning of the control
   */
  public int getPagesAtBeginning()
  {
    return this.pagesAtBeginning;
  }
  
  /**
   * Sets the maximum number of pages shown at the the "beginning" of the 
   * list of pages, that is, before the pages surrounding the current page. 
   * This will include the first page and may be succeeded by an ellipsis. 
   * 
   * @param pagesAtBeginning maximum number of pages to be shown at the 
   *    beginning of the control
   */
  public void setPagesAtBeginning(int pagesAtBeginning)
  {
    this.pagesAtBeginning = pagesAtBeginning;
    recalculate();
  }
  
  /**
   * Returns the maximum number of pages shown at the the "end" of the 
   * list of pages, that is, after the pages surrounding the current page. 
   * This will include the last page and may be preceded by an ellipsis.  
   * 
   * @return the maximum number of pages shown at the end of the control
   */
  public int getPagesAtEnd()
  {
    return this.pagesAtEnd;
  }
  
  /**
   * Sets the maximum number of pages shown at the the "end" of the 
   * list of pages, that is, after the pages surrounding the current page. 
   * This will include the last page and may be preceded by an ellipsis. 
   * 
   * @param pagesAtEnd maximum number of pages to be shown at the 
   *    end of the control
   */
  public void setPagesAtEnd(int pagesAtEnd)
  {
    this.pagesAtEnd = pagesAtEnd;
    recalculate();
  }

  /**
   * Returns the maximum number of items displayed on each page.
   * 
   * @return the maximum number of items displayed on each page
   */
  public int getItemsPerPage()
  {
    return this.itemsPerPage;
  }

  /**
   * Sets the maximum number of items to be displayed on each
   * page.  
   * 
   * @param itemsPerPage the number of items to be displayed
   *    on each page
   */
  public void setItemsPerPage(int itemsPerPage)
  {
    // Enforce bounds.
    this.itemsPerPage = NumberHelper.boundInteger(itemsPerPage,
        this.itemsPerPageMinimum, this.itemsPerPageMaximum);
    recalculate();
  }

  /**
   * Returns the number of the first page displayed in 
   * the middle group of page links.  The middle group
   * of page links are the ones surrounding the current 
   * page.
   * 
   * @return the number of the first page displayed in 
   *    the middle group of page links
   */
  public int getFirstDisplayedPage()
  {
    return this.firstDisplayedPage;
  }

  /**
   * Returns the number of the first item displayed on 
   * the current page.  
   * 
   * @return the number of the first item displayed on 
   *    the current page
   */
  public int getFirstDisplayedResult()
  {
    return this.firstDisplayedResult;
  }

  /**
   * Returns the number of the last page displayed in 
   * the middle group of page links.  The middle group
   * of page links are the ones surrounding the current 
   * page.
   * 
   * @return the number of the last page displayed in 
   *    the middle group of page links
   */
  public int getLastDisplayedPage()
  {
    return this.lastDisplayedPage;
  }

  /**
   * Returns the number of the last item displayed on 
   * the current page.  
   * 
   * @return the number of the last item displayed on 
   *    the current page
   */
  public int getLastDisplayedResult()
  {
    return this.lastDisplayedResult;
  }

  /**
   * Returns the total number of pages in the paging
   * control.  
   * 
   * @return the total number of pages in the paging
   *    control
   */
  public int getPageCount()
  {
    return this.pageCount;
  }
  
  /**
   * Returns <tt>true</tt> if a link to the next page 
   * is displayed.  This will be the case whenever 
   * the current page is not the last page.
   * 
   * @return <tt>true</tt> if a link to the next page 
   *    is displayed
   */
  public boolean isNextPageDisplayed()
  {
    return this.nextPageDisplayed;
  }

  /**
   * Returns <tt>true</tt> if a link to the previous page 
   * is displayed.  This will be the case whenever 
   * the current page is not the first page.
   * 
   * @return <tt>true</tt> if a link to the previous page 
   *    is displayed
   */
  public boolean isPreviousPageDisplayed()
  {
    return this.previousPageDisplayed;
  }

  /**
   * Returns the number of the last page in the beginning 
   * group of pages, the beginning group being the one that 
   * contains the first page.
   * 
   * @return the number of the last page in the beginning 
   *    group of pages
   */
  public int getLastPageAtBeginning()
  {
    return this.lastPageAtBeginning;
  }

  /**
   * Returns the number of the first page in the ending 
   * group of pages, the ending group being the one that 
   * contains the last page.
   * 
   * @return the number of the first page in the ending 
   *    group of pages
   */
  public int getFirstPageAtEnd()
  {
    return this.firstPageAtEnd;
  }

  /**
   * Returns <tt>true</tt> if the first ellipsis, that is,
   * the one separating the first end group from the middle
   * group, is displayed.
   * 
   * @return <tt>true</tt> if the first ellipsis is displayed
   */
  public boolean isFirstEllipsisDisplayed()
  {
    return this.firstEllipsisDisplayed;
  }

  /**
   * Returns <tt>true</tt> if the last ellipsis, that is,
   * the one separating the last end group from the middle
   * group, is displayed.
   * 
   * @return <tt>true</tt> if the last ellipsis is displayed
   */
  public boolean isLastEllipsisDisplayed()
  {
    return this.lastEllipsisDisplayed;
  }
}
