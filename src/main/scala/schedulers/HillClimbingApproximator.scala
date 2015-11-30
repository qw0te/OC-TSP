package schedulers

import tsp.{Instance, Solution, RandomSolutionGenerator}
import neighbourhood.{NeighbourhoodBuilder, BestImprovementSelector}
import java.io.{File, BufferedWriter, FileWriter}

/** Hill climbing approximator of the pareto front.
  *
  * @constructor create a new HillClimbingApproximator.
  * @param instance instance to use to find the pareto front
  * @param neighbourhoodBuilder neighbourhood builder to find the pareto front
  *                             approximation
  * @param weightsVectors weights vectors to approximate the pareto front
  */
class HillClimbingApproximator(
  val instance: Instance,
  val neighbourhoodBuilder: NeighbourhoodBuilder,
  val weightsVectors: Set[Vector[Double]]
) {

  // Each weight vector must have the sae dimension as the instance
  require(
    this.weightsVectors forall {
      vector => vector.length == instance.nbObjectives
    }
  )

  // Create a cost function from a weights vector
  private def createFuncWith(weights: Vector[Double]): Solution => Double =
    solution => {
      val costsAndWeight = (this.instance solutionCosts solution) zip weights

      costsAndWeight.foldLeft(0.0){
        case (acc, (cost, weight)) => acc + cost * weight
      }
    }

  /* Find the best approximation (in the pareto front) for a given solution and
   * a given cost function
   */
  private def findBestApproximationFrom(
    solution: Solution,
    costFunc: Solution => Double
  ): Solution = {
    val selector = new BestImprovementSelector(costFunc)

    def innerApproximate(current: Solution, previous: Solution): Solution =
      if (current == previous)
        current
      else {
        val nghd = this.neighbourhoodBuilder buildNeighbourhood current
        val best = selector.select(current, nghd)

        innerApproximate(best, current)
      }

    innerApproximate(solution, Nil)
  }

  /** Find a pareto front approximation with the weights vectors.
    *
    * @return set of solution which compose the pareto front approximation
    */
  def findParetoFrontApproximation: Set[Solution] =
    this.weightsVectors.foldLeft(Set[Solution]()){
      case (acc, weights) => {
        print(" [" + acc.size * 100 / this.weightsVectors.size + "%]\r")
        val randomSol = RandomSolutionGenerator generate this.instance.nbElem
        val costFunc = this.createFuncWith(weights)

        acc + this.findBestApproximationFrom(randomSol, costFunc)
      }
    }

  /** Create a file with the HillClimbingApproximator pareto front approximation. */
  def writeGnuplotFile: Unit = {
    def processSolution(sCosts: Vector[Int]): String =
      sCosts mkString "\t"

    def writeFile(filename: String, content: Set[Solution]): Unit = {
      val file = new File(filename)
      val bw = new BufferedWriter(new FileWriter(file))
      val text = content map {
        solution => processSolution(this.instance solutionCosts solution)
      }

      bw write (text mkString "\n")
      bw.close
    }

    writeFile("hillclimbing.dat", this.findParetoFrontApproximation)
  }

}
