import org.apache.spark.{SparkContext, SparkConf}

// please don't change the object name
object Task4 {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Task 4")
    val sc = new SparkContext(conf)

    val textFile = sc.textFile(args(0))

    val movieRatings = textFile.map(line => {
      val elems = line.split(",")
      (elems(0), elems.tail.map(rating => if (rating == "") None else Some(rating.toInt))) //makes a list of none values and integers
    })

    // Create all possible pairs of movies
    val allMoviePairs = movieRatings.cartesian(movieRatings)
      .filter { case ((movie1, _), (movie2, _)) => movie1 < movie2 }  //makes sure each pair is unique


    val similarities = allMoviePairs.map { case ((movie1, ratings1), (movie2, ratings2)) =>
      val similarity = ratings1.zip(ratings2).count {
        case (Some(r1), Some(r2)) if r1 == r2 => true
        case _ => false
      }
      movie1 + "," + movie2 + "," + similarity //returns string
    }

    // Saves the output
    similarities.saveAsTextFile(args(1))
  }
}
