package ml_compose

trait PartitionedKeyedMultisetOps[M[_, _]] {
  // For single JVM based implementations, like `Vector` or `List` this can just ignore the serialiser args
  // For RDD we can use the Serialisers to produce WrappedArray, which will be
  // pretty close to Dataset / Dataframe impl assuming the Serialisers provided are good.  Perhaps some day we will
  // want to go lower level.
  def groupByKey[K: Serialiser, V: Serialiser](partitionedKeyedMultiset: M[K, V]): M[K, Iterable[V]]

  // etc, Can do most things with `combineByKey`
}


trait PartitionedMultisetOps[M[_]] {
  // We pass the actual multiset thing (so an RDD or a Vector, or whatever) then we can extend this trait as an `object`
  // So proper FP not smelly OOP.
  // Observe we don't need Serialisers for T & U here, this is vastly better than Dataset already :)
  def map[T, U, ClosureContext: Serialiser](f: (T, ClosureContext) => U)
                                           (partitionedMultiset: M[T])
                                           (implicit context: ClosureContext): M[U]

  // ....  so idea with the "Context" is when we call RDD.map we wrap `f` in a `spore`, which will give us a compile
  // time error if f calls anything outside of `Context`, then we must ensure `Context` has an explicit Serialiser.
  // We always broadcast Context (which currently spark already does, so no additional overhead here).

  // This design will eleviate the vast majority of "Memory leaks", and inefficiencies in algorithms due to unnecessarily
  // serialising the entire Closure.  It will also turn all TSNE into compile errors and encourage us (and users/extenders)
  // to use explicit serialisation for optimisation.

  // Such an approach may seem overkill, but a lot of Machine Learning algorithms create large hashtables and the like.

  def cache[T: Serialiser](partitionedMultiset: M[T]): M[T]
  // etc


  def autoGenerateOptimisedSerialiser[T](data: M[T]): Serialiser[T]

  // It's assumed that M will store it's own MContext (just like RDD stores a pointer to sparkContext)
  // Furthermore it should store information regarding how distributed it wants to be.
}


// We ought to be able to use Shapeless or Macros to generate gratis ones.
// Or have Serialiser[Any] and use java object serialisation (which is what RDDs do)
trait Serialiser[T] {
  def serialiseOne(t: T): Array[Byte]
  def deserialiseOne(a: Array[Byte]): T

  // override in order to provide more efficient implementations.  Will allow for tricks like columnar compression.
  // default impl: usual trick of pick 1 byte to be delim, and escape that delim
  def serialiseMany(t: Array[T]): Array[Byte] = ???
  def deserialiseMany(a: Array[Byte]): Array[T] = ???

  // etc ... more may be necessary for optimisations, e.g. `def countSerialisedMany(a: Array[Byte]): Long`
  // this way we can optimise code to not desrialise for operations like `count`
  // e.g. `def headFromSerialisedMany(a: Array[Byte]): T`, `def lazyDeserialiseMany(a: Array[Byte]): Iterator[T]`, etc

  // This type-class is another fundemental deviation from Datasets ... where Datasets aims to solve the serialisation
  // bottleneck by restricting the user to a god awful API and "encapsulating" all the details of serialisation away
  // from the user, this ad-hoc polymorphic approach would mean users have a slider of how much involvement they have
  // with serialisation and no loss of generality in the API.

  // Some users may not want to worry about serialisation, while other may want to pack in domain knowledge.
  // E.g. some may know their Strings can be indexed with Bytes or their Maps are small (so should be Array[(K, V)])

  // Finally, yet another cool thing about this approach, is that via some AOP magic switch we can include
  // benchmarking stats - currently not really possible in Spark.  In Spark it's difficult to know how much
  // TIME is spent serialising (during shuffles, caching, etc).  In this design we CAN know!!

  // Override for more complex stateful serialisers
  def serialiseSelf: Array[Byte] = this.getClass().getCanonicalName().toCharArray.map(_.toByte)
  def deserialiseSelf: this.type = ??? // some sort of ugly reflection magic
}

object OurLibrary {
  // Something like, this is just an example of using PartitionedMultisetOps, not meant to be real API
  def decisionTree[M[_] : PartitionedMultisetOps](data: M[Array[Int]]) = ???

  // So user will call with `decisionTree(data: RDD[Array[Int]])` then the type-class can determine what execution framework to use

  // We can have optimised PartitionedMultisetOps for different data types. Perhaps some day even switching between
  // frameworks (Spark, Flink, etc) in a single App. Perhaps some day even using special caching mechanisms (in memory managed cloud DBs)
}


