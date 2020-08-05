package com.techempower.gemini.jaxrs.core;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Performance
{
  private Performance()
  {
  }

  public static void test(Class<?> type,
                          int iterationsMetadata,
                          List<List<String>> options)
  {
    test(true, type, iterationsMetadata, options);
  }

  /** Set fork to false to get better error reporting. Otherwise always have
   * it as true for the best/most fair performance reporting. */
  public static void test(boolean fork,
                          Class<?> type,
                          int iterationsMetadata,
                          List<List<String>> options)
  {
    try
    {
      System.out.println(String.format("Running performance test `%s`...",
          type.getSimpleName()));
      long start = System.currentTimeMillis();
      Map<List<String>, Long> totalNanosecondsByOption = new LinkedHashMap<>();
      long COUNT = 10;
      for (int i = 0; i < COUNT; i++)
      {
        for (List<String> args : options)
        {
          String output;
          if (fork)
          {
            output = JavaProcess.exec(type, args.toArray(String[]::new));
          }
          else
          {
            PrintStream originalOut = System.out;
            try
            {
              final ByteArrayOutputStream baos = new ByteArrayOutputStream();
              final String utf8 = StandardCharsets.UTF_8.name();
              try (PrintStream ps = new PrintStream(baos, true, utf8))
              {
                System.setOut(ps);
                type.getDeclaredMethod("main", String[].class)
                    .invoke(null, (Object) args.toArray(String[]::new));
                output = baos.toString(utf8);
              }
            }
            finally
            {
              System.setOut(originalOut);
            }

          }
          if (output.isEmpty())
          {
            throw new RuntimeException("Output was empty for args: "
                + String.join("::", args));
          }
          long milli = Long.parseLong(output);
          totalNanosecondsByOption.compute(args, (key, value) ->
              (value == null ? 0 : value) + milli);
        }
      }
      System.out.println(String.format("    Total time overall: %sms",
          System.currentTimeMillis() - start));
      DecimalFormat commas = new DecimalFormat("#,###");
      DecimalFormat decimal = new DecimalFormat("#.0");
      System.out.println(String.format("The following are the milliseconds" +
          " required for each approach to run %s times, averaged over %s" +
          " separate runs:", commas.format(iterationsMetadata), COUNT));
      totalNanosecondsByOption.forEach((args, totalNano) ->
          System.out.println(String.format("    %s: %sms",
              String.join("::", args),
              decimal.format((double) totalNano / 1e6 / COUNT))));
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  public static <R extends Throwable> void time(ThrowingRunnable<R> runnable)
      throws R
  {
    runnable.run();
    long start = System.nanoTime();
    runnable.run();
    System.out.print(System.nanoTime() - start);
  }

  @FunctionalInterface
  public interface ThrowingRunnable<R extends Throwable>
  {
    void run() throws R;
  }
}
