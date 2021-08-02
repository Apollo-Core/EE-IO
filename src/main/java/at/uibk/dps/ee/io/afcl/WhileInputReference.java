package at.uibk.dps.ee.io.afcl;

/**
 * Class to keep a reference for a given atomic function.
 * 
 * @author Fedor Smirnov
 */
public class WhileInputReference {

  protected final String firstIterationInput;
  protected final String laterIterationsInput;

  /**
   * Standard constructor.
   * 
   * @param firstIterationInput the reference to the input which is used during
   *        the first iteration of the while
   * @param laterIterationsInput the reference to the input which is used during
   *        later iterations of the while
   */
  public WhileInputReference(String firstIterationInput, String laterIterationsInput) {
    super();
    this.firstIterationInput = firstIterationInput;
    this.laterIterationsInput = laterIterationsInput;
  }

  public String getFirstIterationInput() {
    return firstIterationInput;
  }

  public String getLaterIterationsInput() {
    return laterIterationsInput;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((firstIterationInput == null) ? 0 : firstIterationInput.hashCode());
    result =
        prime * result + ((laterIterationsInput == null) ? 0 : laterIterationsInput.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    WhileInputReference other = (WhileInputReference) obj;
    if (firstIterationInput == null) {
      if (other.firstIterationInput != null)
        return false;
    } else if (!firstIterationInput.equals(other.firstIterationInput))
      return false;
    if (laterIterationsInput == null) {
      if (other.laterIterationsInput != null)
        return false;
    } else if (!laterIterationsInput.equals(other.laterIterationsInput))
      return false;
    return true;
  }
}
