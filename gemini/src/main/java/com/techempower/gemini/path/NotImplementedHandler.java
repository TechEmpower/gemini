package com.techempower.gemini.path;

import com.techempower.gemini.BasicContext;

public class NotImplementedHandler<C extends BasicContext>
  implements PathHandler<C>
{
  @Override
  public boolean prehandle(PathSegments segments, C context) {
    //Do Nothing
    return false;
  }

  @Override
  public boolean handle(PathSegments segments, C context)
  {
    context.setStatus(501);
    return true;
  }

  @Override
  public void posthandle(PathSegments segments, C context)
  {
    // Do nothing
  }
}
