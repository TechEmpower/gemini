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
package com.techempower.data;

import java.util.*;

/**
 * An extremely trivial implementation of Iterator that allows you to use
 * a DatabaseConnector (with a result-set already prepared) in Iterator-like 
 * loops.  The "next" operation will move to the next row in a result-set
 * and always return a reference to the DatabaseConnector itself.
 *   <p>
 * Note that because the Java ResultSet object doesn't work quite the same
 * as a standard Iterator (you can't know you're at the last item until
 * you've past it), this Iterator is implemented in a way that is fairly
 * strict.  It actually moves the ResultSet cursor forward in hasNext, so
 * be sure to only call that once per loop.  That is the typical pattern by
 * which Iterators are used, but it's not consistent with the understanding
 * that "hasNext" is a harmless operation.
 */
public class DatabaseConnectorIterator
  implements Iterator<DatabaseConnector>
{
  
  //
  // Member variables.
  //
  
  private final DatabaseConnector dbConn;
  private boolean absorbedInitialNext = false;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public DatabaseConnectorIterator(DatabaseConnector dbConn)
  {
    this.dbConn = dbConn;
  }
  
  /**
   * Implementation of Iterator.next interface method.
   */
  @Override
  public DatabaseConnector next()
  {
    return this.dbConn;
  }
  
  /**
   * Implementation of Iterator.hasNext interface method.
   */
  @Override
  public boolean hasNext()
  {
    if (this.absorbedInitialNext)
    {
      this.dbConn.next();
    }
    else
    {
      this.absorbedInitialNext = true;
    }
    return this.dbConn.more();
  }
  
  /**
   * Implementation of Iterator.remove interface method.
   */
  @Override
  public void remove()
  {
    throw new UnsupportedOperationException("remove method not supported by DatabaseConnectorIterator.");
  }
  
}  // End DatabaseConnectorIterator.
