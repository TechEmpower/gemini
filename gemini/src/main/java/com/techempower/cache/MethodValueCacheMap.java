package com.techempower.cache;

import com.techempower.util.Identifiable;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.util.*;

/**
 * Stores and maintains a nested method value cache, for short/simple use by
 * {@link MethodValueCache}. Does not manage the single-method values of the
 * top-level Node, which is instead managed externally in
 * {@link MethodValueCache} (the same behavior as always).
 */
class MethodValueCacheMap
{
  private final Map<String, TLongObjectMap<Object>> mapMethodNameToIdToValue;
  private final Node rootNode;
  private final LinkedHashSet<List<String>> initializedIntersections
      = new LinkedHashSet<>();

  MethodValueCacheMap(Node rootNode,
                      Map<String, TLongObjectMap<Object>> mapMethodNameToIdToValue)
  {
    this.rootNode = rootNode;
    this.mapMethodNameToIdToValue = mapMethodNameToIdToValue;
  }

  public void update(long id)
  {
    for (List<String> intersection : initializedIntersections)
    {
      Node currentNode = rootNode;
      int numberOfMethods = intersection.size();
      for (int i = 0; i < numberOfMethods - 1; i++)
      {
        String method = intersection.get(i);
        Object value = mapMethodNameToIdToValue.get(method).get(id);
        Map<Object, TLongSet> mapValueToIds = currentNode
            .mapMethodNameToValueToIds.get(method);
        mapValueToIds.computeIfAbsent(value, ignored -> new TLongHashSet())
            .add(id);
        Map<Object, Node> mapValueToNode = currentNode
            .mapMethodNameToValueToNode.get(method);
        final Node branchNode = currentNode;
        currentNode = mapValueToNode
            .computeIfAbsent(value,
                ignored -> new Node(branchNode, method, value));
      }
      String lastMethod = intersection.get(intersection.size() - 1);
      Object lastValue = mapMethodNameToIdToValue.get(lastMethod).get(id);
      currentNode.mapMethodNameToValueToIds
          .computeIfAbsent(lastMethod, ignored -> new HashMap<>())
          .computeIfAbsent(lastValue, ignored -> new TLongHashSet())
          .add(id);
    }
  }

  public void remove(long id)
  {
    List<Node> nodesToTraverse = Arrays.asList(rootNode);
    while (nodesToTraverse.size() > 0)
    {
      List<Node> nextNodes = new ArrayList<>();
      for (Node node : nodesToTraverse)
      {
        for (String method : node.mapMethodNameToValueToIds.keySet())
        {
          Object value = mapMethodNameToIdToValue.get(method).get(id);
          Map<Object, TLongSet> mapValueToIds = node
              .mapMethodNameToValueToIds.get(method);
          TLongSet ids = mapValueToIds != null
              ? mapValueToIds.get(value)
              : null;
          if (ids != null)
          {
            ids.remove(id);
            Map<Object, Node> mapValueToNodes = node.mapMethodNameToValueToNode
                .get(method);
            Node childNode = mapValueToNodes != null
                ? mapValueToNodes.get(value)
                : null;
            if (ids.isEmpty())
            {
              mapValueToIds.remove(value);
              if (mapValueToNodes != null)
              {
                mapValueToNodes.remove(value);
              }
            } else if (childNode != null)
            {
              nextNodes.add(childNode);
            }
          }
        }
      }
      nodesToTraverse = nextNodes;
    }
  }

  public IdSearchResult get(List<String> methods, List<Object> values)
  {
    Node currentNode = rootNode;
    for (int i = 0; i < methods.size() - 1 && currentNode != null; i++)
    {
      String method = methods.get(i);
      Object value = values.get(i);
      Map<Object, Node> mapValueToNode = currentNode
          .mapMethodNameToValueToNode.get(method);
      if (mapValueToNode == null)
      {
        return IdSearchResult.NOT_INITIALIZED;
      }
      currentNode = mapValueToNode.get(value);
    }
    if (currentNode != null)
    {
      String lastMethod = methods.get(methods.size() - 1);
      Object lastValue = values.get(values.size() - 1);
      Map<Object, TLongSet> mapValueToIds = currentNode
          .mapMethodNameToValueToIds.get(lastMethod);
      if (mapValueToIds != null)
      {
        return new IdSearchResult(mapValueToIds.get(lastValue));
      }
    }
    return new IdSearchResult(null);
  }

