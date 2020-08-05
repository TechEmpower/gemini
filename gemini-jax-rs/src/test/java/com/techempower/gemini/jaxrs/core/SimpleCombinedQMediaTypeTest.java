package com.techempower.gemini.jaxrs.core;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.MediaType;

import java.util.Map;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class SimpleCombinedQMediaTypeTest
{

  @RunWith(Parameterized.class)
  public static class compareTo
  {
    private enum CompareResult
    {
      FIRST_GREATER,
      FIRST_LESSER,
      EQUAL;

      public static CompareResult from(int result)
      {
        if (result > 0)
        {
          return FIRST_GREATER;
        }
        else if (result < 0)
        {
          return FIRST_LESSER;
        }
        else
        {
          return EQUAL;
        }
      }
    }

    @Parameterized.Parameters(name = "expected: {0}, first: \"{1}\", second: \"{2}\"")
    public static Object[][] params()
    {
      return new Object[][]{
          // step i
          {
              CompareResult.EQUAL,
              new SimpleCombinedQMediaType("text", "html", 1, 0.7, 0),
              new SimpleCombinedQMediaType("application", "xml", 1, 0.7, 0),
          },
          {
              CompareResult.FIRST_LESSER,
              new SimpleCombinedQMediaType("text", "*", 1, 0.9, 0),
              new SimpleCombinedQMediaType("application", "xml", 1, 0.7, 0),
          },
          {
              CompareResult.EQUAL,
              new SimpleCombinedQMediaType("text", "*", 1, 0.7, 0),
              new SimpleCombinedQMediaType("application", "*", 1, 0.7, 0),
          },
          {
              CompareResult.FIRST_GREATER,
              new SimpleCombinedQMediaType("text", "*", 1, 0.7, 1),
              new SimpleCombinedQMediaType("*", "*", 1, 0.9, 0),
          },
          {
              CompareResult.EQUAL,
              new SimpleCombinedQMediaType("*", "*", 1, 0.7, 0),
              new SimpleCombinedQMediaType("*", "*", 1, 0.7, 0),
          },
          // step ii
          {
              CompareResult.FIRST_LESSER,
              new SimpleCombinedQMediaType("text", "*", 0.9, 0.7, 0),
              new SimpleCombinedQMediaType("application", "*", 1, 0.7, 1),
          },
          // step iii
          {
              CompareResult.FIRST_GREATER,
              new SimpleCombinedQMediaType("text", "*", 1, 0.8, 1),
              new SimpleCombinedQMediaType("application", "*", 1, 0.7, 0),
          },
          // step iv
          {
              CompareResult.FIRST_LESSER,
              new SimpleCombinedQMediaType("text", "*", 1, 0.7, 1),
              new SimpleCombinedQMediaType("application", "xml", 1, 0.7, 0),
          },
          {
              CompareResult.FIRST_GREATER,
              new SimpleCombinedQMediaType("*", "*", 1, 0.7, 1),
              new SimpleCombinedQMediaType("*", "*", 1, 0.7, 2),
          },
          // Incompatible media types
          {
              CompareResult.FIRST_LESSER,
              SimpleCombinedQMediaType.INCOMPATIBLE,
              new SimpleCombinedQMediaType("*", "*", 1, 0.7, 2),
          },
          {
              CompareResult.EQUAL,
              SimpleCombinedQMediaType.INCOMPATIBLE,
              SimpleCombinedQMediaType.INCOMPATIBLE,
          },
      };
    }

    @Parameterized.Parameter
    public CompareResult expected;

    @Parameterized.Parameter(1)
    public SimpleCombinedQMediaType first;

    @Parameterized.Parameter(2)
    public SimpleCombinedQMediaType second;

    @Test
    public void test()
    {
      int result = first.compareTo(second);
      int resultReversed = second.compareTo(first);
      String actualResult = "actual result: " + CompareResult.from(result);
      String actualReversedResult = "actual reversed: " + CompareResult.from(resultReversed);
      if (expected == CompareResult.FIRST_GREATER)
      {
        assertTrue(actualResult, result > 0);
        assertTrue(actualReversedResult, resultReversed < 0);
      }
      else if (expected == CompareResult.FIRST_LESSER)
      {
        assertTrue(actualResult, result < 0);
        assertTrue(actualReversedResult, resultReversed > 0);
      }
      else if (expected == CompareResult.EQUAL)
      {
        assertEquals(actualResult, 0, result);
        assertEquals(actualReversedResult, 0, resultReversed);
      }
      else
      {
        fail("Expected CompareResult not defined");
      }
    }
  }

  @RunWith(Parameterized.class)
  public static class create
  {

    public static class TestMediaType extends WrappedQMediaType
    {

      public TestMediaType(String type, String subtype, double qValue)
      {
        super(new MediaType(type, subtype, Map.of()), qValue);
      }

      @Override
      public String toString()
      {
        return String.format("%s/%s;q=%s", getType(), getSubtype(),
            getQValue());
      }
    }

    @Parameterized.Parameters(name = "S({1}, {2}) = {0}")
    public static Object[][] params()
    {
      return new Object[][]{
          {
              new SimpleCombinedQMediaType("text", "html", 0.9, 0.8, 0),
              new TestMediaType("text", "html", 0.9),
              new TestMediaType("text", "html", 0.8)
          },
          {
              new SimpleCombinedQMediaType("text", "html", 0.5, 0.75, 1),
              new TestMediaType("text", "html", 0.5),
              new TestMediaType("text", "*", 0.75)
          },
          {
              new SimpleCombinedQMediaType("text", "*", 0.5, 0.75, 1),
              new TestMediaType("text", "*", 0.5),
              new TestMediaType("*", "*", 0.75)
          },
          {
              new SimpleCombinedQMediaType("text", "html", 0.5, 1, 2),
              new TestMediaType("text", "html", 0.5),
              new TestMediaType("*", "*", 1)
          },
          {
              new SimpleCombinedQMediaType("text", "*", 0.5, 0.75, 0),
              new TestMediaType("text", "*", 0.5),
              new TestMediaType("text", "*", 0.75)
          },
          {
              new SimpleCombinedQMediaType("*", "*", 0.5, 0.75, 0),
              new TestMediaType("*", "*", 0.5),
              new TestMediaType("*", "*", 0.75)
          },
          {
              SimpleCombinedQMediaType.INCOMPATIBLE,
              new TestMediaType("text", "html", 0.5),
              new TestMediaType("application", "xml", 0.75)
          }
      };
    }

    @Parameterized.Parameter
    public SimpleCombinedQMediaType expected;

    @Parameterized.Parameter(1)
    public QMediaType clientType;

    @Parameterized.Parameter(2)
    public QMediaType serverType;

    @Test
    public void test()
    {
      assertEquals(expected,
          SimpleCombinedQMediaType.create(clientType, serverType));
    }
  }

  @RunWith(Parameterized.class)
  public static class equals
  {
    @Parameterized.Parameters(name = "first: \"{0}\", second: \"{1}\"")
    public static Object[][] params()
    {
      return new Object[][]{
          {
              new SimpleCombinedQMediaType("text", "html", 1, 1, 0),
              new SimpleCombinedQMediaType("text", "html", 1, 1, 0)
          },
          {
              new SimpleCombinedQMediaType("text", "html", 1, 1, 0),
              new SimpleCombinedQMediaType("text", "HTML", 1, 1, 0)
          },
      };
    }

    @Parameterized.Parameter
    public SimpleCombinedQMediaType first;

    @Parameterized.Parameter(1)
    public SimpleCombinedQMediaType second;

    @Test
    public void test()
    {
      assertEquals(first, second);
    }
  }
}