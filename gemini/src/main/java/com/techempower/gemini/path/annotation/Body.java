package com.techempower.gemini.path.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.techempower.gemini.path.RequestBodyAdapter;

/**
 * <p>An annotation indicating that a handler method expects some
 * content to be present in the request's body, and that it should
 * be transformed in some way before being passed to the handler
 * method.</p>
 *
 * <p>You must specify a concrete adapter class that implements
 * {@link RequestBodyAdapter}. The result of invoking that adapter
 * is passed as the *last* argument to your handler method. If your
 * handler method also accepts path parameters, they should be before
 * the body parameter, otherwise the method should accept just one
 * argument for the transformed body.</p>
 *
 * <p>For example:</p>
 *
 * <pre><code>
 *   &#064;Path("foo/{id}")
 *   &#064;Put
 *   &#064;JsonBody // same as &#064;Body(JsonRequestBodyAdapter.class)
 *   public boolean handleUpdateFoo(long id, Foo foo) {
 *     // Handler logic...
 *   }
 * </code></pre>
 *
 * <p>You can also create your own annotations as a shorthand for this
 * if you wish re-use the same adapter without having to specify it
 * directly. Simply create an annotation that is annotated with @Body,
 * then annotate your handler methods with your own annotation.</p>
 *
 * <pre><code>
 *   &#064;Target(ElementType.METHOD)
 *   &#064;Retention(RetentionPolicy.RUNTIME)
 *   &#064;Body(MyXmlRequestBodyAdapter.class)
 *   public @interface XmlBody
 *   {
 *   }
 * </code></pre>
 *
 * <p>Intended to be used only with
 * {@link com.techempower.gemini.path.MethodUriHandler}.</p>
 *
 * @see ConsumesJson
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Body
{
  Class<? extends RequestBodyAdapter<?>> value();
}
