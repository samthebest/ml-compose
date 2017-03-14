

trait MapShuffle[M[_, _] {
  // P is a type for a deserialised parition, e.g. Iterator[String]
  // f takes (serialised raw data, deserialised objects, serialised raw closure, deserialised closure)
  // We optionally return the closure to allow down downstream processes to re-use it

  // Note each param in and out of `f` can be "optional" (i.e. null / empty)
  // Note f is entirely responsible for serialisation and keeping track of what is and isn't serialised
  def mapPartitions[PIn, POut, C](f: (Iterator[Byte], PIn, Iterator[Byte], C) => (Iterator[Byte], POut, Iterator[Byte], C),
                                  closure: Iterator[Byte],
                                  m: M[PIn, C],
                                  settings: Settings): M[POut, C]


  
  
  // If retainClosure = true, and M re-uses processes, then the Closure is preserved, and so the returned M if chained with
  // another operation can re-use the Closure
  def shuffle[Pin, C](f: (Iterator[Byte], Pin, Iterator[Byte], C) => Iterator[(Long, Iterator[Byte])],
                      closure: Iterator[Byte],
                      retainClosure: Boolean,
                      m: M[Pin, C],
                      settings: Settings): M[_, C]
  
  
  def read[P](reader: Reader[P]): M[P, _]
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
  
  
                                  
                                  
}
