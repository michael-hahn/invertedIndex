/**
 * Created by Michael on 4/28/16.
 */
import java.sql.Timestamp
import java.util.logging.{Level, Logger, FileHandler, LogManager}

import org.apache.spark.lineage.rdd.Lineage
import org.apache.spark.rdd.RDD

import org.apache.spark.api.java.JavaRDD
import java.util.{Calendar, ArrayList, List}

import scala.reflect.ClassTag


//remove if not needed
import scala.collection.JavaConversions._
import scala.util.control.Breaks._
import org.apache.spark.lineage.rdd._;

class DD_NonEx_v2 [T:ClassTag]{
  def split(inputRDD: RDD[T], numberOfPartitions: Int, splitFunc: userSplit_v2[T], count: Double): Array[RDD[T]] = {
    splitFunc.usrSplit(inputRDD, numberOfPartitions, count)
  }

  def test(inputRDD: RDD[T], testFunc: userTest[T], lm: LogManager, fh: FileHandler): Boolean = {
    testFunc.usrTest(inputRDD, lm, fh)
  }

  private def dd_helper(inputRDD: RDD[T],
                        numberOfPartitions: Int,
                        testFunc: userTest[T],
                        splitFunc: userSplit_v2[T],
                        lm: LogManager,
                        fh: FileHandler
                         ): RDD[T] = {

    val logger: Logger = Logger.getLogger(getClass.getName)
    logger.addHandler(fh)

    logger.log(Level.INFO, "Running DD_NonEx SCALA")

    var rdd = inputRDD
    var partitions = numberOfPartitions
    var runTime = 1
    var first_rdd_runTime = 0
    var not_first_rdd_runTime = 0
    var mix_match_rdd_runTime = 0
    var granularity_increase = 0
    var bar_offset = 0
    while (true) {
      val startTimeStampe = new Timestamp(Calendar.getInstance.getTime.getTime)
      val startTime = System.nanoTime

      val sizeRDD = rdd.count
      logger.log(Level.INFO, "1Runs :" + runTime)
      logger.log(Level.INFO, "1Size : " + sizeRDD)

      val assertResult = test(rdd, testFunc, lm, fh)
      runTime = runTime + 1
      first_rdd_runTime = first_rdd_runTime + 1
      if (!assertResult) {
        val endTime = System.nanoTime
        logger.log(Level.INFO, "Runs : " + runTime)
        logger.log(Level.INFO, "Time : " + (endTime - startTime) / 1000)
        logger.log(Level.INFO, "Size : " + sizeRDD)
        return null
      }

      if (sizeRDD <= 1) {
        val endTime = System.nanoTime
        logger.log(Level.INFO, "The #" + runTime + " run is done")
        logger.log(Level.INFO, "Total first RDD run: " + first_rdd_runTime)
        logger.log(Level.INFO, "Total not first RDD run: " + not_first_rdd_runTime)
        logger.log(Level.INFO, "Total mix and match RDD run: " + mix_match_rdd_runTime)
        logger.log(Level.INFO, "Granularity increase : " + granularity_increase)
        logger.log(Level.INFO, "RDD Only Holds One Line - End of This Branch of Search")
        logger.log(Level.WARNING, "Delta Debugged Error inducing inputs: ")
        rdd.collect().foreach(s=> {
          logger.log(Level.WARNING, s.toString + "\n")
        })
        logger.log(Level.INFO, "Time : " + (endTime - startTime)/1000)
        return rdd
      }

      val rddList = split(rdd, partitions, splitFunc, sizeRDD)

      var rdd_failed = false
      var rddBar_failed = false
      var next_rdd = rdd
      var next_partitions = partitions

      breakable {
        for (i <- 0 until partitions) {
          val result = test(rddList(i), testFunc,lm, fh)
          runTime = runTime + 1
          if (i == 0) {
            first_rdd_runTime = first_rdd_runTime + 1
          }
          else {
            not_first_rdd_runTime = not_first_rdd_runTime + 1
          }
          if (result) {
            rdd_failed = true
            next_rdd = rddList(i)
            next_partitions = 2
            bar_offset = 0
            break
          }
        }
      }

      if (!rdd_failed) {
        breakable{
          for (j <- 0 until partitions){
            val i = (j + bar_offset) % partitions
            val rddBar = rdd.subtract(rddList(i))
            val result = test(rddBar, testFunc, lm, fh)
            runTime = runTime + 1
            mix_match_rdd_runTime = mix_match_rdd_runTime + 1
            if (result){
              rddBar_failed = true
              //next_rdd = next_rdd.intersection(rddBar)
              next_rdd = rddBar
              next_partitions = next_partitions - 1

              bar_offset = i
              break
            }
          }
        }
      }

      if (!rdd_failed && !rddBar_failed) {
        val rddSiz = rdd.count()
        if (rddSiz <= 2) {
          val endTime = System.nanoTime()
          logger.log(Level.INFO, "Run : " + runTime)
          logger.log(Level.INFO, "First RDD Run : " + first_rdd_runTime)
          logger.log(Level.INFO, "Not First RDD Run : " + not_first_rdd_runTime)
          logger.log(Level.INFO, "Mix and Match Run : " + mix_match_rdd_runTime)
          logger.log(Level.INFO, "Granularity increase : " + granularity_increase)
          logger.log(Level.INFO, "End of This Branch of Search")
          logger.log(Level.INFO, "Size : " + sizeRDD)
          logger.log(Level.WARNING, "Delta Debugged Error inducing inputs: ")
          rdd.collect().foreach(s=> {
            logger.log(Level.WARNING, s.toString + "\n")
          })
          logger.log(Level.INFO, "Time : " + (endTime - startTime)/1000)
          return rdd
        }
        next_partitions = Math.min(rddSiz.asInstanceOf[Int], partitions * 2)
      }
      val endTime = System.nanoTime()
      //      logger.log(Level.INFO, "Finish the " + runTime + "th run of Non-exhaustive DD, taking " + (endTime - startTime) / 1000 + " microseconds")
      //      logger.log(Level.INFO, "This data size is " + sizeRDD)

      rdd = next_rdd
      partitions = next_partitions
      //      logger.log(Level.INFO, "The next partition is " + partitions)
      //      runTime = runTime + 1
    }
    null
  }

  def ddgen(inputRDD: RDD[T], testFunc: userTest[T], splitFunc: userSplit_v2[T],lm: LogManager, fh: FileHandler): RDD[T] = {
    dd_helper(inputRDD, 2, testFunc, splitFunc, lm, fh)
  }

}
