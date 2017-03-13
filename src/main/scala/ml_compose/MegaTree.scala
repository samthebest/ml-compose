
trait MegaTreeOps {

  def megaMapPartitions[ClojureContext: Serialiser](
      mappers: List[(Iterator[Byte], ClojureContext) => Iterator[Byte]])(
      keyers: List[Iterator[Byte] => Long])(
      megaTree: MegaTree)(
      implicit context: ClosureContext): MegaTree

// will need a ton of settings regarding partition sizes

}
