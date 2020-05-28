package com.techempower.gemini.jaxrs.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MediaTypeParserImpl
    implements MediaTypeParser
{
  private final String qValueKey;

  private static final CharSpan WILDCARD = new CharSpan("*");
  private static final Pattern  MEDIA_TYPE_PATTERN;
  private static final Pattern  PARAMETERS_PATTERN;

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
    PARAMETERS_PATTERN = Pattern.compile(
        ows + ";" + ows + "(" + token + ")=(?:(" + token + ")|(?:" + quotedStr + "))");

    // Group 1: type
    // Group 2: subtype
    // Group 3: parameters
    MEDIA_TYPE_PATTERN = Pattern.compile(
        ",?(" + token + ")/(" + token + ")((" + PARAMETERS_PATTERN.pattern() + ")*)");
  }

  MediaTypeParserImpl(String qValueKey)
  {
    this.qValueKey = qValueKey;
  }

  @Override
  public QMediaTypeGroup parse(String mediaType)
  {
    // Immediately fail if a leading comma is present. The regex allows a
    // leading comma for simplicity with capturing multiple matches, but this
    // is invalid for the first match.
    if (mediaType.charAt(0) == ',')
    {
      throw new ProcessingException(String.format(
          "Could not fully parse media type \"%s\"," +
              " parsed up to position 0.", mediaType));
    }
    Matcher mediaTypeMatcher = MEDIA_TYPE_PATTERN.matcher(mediaType);
    List<QMediaType> mediaTypes = new ArrayList<>(1);
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
      CharSpan type = new CharSpan(mediaType, mediaTypeMatcher.start(1),
          mediaTypeMatcher.end(1));
      CharSpan subtype = new CharSpan(mediaType, mediaTypeMatcher.start(2),
          mediaTypeMatcher.end(2));
      if (type.equals(WILDCARD) && !subtype.equals(WILDCARD))
      {
        throw new ProcessingException(String.format(
            "Invalid type/subtype combination \"%s/%s\" in media" +
                " type \"%s\", type must be concrete if subtype is concrete.",
            type, subtype, mediaType));
      }
      double qValue = 1d;
      int mediaTypeGroup3Start = mediaTypeMatcher.start(3);
      Map<CharSpan, CharSpan> parametersMap;
      if (mediaTypeGroup3Start != -1)
      {
        CharSpan parameters = new CharSpan(mediaType, mediaTypeGroup3Start,
            mediaTypeMatcher.end(3));
        Matcher parametersMatcher = PARAMETERS_PATTERN.matcher(parameters);
        parametersMap = new HashMap<>(0);
        while (parametersMatcher.find())
        {
          CharSpan key = new CharSpan(parameters,
              parametersMatcher.start(1), parametersMatcher.end(1));
          CharSpan unquotedValue = new CharSpan(parameters,
              parametersMatcher.start(2), parametersMatcher.end(2));
          CharSpan quotedValue = new CharSpan(parameters,
              parametersMatcher.start(3), parametersMatcher.end(3));
          CharSpan value = unquotedValue.getStart() != -1
              ? unquotedValue : quotedValue;
          parametersMap.put(key, value);
          if (key.toString().equalsIgnoreCase(qValueKey))
          {
            try
            {
              qValue = Double.parseDouble(value.toString());
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
      else
      {
        parametersMap = Map.of();
      }
      mediaTypes.add(new LazyQMediaType(type, subtype, qValue, parametersMap));
    }
    if (mediaTypeEnd != mediaType.length())
    {
      throw new ProcessingException(String.format(
          "Could not fully parse media type \"%s\"," +
              " parsed up to position %s.",
          mediaType, mediaTypeEnd));
    }
    return new QMediaTypeGroup(mediaTypes);
  }
}
