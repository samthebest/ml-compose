
// Question: Are Hadean processes disjoint in their memory space? Any way we can share memory across processes?

// Because we may want to include a Node level concept of a Closure (like a BV in spark), except more general (so can
// allow things like exeucting something once on each node).

trait MapShuffle[M[_, _] {
  // P is a type for a deserialised parition, e.g. Iterator[String]
  // f takes (serialised raw data, deserialised objects, serialised raw closure, deserialised closure)
  // We optionally return the closure to allow down downstream processes to re-use it

  // Note each param in and out of `f` can be "optional" (i.e. null / empty)
  // Note f is entirely responsible for serialisation and keeping track of what is and isn't serialised
  def mapPartitions[PIn, POut, C](f: (Iterator[Byte], PIn, Iterator[Byte], C) => (Iterator[Byte], POut, Iterator[Byte], C),
                                  closure: Iterator[Byte],
                                  m: M[PIn, C]): M[POut, C]

  // settings control how many output partitions, and potentially other stuff.
  // If retainClosure = true, and M re-uses processes, then the Closure is preserved, and so the returned M if chained with
  // another operation can re-use the Closure. This logic might get confusing if the number of output partitions differs.
  def shuffle[Pin, C](f: (Iterator[Byte], Pin, Iterator[Byte], C) => (Iterator[(Long, Iterator[Byte])], Iterator[Byte], C),
                      closure: Iterator[Byte],
                      retainClosure: Boolean,
                      m: M[Pin, C],
                      settings: Settings): M[_, C]
  
  
  def read[P](reader: Reader[P], settings: Settings): M[P, _]

  def readAndCache[P](reader: Reader[P], settings: Settings): M[P, _]

  def write[P](writer: Writer[P], M[P, _]): Unit
  // TODO details
  def collect[P, T](collecter: Collecter[P, T], M[P, _]): T
  
  
  // So we should think of the Closure as the internal state of the partition processors. 
  // It's initial state needs to be serialised and distributed, but when chained with other operations it can be re-used.
  // The Closure stays with a partition processor until it's destroyed.  It doesn't make sense to move them around, as
  // such functionality can be acheived by considering shuffling data.
  
  // Furthermore the Closure should be thought of as different from data, although in theory we could eliminate it
  // by having it read as part of a Reader, but this moves more complexity into the Reader and the functions f.
  // This complexity will be highly non trivial for the user as the Closure is of course supposed to include actual Rust code.
  // The function f can only call functions from the closure.
  
  // My intuition is that Hadean will be able to provide compiler/language features to Rust such that users can easily 
  // serialise their closures.  Scala (someday) will have the ability to check at compile time whether a function
  // calls uses anything outside it's parameters.  Could Rust have this feature??
  
  // Closures ought to be mainly "code"/meta-data generated at runtime (e.g. some lookups, ADTs, etc), 
  // while object-data is read from source (a DB, filesystem, etc).
  
  // Of course all static functions are assumed to be accessible from any `f`.
  
  // In Scala not having such a design causes a lot of headaches as calling methods from a class 
  // (even where that method could easily be thought of as static) still requires the whole class to be serialised. This
  // plus OOP creates hellish programs.
  

  // # Caching

  // Note the `f` can even encapsulate cacheing for the mapPartitions.  The returned Iterator could be backed by some
  // Array (rather than being lazy), so if multiple operations are performed on the returned M[P, C] they could just be
  // reusing an in-memory thing rather than recomputing lazily.

  // It also means subsequent functions can be responsible for the freeing of cached data too, 
  // and therefore optimise how it's done (lazily vs non-lazily).

  // A shuffle will likely mean any caching is lost (since the returned data is just bytes and Long keys).  After a shuffle
  // A trivial mapPartitions could be done to add caching.


  // Node Closures / Broadcast State Machines

  // If Hadean had a Broadcast variable, this could abstract away a lot of nice things like:
  // - big lookups that exist once per node
  // - operations that ought to be done once per node
  // - monoidal aggregation (e.g. counting data at the same time as mapping it)

  // then in order to avoid making the mapPartitions interface any more complex, 
  // I assume a pointer to a broadcast-state-machine could be included in the Clojure.

                                  
}
