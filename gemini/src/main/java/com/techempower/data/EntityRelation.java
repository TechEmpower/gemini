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

import gnu.trove.set.*;

import java.util.*;

import com.techempower.cache.*;
import com.techempower.collection.relation.*;
import com.techempower.util.*;

/**
 * A relation between data entities.  In a database, this would most likely be
 * represented as a table with two bigint columns, each one containing
 * identities for a certain kind of data entity.
 *
 * @param <L> the type of the left values in this relation
 * @param <R> the type of the right values in this relation
 */
public interface EntityRelation<L extends Identifiable, R extends Identifiable>
{
  /**
   * Adds the specified pair to the relation and updates the database.
   *
   * @param leftID the ID of the left value of the pair to be added
   * @param rightID the ID of the right value of the pair to be added
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  boolean add(long leftID, long rightID);

  /**
   * Adds the specified pair to the relation and updates the database.
   *
   * @param leftID the ID of the left value of the pair to be added
   * @param right the right value of the pair to be added
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  boolean add(long leftID, R right);

  /**
   * Adds the specified pair to the relation.
   *
   * @param left the left value of the pair to be added
   * @param rightID the ID of the right value of the pair to be added
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  boolean add(L left, long rightID);

  /**
   * Adds the given pair to the relation and updates the database.
   *
   * @param left the left value of the pair to be added
   * @param right the right value of the pair to be added
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  boolean add(L left, R right);

  /**
   * Adds the given pairs to the relation and updates the database.
   *
   * @param relationToAdd the pairs to be added
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  boolean addAll(LongRelation relationToAdd);

  /**
   * Clears the relation of all pairs and updates the database.
   */
  void clear();

  /**
   * Returns <tt>true</tt> if this relation contains the specified pair.
   *
   * @param leftID the id of the left value of the pair to look for
   * @param rightID the id of the right value of the pair to look for
   * @return <tt>true</tt> if this relation contains the specified pair
   */
  boolean contains(long leftID, long rightID);

  /**
   * Returns <tt>true</tt> if this relation contains the specified pair.
   *
   * @param leftID the id of the left value of the pair to look for
   * @param right the right value of the pair to look for
   * @return <tt>true</tt> if this relation contains the specified pair
   */
  boolean contains(long leftID, R right);

  /**
   * Returns <tt>true</tt> if this relation contains the specified pair.
   *
   * @param left the left value of the pair to look for
   * @param rightID the id of the right value of the pair to look for
   * @return <tt>true</tt> if this relation contains the specified pair
   */
  boolean contains(L left, long rightID);

  /**
   * Returns <tt>true</tt> if this relation contains the given pair.
   *
   * @param left the left value of the pair to look for
   * @param right the right value of the pair to look for
   * @return <tt>true</tt> if this relation contains the specified pair
   */
  boolean contains(L left, R right);

  /**
   * Returns <tt>true</tt> if this relation contains the specified left
   * value.
   *
   * @param leftID the id of the left value to look for
   * @return <tt>true</tt> if this relation contains the specified left
   *         value
   */
  boolean containsLeftValue(long leftID);

  /**
   * Returns <tt>true</tt> if this relation contains the given left value.
   *
   * @param left the left value to look for
   * @return <tt>true</tt> if this relation contains the given left value
   */
  boolean containsLeftValue(L left);

  /**
   * Returns <tt>true</tt> if this relation contains the specified right
   * value.
   *
   * @param rightID the id of the right value to look for
   * @return <tt>true</tt> if this relation contains the specified right
   *         value
   */
  boolean containsRightValue(long rightID);

  /**
   * Returns <tt>true</tt> if this relation contains the given right value.
   *
   * @param right the right value to look for
   * @return <tt>true</tt> if this relation contains the given right value
   */
  boolean containsRightValue(R right);

  /**
   * Returns an array of left IDs associated with the given right value.
   *
   * @param right the right value whose associated left IDs are to be returned
   * @return an long[] of left IDs associated with the given right value.
   */
  long[] leftIDArray(R right);

  /**
   * Returns an array of left IDs associated with the specified right ID.
   *
   * @param rightID the ID of the right value whose associated left IDs are
   *                to be returned.
   * @return an long[] of left IDs associated with the specified right ID.
   */
  long[] leftIDArray(long rightID);

  /**
   * Returns the set of left IDs associated with the specified right value in
   * this relation.
   *
   * @param rightID the id of the right value whose associated left IDs are to
   *                be returned
   * @return the set of left IDs associated with the specified right value in
   *         this relation
   */
  Set<Long> leftIDs(long rightID);

  /**
   * Returns the set of left IDs associated with the given right value in this
   * relation.
   *
   * @param right the right value whose associated left IDs are to be returned
   * @return the set of left IDs associated with the given right value in this
   *         relation
   */
  Set<Long> leftIDs(R right);

