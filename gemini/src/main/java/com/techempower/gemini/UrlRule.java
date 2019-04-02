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

package com.techempower.gemini;

import java.util.*;
import java.util.regex.*;

import com.techempower.helper.*;

/**
 * A single URL rewrite rule.  This is the internal representation of the rules
 * used by {@code UrlRewriter} and {@code PrettyUrlRewriter}.
 */
public class UrlRule
{
  private List<UrlParam> params;
  private String         originalRule;
  private String         processedRule;
  private String         ruleTemplate;
  private String         command;

  public UrlRule(String rule, String command)
  {
    this.command = command;
    this.originalRule = rule;
    this.params = new ArrayList<>();
    processRule();
  }

  /*
   * Known Issues: 
   * - regex sub groups, i.e. (a(c)) is not supported.
   */
  private void processRule()
  {
    // set the processed version of the rule by taking out the
    // named parameter parts that are not valid in a regex
    // we will use this processed version to process incoming
    // urls

    this.processedRule = this.originalRule.replaceAll("\\(\\?<.+?>", "(");

    // test the processed rule to make sure it's a valid regex
    try
    {
      Pattern.compile(this.processedRule);
    }
    catch (PatternSyntaxException e)
    {
      throw new IllegalArgumentException("Invalid regex: "
          + this.originalRule);
    }

    // look for any named parameters. Named parameters look like
    // (?<paramName>//d+). FOr each parameter that's found, create a
    // new UrlParam and add it to our list of params. Also, change the
    // ruleTemplate so that "(?<paramName>//d+)" now looks like
    // "<paramName>". we'll use the template to do some string replacement
    // when constructing a url from a given set of parameters
    this.ruleTemplate = this.originalRule;

    // This regex will match any named parameter in the form of
    // (?<paramName>//d+)
    // Pattern p = Pattern.compile("(\\(.+?\\))");
    Pattern p = Pattern.compile("(\\(\\?<.+?>.+?\\))");
    Matcher m = p.matcher(this.originalRule);
    while (m.find())
    {
      // m.find() will find the next named parameter in the rule and set it as
      // group #1
      if (m.groupCount() > 0)
      {
        String group = m.group(1);
        // let's make certain that the matched string starts with what we
        // expect
        if (group.startsWith("(?<"))
        {
          // lets extract the name, this regex will match "paramName" from
          // (?<paramName>regex)
          Pattern named = Pattern.compile("<(.+)>");
          Matcher mNamed = named.matcher(group);

          if (mNamed.find() && mNamed.groupCount() > 0)
          {
            String name = mNamed.group(1);
            if (StringHelper.isEmptyTrimmed(name))
            {
              // this happens if the syntax looked like (?<>regex)
              // we don't allow for this, so we'll send an exception
              throw new IllegalArgumentException(
                  "Named Parameters must have a non-empty name: " + group);
            }

            // create our UrlParam
            // first we need to extract the regex for this param
            String regex = group.substring(group.indexOf('>') + 1,
                group.length() - 1);
            this.params.add(new UrlParam(name, regex));

            // format the rule template
            this.ruleTemplate = this.ruleTemplate.replaceFirst(
                Pattern.quote(group), "<" + name + ">");
          }
        }
      }
    }
  }

  /**
   * Returns true if a url matches this rule
   */
  public boolean matches(String url)
  {
    Pattern p = Pattern.compile(this.processedRule);
    Matcher m = p.matcher(url);
    return m.matches();
  }

  /**
   * Returns true if the supplied command and params match this rule. If true,
   * UrlRule.toUrl can be used.
   */
  public boolean matches(Map<String, String> toReplace)
  {
    boolean matchingParams = true;
    for (UrlParam param : this.params)
    {
      for (Map.Entry<String, String> entry : toReplace.entrySet())
      {
        if (entry.getKey().equalsIgnoreCase(param.getName()))
        {
          if (!entry.getValue().matches(param.getRegex()))
          {
            matchingParams = false;
            break;
          }
        }
      }
    }

    return matchingParams;
  }

  public Map<String, String> parseParams(String url)
  {
    Map<String, String> toReturn = new HashMap<>();

    Pattern p = Pattern.compile(this.processedRule);
    Matcher m = p.matcher(url);
    if (m.matches())
    {
      for (int i = 1; i <= m.groupCount(); i++)
      {
        toReturn.put(this.params.get(i - 1).getName(), m.group(i));
      }
    }

    return toReturn;
  }

  public String toUrl(Map<String, String> parameters)
  {
    if (!matches(parameters))
    {
      return "/";
    }

    String url = "/" + this.ruleTemplate;

    for (Map.Entry<String, String> entry : parameters.entrySet())
    {
      url = url.replaceFirst("<" + entry.getKey() + ">", entry.getValue());
    }

    return url;
  }

  public String getCommand()
  {
    return this.command;
  }

  public void setCommand(String command)
  {
    this.command = command;
  }
  
  public String getRule()
  {
    return this.originalRule;
  }
  
  private class UrlParam
  {
    protected String name;
    protected String regex;

    public UrlParam(String name, String regex)
    {
      this.name = name;
      this.regex = regex;
    }

    public String getName()
    {
      return this.name;
    }

    public String getRegex()
    {
      return this.regex;
    }
  }

  public static void main(String[] args)
  {
    String rule = "Articles/(?<year>\\d+)/Sports-(?<section>\\d+)-(?<chapter>\\d*)/(?<author>[a-zA-Z]+)";

    UrlRule myrule = new UrlRule(rule, "cmd");

    String url = "Articles/1/Sports-12-/abc";

    Map<String, String> params = null;
    if (myrule.matches(url))
    {
      params = myrule.parseParams(url);
      for (Map.Entry<String, String> entry : params.entrySet())
      {
        System.out.println(entry.getKey() + ": " + entry.getValue());
      }
    }

    if (myrule.matches(params))
    {
      String converted = myrule.toUrl(params);

      System.out.println(converted);
      System.out.println(converted.equals(url));
    }
  }
}
