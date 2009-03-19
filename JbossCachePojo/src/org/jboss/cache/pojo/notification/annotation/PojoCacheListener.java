/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.cache.pojo.notification.annotation;

import org.jboss.cache.pojo.notification.NotificationContext;
import org.jboss.cache.pojo.notification.event.AttachedEvent;
import org.jboss.cache.pojo.notification.event.DetachedEvent;
import org.jboss.cache.pojo.notification.event.FieldModifiedEvent;
import org.jboss.cache.pojo.notification.event.ListModifiedEvent;
import org.jboss.cache.pojo.notification.event.MapModifiedEvent;
import org.jboss.cache.pojo.notification.event.SetModifiedEvent;
import org.jboss.cache.pojo.notification.event.TransactionCompletedEvent;
import org.jboss.cache.pojo.notification.event.TransactionRegisteredEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a class should receive POJO notification events. The class may
 * have zero or more annotated notification methods. Each method may have any
 * name, but must have a method signature that contains the required event type
 * (or super type).
 * <p/>
 * <p/>
 * There can be multiple methods that are annotated to receive the same event,
 * and a method may receive multiple events by using a super type.
 * <p/>
 * <h4>Delivery Semantics</h4>
 * <p/>
 * An event is delivered immediately after the
 * respective operation, but before the underlying cache call returns. For this
 * reason it is important to keep listener processing logic short-lived. If a
 * long running task needs to be performed, it's recommended to use another
 * thread.
 * <p/>
 * <h4>Transactional Semantics</h4>
 * <p/>
 * Since the event is delivered during the actual cache call, the transactional
 * outcome is not yet known. For this reason, <i>events are always delivered, even
 * if the changes they represent are discarded by their containing transaction</i>.
 * For applications that must only process events that represent changes in a
 * completed transaction, {@link NotificationContext#getTransaction()} can be used,
 * along with {@link TransactionCompletedEvent#isSuccessful()} to record events and
 * later process them once the transaction has been successfully committed.
 * Example 4 demonstrates this.
 * <p/>
 * <h4>Threading Semantics</h4>
 * <p/>
 * A listener implementation must be capable of handling concurrent invocations. Local
 * notifications reuse the calling thread; remote notifications reuse the network thread.
 * <p/>
 * <p/>
 * <b>Summary of Notification Annotations</b>
 * <table border="1" cellpadding="1" cellspacing="1" summary="Summary of notification annotations">
 * <tr>
 * <th bgcolor="#CCCCFF" align="left">Annotation</th>
 * <th bgcolor="#CCCCFF" align="left">Event</th>
 * <th bgcolor="#CCCCFF" align="left">Description</th>
 * </tr>
 * <tr>
 * <td valign="top">{@link Attached}</td>
 * <td valign="top">{@link AttachedEvent}</td>
 * <td valign="top">An object was attached.</td>
 * </tr>
 * <tr>
 * <td valign="top">{@link Detached}</td>
 * <td valign="top">{@link DetachedEvent}</td>
 * <td valign="top">An object was detached.</td>
 * </tr>
 * <tr>
 * <td valign="top">{@link FieldModified}</td>
 * <td valign="top">{@link FieldModifiedEvent}</td>
 * <td valign="top">An attached object's field was modified.</td>
 * </tr>
 * <tr>
 * <td valign="top">{@link ListModified}</td>
 * <td valign="top">{@link ListModifiedEvent}</td>
 * <td valign="top">An attached list was modified.</td>
 * </tr>
 * <tr>
 * <td valign="top">{@link SetModified}</td>
 * <td valign="top">{@link SetModifiedEvent}</td>
 * <td valign="top">An attached set was modified.</td>
 * </tr>
 * <tr>
 * <td valign="top">{@link MapModified}</td>
 * <td valign="top">{@link MapModifiedEvent}</td>
 * <td valign="top">An attached map was modified.</td>
 * </tr>
 * <tr>
 * <td valign="top">{@link TransactionRegistered}</td>
 * <td valign="top">{@link TransactionRegisteredEvent}</td>
 * <td valign="top">A transaction was registered.</td>
 * </tr>
 * <tr>
 * <td valign="top">{@link TransactionCompleted}</td>
 * <td valign="top">{@link TransactionCompletedEvent}</td>
 * <td valign="top">A transaction was completed.</td>
 * </tr>
 * </table>
 * <p/>
 * <h4>Example 1 - Method receiving a single event</h4>
 * <pre>
 *    &#064;PojoCacheListener
 *    public class SingleEventListener
 *    {
 *       &#064;Attached
 *       public void handleAttached(AttachedEvent event)
 *       {
 *          System.out.println(&quot;Attached = &quot; + event.getSource());
 *       }
 *    }
 * </pre>
 * <p/>
 * <h4>Example 2 - Method receiving multiple events</h4>
 * <pre>
 *    &#064;PojoCacheListener
 *    public class MultipleEventListener
 *    {
 *       &#064;Attached
 *       &#064;Detached
 *       public void handleAttachDetach(Event event)
 *       {
 *          if (event instanceof AttachedEvent)
 *             System.out.println(&quot;Attached = &quot; + event.getSource());
 *          else if (event instanceof DetachedEvent)
 *             System.out.println(&quot;Detached = &quot; + event.getSource());
 *       }
 *    }
 * </pre>
 * <p/>
 * <h4>Example 3 - Multiple methods receiving the same event</h4>
 * <pre>
 *    &#064;PojoCacheListener
 *    public class SingleEventListener
 *    {
 *       &#064;Attached
 *       public void handleAttached(AttachedEvent event)
 *       {
 *          System.out.println(&quot;Attached = &quot; event.getSource());
 *       }
 *       &#064;Attached
 *       &#064;Detached
 *       &#064;FieldModified
 *       &#064;ListModified
 *       &#064;MapModified
 *       &#064;SetModified
 *       &#064;TransactionRegistered
 *       &#064;TransactionCompleted
 *       public void handleAll(Event event)
 *       {
 *          System.out.println(event);
 *       }
 *    }
 * </pre>
 * <p/>
 * <p/>
 * <b>Example 4 - Processing only events with a committed transaction.</b>
 * <p/>
 * <pre>
 *    &#064;PojoCacheListener
 *    public class TxGuaranteedListener
 *    {
 *       private class TxEventQueue
 *       {
 *          private ConcurrentMap&lt;Transaction, Queue&lt;Event&gt;&gt; map = new ConcurrentHashMap&lt;Transaction, Queue&lt;Event&gt;&gt;();
 * <p/>
 *          public void offer(Event event)
 *          {
 *             Queue&lt;Event&gt; queue = getQueue(event.getContext().getTransaction());
 *             queue.offer(event);
 *          }
 * <p/>
 *          private Queue&lt;Event&gt; getQueue(Transaction transaction)
 *          {
 *             Queue&lt;Event&gt; queue = map.get(transaction);
 *             if (queue == null)
 *             {
 *                queue = new ConcurrentLinkedQueue&lt;Event&gt;();
 *                map.putIfAbsent(transaction, queue);
 *             }
 * <p/>
 *             return queue;
 *          }
 * <p/>
 *          public Queue&lt;Event&gt; takeAll(Transaction transaction)
 *          {
 *             return map.remove(transaction);
 *          }
 *       }
 * <p/>
 *       private TxEventQueue events = new TxEventQueue();
 * <p/>
 *       &#064;Attached
 *       &#064;Detached
 *       &#064;FieldModified
 *       &#064;ListModified
 *       &#064;SetModified
 *       &#064;MapModified
 *       public void handle(Event event)
 *       {
 *          events.offer(event);
 *       }
 * <p/>
 *       &#064;TransactionCompleted
 *       public void handleTx(TransactionCompletedEvent event)
 *       {
 *          Queue&lt;Event&gt; completed = events.takeAll(event.getContext().getTransaction());
 *          if (completed != null &amp;&amp; event.isSuccessful())
 *             System.out.println("Comitted events = " + completed);
 *       }
 *    }
 * </pre>
 *
 * @author Jason T. Greene
 * @see org.jboss.cache.notifications.annotation.CacheListener
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PojoCacheListener
{
}