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

package com.techempower.collection;

import java.util.*;

import com.esotericsoftware.reflectasm.*;
import com.techempower.helper.*;

/**
 * ReflectiveComparator is an implementation of the java.util.Comparator
 * interface that compares objects based on an individual field contained
 * within each object.  The fields should be declared as "public" and must
 * also be Comparable (String, Integer, and other simple Objects are
 * Comparable.  Custom objects used as fields will need to implement the
 * Comparable interface).
 *    <p>
 * Reflection is used to access the value of the fields.
 */
public class ReflectiveComparator<O extends Object>
  implements Comparator<O>
{

  //
  // Constants.
  //

  public static final int BY_FIELD   = 1;
  public static final int BY_METHOD  = 2;

  public static final int ASCENDING  = 0;
  public static final int DESCENDING = 1;

  /**
   * Ascending return values.
   */
  private static final int[] ASCENDING_RETURNS = new int[] { -1, 1 };

  /**
   * Descending return values.
   */
  private static final int[] DESCENDING_RETURNS = new int[] { 1, -1 };

  /**
   * Arrays for providing no parameters to methods.
   */
  private static final Class<?>[]  NO_PARAMETER_LIST = new Class[0];
  private static final Object[]    NO_ARGUMENTS      = new Object[0];

  //
  // Member variables.
  //

  private String  fieldName;
  private String  methodName;
  //Field   field                    = null;
  private FieldAccess  fieldAccess         = null;
  private int     fieldIndex               = 0;
  //Method  method                   = null;
  private MethodAccess methodAccess        = null;
  private int     methodIndex              = 0;
  private int     comparisonType           = BY_FIELD;
  private int     ordering                 = ASCENDING;
  private boolean ignoreCaseOfStrings      = false;
  private boolean goodState                = true;
  
  private Map<String,String> memberAliases = null;

  //
  // Member methods.
  //

  /**
   * Full constructor.  Takes a Class instead of an example object.
   * Specifies the sort order (ASCENDING or DESCENDING).
   *
   * @param memberName the object's field name to use as a basis for
   *                  comparison.
   * @param theClass the Class object representing objects of the type
   *                 to be compared.
   * @param comparisonType either BY_FIELD or BY_METHOD, if by method,
   *                       the memberName is treated as a method name.
   * @param ordering either ASCENDING or DESCENDING; sets the sort order.
   */
  public ReflectiveComparator(String memberName, Class<?> theClass,
    int comparisonType, int ordering)
  {
    setMemberName(memberName, comparisonType);
    setOrdering(ordering);
    cacheMemberObject(theClass);
  }

  /**
   * Full constructor plus aliases.  Takes a Class instead of an example object.
   * Specifies the sort order (ASCENDING or DESCENDING) and aliases to use.
   *
   * @param memberName the object's field name to use as a basis for
   *                  comparison.
   * @param theClass the Class object representing objects of the type
   *                 to be compared.
   * @param comparisonType either BY_FIELD or BY_METHOD, if by method,
   *                       the memberName is treated as a method name.
   * @param ordering either ASCENDING or DESCENDING; sets the sort order.
   * @param aliases aliases that can be used for member names.
   * @param memberNames the names that correspond to the aliases.
   */
  public ReflectiveComparator(String memberName, Class<?> theClass,
    int comparisonType, int ordering, String[] aliases, String[] memberNames)
  {
    bindAliases(aliases, memberNames);
    setMemberName(memberName, comparisonType);
    setOrdering(ordering);
    cacheMemberObject(theClass);
  }

  /**
   * Full constructor plus aliases.  Takes a Class instead of an example object.
   * Specifies the sort order (ASCENDING or DESCENDING) and aliases to use.
   *
   * @param memberName the object's field name to use as a basis for
   *                  comparison.
   * @param theClass the Class object representing objects of the type
   *                 to be compared.
   * @param comparisonType either BY_FIELD or BY_METHOD, if by method,
   *                       the memberName is treated as a method name.
   * @param ordering either ASCENDING or DESCENDING; sets the sort order.
   * @param aliases aliases that can be used for member names.
   */
  public ReflectiveComparator(String memberName, Class<?> theClass,
    int comparisonType, int ordering, Map<String,String> aliases)
  {
    bindAliases(aliases);
    setMemberName(memberName, comparisonType);
    setOrdering(ordering);
    cacheMemberObject(theClass);
  }

  /**
   * Constructor.  Takes a Class instead of an example object.  Assumes
   * an ASCENDING sort order.
   *
   * @param memberName the object's field name to use as a basis for
   *                  comparison.
   * @param theClass the Class object representing objects of the type
   *                 to be compared.
   * @param comparisonType either BY_FIELD or BY_METHOD, if by method,
   *                       the memberName is treated as a method name.
   */
  public ReflectiveComparator(String memberName, Class<?> theClass,
    int comparisonType)
  {
    this(memberName, theClass, comparisonType, ASCENDING);
  }

  /**
   * Constructor.  Takes an example object.  Assumes an ASCENDING sort
   * order.
   *
   * @param memberName the object's field name to use as a basis for
   *                  comparison.
   * @param exampleObject an example object of the type to be compared.
   * @param comparisonType either BY_FIELD or BY_METHOD, if by method,
   *                       the memberName is treated as a method name.
   */
  public ReflectiveComparator(String memberName, Object exampleObject,
    int comparisonType)
  {
    this(memberName, exampleObject.getClass(), comparisonType, ASCENDING);
  }

  /**
   * Constructor.  This Constructor performs a lazy configuration.  The
   * Comparator will cache a reference to the Field to be compared
   * on the first call to compare().  Assumes an ASCENDING sort order.
   *
   * @param memberName the object's field name to use as a basis for
   *                  comparison.
   * @param comparisonType either BY_FIELD or BY_METHOD, if by method,
   *                       the memberName is treated as a method name.
   */
  public ReflectiveComparator(String memberName, int comparisonType)
  {
    setMemberName(memberName, comparisonType);
  }

  /**
   * Constructor.  This Constructor performs a lazy configuration.  The
   * Comparator will cache a reference to the Field to be compared
   * on the first call to compare().  Assumes an ASCENDING sort order.
   *
   * @param memberName the object's field name to use as a basis for
   *                  comparison.
   * @param comparisonType either BY_FIELD or BY_METHOD, if by method,
   *                       the memberName is treated as a method name.
   * @param ordering either ASCENDING or DESCENDING; sets the sort order.
   */
  public ReflectiveComparator(String memberName, int comparisonType,
    int ordering)
  {
    setMemberName(memberName, comparisonType);
    setOrdering(ordering);
  }

  /**
   * Constructor.  Takes a Class instead of an example object.
   * Defaults to BY_FIELD comparison.  Assumes an ASCENDING sort
   * order.
   *
   * @param memberName the object's field name to use as a basis for
   *                  comparison.
   * @param theClass the Class object representing objects of the type
   *                 to be compared.
   */
  public ReflectiveComparator(String memberName, Class<?> theClass)
  {
    this(memberName, theClass, BY_FIELD, ASCENDING);
  }

  /**
   * Constructor.  Takes an example object.  Defaults to BY_FIELD
   * comparison.  Assumes an ASCENDING sort order.
   *
   * @param memberName the object's field name to use as a basis for
   *                  comparison.
   * @param exampleObject an example object of the type to be compared.
   */
  public ReflectiveComparator(String memberName, Object exampleObject)
  {
    this(memberName, exampleObject.getClass(), BY_FIELD, ASCENDING);
  }

  /**
   * Constructor.  This Constructor performs a lazy configuration.  The
   * Comparator will cache a reference to the Field to be compared
   * on the first call to compare().  Defaults to BY_FIELD comparison.
   * Assumes an ASCENDING sort order.
   *
   * @param memberName the object's field name to use as a basis for
   *                  comparison.
   */
  public ReflectiveComparator(String memberName)
  {
    this(memberName, BY_FIELD);
  }

  /**
   * Sets whether or not String case should be ignored during comparisons.
   * This flag has no effect when the Comparable objects being compared are
   * not Strings.
   */
  public void setIgnoreCase(boolean ignoreCaseOfStrings)
  {
    this.ignoreCaseOfStrings = ignoreCaseOfStrings;
  }

  /**
   * Sets the ordering (ASCENDING or DESCENDING)
   *
   * @param ordering either ASCENDING or DESCENDING.
   */
  public void setOrdering(int ordering)
  {
    if ( (ordering == ASCENDING)
      || (ordering == DESCENDING)
      )
    {
      this.ordering = ordering;
    }
  }

  /**
   * Sets the field or method name to compare on.
   *
   * @param memberName the name of the variable (field) or method to call to
   *        use for sorting.
   * @param comparisonType specified BY_METHOD or BY_FIELD.
   */
  public void setMemberName(String memberName, int comparisonType)
  {
    // check for an alias
    String aliasedMemberName = this.checkAlias(memberName);

    this.comparisonType = comparisonType;

    // It is important to make sure that the state is in fact changing so that
    // the cached members are not unnecessarily cleared.
    if (comparisonType == BY_METHOD && !Objects.equals(this.methodName, aliasedMemberName))
    {
      this.methodName = aliasedMemberName;

      // Nullify the cached Method object.
      //this.method = null;
      this.methodAccess = null;
    }
    else if (!Objects.equals(this.fieldName, aliasedMemberName))
    {
      this.fieldName = aliasedMemberName;

      // Nullify the cached Field object.
      //this.field = null;
      this.fieldAccess = null;
    }
  }

  /**
   * Caches the Field or Method object for the field name specified.
   */
  protected void cacheMemberObject(Class<?> theClass)
  {
    if (this.comparisonType == BY_METHOD)
    {
      this.methodAccess = MethodAccess.get(theClass);
      this.methodIndex = this.methodAccess.getIndex(this.methodName, NO_PARAMETER_LIST);
      
      /*
      try
      {
        // Try first to find a declared method by the name provided.
        this.method = theClass.getDeclaredMethod(this.methodName, NO_PARAMETER_LIST);
      }
      catch (NoSuchMethodException exc)
      {
        // If the class in question does not declare the method, we will
        // need to look for a public method of the same name.  This will
        // catch methods that exist in the superclass.
        this.method = theClass.getMethod(this.methodName, NO_PARAMETER_LIST);
      }
      */
    }
    else
    {
      this.fieldAccess = FieldAccess.get(theClass);
      this.fieldIndex = this.fieldAccess.getIndex(this.fieldName);
      
      /*
      try
      {
        // Try first to find a declared field by the name provided.
        this.field = theClass.getDeclaredField(this.fieldName);
      }
      catch (NoSuchFieldException exc)
      {
        // If the class in question does not declare the field, we will
        // need to look for a public field of the same name.  This will
        // catch fields that exist in the superclass.
        this.field = theClass.getField(this.fieldName);
      }
      */
    }
  }

  /**
   * Compares two objects for order.  Uses Java Reflection to access the
   * field (provided by the constructor to this Comparator) that is the
   * basis of the comparison.
   *
   * @param object1 The first object to compare.
   * @param object2 The second object to compare.
   */
  @Override
  public int compare(Object object1, Object object2)
  {
    if (  (  (this.comparisonType == BY_METHOD) 
          && (this.methodAccess == null)
          )
       || (  (this.comparisonType == BY_FIELD) 
          && (this.fieldAccess == null)
          )
       )
    {
      try
      {
        cacheMemberObject(object1.getClass());
      }
      catch (Exception exc)
      {
        System.out.println("ReflectiveComparator: " + exc);

        // Set the state to bad.
        this.goodState = false;
      }
    }

    // Only proceed if the state of this ReflectiveComparator is still good.
    if (this.goodState)
    {
      /*
      try
      {
      */
      Object comparable1;
      Object comparable2;

      // Invoke methods if specified.
      if (this.comparisonType == BY_METHOD)
      {
        //comparable1 = this.method.invoke(object1, NO_ARGUMENTS);
        //comparable2 = this.method.invoke(object2, NO_ARGUMENTS);
        comparable1 = this.methodAccess.invoke(object1, this.methodIndex, NO_ARGUMENTS);
        comparable2 = this.methodAccess.invoke(object2, this.methodIndex, NO_ARGUMENTS);
      }

      // Otherwise read fields directly.
      else
      {
        //comparable1 = this.field.get(object1);
        //comparable2 = this.field.get(object2);
        comparable1 = this.fieldAccess.get(object1, this.fieldIndex);
        comparable2 = this.fieldAccess.get(object2, this.fieldIndex);
      }

      // Return the comparison.
      return compareTwoObjects(comparable1, comparable2);
      /*
      }
      catch (IllegalAccessException iaexc)
      {
        System.out.println("ReflectiveComparator: Illegal access exception during compare!");
      }
      catch (InvocationTargetException itexc)
      {
        System.out.println("ReflectiveComparator: InvocationTargetException during compare!");
      }
      */
    }
    else
    {
      // Bad state.  Tell the user about it.  Note that this is not something
      // that should ever come up in production because these sorts of issues
      // should be fully discovered during testing (e.g., bad method/field
      // names provided to the RC).
      System.out.println("ReflectiveComparator: State is corrupt.  See previous error.");
    }

    // Worst case scenario.  We don't know the comparison type.  In this
    // case, the sort cannot work.
    return 0;
  }

  /**
   * Compares two objects and returns a value compliant with the compareTo
   * method.
   */
  @SuppressWarnings("unchecked")
  public int compareTwoObjects(Object comparable1, Object comparable2)
  {
    if ((comparable1 == null) && (comparable2 == null))
    {
      return 0;
    }
    else if (comparable1 == null)
    {
      return DESCENDING_RETURNS[this.ordering];
    }
    else if (comparable2 == null)
    {
      return ASCENDING_RETURNS[this.ordering];
    }
    // Nothing is null, proceed with the real comparisons.
    else
    {
      // Handle the String-only ignore-case scenario.
      if (this.ignoreCaseOfStrings)
      {
        // Bypass this scenario if we're not dealing with Strings.
        if ( (comparable1 instanceof String)
          && (comparable2 instanceof String)
          )
        {
          String string1 = (String)comparable1;
          String string2 = (String)comparable2;
          if (this.ordering == ASCENDING)
          {
            return string1.compareToIgnoreCase(string2);
          }
          else
          {
            return -(string1.compareToIgnoreCase(string2));
          }
        }
      }

      // Handle the regular scenario.
      if (this.ordering == ASCENDING)
      {
        return ((Comparable<Object>)comparable1).compareTo(comparable2);
      }
      else
      {
        return -(((Comparable<Object>)comparable1).compareTo(comparable2));
      }
    }
  }

  /**
   * Binds the given aliases to the given member names.
   * Aliases will only take effect the next time the memberName is set.
   *
   * @param aliases aliases that can be used for member names.
   * @param memberNames the names that correspond to the aliases.
   */
  public void bindAliases(String[] aliases, String[] memberNames)
  {
    Map<String,String> newMemberAliases = new HashMap<>(aliases.length);

    for (int i = 0; i < aliases.length && i < memberNames.length; i++)
    {
      newMemberAliases.put(aliases[i], memberNames[i]);
    }

    this.bindAliases(newMemberAliases);
  }

  /**
   * Binds the given aliases.
   * Aliases will only take effect the next time the memberName is set.
   *
   * @param aliases aliases that can be used for member names.
   */
  public void bindAliases(Map<String,String> aliases)
  {
    if (CollectionHelper.isEmpty(aliases))
    {
      this.memberAliases = null;
    }
    this.memberAliases = aliases;
  }

  /**
   * Checks the given name to see if it is an alias.
   * If so the actual name will be returned, otherwise the given
   * value will pass through.
   */
  protected String checkAlias(String name)
  {
    if (this.memberAliases != null)
    {
      String actualName = this.memberAliases.get(name);

      if (actualName != null)
      {
        return actualName;
      }
    }

    return name;
  }

}   // End ReflectiveComparator.
