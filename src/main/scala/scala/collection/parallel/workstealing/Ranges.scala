package scala.collection.parallel
package workstealing



import scala.language.experimental.macros
import scala.reflect.macros._
import scala.collection.parallel.generic._



object Ranges {

  trait Scope {
    implicit def rangeOps(r: Par[Range]) = new Ranges.Ops(r.seq)
    implicit def canMergeRange[T]: CanMergeFrom[Range, Int, Par[Conc[T]]] = ???
    implicit def rangeIsZippable = new IsZippable[Range, Int] {
      def apply(pr: Par[Range]) = ???
    }
  }

  class Ops(val r: collection.immutable.Range) extends AnyVal with Zippables.OpsLike[Int, collection.immutable.Range] {
    def stealer: Stealer[Int] = new RangeStealer(r, 0, r.length)
    override def reduce[U >: Int](operator: (U, U) => U)(implicit ctx: WorkstealingTreeScheduler): U = macro methods.RangesMacros.reduce[U]
    override def fold[U >: Int](z: => U)(op: (U, U) => U)(implicit ctx: WorkstealingTreeScheduler): U = macro methods.RangesMacros.fold[U]
    def aggregate[S](z: S)(combop: (S, S) => S)(seqop: (S, Int) => S)(implicit ctx: WorkstealingTreeScheduler): S = macro methods.RangesMacros.aggregate[S]
    def sum[U >: Int](implicit num: Numeric[U], ctx: WorkstealingTreeScheduler): U = macro methods.RangesMacros.sum[U]
    def product[U >: Int](implicit num: Numeric[U], ctx: WorkstealingTreeScheduler): U = macro methods.RangesMacros.product[U]
    def min[U >: Int](implicit ord: Ordering[U], ctx: WorkstealingTreeScheduler): Int = macro methods.RangesMacros.min[U]
    def max[U >: Int](implicit ord: Ordering[U], ctx: WorkstealingTreeScheduler): Int = macro methods.RangesMacros.max[U]
    def find(p:Int=> Boolean)(implicit ctx:WorkstealingTreeScheduler): Option[Int] = macro methods.RangesMacros.find
    def exists(p:Int=> Boolean)(implicit ctx:WorkstealingTreeScheduler): Boolean = macro methods.RangesMacros.exists
    def count(p:Int=> Boolean)(implicit ctx:WorkstealingTreeScheduler): Int = macro methods.RangesMacros.count
    def forall(p:Int=> Boolean)(implicit ctx:WorkstealingTreeScheduler): Boolean = macro methods.RangesMacros.forall
    override def copyToArray[U >: Int](arr: Array[U], start: Int, len: Int)(implicit ctx:WorkstealingTreeScheduler): Unit = macro methods.RangesMacros.copyToArray[U]
    def copyToArray[U >: Int](arr: Array[U], start: Int)(implicit ctx:WorkstealingTreeScheduler): Unit = macro methods.RangesMacros.copyToArray2[U]
    def copyToArray[U >: Int](arr: Array[U])(implicit ctx:WorkstealingTreeScheduler): Unit = macro methods.RangesMacros.copyToArray3[U]
  }

  /* stealer implementation */

  import WorkstealingTreeScheduler.{ Kernel, Node }

  class RangeStealer(val range: collection.immutable.Range, start: Int, end: Int) extends IndexedStealer.Flat[Int](start, end) {
    type StealerType = RangeStealer

    var padding8: Int = _
    var padding9: Int = _
    var padding10: Int = _
    var padding11: Int = _
    var padding12: Int = _
    var padding13: Int = _
    var padding14: Int = _
    var padding15: Int = _

    def next(): Int = {
      val idx = nextProgress
      nextProgress += 1
      range.apply(idx)
    }

    def newStealer(s: Int, u: Int) = new RangeStealer(range, s, u)
  }

  abstract class RangeKernel[@specialized R] extends IndexedStealer.IndexedKernel[Int, R] {
    def apply(node: Node[Int, R], chunkSize: Int): R = {
      val stealer = node.stealer.asInstanceOf[RangeStealer]
      val nextProgress = stealer.nextProgress
      val nextUntil = stealer.nextUntil
      val range = stealer.range
      val from = range.apply(nextProgress)

      if (nextProgress == nextUntil) apply0(node, from)
      else {
        val to = range.apply(nextUntil - 1)
        val step = range.step

        if (step == 1) apply1(node, from, to)
        else applyN(node, from, to, step)
      }
    }
    def apply0(node: Node[Int, R], at: Int): R
    def apply1(node: Node[Int, R], from: Int, to: Int): R
    def applyN(node: Node[Int, R], from: Int, to: Int, stride: Int): R
  }

  val EMPTY_RESULT = new AnyRef

}

