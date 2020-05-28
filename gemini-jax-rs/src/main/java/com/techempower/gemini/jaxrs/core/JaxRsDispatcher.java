package com.techempower.gemini.jaxrs.core;

import com.caucho.util.LruCache;
import com.esotericsoftware.reflectasm.MethodAccess;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JaxRsDispatcher
{
  private List<Endpoint> endpoints;
  private LoadingCache<DispatchMatchSource, DispatchBestMatchInfo> bestMatches = CacheBuilder.newBuilder()
      .maximumSize(100)
      .build(new CacheLoader<>() {
        @Override public DispatchBestMatchInfo load(DispatchMatchSource dispatchMatchSource) throws Exception {
          return getBestMatchInfo(dispatchMatchSource);
        }
      });
  DispatchBlock rootBlock = new RootDispatchBlock();
  LruCache<DispatchMatchSource, DispatchBestMatchInfo> bestMatchesResinCache = new LruCache<>(100);
  com.github.benmanes.caffeine.cache.LoadingCache<DispatchMatchSource, DispatchBestMatchInfo> bestMatchesCaff = Caffeine.newBuilder()
      .maximumSize(100)
      .build(this::getBestMatchInfo);
  private boolean cacheEnabled;
  private boolean useResinCache;
  private boolean useCaffCache;

  public boolean isCacheEnabled()
  {
    return cacheEnabled;
  }

  public void setCacheEnabled(boolean cacheEnabled)
  {
    this.cacheEnabled = cacheEnabled;
  }

  public boolean isUseResinCache()
  {
    return useResinCache;
  }

  public void setUseResinCache(boolean useResinCache)
  {
    this.useResinCache = useResinCache;
  }

  public boolean isUseCaffCache()
  {
    return useCaffCache;
  }

  public void setUseCaffCache(boolean useCaffCache)
  {
    this.useCaffCache = useCaffCache;
  }

  public void register(Object instance)
  {
    // TODO: Refactor to support class-based registration. Shouldn't be hard.
    // TODO: Refactor to support finding annotations in class hierarchy
    if (instance.getClass().isAnnotationPresent(Path.class))
    {
      registerResource(instance);
    }
  }

  private void registerResource(Object resourceInstance)
  {
    Method[] methods = Arrays.stream(resourceInstance.getClass().getMethods())
        .filter(method -> method.isAnnotationPresent(Path.class))
        .toArray(Method[]::new);

    // TODO: Refactor to support finding annotations in class hierarchy
    for (Method method : methods)
    {
      registerMethod(resourceInstance, method);
    }
  }

  private void registerMethod(Object resourceInstance, Method method)
  {
    String classLevelPath = resourceInstance.getClass()
        .getAnnotation(Path.class).value();
    String methodLevelPath = method.getAnnotation(Path.class).value();
    String separator = methodLevelPath.startsWith("/") ? "" : "/";
    String fullPath = classLevelPath + separator + methodLevelPath;
    Resource resource = new SingletonResource(resourceInstance);
    Endpoint endpoint = new Endpoint(resource, method);
    registerEndpoint(fullPath, endpoint);
  }

  // TODO: Per the jax-rs spec, this will need to "pick" a best match class/set
  //  of classes first by finding the best match class, then filtering out the
  //  classes that aren't in-line with that class. Then it proceeds to the
  //  method level. This will for now just go right to the method level, then
  //  later I can have it filter out the class stuff after the fact, or even
  //  optimize it so that it directly stops at the class level the first time
  //  so it can filter those down before proceeding to the method level.

  static class WordBlockToken
  {
    private final String word;

    public WordBlockToken(String word)
    {
      this.word = word;
    }
  }

  static class PureVariableBlockToken
  {
    private final String name;

    PureVariableBlockToken(String name)
    {
      this.name = name;
    }
  }

  static class RegexVariableBlockToken
  {
    private final String name;
    // TODO: Turn this into a Pattern in the constructor. Needs to be URI
    //  encoded first, then have its regex characters escaped.
    private final String regex;

    public RegexVariableBlockToken(String name, String regex)
    {
      this.name = name;
      this.regex = regex;
    }
  }

  static class SlashToken
  {
    // Basically just a flag.
  }

  private void registerEndpoint(String path, Endpoint endpoint)
  {
    // TODO: Might want to gracefully handle the case where a resource could
    //  not be fully registered due to some issue with a warning, though
    //  blowing up could also be an option. Check the specs to see if it should
    //  blow up, but if it doesn't specify then just handle it gracefully and
    //  log a warning. Handling it gracefully means it should "revert" any
    //  additions made to the root block and any other blocks/etc.
    // TODO: Need to handle URI encoding/decoding
    StringBuilder parsedPath = new StringBuilder();
    String remainingPath = path;
    if (remainingPath.startsWith("/"))
    {
      remainingPath = remainingPath.substring(1);
    }
    // TODO: Break these up into classes, might as well just set these up for
    //  unit tests to make it easier to test without needing the whole thing to
    //  be implemented.
    Pattern wordBlockToken = Pattern.compile("^[^/{]+");
    Pattern variableBlockToken = Pattern.compile("^\\{[ \t]*(?<name>\\w[\\w.-]*)[ \t]*(:[ \t]*(?<regex>([^{}]|\\{[^{}]*})*)[ \t]*)?}");
    DispatchBlock currentBlock = rootBlock;
    List<Object> tokensSinceLastSlash = new ArrayList<>();
    boolean regexBlockMode = false;
    while (!remainingPath.isEmpty())
    {
      while (remainingPath.length() > 0 && remainingPath.charAt(0) != '/')
      {
        {
          Matcher matcher = wordBlockToken.matcher(remainingPath);
          if (matcher.find())
          {
            String group = matcher.group();
            parsedPath.append(group);
            WordBlockToken token = new WordBlockToken(group);
            tokensSinceLastSlash.add(token);
            remainingPath = remainingPath.substring(matcher.end());
            continue;
          }
        }
        {
          Matcher matcher = variableBlockToken.matcher(remainingPath);
          if (matcher.find())
          {
            parsedPath.append(matcher.group());
            String name = matcher.group("name");
            String regex = matcher.group("regex");
            Object token = regex == null
                ? new PureVariableBlockToken(name)
                : new RegexVariableBlockToken(name, regex);
            tokensSinceLastSlash.add(token);
            remainingPath = remainingPath.substring(matcher.end());
            continue;
          }
        }
        throw new RuntimeException("Could not parse URI at position: "
            + parsedPath + "\u032D" + remainingPath);
      }
      if (tokensSinceLastSlash.isEmpty())
      {
        // TODO: Probably two cases here:
        //  1) double-stacked slash. This is bad. Throw an exception.
        //  b) It's an @Path("") or @Path("/") "root" handler. Currently not
        //  well-handled probably, but I'll get around to it once the main
        //  stuff is done.
        throw new UnsupportedOperationException("TODO");
      }
      else if (tokensSinceLastSlash.size() == 1)
      {
        Object token = tokensSinceLastSlash.get(0);
        // TODO: Refactor to avoid using instanceof
        if (token instanceof WordBlockToken)
        {
          String word = ((WordBlockToken) token).word;
          WordDispatchBlock wordDispatchBlock;
          if (currentBlock.childrenByWord.containsKey(word))
          {
            wordDispatchBlock = currentBlock.childrenByWord.get(word);
          }
          else
          {
            wordDispatchBlock = new WordDispatchBlock();
            currentBlock.addWordChild(word, wordDispatchBlock);
          }
          currentBlock = wordDispatchBlock;
        }
        else if (token instanceof PureVariableBlockToken)
        {
          FullSegmentPureVariableDispatchBlock fullSegmentPureVariableDispatchBlock;
          String name = ((PureVariableBlockToken) token).name;
          if (currentBlock.fullSegmentPureVariableChild != null)
          {
            fullSegmentPureVariableDispatchBlock = currentBlock.fullSegmentPureVariableChild;
            if (!name.equals(fullSegmentPureVariableDispatchBlock.name))
            {
              throw new RuntimeException("Multiple variable names exist at the" +
                  " same exact path" /* TODO: Include the conflicting paths */);
            }
          }
          else
          {
            fullSegmentPureVariableDispatchBlock = new FullSegmentPureVariableDispatchBlock(name);
            currentBlock.fullSegmentPureVariableChild = fullSegmentPureVariableDispatchBlock;
          }
          currentBlock = fullSegmentPureVariableDispatchBlock;
        }
        else
        {
          regexBlockMode = true;
        }
      }
      else
      {
        regexBlockMode = true;
      }
      if (!remainingPath.isEmpty())
      {
        if (remainingPath.charAt(0) != '/')
        {
          throw new RuntimeException("Failure encountered during path" +
              " parsing. Please report this path to the developers" +
              " of Gemini: " + path);
        }
        else
        {
          parsedPath.append('/');
          remainingPath = remainingPath.substring(1);
          if (regexBlockMode)
          {
            tokensSinceLastSlash.add(new SlashToken());
          }
        }
      }
      if (!regexBlockMode)
      {
        tokensSinceLastSlash.clear();
      }
    }
    if (regexBlockMode)
    {
      RegexVariableDispatchBlock block = new RegexVariableDispatchBlock(
          tokensSinceLastSlash);
      if (currentBlock.regexChildren.stream()
          .map(RegexVariableDispatchBlock::getPatternString)
          .anyMatch(block.getPatternString()::equals))
      {
        throw new RuntimeException("Duplicate regex block found: "
            + block.getPatternString());
      }
      currentBlock.regexChildren.add(block);
      currentBlock = block;
    }
    Set<String> httpMethods = new HashSet<>();
    for (Annotation annotation : endpoint.method.getAnnotations())
    {
      Class<?> annotationClass = annotation.annotationType();
      if (annotationClass.isAnnotationPresent(HttpMethod.class))
      {
        HttpMethod httpMethod = annotationClass
            .getAnnotation(HttpMethod.class);
        httpMethods.add(httpMethod.value());
      }
    }
    for (Annotation annotation : endpoint.resource
        .getInstanceClass().getAnnotations())
    {
      Class<?> annotationClass = annotation.annotationType();
      if (annotationClass.isAnnotationPresent(HttpMethod.class))
      {
        HttpMethod httpMethod = annotationClass
            .getAnnotation(HttpMethod.class);
        httpMethods.add(httpMethod.value());
      }
    }
    for (String httpMethod : httpMethods)
    {
      if (currentBlock.endpointsByHttpMethod.containsKey(httpMethod))
      {
        throw new RuntimeException("Path " + path + " and HttpMethod "
            + httpMethod + " is already associated with another endpoint");
        // TODO: Would be nice to include the other endpoint.
        // TODO: Include logging, not just thrown exceptions.
      }
      currentBlock.endpointsByHttpMethod.put(httpMethod, endpoint);
    }
  }

  static class DispatchMatch
  {
    private final DispatchBlock       block;
    private final Endpoint            endpoint;
    final         Map<String, String> values;
    final         List<DispatchMatch> matchChildren;

    public DispatchMatch(DispatchBlock block,
                         Endpoint endpoint,
                         Map<String, String> values,
                         List<DispatchMatch> matchChildren)
    {
      this.block = block;
      this.endpoint = endpoint;
      this.values = values;
      this.matchChildren = matchChildren;
    }

    // for debugging
    List<String> getLeafValues()
    {
      // TODO: I suppose this could have a value and children at the same time.
      //  Need to understand for myself what a "DispatchMatch" actually is,
      //  I've forgotten since last night. Should probably write-up a new
      //  explanation like the (incorrect) one in the dispatch method.
      if (matchChildren != null)
      {
        return matchChildren.stream()
            .map(DispatchMatch::getLeafValues)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
      }
      if (values != null)
      {
        return List.copyOf(values.values());
      }
      return null;
    }

    // for debugging
    List<DispatchMatch> getLeafMatches()
    {
      // TODO: See TODO above in #getLeafValues.
      if (matchChildren != null)
      {
        return matchChildren.stream()
            .map(DispatchMatch::getLeafMatches)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
      }
      return List.of(this);
    }

    void getBestMatch(DispatchBestMatchInfo info)
    {
      if (values != null)
      {
        info.values.putAll(values);
      }
      if (matchChildren != null)
      {
        matchChildren.get(0).getBestMatch(info);
      }
      else
      {
        info.endpoint = this.endpoint;
      }
    }
  }

  static class DispatchBestMatchInfo
  {
    private Endpoint            endpoint;
    private Map<String, String> values = new HashMap<>();

    public Map<String, String> getValues()
    {
      return values;
    }
  }

  abstract static class DispatchBlock
  {
    Map<String, WordDispatchBlock>       childrenByWord        = new HashMap<>(0);
    FullSegmentPureVariableDispatchBlock fullSegmentPureVariableChild;
    List<RegexVariableDispatchBlock>     regexChildren         = new ArrayList<>(0);
    Map<String, Endpoint>                endpointsByHttpMethod = new HashMap<>(0);

    void addWordChild(String word, WordDispatchBlock block)
    {
      childrenByWord.put(word, block);
    }

    void setFullSegmentPureVariableChild(FullSegmentPureVariableDispatchBlock block)
    {
      fullSegmentPureVariableChild = block;
    }

    void addRegexChild(RegexVariableDispatchBlock block)
    {
      regexChildren.add(block);
    }

    // TODO: Can potentially optimize away the list initializations and
    //  additions by pre-caching the relative index of all endpoints for a
    //  global sort, then having the below simply discard the existing match
    //  found if a new match found is of a lower index AKA higher precedence.
    final List<DispatchMatch> getChildMatches(String httpMethod,
                                              String[] segments,
                                              int index)
    {
      String segment = segments[index];
      List<DispatchMatch> matches = null;

      DispatchBlock childWordBlock = childrenByWord.get(segment);
      if (childWordBlock != null)
      {
        DispatchMatch match = childWordBlock.getDispatchMatch(httpMethod,
            segments, index);
        if (match != null)
        {
          matches = new LinkedList<>();
          matches.add(match);
        }
      }

      if (fullSegmentPureVariableChild != null)
      {
        DispatchMatch match = fullSegmentPureVariableChild
            .getDispatchMatch(httpMethod, segments, index);
        if (match != null)
        {
          if (matches == null)
          {
            matches = new LinkedList<>();
          }
          matches.add(match);
        }
      }
      for (DispatchBlock regexChild : regexChildren)
      {
        DispatchMatch match = regexChild
            .getDispatchMatch(httpMethod, segments, index);
        if (match != null)
        {
          if (matches == null)
          {
            matches = new LinkedList<>();
          }
          matches.add(match);
        }
      }
      return matches;
    }

    Endpoint getMatchingEndpoint(String httpMethod)
    {
      return endpointsByHttpMethod.get(httpMethod);
    }

    abstract DispatchMatch getDispatchMatch(String httpMethod,
                                            String[] segments,
                                            int index);
  }

  static class RootDispatchBlock extends DispatchBlock
  {

    @Override
    DispatchMatch getDispatchMatch(String httpMethod,
                                   String[] segments,
                                   int index)
    {
      if (segments.length == 0)
      {
        // TODO: In theory this could exist, but for now lets assume it doesn't
        return null;
      }
      else
      {
        List<DispatchMatch> childMatches = getChildMatches(httpMethod,
            segments, index);
        if (childMatches != null)
        {
          return new DispatchMatch(this, null, null, childMatches);
        }
        else
        {
          return null;
        }
      }
    }
  }

  static class WordDispatchBlock extends DispatchBlock
  {
    @Override
    DispatchMatch getDispatchMatch(String httpMethod,
                                   String[] segments,
                                   int index)
    {
      if (segments.length - 1 == index)
      {
        // If this has a matching endpoint, consider it a dispatch candidate
        Endpoint endpoint = getMatchingEndpoint(httpMethod);
        if (endpoint != null)
        {
          return new DispatchMatch(this, endpoint, null, null);
        }
        return null;
      }
      else
      {
        // For word blocks it's assumed that you match once in this method,
        // because the check-match is performed outside of it using the map.
        List<DispatchMatch> childMatches = getChildMatches(httpMethod,
            segments, index + 1);
        if (childMatches != null)
        {
          return new DispatchMatch(this, null, null, childMatches);
        }
        else
        {
          return null;
        }
      }
    }
  }

  static class FullSegmentPureVariableDispatchBlock extends DispatchBlock
  {
    private final String name;

    FullSegmentPureVariableDispatchBlock(String name)
    {
      this.name = name;
    }

    @Override
    DispatchMatch getDispatchMatch(String httpMethod,
                                   String[] segments,
                                   int index)
    {
      if (segments.length - 1 == index)
      {
        // If this has a matching endpoint, consider it a dispatch candidate
        Endpoint endpoint = getMatchingEndpoint(httpMethod);
        if (endpoint != null)
        {
          return new DispatchMatch(this, endpoint,
              Map.of(name, segments[index]), null);
        }
        return null;
      }
      else
      {
        // For full segment variable blocks it's assumed that your match is the
        // entire segment, because that's simply the definition of this type of
        // segment. That is what it represents.
        List<DispatchMatch> childMatches = getChildMatches(httpMethod,
            segments, index + 1);
        if (childMatches != null)
        {
          return new DispatchMatch(this, null,
              Map.of(name, segments[index]), childMatches);
        }
        else
        {
          return null;
        }
      }
    }
  }

  static class RegexVariableDispatchBlock extends DispatchBlock
  {
    private final Pattern             pattern;
    private final Map<String, String> variableNameToGroupName = new HashMap<>();

    static String generateGroupName()
    {
      return "g" + UUID.randomUUID().toString().replaceAll("-", "");
    }

    public RegexVariableDispatchBlock(List<Object> tokens)
    {
      StringBuilder patternString = new StringBuilder();
      for (Object token : tokens)
      {
        if (token instanceof WordBlockToken)
        {
          // TODO: Encode and escape stuff.
          patternString.append(((WordBlockToken) token).word);
        }
        else if (token instanceof PureVariableBlockToken)
        {
          String groupName = generateGroupName();
          // TODO: Encode and escape stuff.
          String pattern = String.format("(?<%s>[^/]+?)", groupName);
          String name = ((PureVariableBlockToken) token).name;
          patternString.append(pattern);
          variableNameToGroupName.put(name, groupName);
        }
        else if (token instanceof RegexVariableBlockToken)
        {
          String groupName = generateGroupName();
          String name = ((RegexVariableBlockToken) token).name;
          String regex = ((RegexVariableBlockToken) token).regex;
          // TODO: Encode and escape stuff.
          String pattern = String.format("(?<%s>%s)", groupName, regex);
          patternString.append(pattern);
          variableNameToGroupName.put(name, groupName);
        }
        else if (token instanceof SlashToken)
        {
          patternString.append("/");
        }
        else
        {
          throw new RuntimeException("Unexpected token type.");
        }
      }
      if (!patternString.toString().endsWith("/")) {
        patternString.append("/");
      }
      pattern = Pattern.compile(patternString.toString());
    }

    String getPatternString()
    {
      return pattern.pattern();
    }

    @Override
    DispatchMatch getDispatchMatch(String httpMethod,
                                   String[] segments,
                                   int index)
    {
      String uri = String.join("/", segments);
      if (!uri.endsWith("/")) {
        uri += "/";
      }
      Matcher matcher = pattern.matcher(uri);
      if (matcher.find())
      {
        Map<String, String> values = new HashMap<>();
        for (var entry : variableNameToGroupName.entrySet())
        {
          values.put(entry.getKey(), matcher.group(entry.getValue()));
        }
        // If this has a matching endpoint, consider it a dispatch candidate
        Endpoint endpoint = getMatchingEndpoint(httpMethod);
        if (endpoint != null)
        {
          return new DispatchMatch(this, endpoint, values, null);
        }
      }
      return null;
    }
  }

  static class DispatchMatchSource {
    private final String uri;
    private final String httpMethod;

    public DispatchMatchSource(String httpMethod, String uri)
    {
      this.uri = uri;
      this.httpMethod = httpMethod;
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DispatchMatchSource that = (DispatchMatchSource) o;
      return uri.equals(that.uri) &&
          httpMethod.equals(that.httpMethod);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(uri, httpMethod);
    }
  }

  interface Resource
  {
    Object getInstance();

    Class<?> getInstanceClass();
  }

  static class SingletonResource implements Resource
  {
    private final Object singleton;

    SingletonResource(Object singleton)
    {
      this.singleton = singleton;
    }

    @Override
    public Object getInstance()
    {
      return singleton;
    }

    @Override
    public Class<?> getInstanceClass()
    {
      return singleton.getClass();
    }
  }

  static class ClassResource implements Resource
  {
    private final Class<?> resourceClass;

    ClassResource(Class<?> resourceClass)
    {
      this.resourceClass = resourceClass;
    }


    @Override
    public Object getInstance()
    {
      // TODO: Find out how to do dependency injection. Also might want to use
      //  the fast reflection library for this if/when possible.
      try
      {
        return resourceClass.getConstructor().newInstance();
      }
      catch (InstantiationException
          | IllegalAccessException
          | InvocationTargetException
          | NoSuchMethodException e)
      {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Class<?> getInstanceClass()
    {
      return resourceClass;
    }
  }

  static class Endpoint
  {
    private final Resource resource;
    private final Method          method;
    private final QMediaTypeGroup mediaTypeConsumes;
    private final QMediaTypeGroup mediaTypeProduces;

    // TODO: Change method to "callable" or some such.
    Endpoint(Resource resource, Method method)
    {
      this.resource = resource;
      this.method = method;
      // TODO: Obtain from the method, likely outside then passed in so that
      //  Method can become "callable" instead. Also, the media type can come
      //  from the class.
      // TODO: Look up "entity provider", section 4.2.3
      mediaTypeConsumes = null;
      mediaTypeProduces = null;
    }
  }

  DispatchMatch getDispatchMatches(String httpMethod, String uri)
  {
    // TODO: This should be commented back in, but I'm leaving it commented
    //  out while I try to find a way around this to avoid the performance hit.
    /*int uriStart = uri.startsWith("/") ? 1 : 0;
    int uriEnd = uri.length() - (uri.endsWith("/") ? 1 : 0);
    String normalizedUri = uri.substring(uriStart, uriEnd);*/
    // TODO: Switch to CharSpan. Can also use a modified CharSpan to avoid the
    //  need to create a normalized uri, since the split can just be told to
    //  ignore the unnecessary characters (leading/trailing slash, though a
    //  trailing slash is already ignored by String.split).
    String[] segments = uri.split("/");
    return rootBlock.getDispatchMatch(httpMethod, segments, 0);
  }

  public DispatchBestMatchInfo getBestMatchInfo(DispatchMatchSource dispatchMatchSource) {
    String uri = dispatchMatchSource.uri;
    String httpMethod = dispatchMatchSource.httpMethod;
    int uriStart = uri.startsWith("/") ? 1 : 0;
    int uriEnd = uri.length() - (uri.endsWith("/") ? 1 : 0);
    String normalizedUri = uri.substring(uriStart, uriEnd);
    DispatchMatch dispatchMatch = getDispatchMatches(httpMethod, normalizedUri);
    DispatchBestMatchInfo matchInfo = new DispatchBestMatchInfo();
    // TODO: Need to filter the dispatch match down to only the correct one so
    //  the path params can be extracted. There's a TO-DO...somewhere in this
    //  file (or maybe the test) for how to do that naturally and only come out
    //  with the right match per the sorting rules, and not have to deal with
    //  the tree of matches.
    dispatchMatch.getBestMatch(matchInfo);
    return matchInfo;
  }

  public DispatchBestMatchInfo getBestMatchInfo(String httpMethod, String uri) {
    DispatchMatchSource matchSource = new DispatchMatchSource(httpMethod, uri);
    if (cacheEnabled) {
      if (useResinCache) {
        DispatchBestMatchInfo matchInfo = bestMatchesResinCache.get(matchSource);
        if (matchInfo == null) {
          matchInfo = getBestMatchInfo(matchSource);
          bestMatchesResinCache.put(matchSource, matchInfo);
        }
        return matchInfo;
      } else if (useCaffCache) {
        return bestMatchesCaff.get(matchSource);
      } else {
        try
        {
          return bestMatches.get(matchSource);
        }
        catch (ExecutionException e)
        {
          throw new RuntimeException(e);
        }
      }
    }
    return getBestMatchInfo(matchSource);
  }

  // TODO: Not use Gemini's Context eventually, probably.
  //public void dispatch(Context context)
  public Object dispatch(String httpMethod, String uri)
  {
    // TODO: Add `getServletPath` as a method in Context so that this can
    //       only match the URI relative to where the servlet is hosted.
    //       `getServletPath` is a method of HttpServletRequest. For
    //       non-servlet containers this can just default to "" or "/". Find
    //       out what the default is for Resin and use that.
    DispatchBestMatchInfo matchInfo = getBestMatchInfo(httpMethod, uri);

    Method method = matchInfo.endpoint.method;
    // TODO: For now I'm just gonna support valueOf for simplicity,
    //  and assume it's static.
    List<Object> arguments = new ArrayList<>();
    Object instance = matchInfo.endpoint.resource.getInstance();
    Class<?> instanceClass = matchInfo.endpoint.resource.getInstanceClass();
    Class<?>[] parameterTypes = method.getParameterTypes();
    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    for (Parameter parameter : method.getParameters())
    {
      Class<?> parameterType = parameter.getType();
      PathParam pathParam = parameter.getAnnotation(PathParam.class);
      String stringValue = matchInfo.values.get(pathParam.value());
      Object val;
      if (parameterType == String.class)
      {
        val = stringValue;
      }
      else
      {
        MethodAccess parameterTypeAccess = MethodAccess.get(parameterType);
        int index = parameterTypeAccess.getIndex("fromString", String.class);
        if (index < 0) {
          index = parameterTypeAccess.getIndex("valueOf", String.class);
        }
        val = parameterTypeAccess.invoke(null, index, stringValue);
      }
      arguments.add(val);
    }
    try
    {
      // TODO: Temporarily having dispatch return be the method invocation
      //  return value so I can test it easier. Should refactor to make this
      //  easier.
      return method.invoke(instance, arguments.toArray());
    }
    catch (IllegalAccessException | InvocationTargetException e)
    {
      throw new RuntimeException(e);
    }
  }
}