  public void initialize(List<String> methods)
  {
    int numberOfMethods = methods.size();
    for (int i = 1; i <= numberOfMethods; i++)
    {
      List<String> methodsSublist = methods.subList(0, i);
      if (!isInitialized(methodsSublist))
      {
        populate(methodsSublist);
        markInitialized(methodsSublist);
      }
    }
  }

  boolean isInitialized(List<String> methods)
  {
    return initializedIntersections.contains(methods);
  }

  private void markInitialized(List<String> methods)
  {
    initializedIntersections.add(methods);
  }

  private void populate(List<String> methods)
  {
    Map<Object, Node> mapValuesToNode = null;
    Node currentNode = null;
    int numberOfMethods = methods.size();
    String methodToPopulate = methods.get(methods.size() - 1);

    Collection<Node> nodesToTraverse = Arrays.asList(rootNode);
    for (int i = 0; i < numberOfMethods - 1; i++)
    {
      List<Node> nextNodes = new ArrayList<>();
      String method = methods.get(i);
      for (Node node : nodesToTraverse)
      {
        nextNodes.addAll(node.mapMethodNameToValueToNode
            .get(method)
            .values());
      }
      nodesToTraverse = nextNodes;
    }
    for (Node node : nodesToTraverse)
    {
      TLongSet parentIds = node.parent != null
          ? node.parent
          .mapMethodNameToValueToIds
          .get(node.method)
          .get(node.value)
          : null;
      Map<Object, Node> parentMapValueToNodes = node
          .mapMethodNameToValueToNode
          .computeIfAbsent(methodToPopulate, ignored -> new HashMap<>());
      Map<Object, TLongSet> newMapValueToIds = new HashMap<>();
      Map<Object, TLongSet> rootMapValueToIds = rootNode
          .mapMethodNameToValueToIds.get(methodToPopulate);
      for (Object leafValue : rootMapValueToIds.keySet())
      {
        Node leafNode = new Node(node, methodToPopulate, leafValue);
        TLongSet rootIds = rootNode.mapMethodNameToValueToIds
            .get(methodToPopulate).get(leafValue);
        TLongSet intersectingIds;
        if (node.parent != null)
        {
          intersectingIds = new TLongHashSet(parentIds);
          intersectingIds.retainAll(rootIds);
        } else
        {
          intersectingIds = new TLongHashSet(rootIds);
        }
        newMapValueToIds.put(leafValue, intersectingIds);
        parentMapValueToNodes.put(leafValue, leafNode);
      }
      node.mapMethodNameToValueToIds.put(methodToPopulate, newMapValueToIds);
    }
  }

  void reset()
  {
    initializedIntersections.clear();
    rootNode.mapMethodNameToValueToNode.clear();
  }

  static class Node
  {
    final Map<String, Map<Object, TLongSet>> mapMethodNameToValueToIds
        = new HashMap<>();
    final Map<String, Map<Object, Node>> mapMethodNameToValueToNode
        = new HashMap<>();
    final Node parent;
    final String method;
    final Object value;


    Node()
    {
      this(null, null, null);
    }

    Node(Node parent, String method, Object value)
    {
      this.parent = parent;
      this.method = method;
      this.value = value;
    }
  }

  static class IdSearchResult
  {
    static final IdSearchResult NOT_INITIALIZED = new IdSearchResult();
    final TLongSet ids;
    final boolean initialized;

    IdSearchResult(TLongSet ids)
    {
      this.ids = ids;
      this.initialized = true;
    }

    IdSearchResult()
    {
      this.ids = null;
      this.initialized = false;
    }
  }
}
