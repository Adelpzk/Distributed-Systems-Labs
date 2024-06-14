import org.apache.spark.{SparkContext, SparkConf}

// please don't change the object name
object Task2 {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Task 2")
    val sc = new SparkContext(conf)

    val textFile = sc.textFile(args(0))

    val accum = sc.longAccumulator("Number of Ratings Accumulator")

    textFile.flatmap(line => line.split(",")).foreach(rating => if (rating.nonEmpty) accum.add(1))

    // modify this code
    sc.parallelize(Seq(accum.value.toString)).saveAsTextFile(args[1])
  }
}
