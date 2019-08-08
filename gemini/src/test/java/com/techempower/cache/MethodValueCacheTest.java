package com.techempower.cache;

import com.techempower.TechEmpowerApplication;
import com.techempower.log.ComponentLog;
import com.techempower.log.LogWriterManager;
import com.techempower.util.Identifiable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.junit.runners.Parameterized.*;
import static com.techempower.cache.MethodValueCacheTest.*;

@RunWith(MethodValueCacheTest.class)
@Suite.SuiteClasses({
    getObject.class,
    getObjectInt.class,
    getObjects.class,
    getObjectsInt.class,
    addMethod.class,
    delete.class,
    reset.class,
    update.class
})
public class MethodValueCacheTest extends Suite
{
  public MethodValueCacheTest(Class<?> klass, RunnerBuilder builder) throws InitializationError
  {
    super(klass, builder);
  }

  static class House implements Identifiable
  {
    private long id;
    private int cityId;
    private String dog;
    private String owner;

    public House()
    {
    }

    public House(long id)
    {
      setId(id);
    }

    @Override
    public long getId()
    {
      return id;
    }

    @Override
    public void setId(long id)
    {
      this.id = id;
    }

    public int getCityId()
    {
      return cityId;
    }

    public String getOwner()
    {
      return owner;
    }

    public House setOwner(String owner)
    {
      this.owner = owner;
      return this;
    }

    public House setCityId(int cityId)
    {
      this.cityId = cityId;
      return this;
    }

    public String getDog()
    {
      return dog;
    }

    public House setDog(String dog)
    {
      this.dog = dog;
      return this;
    }
  }

  static class Param
  {
    String description;
    List<House> inputHouses;
    Consumer<InnerTestArguments> test;

    Object[] toParams()
    {
      return new Object[]{description, inputHouses, test};
    }
  }

  static final String getCityId = "getCityId";
  static final String getOwner = "getOwner";
  static final String getDog = "getDog";

  /**
   * Provides the app, store, and a basic mutable entity cache map,
   * prepopulated with the House entity list (empty).
   */
  public static class CoreTestBase
  {
    TechEmpowerApplication app;
    EntityStore store;
    Map<Class, List<Identifiable>> mockCache;

    @Before
    public void setUp() throws Exception
    {
      mockCache = new HashMap<>();
      mockCache.put(House.class, new ArrayList<>());
      app = new TechEmpowerApplication()
      {
        @Override
        protected LogWriterManager constructLogCloser()
        {
          return new LogWriterManager(this)
          {
            @Override
            public void begin()
            {
            }
          };
        }

        @Override
        public ComponentLog getLog(String componentCode)
        {
          return new ComponentLog(null, componentCode)
          {
            @Override
            public void log(String logString, int debugLevel)
            {
            }

            @Override
            public void log(String logString)
            {
            }

            @Override
            public void log(String debugString, int debugLevel, Throwable exception)
            {
            }

            @Override
            public void log(String debugString, Throwable exception)
            {
            }

            @Override
            public void assertion(boolean evalExpression, String debugString, int debugLevel)
            {
            }

            @Override
            public void assertion(boolean evalExpression, String debugString)
            {
            }
          };
        }
      };
      store = new EntityStore(app, null)
      {
        @Override
        public <T extends Identifiable> T get(Class<T> type, long identifier)
        {
          return list(type)
              .stream()
              .filter(identifiable -> identifiable.getId() == identifier)
              .findFirst()
              .orElse(null);
        }

        @Override
        public <T extends Identifiable> List<T> list(Class<T> type)
        {
          return (List<T>) new ArrayList<>(mockCache.get(type));
        }
      };
    }
  }

  /**
   * Sets up the parameterization structure, so only the parameters themselves
   * and the test method need to be set up.
   */
  public static class CommonTestBase
      extends CoreTestBase
  {
    static Collection<Object[]> params(Param... params)
    {
      return Stream.of(params)
          .map(Param::toParams)
          .collect(Collectors.toList());
    }

    public InnerTestArguments args(MethodValueCache<House> methodValueCache,
                                   ExpectedException thrown)
    {
      return new InnerTestArguments(methodValueCache, thrown, mockCache);
    }

    @Parameter(0)
    public String description;

    @Parameter(1)
    public List<House> inputHouses;

    @Parameter(2)
    public Consumer<InnerTestArguments> test;

    @Rule
    public ExpectedException thrown = ExpectedException.none();
  }

