package com.techempower.gemini.jaxrs.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MediaTypeParserImpl
    implements MediaTypeParser
{
  private final String qValueKey;

  private static final String  WILDCARD = "*";
  private static final Pattern mediaTypePattern;
  private static final Pattern parametersPattern;

  static
  {
    var vCharRange = "-!#%&'*+.^`|~\\w$";
    var token = "[" + vCharRange + "]+";
    var obsTextRange = "\\x80-\\xFF";
    var qdText = "[\t \\x21\\x23-\\x5B\\x5D-\\x7E" + obsTextRange + "]";
    var quotedPair = "\\\\[\t " + vCharRange + obsTextRange + "]";
    var quotedStr = "\"((?:" + qdText + "|" + quotedPair + ")*)\"";
    var ows = "[ \t]*";

    // Group 1: key
    // Group 2: unquoted value
    // Group 3: quoted value
    parametersPattern = Pattern.compile(
        ows + ";" + ows + "(" + token + ")=(?:(" + token + ")|(" + quotedStr + "))");

    // Group 1: type
    // Group 2: subtype
    // Group 3: parameters
    mediaTypePattern = Pattern.compile(
        ",?(" + token + ")/(" + token + ")((" + parametersPattern.pattern() + ")*)");
  }

  MediaTypeParserImpl(String qValueKey)
  {
    this.qValueKey = qValueKey;
  }

  @Override
  public MediaTypeDataGroup parse(String mediaType)
  {
    // Immediately fail if a leading comma is present. For simplicity with
    // capturing multiple groups, the regex allows a leading comma, but this is
    // invalid for the first group.
    if (mediaType.charAt(0) == ',')
    {
      return new MediaTypeDataGroup(List.of());
    }
    Matcher mediaTypeMatcher = mediaTypePattern.matcher(mediaType);
    List<MediaTypeData> dataList = new ArrayList<>(1);
    int mediaTypeEnd = 0;
    while (mediaTypeMatcher.find())
    {
      if (mediaTypeEnd != mediaTypeMatcher.start())
      {
        throw new ProcessingException(String.format(
            "Could not fully parse media type \"%s\"," +
                " parsed up to position %s.",
            mediaType, mediaTypeEnd));
      }
      mediaTypeEnd = mediaTypeMatcher.end();
      String type = mediaTypeMatcher.group(1);
      String subtype = mediaTypeMatcher.group(2);
      if (type.equals(WILDCARD) && !subtype.equals(WILDCARD))
      {
        throw new ProcessingException(String.format(
            "Invalid type/subtype combination \"%s/%s\" in media" +
                " type \"%s\", type must be concrete if subtype is concrete.",
            type, subtype, mediaType));
      }
      double qValue = 1d;
      String parameters = mediaTypeMatcher.group(3);
      if (parameters != null)
      {
        Matcher parametersMatcher = parametersPattern.matcher(parameters);
        while (parametersMatcher.find())
        {
          String key = parametersMatcher.group(1);
          String unquotedValue = parametersMatcher.group(2);
          // quotedValue will be needed later for lazily-evaluated methods
          String quotedValue = parametersMatcher.group(3);
          if (unquotedValue == null && quotedValue == null)
          {
            break;
          }
          if (key.equalsIgnoreCase(qValueKey) && unquotedValue != null)
          {
            try
            {
              qValue = Double.parseDouble(unquotedValue);
            }
            catch (NumberFormatException e)
            {
              throw new ProcessingException(String.format(
                  "Invalid q-value \"%s\" in media type \"%s\"," +
                      " failed to parse number.", qValue, mediaType), e);
            }
            if (qValue * 1e4 % 10 != 0)
            {
              throw new ProcessingException(String.format(
                  "Invalid q-value \"%s\" in media type \"%s\"," +
                      " more than 3 decimal places were specified.",
                  qValue, mediaType));
            }
            if (qValue < 0 || qValue > 1)
            {
              throw new ProcessingException(String.format(
                  "Invalid q-value \"%s\" in media type \"%s\"," +
                      " q-value must be between 0 and 1, inclusive.",
                  qValue, mediaType));
            }
            break;
          }
        }
      }
      dataList.add(new MediaTypeData(type, subtype, qValue));
    }
    if (mediaTypeEnd != mediaType.length())
    {
      throw new ProcessingException(String.format(
          "Could not fully parse media type \"%s\"," +
              " parsed up to position %s.",
          mediaType, mediaTypeEnd));
    }
    return new MediaTypeDataGroup(dataList);
  }
}
