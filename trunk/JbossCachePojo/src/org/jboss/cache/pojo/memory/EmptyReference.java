package org.jboss.cache.pojo.memory;

public class EmptyReference extends PersistentReference
{

   public EmptyReference()
   {
      super(null, null, REFERENCE_WEAK);
   }

   public Object rebuildReference() throws Exception
   {
      return null;
   }

}

