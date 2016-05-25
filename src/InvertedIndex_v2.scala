/**
 * Created by Michael on 4/5/16.
 */

import java.io.{PrintWriter, File}
import java.lang.Exception
import java.util.logging._
import org.apache.spark.SparkContext._
import org.apache.spark.{rdd, SparkConf, SparkContext}
import org.apache.spark.api.java.JavaPairRDD
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.api.java.function.{FlatMapFunction, Function2, PairFunction}
import org.apache.spark.rdd.{PairRDDFunctions, RDD}
import scala.Tuple2
import java.util.Calendar
import java.util.StringTokenizer

import scala.collection.mutable.MutableList
import scala.io.Source
import scala.reflect.ClassTag

//remove if not needed
import scala.collection.JavaConversions._

import scala.util.control.Breaks._
import org.apache.spark.lineage.LineageContext
import org.apache.spark.lineage.LineageContext._
import scala.sys.process._

object InvertedIndex_v2 {
  private val exhaustive = 0

  def main(args: Array[String]): Unit = {
    try {
      //set up logging
      val lm: LogManager = LogManager.getLogManager
      val logger: Logger = Logger.getLogger(getClass.getName)
      val fh: FileHandler = new FileHandler("myLog")
      fh.setFormatter(new SimpleFormatter)
      lm.addLogger(logger)
      logger.setLevel(Level.INFO)
      logger.addHandler(fh)

      //set up spark configuration
      val sparkConf = new SparkConf().setMaster("local[8]")
      sparkConf.setAppName("InvertedIndex_LineageDD")
        .set("spark.executor.memory", "2g")


      //set up lineage
      var lineage = true
      var logFile = "hdfs://scai01.cs.ucla.edu:9000/clash/data/"
      if (args.size < 2) {
        logFile = "test_log"
        lineage = true
      } else {
        lineage = args(0).toBoolean
        logFile += args(1)
        sparkConf.setMaster("spark://SCAI01.CS.UCLA.EDU:7077")
      }
      //

      //set up spark context
      val ctx = new SparkContext(sparkConf)


      //set up lineage context
      val lc = new LineageContext(ctx)
      lc.setCaptureLineage(lineage)
      //



      //Prepare for Hadoop MapReduce - for correctness test only
      /*
      val clw = new commandLineOperations()
      clw.commandLineWorks()
      //Run Hadoop to have a groundTruth
      Seq("hadoop", "jar", "/Users/Michael/Documents/UCLA Senior/F15/Research-Fall2015/benchmark/examples/InvertedIndex.jar", "org.apache.hadoop.examples.InvertedIndex", "-m", "3", "-r", "1", "/Users/Michael/IdeaProjects/textFile2s_Mod", "output").!!
      */

      //start recording time for lineage
//      val LineageStartTimestamp = new java.sql.Timestamp(Calendar.getInstance.getTime.getTime)
      val LineageStartTime = System.nanoTime()
//      logger.log(Level.INFO, "Record Lineage time starts at " + LineageStartTimestamp)

      val lines = lc.textFile("/Users/Michael/IdeaProjects/textFile2s.data", 1)

      val wordDoc = lines.flatMap(s => {
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
        .groupByKey()
//        .map(pair => {
//          var value = new String("")
//          val itr = pair._2.toIterator
//          while (itr.hasNext) {
//            value += itr.next()
//            value += ","
//          }
//          value = value.substring(0, value.length)
//          if (value.contains("1341876933:898885"))
//            value += "*"
//          (pair._1, value)
//        })

      val output = wordDoc.collectWithId()

      lc.setCaptureLineage(false)
      Thread.sleep(1000)

//      val pw = new PrintWriter(new File("/Users/Michael/IdeaProjects/InvertedIndex/lineageResult"))


      //      print out the result for debugging purposes
//      for (o <- output) {
//        println(o._1._1 + ": " + o._1._2 + " - " + o._2)
//      }


      //generate the list of bad output lineage ID
      var list = List[Long]()
      for (o <- output) {
        if (o._1._1.length > 35) {
//                    println(o._1._1 + ": " + o._1._2 + " - " + o._2)
          list = o._2 :: list
        }
      }

            //      println("************************************")
            //      for (l <- list) {
            //        println(l)
            //      }
            //      println("************************************")

      var linRdd = wordDoc.getLineage()
      linRdd.collect

      linRdd = linRdd.filter{l => list.contains(l)}
      linRdd = linRdd.goBackAll()
            //At this stage, technically lineage has already find all the faulty data set, we record the time
//            val lineageEndTime = System.nanoTime()
//            val lineageEndTimestamp = new java.sql.Timestamp(Calendar.getInstance.getTime.getTime)
//            logger.log(Level.INFO, "Lineage takes " + (lineageEndTime - LineageStartTime)/1000 + " microseconds")
//            logger.log(Level.INFO, "Lineage ends at " + lineageEndTimestamp)


            //      linRdd.show.collect().foreach(s => {
            //        pw.append(s.toString)
            //        pw.append('\n')
            //      })
            //
            //      pw.close()



      val showMeRdd = linRdd.show()

      val mappedRDD = showMeRdd.map(s => {
        val colonIndex = s.lastIndexOf(":")
        val docName = s.substring(0, colonIndex)
        val content = s.substring(colonIndex + 1)
        (docName, content)
      })

//            println("MappedRDD has " + mappedRDD.count() + " records")



            //      //val lineageResult = ctx.textFile("/Users/Michael/IdeaProjects/InvertedIndex/lineageResult", 1)
            //      val lineageResult = ctx.textFile("/Users/Michael/IdeaProjects/textFile2s_Mod", 1)
            //
            //      val num = lineageResult.count()
            //      logger.log(Level.INFO, "Lineage caught " + num + " records to run delta-debugging")


            //Remove output before delta-debugging
//            val outputFile = new File("/Users/Michael/IdeaProjects/InvertedIndex/output")
//            if (outputFile.isDirectory) {
//              for (list <- Option(outputFile.listFiles()); child <- list) child.delete()
//            }
//            outputFile.delete

//            val DeltaDebuggingStartTimestamp = new java.sql.Timestamp(Calendar.getInstance.getTime.getTime)
//            val DeltaDebuggingStartTime = System.nanoTime()
//            logger.log(Level.INFO, "Record DeltaDebugging (unadjusted) time starts at " + DeltaDebuggingStartTimestamp)

            /** **************
              * **********
              */
            //      lineageResult.cache()

            //      if (exhaustive == 1) {
            //        val delta_debug: DD[String] = new DD[String]
            //        delta_debug.ddgen(lineageResult, new Test,
            //          new Split, lm, fh)
            //      } else {
      //val delta_debug = new DD_NonEx_v2[(String, String)]
      val delta_debug = new DD[(String, String)]
      val returnedRDD = delta_debug.ddgen(mappedRDD, new Test, new Split_v2, lm, fh)
            //      }
//      val ss = returnedRDD.collect()
//      ss.foreach(println)
//            linRdd = wordDoc.getLineage()
//            linRdd.collect
//            linRdd = linRdd.goBack().goBack().filter(l => {
//              //        println("*** => " + l)
//              if(l.asInstanceOf[((Int, Int), (Int, Int))]._2._2 == ss(0)._2.toInt){
//                println("*** => " + l)
//                true
//              }else false
//            })
//
//            linRdd = linRdd.goBackAll()
//            linRdd.collect()
//            linRdd.show()
//
//
//            val DeltaDebuggingEndTime = System.nanoTime()
//            val DeltaDebuggingEndTimestamp = new java.sql.Timestamp(Calendar.getInstance.getTime.getTime)
//            logger.log(Level.INFO, "DeltaDebugging (unadjusted) ends at " + DeltaDebuggingEndTimestamp)
//            logger.log(Level.INFO, "DeltaDebugging (unadjusted) takes " + (DeltaDebuggingEndTime - DeltaDebuggingStartTime)/1000 + " milliseconds")

            //total time
//            logger.log(Level.INFO, "Record total time: Delta-Debugging + Linegae + goNext:" + (DeltaDebuggingEndTime - LineageStartTime)/1000 + " microseconds")

      val endTime = System.nanoTime()
      logger.log(Level.INFO, "Record total time: Delta-Debugging + Linegae + goNext:" + (endTime - LineageStartTime)/1000 + " microseconds")

      println("Job's DONE! WORKS!")
      ctx.stop()
    }

  }
}
