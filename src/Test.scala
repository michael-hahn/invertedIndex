/**
 * Created by Michael on 11/13/15.
 */

import java.util.StringTokenizer
import java.util.logging.{Level, Logger, FileHandler, LogManager}

import org.apache.spark.api.java.JavaRDD
import org.apache.spark.SparkContext._
import org.apache.spark.lineage.rdd.ShowRDD
import org.apache.spark.rdd.{PairRDDFunctions, RDD}
import scala.collection.mutable
import scala.collection.mutable.MutableList
import scala.sys.process._
import scala.io.Source
import scala.util.control.Breaks._

import java.io.File
import java.io._

import org.apache.spark.delta.DeltaWorkflowManager



class Test extends userTest[(String, String)] with Serializable {
  var num = 0
  def usrTest(inputRDD: RDD[(String, String)], lm: LogManager, fh: FileHandler): Boolean = {
    //use the same logger as the object file
    val logger: Logger = Logger.getLogger(classOf[Test].getName)
    lm.addLogger(logger)
    logger.addHandler(fh)

    //assume that test will pass (which returns false)
    var returnValue = false

    val finalRDD = inputRDD
      .flatMap(s => {
        val wordDocList: MutableList[(String, String)] = MutableList()
        val wordList = s._2.trim.split(" ")
        for (w <- wordList) {
          wordDocList += Tuple2(w, s._1)
        }
        wordDocList.toList
      })
      .groupByKey()

    val start = System.nanoTime

    val out = finalRDD.collect()
    logger.log(Level.INFO, "TimeTest : " + (System.nanoTime() - start) / 1000)
    num = num + 1
    logger.log(Level.INFO, "TestRuns : " + num)
    println(s""">>>>>>>>>>>>>>>>>>>>>>>>>>>> Number of Runs $num <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<""")

    breakable {
      for (o <- out) {
        if (o._1.length > 35) {
          //          println(o._1._1 + ": " + o._1._2 + " - " + o._2)
          returnValue = true
          break
        }
      }
    }
    returnValue
  }

  //FOR LOCAL COMPUTATION TEST WILL ALWAYS PASS
  def usrTest(inputRDD: Array[(String,String)], lm: LogManager, fh: FileHandler): Boolean = {
    //use the same logger as the object file
    val logger: Logger = Logger.getLogger(classOf[Test].getName)
    lm.addLogger(logger)
    logger.addHandler(fh)

    //assume that test will pass (which returns false)
    var returnValue = false

    val start = System.nanoTime
    val finalRDD = inputRDD
      .flatMap(s => {
        val wordDocList: MutableList[(String, String)] = MutableList()
        val wordList = s._2.trim.split(" ")
        for (w <- wordList) {
          wordDocList += Tuple2(w, s._1)
        }
        wordDocList.toList
      })
      .groupBy(_._1)

    val out = finalRDD
    logger.log(Level.INFO, "LTimeTest : " + (System.nanoTime() - start) / 1000)
    num = num +1
    logger.log(Level.INFO, "LTestRuns : " + num)
    println(s""" >>>>>>>>>>>>>>>>>>>>>>>>>> The number of runs are $num <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<,""")

    breakable {
      for (o <- out) {
        if (o._1.length > 35) {
          //          println(o._1._1 + ": " + o._1._2 + " - " + o._2)
          returnValue = true
          break
        }
      }
    }
    returnValue

  }

}