  public static class InnerTestArguments
  {
    MethodValueCache<House> methodValueCache;
    ExpectedException thrown = ExpectedException.none();
    Map<Class, List<Identifiable>> mockCache;

    public InnerTestArguments(MethodValueCache<House> methodValueCache,
                              ExpectedException thrown,
                              Map<Class, List<Identifiable>> mockCache)
    {
      this.methodValueCache = methodValueCache;
      this.thrown = thrown;
      this.mockCache = mockCache;
    }
  }

  /**
   * Generates an empty list of houses.
   */
  static List<House> empty()
  {
    return new ArrayList<>();
  }

  /**
   * Generates a list of various houses with miscellanous data.
   */
  static List<House> populated()
  {
    return new ArrayList<>(Arrays.asList(
        new House(1)
            .setCityId(3)
            .setOwner("Tom")
            .setDog("Itchy"),
        new House(2)
            .setCityId(5)
            .setOwner("Hank")
            .setDog("Scratchy"),
        new House(3)
            .setCityId(8)
            .setOwner("Joe")
            .setDog("Poppy"),
        new House(4)
            .setCityId(10)
            .setOwner("Boe")
            .setDog("Cat"),
        new House(5)
            .setCityId(7)
            .setOwner("Boe")
            .setDog("Cow"),
        new House(6)
            .setCityId(10)
            .setOwner("Moe")
            .setDog("Poppy"),
        new House(7)
            .setCityId(5)
            .setOwner("Boe")
            .setDog("Spot"),
        new House(8)
            .setCityId(7)
            .setOwner("John")
            .setDog("Cow"),
        new House(9)
            .setCityId(11)
            .setOwner("Moe")
            .setDog("Poppy"),
        new House(10)
            .setCityId(12)
            .setOwner("Moe")
            .setDog("Cupcake"),
        new House(11)
            .setCityId(11)
            .setOwner("Moe")
            .setDog("Poppy")
    ));
  }

