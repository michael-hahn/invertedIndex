import org.apache.spark.rdd.RDD

/**
 * Created by Michael on 4/28/16.
 */

trait userSplit_v2[T] {
  def usrSplit(inputList: RDD[T], splitTimes: Int, count: Double): Array[RDD[T]]
  def usrSplit(inputList: Array[T], splitTimes: Int): List[Array[T]]

}
