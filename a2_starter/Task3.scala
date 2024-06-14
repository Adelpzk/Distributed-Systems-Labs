import org.apache.spark.{SparkContext, SparkConf}

// please don't change the object name
object Task3 {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Task 3")
    val sc = new SparkContext(conf)

    val textFile = sc.textFile(args(0))

    val counts = textFile.flatMap(line => line.split(",", -1))
      .drop(1)
      .zipWithIndex
      .filter(!_._1.isEmpty)
      .map(_, user => (user, 1))
      .reduceByKey(_ + _)
      .map(x => x._1 + "," + x._2)
      .saveAsTextFile(args(1))
  }
}