  @RunWith(Parameterized.class)
  public static class getObject extends CommonTestBase
  {
    @Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
      return params(
          new Param()
          {{
            description = "should find no matching entities if no entities " +
                "are present.";
            inputHouses = empty();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObject(getOwner, "Hank");
                assertNull(result);
              }
            };
          }},
          new Param()
          {{
            description = "should find no matching entities if no entities " +
                "are matching.";
            inputHouses = populated();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObject(getOwner, "Shmoe");
                assertNull(result);
              }
            };
          }},
          new Param()
          {{
            description = "should get the matching entity when queried by " +
                "a single value if only one entity is matching.";
            inputHouses = populated();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObject(getOwner, "Hank");
                assertNotNull(result);
                assertEquals(2, result.getId());
              }
              {
                House result = args.methodValueCache
                    .getObject(getCityId, 8);
                assertNotNull(result);
                assertEquals(3, result.getId());
              }
              {
                House result = args.methodValueCache
                    .getObject(getDog, "Itchy");
                assertNotNull(result);
                assertEquals(1, result.getId());
              }
            };
          }},
          new Param()
          {{
            description = "should get any matching entity when queried by " +
                "a single value if multiple entities are matching.";
            inputHouses = populated();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObject(getCityId, 10);
                assertNotNull(result);
                assertTrue(Arrays.asList(4L, 6L).contains(result.getId()));
              }
              {
                House result = args.methodValueCache
                    .getObject(getOwner, "Boe");
                assertNotNull(result);
                assertTrue(Arrays.asList(4L, 5L, 7L).contains(result.getId()));
              }
            };
          }}
      );
    }

    @Test
    public void test()
    {
      mockCache.get(House.class).addAll(inputHouses);

      MethodValueCache<House> methodValueCache = new MethodValueCache<>(store,
          House.class);
      methodValueCache.addMethod(getCityId);
      methodValueCache.addMethod(getOwner);
      methodValueCache.addMethod(getDog);
      test.accept(args(methodValueCache, thrown));
    }
  }

  @RunWith(Parameterized.class)
  public static class getObjectInt extends CommonTestBase
  {
    @Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
      return params(
          new Param()
          {{
            description = "should find no matching entities if no entities " +
                "are present.";
            inputHouses = empty();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObjectInt(new FieldIntersection<>(House.class,
                        getOwner, "Hank"));
                assertNull(result);
              }
            };
          }},
          new Param()
          {{
            description = "should find no matching entities if no entities " +
                "are matching.";
            inputHouses = populated();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObjectInt(new FieldIntersection<>(House.class,
                        getOwner, "Shmoe"));
                assertNull(result);
              }
            };
          }},
          new Param()
          {{
            description = "should get the matching entity when queried by " +
                "a single value if only one entity is matching.";
            inputHouses = populated();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObjectInt(new FieldIntersection<>(House.class,
                        getOwner, "Hank"));
                assertNotNull(result);
                assertEquals(2, result.getId());
              }
              {
                House result = args.methodValueCache
                    .getObjectInt(new FieldIntersection<>(House.class,
                        getCityId, 8));
                assertNotNull(result);
                assertEquals(3, result.getId());
              }
              {
                House result = args.methodValueCache
                    .getObjectInt(new FieldIntersection<>(House.class,
                        getDog, "Itchy"));
                assertNotNull(result);
                assertEquals(1, result.getId());
              }
            };
          }},
          new Param()
          {{
            description = "should get any matching entity when queried by " +
                "a single value if multiple entities are matching.";
            inputHouses = populated();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObjectInt(new FieldIntersection<>(House.class,
                        getCityId, 10));
                assertNotNull(result);
                assertTrue(Arrays.asList(4L, 6L).contains(result.getId()));
              }
              {
                House result = args.methodValueCache
                    .getObjectInt(new FieldIntersection<>(House.class,
                        getOwner, "Boe"));
                assertNotNull(result);
                assertTrue(Arrays.asList(4L, 5L, 7L).contains(result.getId()));
              }
            };
          }},
          new Param()
          {{
            description = "should get the matching entity when queried by " +
                "multiple values if only one entity is matching.";
            inputHouses = populated();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObjectInt(
                        new FieldIntersection(House.class,
                            getOwner, "Boe",
                            getDog, "Cat"));
                assertNotNull(result);
                assertEquals(4, result.getId());
              }
              {
                House result = args.methodValueCache
                    .getObjectInt(new FieldIntersection<>(House.class,
                        getCityId, 8,
                        getOwner, "Joe"));
                assertNotNull(result);
                assertEquals(3, result.getId());
              }
              {
                House result = args.methodValueCache
                    .getObjectInt(new FieldIntersection<>(House.class,
                        getDog, "Spot",
                        getCityId, 5));
                assertNotNull(result);
                assertEquals(7, result.getId());
              }
            };
          }},
          new Param()
          {{
            description = "should get any matching entity when queried by " +
                "multiple values if multiple entities are matching.";
            inputHouses = populated();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObjectInt(new FieldIntersection<>(House.class,
                        getCityId, 7,
                        getDog, "Cow"));
                assertNotNull(result);
                assertTrue(Arrays.asList(5L, 8L).contains(result.getId()));
              }
              {
                House result = args.methodValueCache
                    .getObjectInt(new FieldIntersection<>(House.class,
                        getOwner, "Moe",
                        getDog, "Poppy"));
                assertNotNull(result);
                assertTrue(Arrays.asList(6L, 9L, 11L).contains(result.getId()));
              }
            };
          }}
      );
    }

    @Test
    public void test()
    {
      mockCache.get(House.class).addAll(inputHouses);

      MethodValueCache<House> methodValueCache = new MethodValueCache<>(store,
          House.class);
      methodValueCache.addMethod(getCityId);
      methodValueCache.addMethod(getOwner);
      methodValueCache.addMethod(getDog);
      test.accept(args(methodValueCache, thrown));
    }
  }

  @RunWith(Parameterized.class)
  public static class getObjects extends CommonTestBase
  {
    @Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
      return params(
          new Param()
          {{
            description = "should find no matching entities if no entities " +
                "are present.";
            inputHouses = empty();
            test = args -> {
              {
                List<House> result = args.methodValueCache
                    .getObjects(getOwner, "Hank");
                assertNotNull(result);
                assertTrue(result.isEmpty());
              }
            };
          }},
          new Param()
          {{
            description = "should find no matching entities if no entities " +
                "are matching.";
            inputHouses = populated();
            test = args -> {
              {
                List<House> result = args.methodValueCache
                    .getObjects(getOwner, "Shmoe");
                assertNotNull(result);
                assertTrue(result.isEmpty());
              }
            };
          }},
          new Param()
          {{
            description = "should get the matching entity when queried by " +
                "a single value if only one entity is matching.";
            inputHouses = populated();
            test = args -> {
              {
                List<House> result = args.methodValueCache
                    .getObjects(getOwner, "Hank");
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(2L)),
                    result.stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
              {
                List<House> result = args.methodValueCache
                    .getObjects(getCityId, 8);
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(3L)),
                    result
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
              {
                List<House> result = args.methodValueCache
                    .getObjects(getDog, "Itchy");
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(1L)),
                    result.stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
            };
          }},
          new Param()
          {{
            description = "should get all matching entities when queried by " +
                "a single value if multiple entities are matching.";
            inputHouses = populated();
            test = args -> {
              {
                List<House> result = args.methodValueCache
                    .getObjects(getCityId, 10);
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(4L, 6L)),
                    result
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
              {
                List<House> result = args.methodValueCache
                    .getObjects(getOwner, "Boe");
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(4L, 5L, 7L)),
                    result
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
            };
          }}
      );
    }

    @Test
    public void test()
    {
      mockCache.get(House.class).addAll(inputHouses);

      MethodValueCache<House> methodValueCache = new MethodValueCache<>(store,
          House.class);
      methodValueCache.addMethod(getCityId);
      methodValueCache.addMethod(getOwner);
      methodValueCache.addMethod(getDog);
      test.accept(args(methodValueCache, thrown));
    }
  }

  @RunWith(Parameterized.class)
  public static class getObjectsInt extends CommonTestBase
  {
    @Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
      return params(
          new Param()
          {{
            description = "should find no matching entities if no entities " +
                "are present.";
            inputHouses = empty();
            test = args -> {
              {
                List<House> result = args.methodValueCache
                    .getObjectsInt(new FieldIntersection<>(House.class,
                        getOwner, "Hank"));
                assertNotNull(result);
                assertTrue(result.isEmpty());
              }
            };
          }},
          new Param()
          {{
            description = "should find no matching entities if no entities " +
                "are matching.";
            inputHouses = populated();
            test = args -> {
              {
                List<House> result = args.methodValueCache
                    .getObjectsInt(new FieldIntersection<>(House.class,
                        getOwner, "Shmoe"));
                assertNotNull(result);
                assertTrue(result.isEmpty());
              }
            };
          }},
          new Param()
          {{
            description = "should get the matching entity when queried by " +
                "a single value if only one entity is matching.";
            inputHouses = populated();
            test = args -> {
              {
                List<House> result = args.methodValueCache
                    .getObjectsInt(new FieldIntersection<>(House.class,
                        getOwner, "Boe",
                        getDog, "Cat"));
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(4L)),
                    result
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
              {
                List<House> result = args.methodValueCache
                    .getObjectsInt(new FieldIntersection<>(House.class,
                        getCityId, 8,
                        getOwner, "Joe"));
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(3L)),
                    result
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
              {
                List<House> result = args.methodValueCache
                    .getObjectsInt(new FieldIntersection<>(House.class,
                        getDog, "Spot",
                        getCityId, 5));
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(7L)),
                    result
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
            };
          }},
          new Param()
          {{
            description = "should get all matching entities when queried by " +
                "a single value if multiple entities are matching.";
            inputHouses = populated();
            test = args -> {
              {
                List<House> result = args.methodValueCache
                    .getObjectsInt(new FieldIntersection<>(House.class,
                        getCityId, 10));
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(4L, 6L)),
                    result
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
              {
                List<House> result = args.methodValueCache
                    .getObjectsInt(new FieldIntersection<>(House.class,
                        getOwner, "Boe"));
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(4L, 5L, 7L)),
                    result
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
            };
          }},
          new Param()
          {{
            description = "should get the matching entity when queried by " +
                "multiple values if only one entity is matching.";
            inputHouses = populated();
            test = args -> {
              {
                List<House> result = args.methodValueCache
                    .getObjectsInt(new FieldIntersection<>(House.class,
                        getOwner, "Hank"));
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(2L)),
                    result
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
              {
                List<House> result = args.methodValueCache
                    .getObjectsInt(new FieldIntersection<>(House.class,
                        getCityId, 8));
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(3L)),
                    result
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
              {
                List<House> result = args.methodValueCache
                    .getObjectsInt(new FieldIntersection<>(House.class,
                        getDog, "Itchy"));
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(1L)),
                    result.stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
            };
          }},
          new Param()
          {{
            description = "should get all matching entities when queried by " +
                "multiple values if multiple entities are matching.";
            inputHouses = populated();
            test = args -> {
              {
                List<House> result = args.methodValueCache
                    .getObjectsInt(new FieldIntersection<>(House.class,
                        getCityId, 7,
                        getDog, "Cow"));
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(5L, 8L)),
                    result
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
              {
                List<House> result = args.methodValueCache
                    .getObjectsInt(new FieldIntersection<>(House.class,
                        getOwner, "Moe",
                        getDog, "Poppy"));
                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(6L, 9L, 11L)),
                    result
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
            };
          }},
          new Param()
          {{
            description = "should get all matching entities when queried " +
                "with a WhereInSet value if multiple entities are matching.";
            inputHouses = populated();
            test = args -> {
              {
                List<House> result = args.methodValueCache
                    .getObjectsInt(new FieldIntersection<>(House.class,
                        getCityId, new WhereInSet(Arrays.asList(10, 12)),
                        getOwner, "Moe"
                    ));

                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(6L, 10L)),
                    result
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
              {
                List<House> result = args.methodValueCache
                    .getObjectsInt(new FieldIntersection<>(House.class,
                        getDog, new WhereInSet(
                        Arrays.asList("Poppy", "Cow", "Cupcake", "Cat")),
                        getOwner, new WhereInSet(Arrays.asList("Boe", "Joe"))
                    ));

                assertNotNull(result);
                assertEquals(
                    new HashSet<>(Arrays.asList(3L, 4L, 5L)),
                    result
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
            };
          }}
      );
    }

    @Test
    public void test()
    {
      mockCache.get(House.class).addAll(inputHouses);

      MethodValueCache<House> methodValueCache = new MethodValueCache<>(store,
          House.class);
      methodValueCache.addMethod(getCityId);
      methodValueCache.addMethod(getOwner);
      methodValueCache.addMethod(getDog);
      test.accept(args(methodValueCache, thrown));
    }
  }

  @RunWith(Parameterized.class)
  public static class addMethod extends CommonTestBase
  {
    @Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
      return params(
          new Param()
          {{
            description = "should complete gracefully if a real method is " +
                "added.";
            inputHouses = empty();
            test = args -> {
              {
                args.methodValueCache.addMethod(getOwner);
              }
            };
          }},
          new Param()
          {{
            description = "should throw an exception if a method is added " +
                "that does not exist.";
            inputHouses = empty();
            test = args -> {
              args.thrown.expect(IllegalArgumentException.class);
              args.methodValueCache.addMethod("getOwners");
            };
          }}
      );
    }

    @Test
    public void test()
    {
      MethodValueCache<House> methodValueCache = new MethodValueCache<>(store,
          House.class);
      test.accept(args(methodValueCache, thrown));
    }
  }

  @RunWith(Parameterized.class)
  public static class delete extends CommonTestBase
  {
    @Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
      return params(
          new Param()
          {{
            description = "should remove entities from the map of matching " +
                "objects, and should work if it was the only match.";
            inputHouses = populated();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObject(getDog, "Scratchy");
                assertNotNull(result);
                assertEquals(2, result.getId());
                args.mockCache.get(House.class).remove(result);
                args.methodValueCache.delete(result.getId());
                assertNull(args.methodValueCache
                    .getObject(getDog, "Scratchy"));
              }
            };
          }},
          new Param()
          {{
            description = "should remove entities from the map of matching " +
                "objects, and should work if it was not the only match.";
            inputHouses = populated();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObjects(getOwner, "Boe")
                    .stream()
                    .filter(house -> house.getId() == 5)
                    .findFirst()
                    .orElse(null);
                assertNotNull(result);
                args.mockCache.get(House.class).remove(result);
                args.methodValueCache.delete(result.getId());
                assertEquals(
                    new HashSet<>(Arrays.asList(4L, 7L)),
                    args.methodValueCache
                        .getObjects(getOwner, "Boe")
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
            };
          }},
          new Param()
          {{
            description = "should remove entities from the map of matching " +
                "objects, including their indexed methods' return values " +
                "for multi-value indexes.";
            inputHouses = populated();
            test = args -> {
              {
                House result = (House) args.mockCache.get(House.class)
                    .stream()
                    .filter(house -> house.getId() == 6L)
                    .findFirst()
                    .orElse(null);

                assertNotNull(result);
                assertEquals(6, result.getId());

                // Trigger the population of the cached values
                args.methodValueCache.getObjectsInt(
                    new FieldIntersection<>(House.class,
                        getOwner, "Moe",
                        getDog, "Poppy"));

                args.mockCache.get(House.class).remove(result);
                args.methodValueCache.delete(result.getId());

                assertEquals(
                    new HashSet<>(Arrays.asList(9L, 11L)),
                    args.methodValueCache.getObjectsInt(
                        new FieldIntersection<>(House.class,
                            getOwner, "Moe",
                            getDog, "Poppy"))
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
            };
          }}
      );
    }

    @Test
    public void test()
    {
      mockCache.get(House.class).addAll(inputHouses);

      MethodValueCache<House> methodValueCache = new MethodValueCache<>(store,
          House.class);
      methodValueCache.addMethod(getCityId);
      methodValueCache.addMethod(getOwner);
      methodValueCache.addMethod(getDog);
      test.accept(args(methodValueCache, thrown));
    }
  }

  @RunWith(Parameterized.class)
  public static class reset extends CommonTestBase
  {
    @Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
      return params(
          new Param()
          {{
            description = "should update the cache to reflect all " +
                "added/removed entities.";
            inputHouses = populated();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObject(getDog, "Scratchy");
                House resultB = args.methodValueCache
                    .getObject(getDog, "Itchy");

                assertNotNull(result);
                assertEquals(2, result.getId());
                assertEquals(1, resultB.getId());

                args.mockCache.get(House.class).remove(result);
                args.mockCache.get(House.class).remove(resultB);
                args.methodValueCache.delete(result.getId());
                args.methodValueCache.delete(resultB.getId());

                assertNull("should be missing after deletion",
                    args.methodValueCache.getObject(getDog, "Scratchy"));
                assertNull("should be missing after deletion",
                    args.methodValueCache.getObject(getDog, "Itchy"));

                args.mockCache.get(House.class).add(result);
                args.mockCache.get(House.class).add(resultB);

                assertNull("should still be missing before reset",
                    args.methodValueCache.getObject(getDog, "Scratchy"));
                assertNull("should still be missing before reset",
                    args.methodValueCache.getObject(getDog, "Itchy"));

                args.methodValueCache.reset();

                assertNotNull("should be re-added after reset",
                    args.methodValueCache.getObject(getDog, "Scratchy"));
                assertNotNull("should be re-added after reset",
                    args.methodValueCache.getObject(getDog, "Itchy"));
              }
            };
          }},
          new Param()
          {{
            description = "should update the cache to reflect all changes " +
                "in entities and their indexed methods' return values.";
            inputHouses = populated();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObject(getDog, "Scratchy");
                House resultB = args.methodValueCache
                    .getObject(getDog, "Itchy");

                assertNotNull(result);
                assertNotNull(resultB);
                assertEquals(2, result.getId());
                assertEquals(1, resultB.getId());

                result.setDog("Cat");
                resultB.setDog("Cat");

                assertNotNull("should not detect changes until after " +
                        "reset",
                    args.methodValueCache.getObject(getDog, "Scratchy"));
                assertNotNull("should not detect changes until after " +
                        "reset",
                    args.methodValueCache.getObject(getDog, "Itchy"));

                args.methodValueCache.reset();

                assertNull("should not be associated with old " +
                        "value after reset",
                    args.methodValueCache.getObject(getDog, "Scratchy"));
                assertNull("should not be associated with old " +
                        "value after reset",
                    args.methodValueCache.getObject(getDog, "Itchy"));

                assertEquals(
                    new HashSet<>(Arrays.asList(1L, 2L, 4L)),
                    args.methodValueCache.getObjects(getDog, "Cat")
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
            };
          }},
          new Param()
          {{
            description = "should update the cache to reflect all changes " +
                "in entities and their indexed methods' return values for " +
                "multi-value indexes.";
            inputHouses = populated();
            test = args -> {
              {
                House result = (House) args.mockCache.get(House.class)
                    .stream()
                    .filter(house -> house.getId() == 6L)
                    .findFirst()
                    .orElse(null);
                House resultB = (House) args.mockCache.get(House.class)
                    .stream()
                    .filter(house -> house.getId() == 9L)
                    .findFirst()
                    .orElse(null);

                assertNotNull(result);
                assertNotNull(resultB);
                assertEquals(6, result.getId());
                assertEquals(9, resultB.getId());

                // Trigger the population of the cached values
                args.methodValueCache.getObjectsInt(
                    new FieldIntersection<>(House.class,
                        getOwner, "Moe",
                        getDog, "Poppy"));

                result.setDog("Cat");
                resultB.setDog("Cat");

                assertEquals("should not detect changes until after " +
                        "reset",
                    new HashSet<>(Arrays.asList(6L, 9L, 11L)),
                    args.methodValueCache.getObjectsInt(
                        new FieldIntersection<>(House.class,
                            getOwner, "Moe",
                            getDog, "Poppy"))
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));

                args.methodValueCache.reset();

                assertEquals(
                    new HashSet<>(Arrays.asList(11L)),
                    args.methodValueCache.getObjectsInt(
                        new FieldIntersection<>(House.class,
                            getOwner, "Moe",
                            getDog, "Poppy"))
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
            };
          }}
      );
    }

    @Test
    public void test()
    {
      mockCache.get(House.class).addAll(inputHouses);

      MethodValueCache<House> methodValueCache = new MethodValueCache<>(store,
          House.class);
      methodValueCache.addMethod(getCityId);
      methodValueCache.addMethod(getOwner);
      methodValueCache.addMethod(getDog);
      test.accept(args(methodValueCache, thrown));
    }
  }

  @RunWith(Parameterized.class)
  public static class update extends CommonTestBase
  {
    @Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
      return params(
          new Param()
          {{
            description = "should update the cache to reflect changes to an " +
                "entity if it has been added/removed.";
            inputHouses = populated();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObject(getDog, "Scratchy");
                House resultB = args.methodValueCache
                    .getObject(getDog, "Itchy");

                assertNotNull(result);
                assertEquals(2, result.getId());
                assertEquals(1, resultB.getId());

                args.mockCache.get(House.class).remove(result);
                args.mockCache.get(House.class).remove(resultB);
                args.methodValueCache.update(result.getId());
                args.methodValueCache.update(resultB.getId());

                assertNull("should be missing after deletion",
                    args.methodValueCache.getObject(getDog, "Scratchy"));
                assertNull("should be missing after deletion",
                    args.methodValueCache.getObject(getDog, "Itchy"));

                args.mockCache.get(House.class).add(result);
                args.mockCache.get(House.class).add(resultB);

                assertNull("should still be missing before update",
                    args.methodValueCache.getObject(getDog, "Scratchy"));
                assertNull("should still be missing before update",
                    args.methodValueCache.getObject(getDog, "Itchy"));

                args.methodValueCache.update(result.getId());

                assertNotNull("should be re-added after update",
                    args.methodValueCache.getObject(getDog, "Scratchy"));
                assertNull("should still be missing without update",
                    args.methodValueCache.getObject(getDog, "Itchy"));
              }
            };
          }},
          new Param()
          {{
            description = "should update the cache to reflect any changes " +
                "in entities and their indexed methods' return values.";
            inputHouses = populated();
            test = args -> {
              {
                House result = args.methodValueCache
                    .getObject(getDog, "Scratchy");
                House resultB = args.methodValueCache
                    .getObject(getDog, "Itchy");

                assertNotNull(result);
                assertNotNull(resultB);
                assertEquals(2, result.getId());
                assertEquals(1, resultB.getId());

                result.setDog("Cat");
                resultB.setDog("Cat");

                assertNotNull("should not detect changes until after " +
                        "update",
                    args.methodValueCache.getObject(getDog, "Scratchy"));
                assertNotNull("should not detect changes until after " +
                        "update",
                    args.methodValueCache.getObject(getDog, "Itchy"));

                args.methodValueCache.update(result.getId());

                assertNull("should not be associated with old " +
                        "value after reset",
                    args.methodValueCache.getObject(getDog, "Scratchy"));
                assertNotNull("should still be associated with old " +
                        "value without update",
                    args.methodValueCache.getObject(getDog, "Itchy"));

                assertEquals(
                    new HashSet<>(Arrays.asList(2L, 4L)),
                    args.methodValueCache.getObjects(getDog, "Cat")
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
            };
          }},
          new Param()
          {{
            description = "should update the cache to reflect any changes " +
                "in entities and their indexed methods' return values for " +
                "multi-value indexes.";
            inputHouses = populated();
            test = args -> {
              {
                House result = (House) args.mockCache.get(House.class)
                    .stream()
                    .filter(house -> house.getId() == 6L)
                    .findFirst()
                    .orElse(null);
                House resultB = (House) args.mockCache.get(House.class)
                    .stream()
                    .filter(house -> house.getId() == 9L)
                    .findFirst()
                    .orElse(null);

                assertNotNull(result);
                assertNotNull(resultB);
                assertEquals(6, result.getId());
                assertEquals(9, resultB.getId());

                // Trigger the population of the cached values
                args.methodValueCache.getObjectsInt(
                    new FieldIntersection<>(House.class,
                        getOwner, "Moe",
                        getDog, "Poppy"));

                result.setDog("Cat");
                resultB.setDog("Cat");

                assertEquals("should not detect changes until after " +
                        "update",
                    new HashSet<>(Arrays.asList(6L, 9L, 11L)),
                    args.methodValueCache.getObjectsInt(
                        new FieldIntersection<>(House.class,
                            getOwner, "Moe",
                            getDog, "Poppy"))
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));

                args.methodValueCache.update(result.getId());

                assertEquals("should only affect mapping for updated " +
                        "entity.",
                    new HashSet<>(Arrays.asList(9L, 11L)),
                    args.methodValueCache.getObjectsInt(
                        new FieldIntersection<>(House.class,
                            getOwner, "Moe",
                            getDog, "Poppy"))
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));

                args.methodValueCache.update(resultB.getId());

                assertEquals(
                    new HashSet<>(Arrays.asList(11L)),
                    args.methodValueCache.getObjectsInt(
                        new FieldIntersection<>(House.class,
                            getOwner, "Moe",
                            getDog, "Poppy"))
                        .stream()
                        .map(House::getId)
                        .collect(Collectors.toSet()));
              }
            };
          }}
      );
    }

    @Test
    public void test()
    {
      mockCache.get(House.class).addAll(inputHouses);

      MethodValueCache<House> methodValueCache = new MethodValueCache<>(store,
          House.class);
      methodValueCache.addMethod(getCityId);
      methodValueCache.addMethod(getOwner);
      methodValueCache.addMethod(getDog);
      test.accept(args(methodValueCache, thrown));
    }
  }
}