  /**
   * Returns the number of left values associated with the specified right
   * value.
   */
  int leftSize(long rightID);

  /**
   * Returns the number of left values associated with the given right value.
   */
  int leftSize(R right);

  /**
   * Returns the type of the left values in this relation.
   *
   * @return the type of the left values in this relation
   */
  Class<L> leftType();

  /**
   * Returns the list of left values associated with the specified right value
   * in this relation.  The order of the values in the list is arbitrary.
   *
   * @param rightID the id of the right value whose associated left values are
   *                to be returned
   * @return the list of left values associated with the specified right value
   *         in this relation
   */
  List<L> leftValueList(long rightID);

  /**
   * Returns the list of left values associated with the given right value in
   * this relation.  The order of the values in the list is arbitrary.
   *
   * @param right the right value whose associated left values are to be
   *              returned
   * @return the list of left values associated with the given right value in
   *         this relation
   */
  List<L> leftValueList(R right);

  /**
   * Returns the set of left values associated with the specified right value in
   * this relation.
   *
   * @param rightID the id of the right value whose associated left values are
   *               to be returned
   * @return the set of left values associated with the specified right value in
   *         this relation
   */
  Set<L> leftValueSet(long rightID);

  /**
   * Returns the set of left values associated with the given right value in
   * this relation.
   *
   * @param right the right value whose associated left values are to be
   *              returned
   * @return the set of left values associated with the given right value in
   *         this relation
   */
  Set<L> leftValueSet(R right);

  /**
   * Returns a copy of the underlying long relation between left and right
   * IDs.  Modifications to the returned copy will not affect this relation.
   * <p>
   * Use this method if you wish to iterate over the leftID, rightID pairs
   * contained in this relation.
   *
   * @return a clone of the underlying long relation between left and right
   *         IDs
   */
  LongRelation relation();

  /**
   * Removes the specified pair of values from this relation and updates the
   * database.
   *
   * @param leftID the id of the left value of the pair to be removed
   * @param rightID the id of the right value of the pair to be removed
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  boolean remove(long leftID, long rightID);

  /**
   * Removes the specified pair of values from this relation and updates the
   * database.
   *
   * @param leftID the id of the left value of the pair to be removed
   * @param right the right value of the pair to be removed
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  boolean remove(long leftID, R right);

  /**
   * Removes the specified pair of values from this relation and updates the
   * database.
   *
   * @param left the left value of the pair to be removed
   * @param rightID the id of the right value of the pair to be removed
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  boolean remove(L left, long rightID);

  /**
   * Removes the given pair of values from this relation and updates the
   * database.
   *
   * @param left the left value of the pair to be removed
   * @param right the right value of the pair to be removed
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  boolean remove(L left, R right);

  /**
   * Removes the given pairs from the relation
   *
   * @param relationToRemove the pairs to be removed
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  boolean removeAll(LongRelation relationToRemove);

  /**
   * Conditionally removes the given object from the relation if the object's
   * type is the relation's left or right type.
   *
   * @param object the object to be removed
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  <T extends Identifiable> boolean removeEntity(T object);

  /**
   * Conditionally removes the specified object from the relation if the given
   * type is the relation's left or right type.
   *
   * @param type the type of object to be removed
   * @param idToRemove the id of the object to be removed
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  <T extends Identifiable> boolean removeEntity(Class<T> type, long idToRemove);

  /**
   * Removes the specified left value from this relation and updates the
   * database.  This removes all rows with the specified left value.
   *
   * @param leftID the id of the left value to be removed from this relation
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  boolean removeLeftValue(long leftID);

  /**
   * Removes the given left value from this relation and updates the database.
   * This removes all rows with the specific left value.
   *
   * @param left the left value to be removed from this relation
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  boolean removeLeftValue(L left);

  /**
   * Removes the specified right value from this relation and updates the
   * database.  This removes all rows with the specified right value.
   *
   * @param rightID the id of the right value to be removed from this
   *                relation
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  boolean removeRightValue(long rightID);

  /**
   * Removes the given right value from this relation and updates the database.
   * This removes all rows with the specified right value.
   *
   * @param right the right value to be removed from this relation
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  boolean removeRightValue(R right);

  /**
   * <p>Clears the existing relation, then sets the relation to the passed in
   * relation.</p>
   *
   * <p>Note that this is generally preferable to doing the following:</p>
   *
   * <pre>
   * {@code
   * // foo is an EntityRelation.
   * foo.clear();
   * foo.addAll( .. );
   * }
   * </pre>
   *
   * <p>The above will cause foo to be empty for a certain window of time.
   * Using replaceAll( .. ) will achieve the same end goal of clearing the
   * current relation and then adding the passed in relation, but will never
   * result in a call to this object seeing an empty relation.</p>
   *
   * <p>This call will block until the database writes are completed.</p>
   *
   * @param relationToReplace the pairs to be added after clearing the current
   *                          relation.
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  boolean replaceAll(LongRelation relationToReplace);

  /**
   * Returns an array of right IDs associated with the given left value.
   *
   * @param left the left value whose associated right IDs are to be returned
   * @return an long[] of right IDs associated with the given left value.
   */
  long[] rightIDArray(L left);

