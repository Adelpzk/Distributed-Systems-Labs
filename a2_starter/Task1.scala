import org.apache.spark.{SparkContext, SparkConf}

// please don't change the object name
object Task1 {
  def getUserWithHighestRating(line: String): (String) = {
    val elems = line.split(",")
    val movie = elems(0)
    val ratings = elems.drop(1)

    var highestRating = Int.MinValue
    var usersWithHighestRating = ArrayBuffer[Int]()

    for (i <- ratings.indices) {
      val rating = ratings(i).toInt
      if (rating > highestRating) {
        highestRating = rating
        usersWithHighestRating.clear()
        usersWithHighestRating += (i + 1)
      } else if (rating == highestRating) {
        usersWithHighestRating += (i + 1)
      }
    }

    return movie + "," + usersWithHighestRating.mkString(",")
  }

  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Task 1")
    val sc = new SparkContext(conf)

    val textFile = sc.textFile(args(0))

    // modify this code
    val output = textFile.map(x => x);
    
    output.saveAsTextFile(args(1))
  }
}
