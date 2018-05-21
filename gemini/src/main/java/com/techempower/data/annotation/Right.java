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
package com.techempower.data.annotation;

import java.lang.annotation.*;

/**
 * This annotation specifies the "right" class of a CachedRelation. 
 * <p>
 * <pre>
 *   &#064;Relation
 *   public class UsersToGroups
 *   {
 *     &#064;Left
 *     public User user;
 *     
 *     &#064;Right
 *     public Group group;
 *   }
 * </pre>
 * <p>
 * The field name, in this case "group" will be used to specify the column name, if your
 * column name in the database is GroupId, then your class would look like this
 * <p>
 * <pre>
 *   &#064;Relation
 *   public class UsersToGroups
 *   {
 *     &#064;Left
 *     public User user;
 *     
 *     &#064;Right
 *     public Group GroupId;
 *   }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Right
{
    /**
     * The database column that stores the identifier for the related entity.
     *
     * @return the database column that stores the identifier for the related
     * entity
     * @see com.techempower.cache.CachedRelation.Builder#rightColumn(String)
     */
    String column() default "";
}
