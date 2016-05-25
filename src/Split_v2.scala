import org.apache.spark.rdd.RDD

/**
 * Created by Michael on 4/28/16.
 */
class Split_v2 extends userSplit_v2[(String, String)]{
  def usrSplit(inputList: RDD[(String, String)], splitTimes: Int, count: Double): Array[RDD[(String, String)]] = {
    val weights = Array.ofDim[Double](splitTimes)
    for (i <- 0 until splitTimes) {
      weights(i) = 1.0 / splitTimes.toDouble
    }
    val rddList = split(weights, inputList, count)
    rddList
  }

  def usrSplit(inputList: Array[(String, String)], splitTimes: Int) :  List[Array[(String, String)]]= {
    val count  = inputList.length
    val w = Array.ofDim[Double](splitTimes)
    for (i <- 0 until splitTimes) {
      w(i) = 1.0 / splitTimes.toDouble
    }
    val zipped = inputList.zipWithIndex
    val sum = w.reduce(_ + _)
    val sumweights = w.map(_ / sum).scanLeft(0.0d)(_ + _)
    val rddlist = sumweights.sliding(2).map { x =>
      zipped.filter { y =>
        val in = y._2.toDouble / count
        x(0) <= in && in < x(1)
      }.map(x => x._1)
    }
    rddlist.toList
  }

  def split(w: Array[Double], rdd: RDD[(String, String)], count: Double) = {
    val zipped = rdd.zipWithIndex()
    val sum = w.reduce(_ + _)
    val sumweights = w.map(_ / sum).scanLeft(0.0d)(_ + _)
    val rddlist = sumweights.sliding(2).map { x =>
      zipped.filter { y =>
        val in = y._2.toDouble / count
        x(0) <= in && in < x(1)
      }.map(x => x._1)
    }
    rddlist.toArray
  }

}