  /**
   * Returns an array of right IDs associated with the specified left ID.
   *
   * @param leftID the ID of the left value whose associated right IDs are
   *               to be returned.
   * @return a long[] of right IDs associated with the specified left ID.
   */
  long[] rightIDArray(long leftID);

  /**
   * A copy of rightIDArray that returns a long set instead.
   *
   * @param leftID the ID of the left value whose associated right IDs are
   *               to be returned.
   * @return a long set of right IDs associated with the specified left ID.
   */
  TLongSet rightIDsLongSet(long leftID);

  /**
   * Returns the set of right IDs associated with the specified left value in
   * this relation.
   *
   * @param leftID the id of the left value whose associated right IDs are to be
   *               returned
   * @return the set of right IDs associated with the specified left value in
   *         this relation
   */
  Set<Long> rightIDs(long leftID);

  /**
   * Returns the set of right IDs associated with the given left value in this
   * relation.
   *
   * @param left the left value whose associated right IDs are to be returned
   * @return the set of right IDs associated with the given left value in this
   *         relation
   */
  Set<Long> rightIDs(L left);

  /**
   * A copy of rightIDs() that returns a LongSet instead.
   *
   * @param left the left value whose associated right IDs are to be returned
   * @return the set of right IDs associated with the given left value in this
   *         relation
   */
  TLongSet rightIDsLongSet(L left);

  /**
   * Returns the number of right values associated with the specified left
   * value.
   */
  int rightSize(long leftID);

  /**
   * Returns the number of right values associated with the given left value.
   */
  int rightSize(L left);

  /**
   * Returns the number of right values associated with the given left value.
   *
   * @param filterRightIds Only right values contained in this will be
   *                       counted. All are counted if this is null or empty.
   */
  int rightSize(L left, Collection<Long> filterRightIds);

  /**
   * Returns the number of right values associated with the given left value.
   *
   * @param filterRightIds Only right values contained in this will be
   *                       counted. All are counted if this is null or empty.
   */
  int rightSize(L left, TLongSet filterRightIds);

  /**
   * Returns the number of right values associated with the given left value.
   *
   * @param filterRightIds Only right values contained in this will be
   *                       counted. All are counted if this is null or empty.
   */
  int rightSize(long leftID, Collection<Long> filterRightIds);

  /**
   * Returns the number of right values associated with the given left value.
   *
   * @param filterRightIds Only right values contained in this will be
   *                       counted. All are counted if this is null or empty.
   */
  int rightSize(long leftID, TLongSet filterRightIds);

  /**
   * Returns the type of the right values in this relation.
   *
   * @return the type of the right values in this relation
   */
  Class<R> rightType();

  /**
   * Returns the list of right values associated with the specified left value
   * in this relation.  The order of the values in the list is arbitrary.
   *
   * @param leftID the id of the left value whose associated right values are to
   *               be returned
   * @return the list of right values associated with the specified left value
   *         in this relation
   */
  List<R> rightValueList(long leftID);

  /**
   * Returns the list of right values associated with the given left value in
   * this relation.  The order of the values in the list is arbitrary.
   *
   * @param left the left value whose associated right values are to be returned
   * @return the list of right values associated with the given left value in
   *         this relation
   */
  List<R> rightValueList(L left);

  /**
   * Returns the set of right values associated with the specified left value in
   * this relation.
   *
   * @param leftID the id of the left value whose associated right values are to
   *               be returned
   * @return the set of right values associated with the specified left value in
   *         this relation
   */
  Set<R> rightValueSet(long leftID);

  /**
   * Returns the set of right values associated with the given left value in
   * this relation.
   *
   * @param left the left value whose associated right values are to be returned
   * @return the set of right values associated with the given left value in
   *         this relation
   */
  Set<R> rightValueSet(L left);

  /**
   * Returns the number of pairs in this relation.
   *
   * @return the number of pairs in this relation
   */
  int size();

  /**
   * Returns the identifier for the source of this relation in the data store.
   * In a SQL database, this would be a SQL table name.  This value should
   * never be {@code null}.
   *
   * @return the identifier for the source of this relation
   */
  String tableName();

  /**
   * Creates new instances of {@link EntityRelation}.
   *
   * @param <L> the type of the left values in the relation
   * @param <R> the type of the right values in the relation
   * @param <E> the type of relation this builder produces
   */
  interface Builder<L extends Identifiable, R extends Identifiable, E extends EntityRelation<L,R>>
  {
    /**
     * Returns a new {@link EntityRelation} with parameters set by the builder.
     */
    E build(EntityStore store);
  }
}
