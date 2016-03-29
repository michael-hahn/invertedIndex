import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._


import scala.collection.mutable.MutableList

/**
 * Created by Michael on 1/14/16.
 */
class sparkOperations extends Serializable {
    def sparkWorks(text: RDD[String]): RDD[(String, Iterable[String])] = {
      val wordDoc = text.flatMap(s => {
        val wordDocList: MutableList[(String, String)] = MutableList()
        val colonIndex = s.lastIndexOf(":")
        val docName = s.substring(0, colonIndex)
        val content = s.substring(colonIndex + 1)
        val wordList = content.trim.split(" ")
        for (w <- wordList) {
          wordDocList += Tuple2(w, docName)
        }
        wordDocList.toList
      })
      .groupByKey
      .sortByKey()


//      for (tuple <- wordDoc) {
//        println(tuple._1 +  " : " + tuple._2)
//      }
      wordDoc
    }
}
