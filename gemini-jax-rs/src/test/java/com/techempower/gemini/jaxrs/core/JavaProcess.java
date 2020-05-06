package com.techempower.gemini.jaxrs.core;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

// Modified from https://stackoverflow.com/a/35275894 and https://stackoverflow.com/a/58259581
public final class JavaProcess
{
  private JavaProcess()
  {
  }

  public static String exec(Class<?> klass, String... args)
      throws IOException, InterruptedException
  {
    String javaHome = System.getProperty("java.home");
    String javaBin = javaHome +
        File.separator + "bin" +
        File.separator + "java";
    String classpath = System.getProperty("java.class.path");
    String className = klass.getName();

    List<String> command = new LinkedList<>();
    command.add(javaBin);
    command.add("-cp");
    command.add(classpath);
    command.add(className);
    if (args != null)
    {
      command.addAll(Arrays.asList(args));
    }

    ProcessBuilder builder = new ProcessBuilder(command);
    String result = "";
    Process process;
    try
    {
      process = builder.start();
      result = new String(process.getInputStream().readAllBytes());

      process.waitFor();
      process.destroy();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return result;
  }
}
