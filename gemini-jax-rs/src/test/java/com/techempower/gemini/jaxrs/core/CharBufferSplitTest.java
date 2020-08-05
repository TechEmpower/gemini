package com.techempower.gemini.jaxrs.core;

import com.caucho.util.CharSegment;
import org.junit.Test;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CharBufferSplitTest
{
  static class StringSplitPerformanceTest
  {
    public static class Runner
    {
      public static void main(String... args)
      {
        Performance.test(StringSplitPerformanceTest.class,
            ITERATIONS,
            List.of(
                List.of(CHAR_BUFFER_SPLIT_LINKED_LIST),
                List.of(CHAR_BUFFER_SPLIT_ARRAY_LIST),
                List.of(CHAR_SEGMENTS_SPLIT_ARRAY_LIST),
                List.of(CHAR_SPAN_SPLIT_ARRAY_LIST),
                List.of(CHAR_BUFFER_SPLIT_ARRAY),
                List.of(STRING_SPLIT)
            ));
      }
    }

    static final int    ITERATIONS                     = 5_400_000;
    static final String CHAR_BUFFER_SPLIT_LINKED_LIST  = "CharBuffer split LinkedList";
    static final String CHAR_BUFFER_SPLIT_ARRAY_LIST   = "CharBuffer split ArrayList";
    static final String CHAR_SEGMENTS_SPLIT_ARRAY_LIST = "Resin CharSegment split ArrayList";
    static final String CHAR_SPAN_SPLIT_ARRAY_LIST     = "CharSpan split ArrayList";
    static final String CHAR_BUFFER_SPLIT_ARRAY        = "CharBuffer split array (unrealistic)";
    static final String STRING_SPLIT                   = "String split";

    public static void main(String... args)
        throws Exception
    {
      String uri = "foo/bar";
      char[] chars = uri.toCharArray();
      if (args.length > 0)
      {
        switch (args[0])
        {
          case CHAR_BUFFER_SPLIT_LINKED_LIST:
            perfTestCharBufferSplitLinkedList(uri);
            break;
          case CHAR_BUFFER_SPLIT_ARRAY_LIST:
            perfTestCharBufferSplitArrayList(uri);
            break;
          case CHAR_SEGMENTS_SPLIT_ARRAY_LIST:
            perfTestCharSegmentsSplitArrayList(uri, chars);
            break;
          case CHAR_SPAN_SPLIT_ARRAY_LIST:
            perfTestCharSpanSplitArrayList(uri);
            break;
          case CHAR_BUFFER_SPLIT_ARRAY:
            perfTestCharBufferSplitArray(uri);
            break;
          case STRING_SPLIT:
            perfTestStringSplit(uri);
            break;
        }
      }
    }

    public static void perfTestCharBufferSplitLinkedList(String uri)
    {
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          List<CharBuffer> charBuffers = splitCharBufferLinkedList(uri);
        }
      });
    }

    public static void perfTestCharBufferSplitArrayList(String uri)
    {
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          List<CharBuffer> charBuffers = splitCharBufferArrayList(uri);
        }
      });
    }

    private static void perfTestCharSegmentsSplitArrayList(String uri, char[] chars)
    {
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          List<CharSegment> segments = splitCharSegmentArrayList(uri, chars);
        }
      });
    }

    private static void perfTestCharSpanSplitArrayList(String uri)
    {
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          List<CharSpan> charSpans = splitCharSpanArrayList(uri);
        }
      });
    }

    private static void perfTestCharBufferSplitArray(String uri)
    {
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          CharBuffer[] charBuffers = splitCharBufferArray(uri);
        }
      });
    }

    private static void perfTestStringSplit(String uri)
    {
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          String[] strings = splitStringArray(uri);
        }
      });
    }
  }

  private static List<CharBuffer> splitCharBufferLinkedList(String str)
  {
    int length = str.length();
    if (length == 0)
    {
      return List.of();
    }
    LinkedList<CharBuffer> buffers = new LinkedList<>();
    int current = 0;
    int next;
    while ((next = str.indexOf("/", current)) != -1)
    {
      buffers.add(CharBuffer.wrap(str, current, next));
      current = next + 1;
    }
    if (current < length - 1)
    {
      buffers.add(CharBuffer.wrap(str, current, length));
    }
    return buffers;
  }

  private static List<CharBuffer> splitCharBufferArrayList(String str)
  {
    int length = str.length();
    if (length == 0)
    {
      return List.of();
    }
    ArrayList<CharBuffer> buffers = new ArrayList<>(8);
    int current = 0;
    int next;
    while ((next = str.indexOf("/", current)) != -1)
    {
      buffers.add(CharBuffer.wrap(str, current, next));
      current = next + 1;
    }
    if (current < length - 1)
    {
      buffers.add(CharBuffer.wrap(str, current, length));
    }
    return buffers;
  }

  private static List<CharSegment> splitCharSegmentArrayList(String str, char[] bytes)
  {
    int length = bytes.length;
    if (length == 0)
    {
      return List.of();
    }
    ArrayList<CharSegment> segments = new ArrayList<>(8);
    int current = 0;
    int next;
    while ((next = str.indexOf("/", current)) != -1)
    {
      segments.add(new CharSegment(bytes, current, next - current));
      current = next + 1;
    }
    if (current < length - 1)
    {
      segments.add(new CharSegment(bytes, current, length - current));
    }
    return segments;
  }

  private static List<CharSpan> splitCharSpanArrayList(String str)
  {
    int length = str.length();
    if (length == 0)
    {
      return List.of();
    }
    ArrayList<CharSpan> segments = new ArrayList<>(8);
    int current = 0;
    int next;
    while ((next = str.indexOf("/", current)) != -1)
    {
      segments.add(new CharSpan(str, current, next));
      current = next + 1;
    }
    if (current < length - 1)
    {
      segments.add(new CharSpan(str, current, length));
    }
    return segments;
  }

  private static CharBuffer[] splitCharBufferArray(String str)
  {
    int length = str.length();
    if (length == 0)
    {
      return new CharBuffer[0];
    }
    CharBuffer[] buffers = new CharBuffer[2];
    int current = 0;
    int next;
    int index = 0;
    while ((next = str.indexOf("/", current)) != -1)
    {
      buffers[index++] = CharBuffer.wrap(str, current, next);
      current = next + 1;
    }
    if (current < length - 1)
    {
      buffers[index] = CharBuffer.wrap(str, current, length);
    }
    return buffers;
  }

  @Test
  public void doTest()
  {
    assertEquals(
        List.of(CharBuffer.wrap("foo"), CharBuffer.wrap("bar")),
        splitCharBufferLinkedList("foo/bar"));
    assertEquals(
        List.of(CharBuffer.wrap(""), CharBuffer.wrap("foo"), CharBuffer.wrap("bar")),
        splitCharBufferLinkedList("/foo/bar"));
    assertEquals(
        List.of(CharBuffer.wrap(""), CharBuffer.wrap("foo"), CharBuffer.wrap("bar")),
        splitCharBufferLinkedList("/foo/bar/"));
    assertEquals(
        List.of(CharBuffer.wrap("foo"), CharBuffer.wrap("bar")),
        splitCharBufferLinkedList("foo/bar/"));
    assertEquals(
        List.of(CharBuffer.wrap("foo")),
        splitCharBufferLinkedList("foo"));
    assertEquals(
        List.of(CharBuffer.wrap(""), CharBuffer.wrap("foo")),
        splitCharBufferLinkedList("/foo"));
    assertEquals(
        List.of(CharBuffer.wrap(""), CharBuffer.wrap("foo")),
        splitCharBufferLinkedList("/foo/"));
    assertEquals(
        List.of(CharBuffer.wrap("foo")),
        splitCharBufferLinkedList("foo/"));
  }

  @Test
  public void doTest2()
  {
    assertArrayEquals(
        new String[]{"foo", "bar"},
        splitStringArray("foo/bar"));
    assertArrayEquals(
        new String[]{"", "foo", "bar"},
        splitStringArray("/foo/bar"));
    assertArrayEquals(
        new String[]{"", "foo", "bar"},
        splitStringArray("/foo/bar/"));
    assertArrayEquals(
        new String[]{"foo", "bar"},
        splitStringArray("foo/bar/"));
    assertArrayEquals(
        new String[]{"foo"},
        splitStringArray("foo"));
    assertArrayEquals(
        new String[]{"", "foo"},
        splitStringArray("/foo"));
    assertArrayEquals(
        new String[]{"", "foo"},
        splitStringArray("/foo/"));
    assertArrayEquals(
        new String[]{"foo"},
        splitStringArray("foo/"));
  }

  private static String[] splitStringArray(String str)
  {
    return str.split("/");
  }

}
