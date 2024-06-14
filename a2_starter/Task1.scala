import org.apache.spark.{SparkContext, SparkConf}

// please don't change the object name
object Task1 {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Task 1")
    val sc = new SparkContext(conf)

    val textFile = sc.textFile(args(0))

    // modify this code
    val output = textFile.map { line =>
      val elems = line.split(",")
      val movie = elems(0)
      val ratings = elems.drop(1)

      val maxRating = ratings.max

      val users = ratings.zipWithIndex
        .filter { case (value, index) => value == maxRating }
        .map { case (value, index) => (index + 1).toString }

      movie + "," + users.mkString(",")
    }
    
    output.saveAsTextFile(args(1))

    sc.stop()
  }
}
