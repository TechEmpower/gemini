package com.techempower.gemini.jaxrs.core;

import com.techempower.gemini.Context;

import java.util.List;

public class JaxRsDispatcher
{
  private List<Object> resources;

  public void register(Object instance) {

  }

  // TODO: Not use Gemini's Context eventually
  public void dispatch(Context context) {
    // Steps:
    // 1. Find the target resource
    // 2. Determine what is "in front" of that resource
    // 3. Dispatch to those things
    // 4. Dispatch to the resource if necessary
    // 5. Handle return value of resource if response not already complete
    //    based on annotations of the target resource.


    // To find the target resource:
    // - Regular Expressions are supported in the path variables
    //   - By default, those path variables can be simplified as index-based,
    //     because the non-regex form is equivalent to splitting the URI by the
    //     slashes then using the relative index.


    // Approach:
    //   At startup, tokenize all the endpoints from any resource.
    //   To do so, break up every endpoint path into the tokens:
    //   PATH := SLASH? ( BLOCK SLASH? )*
    //   SLASH := "/"
    //   BLOCK := WORD | VARIABLE
    //   WORD := [^/]+
    //   VARIABLE := "{" \s* NAME \s* ( ":" \s* REGEX \s* )? "}"
    //   NAME := \w[\w\.-]*
    //   REGEX := ( [^{}] | "{" [^{}]* "}" )*
    //
    // Place each BLOCK into a block class. There should be "word" blocks,
    // "pure variable" blocks, and "regex variable" blocks. "word" blocks match
    // a static word, "variable" blocks match a variable that does not have a
    // regex, and "regex variable" blocks match variables with a regex. Each
    // block should be given the information relevant to it, as well as its
    // children.
    //
    // To dispatch a URI:
    // 1) Consider the current block to be a general "root" block.
    // 2) Split the URI by "/".
    // 3) Consider the current index to be 0.
    // 4) At the current index:
    //        4..1) For each of the following, if the matching block is a
    //              method, also check to see if the block matches the relevant
    //              http method.
    //     4.1) Check if there is a matching "word" block. If so, move to it
    //          and continue to step 5.
    //     4.2) Check if there is a "pure variable" child block. If so, move to
    //          it and continue to step 5.
    //     4.3) Check if there is a matching "regex variable" child block. To
    //          do so:
    //          4.3.1) Join the path URI segments at and after the current
    //                 index then apply the regex to that.
    //          4.3.2) If it matches, remove the first matching group from the
    //                 front of the joined subset of the URI, then restart from
    //                 step 2 starting from this block with the new sub-URI as
    //                 the URI mentioned in that step.
    //     4.4) If no match has been found, this block does have not have a
    //          match.
    // 5) If the current index not the last in the split URI, increment the
    //    current index by 1, consider the block from step 4 as the current
    //    block, and return to step 4. If the current index is the last in the
    //    split URI, check if the current block represents a method. If it
    //    does, the method is the method to dispatch to, using the chain of
    //    blocks used to find this method. If it does not, the current block
    //    does not have a match.
    // 6) If a method was found, use the chain of blocks used to find the
    //    method to populate the path params (if any) required by the method
    //    and any interceptors.


    // Once a method has been found, determine the interceptors relevant to it,
    // which likely should be stored in a map by each handler instance/class,
    // and run each of those in the appropriate order. If the handler method
    // should still be dispatched to, dispatch to it. The parameters the
    // handler requires should also be cached, likely in a class that wraps the
    // method and stores all information relevant about how to both call it and
    // handle the return value. Check for thrown exceptions and if any are
    // present then apply any necessary ExceptionMappers. Then check to see if
    // the response has already been sent before attempting to handle it.
  }
}
